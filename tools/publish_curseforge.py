"""Uploads the built jars to CurseForge.

Requirements:
  * The CurseForge project already exists (it is created by hand in the author
    account, the API can't create projects) and its project ID is set in
    PROJECT_ID below or passed with --project-id.
  * An API token from the CurseForge account is in the environment variable
    CURSEFORGE_TOKEN.
  * The jars have been built beforehand:
    ./gradlew :1.21.11:buildAndCollect :26.1.x:buildAndCollect :26.2.x:buildAndCollect :26.3.x:buildAndCollect

Which Minecraft versions a jar covers isn't defined here but in
stonecutter.properties.toml (mod.mc_releases) - the same source fabric.mod.json
gets its value from. The numeric version IDs CurseForge expects are resolved at
runtime via the API.

API documentation:
https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api

Usage:
  python tools/publish_curseforge.py --dry-run
  python tools/publish_curseforge.py
"""

import argparse
import json
import mimetypes
import os
import sys
import tomllib
import urllib.error
import urllib.request
import uuid
from pathlib import Path

# Shown on the project page in the CurseForge author account. Until the project
# exists, the ID has to be passed with --project-id.
PROJECT_ID = None

BASE_URL = "https://minecraft.curseforge.com"
MODLOADER = "Fabric"

ROOT = Path(__file__).resolve().parent.parent
PROPERTIES = ROOT / "stonecutter.properties.toml"
CHANGELOG = ROOT / "branding/curseforge-changelog.md"


def die(message):
    sys.exit("Aborted: %s" % message)


def load_properties():
    with open(PROPERTIES, "rb") as handle:
        return tomllib.load(handle)


def java_version_for(release):
    """1.21.11 runs on Java 21, from 26.1 on Minecraft requires Java 25."""
    return 21 if release.startswith("1.") else 25


def collect_jars(properties):
    """Maps each built jar to the Minecraft versions of its node."""
    version = properties["mod"]["version"]
    libs = ROOT / "build/libs" / version
    if not libs.is_dir():
        die("%s is missing - build first (see the top of this file)." % libs)

    # Nodes from the properties file: everything that defines mod.mc_releases
    nodes = {
        name: table["mod"]["mc_releases"]
        for name, table in properties.items()
        if isinstance(table, dict) and "mod" in table and "mc_releases" in table["mod"]
    }

    jars = []
    for jar in sorted(libs.glob("*.jar")):
        # File name: <mod-id>-<mod-version>+<built Minecraft version>.jar
        built_for = jar.stem.split("+", 1)[-1]
        matches = [releases for releases in nodes.values() if built_for in releases]
        if len(matches) != 1:
            die("%s can't be matched to a node in %s." % (jar.name, PROPERTIES.name))
        jars.append((jar, matches[0]))

    if len(jars) != len(nodes):
        die("Found %d jars in %s, expected %d - please build all nodes."
            % (len(jars), libs, len(nodes)))
    return jars


def api_get(path, token):
    request = urllib.request.Request(BASE_URL + path, headers={"X-Api-Token": token})
    try:
        with urllib.request.urlopen(request) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        die("GET %s -> HTTP %s: %s" % (path, error.code, error.read().decode("utf-8", "replace")))


def build_version_index(token):
    """name/slug -> ID. CurseForge keeps game versions, mod loaders and Java in
    the same list, so all three end up in the same index."""
    index = {}
    for entry in api_get("/api/game/versions", token):
        index.setdefault(entry["name"], entry["id"])
        index.setdefault(entry.get("slug", ""), entry["id"])
    index.pop("", None)
    return index


def resolve(index, names, optional=()):
    """Converts names to IDs. If a required name is missing, the script aborts."""
    ids = []
    for name in names:
        if name in index:
            ids.append(index[name])
        elif name in optional:
            print("    Note: CurseForge doesn't know '%s', skipping it." % name)
        else:
            die("CurseForge doesn't know the version '%s'. Available include: %s"
                % (name, ", ".join(sorted(index)[:25])))
    return ids


def encode_multipart(fields, file_path):
    """multipart/form-data by hand - the upload needs exactly the fields
    'metadata' and 'file'."""
    boundary = uuid.uuid4().hex
    body = bytearray()
    for name, value in fields.items():
        body += ('--%s\r\nContent-Disposition: form-data; name="%s"\r\n\r\n%s\r\n'
                 % (boundary, name, value)).encode("utf-8")
    mime = mimetypes.guess_type(file_path.name)[0] or "application/java-archive"
    body += ('--%s\r\nContent-Disposition: form-data; name="file"; filename="%s"\r\n'
             'Content-Type: %s\r\n\r\n' % (boundary, file_path.name, mime)).encode("utf-8")
    body += file_path.read_bytes()
    body += ("\r\n--%s--\r\n" % boundary).encode("utf-8")
    return bytes(body), "multipart/form-data; boundary=%s" % boundary


def upload(project_id, token, jar, metadata):
    body, content_type = encode_multipart({"metadata": json.dumps(metadata)}, jar)
    request = urllib.request.Request(
        "%s/api/projects/%s/upload-file" % (BASE_URL, project_id),
        data=body,
        headers={"X-Api-Token": token, "Content-Type": content_type},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        die("Upload of %s -> HTTP %s: %s"
            % (jar.name, error.code, error.read().decode("utf-8", "replace")))


def main():
    parser = argparse.ArgumentParser(description="Uploads the built jars to CurseForge.")
    parser.add_argument("--project-id", default=PROJECT_ID,
                        help="project ID from the CurseForge project page")
    parser.add_argument("--release-type", default="release",
                        choices=["release", "beta", "alpha"])
    parser.add_argument("--changelog", type=Path, default=CHANGELOG)
    parser.add_argument("--dry-run", action="store_true",
                        help="only show what would be uploaded")
    args = parser.parse_args()

    token = os.environ.get("CURSEFORGE_TOKEN")
    if not token:
        die("CURSEFORGE_TOKEN is not set.")
    if not args.project_id:
        die("No project ID. Pass it with --project-id or set PROJECT_ID at the top.")
    if not args.changelog.is_file():
        die("Changelog %s is missing." % args.changelog)

    properties = load_properties()
    mod_name = properties["mod"]["name"]
    mod_version = properties["mod"]["version"]
    changelog = args.changelog.read_text(encoding="utf-8")
    jars = collect_jars(properties)

    print("%s %s -> project %s (%s)" % (mod_name, mod_version, args.project_id, args.release_type))
    index = build_version_index(token)

    for jar, releases in jars:
        java = "Java %d" % java_version_for(releases[0])
        # CurseForge lists mod loader and Java as separate "game versions".
        # Java isn't maintained for every game, so it's optional.
        wanted = list(releases) + [MODLOADER, java]
        print("\n  %s" % jar.name)
        print("    Versions: %s" % ", ".join(wanted))
        version_ids = resolve(index, wanted, optional=(java,))

        metadata = {
            "changelog": changelog,
            "changelogType": "markdown",
            "displayName": "%s %s (Minecraft %s)" % (mod_name, mod_version, ", ".join(releases)),
            "gameVersions": version_ids,
            "releaseType": args.release_type,
        }
        if args.dry_run:
            print("    Dry run, no upload.")
            continue
        result = upload(args.project_id, token, jar, metadata)
        print("    uploaded, file ID %s" % result.get("id"))

    if args.dry_run:
        print("\nDry run finished, nothing was uploaded.")
    else:
        print("\nDone. New files go through CurseForge review first.")


if __name__ == "__main__":
    main()
