"""Laedt die gebauten Jars auf CurseForge hoch.

Voraussetzungen:
  * Das CurseForge-Projekt existiert bereits (es wird von Hand im Autoren-Konto
    angelegt, die API kann keine Projekte erstellen) und seine Projekt-ID steht
    unten in PROJECT_ID oder wird mit --project-id uebergeben.
  * Ein API-Token aus dem CurseForge-Konto steht in der Umgebungsvariablen
    CURSEFORGE_TOKEN.
  * Die Jars wurden vorher gebaut:
    ./gradlew :1.21.11:buildAndCollect :26.1.x:buildAndCollect :26.2.x:buildAndCollect

Welche Minecraft-Versionen ein Jar abdeckt, steht nicht hier, sondern in
stonecutter.properties.toml (mod.mc_releases) - dieselbe Quelle, aus der auch die
fabric.mod.json ihre Angabe bekommt. Die numerischen Versions-IDs, die CurseForge
erwartet, werden zur Laufzeit ueber die API aufgeloest.

API-Beschreibung:
https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api

Aufruf:
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

# Steht auf der Projektseite im CurseForge-Autoren-Konto. Bis das Projekt
# angelegt ist, muss die ID mit --project-id uebergeben werden.
PROJECT_ID = None

BASE_URL = "https://minecraft.curseforge.com"
MODLOADER = "Fabric"

ROOT = Path(__file__).resolve().parent.parent
PROPERTIES = ROOT / "stonecutter.properties.toml"
CHANGELOG = ROOT / "branding/curseforge-changelog.md"


def die(message):
    sys.exit("Abbruch: %s" % message)


def load_properties():
    with open(PROPERTIES, "rb") as handle:
        return tomllib.load(handle)


def java_version_for(release):
    """1.21.11 laeuft auf Java 21, ab 26.1 verlangt Minecraft Java 25."""
    return 21 if release.startswith("1.") else 25


def collect_jars(properties):
    """Ordnet jedem gebauten Jar die Minecraft-Versionen seines Knotens zu."""
    version = properties["mod"]["version"]
    libs = ROOT / "build/libs" / version
    if not libs.is_dir():
        die("%s fehlt - zuerst bauen (siehe Kopf dieser Datei)." % libs)

    # Knoten aus der Properties-Datei: alles, was mod.mc_releases definiert
    nodes = {
        name: table["mod"]["mc_releases"]
        for name, table in properties.items()
        if isinstance(table, dict) and "mod" in table and "mc_releases" in table["mod"]
    }

    jars = []
    for jar in sorted(libs.glob("*.jar")):
        # Dateiname: <mod-id>-<mod-version>+<gebaute Minecraft-Version>.jar
        built_for = jar.stem.split("+", 1)[-1]
        matches = [releases for releases in nodes.values() if built_for in releases]
        if len(matches) != 1:
            die("%s laesst sich keinem Knoten in %s zuordnen." % (jar.name, PROPERTIES.name))
        jars.append((jar, matches[0]))

    if len(jars) != len(nodes):
        die("Es liegen %d Jars in %s, erwartet werden %d - bitte alle Knoten bauen."
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
    """name/slug -> ID. CurseForge fuehrt Spielversionen, Modloader und Java in
    derselben Liste, deshalb landen alle drei im selben Index."""
    index = {}
    for entry in api_get("/api/game/versions", token):
        index.setdefault(entry["name"], entry["id"])
        index.setdefault(entry.get("slug", ""), entry["id"])
    index.pop("", None)
    return index


def resolve(index, names, optional=()):
    """Setzt Namen in IDs um. Fehlt ein Pflichtname, bricht das Skript ab."""
    ids = []
    for name in names:
        if name in index:
            ids.append(index[name])
        elif name in optional:
            print("    Hinweis: '%s' kennt CurseForge nicht, wird weggelassen." % name)
        else:
            die("CurseForge kennt die Version '%s' nicht. Vorhanden sind unter anderem: %s"
                % (name, ", ".join(sorted(index)[:25])))
    return ids


def encode_multipart(fields, file_path):
    """multipart/form-data von Hand - der Upload braucht genau die Felder
    'metadata' und 'file'."""
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
        die("Upload von %s -> HTTP %s: %s"
            % (jar.name, error.code, error.read().decode("utf-8", "replace")))


def main():
    parser = argparse.ArgumentParser(description="Laedt die gebauten Jars auf CurseForge hoch.")
    parser.add_argument("--project-id", default=PROJECT_ID,
                        help="Projekt-ID von der CurseForge-Projektseite")
    parser.add_argument("--release-type", default="release",
                        choices=["release", "beta", "alpha"])
    parser.add_argument("--changelog", type=Path, default=CHANGELOG)
    parser.add_argument("--dry-run", action="store_true",
                        help="nur anzeigen, was hochgeladen wuerde")
    args = parser.parse_args()

    token = os.environ.get("CURSEFORGE_TOKEN")
    if not token:
        die("CURSEFORGE_TOKEN ist nicht gesetzt.")
    if not args.project_id:
        die("Keine Projekt-ID. Mit --project-id uebergeben oder oben in PROJECT_ID eintragen.")
    if not args.changelog.is_file():
        die("Changelog %s fehlt." % args.changelog)

    properties = load_properties()
    mod_name = properties["mod"]["name"]
    mod_version = properties["mod"]["version"]
    changelog = args.changelog.read_text(encoding="utf-8")
    jars = collect_jars(properties)

    print("%s %s -> Projekt %s (%s)" % (mod_name, mod_version, args.project_id, args.release_type))
    index = build_version_index(token)

    for jar, releases in jars:
        java = "Java %d" % java_version_for(releases[0])
        # Modloader und Java fuehrt CurseForge als eigene "Spielversionen".
        # Java ist nicht bei jedem Spiel gepflegt, deshalb optional.
        wanted = list(releases) + [MODLOADER, java]
        print("\n  %s" % jar.name)
        print("    Versionen: %s" % ", ".join(wanted))
        version_ids = resolve(index, wanted, optional=(java,))

        metadata = {
            "changelog": changelog,
            "changelogType": "markdown",
            "displayName": "%s %s (Minecraft %s)" % (mod_name, mod_version, ", ".join(releases)),
            "gameVersions": version_ids,
            "releaseType": args.release_type,
        }
        if args.dry_run:
            print("    Probelauf, kein Upload.")
            continue
        result = upload(args.project_id, token, jar, metadata)
        print("    hochgeladen, Datei-ID %s" % result.get("id"))

    if args.dry_run:
        print("\nProbelauf beendet, es wurde nichts hochgeladen.")
    else:
        print("\nFertig. Neue Dateien gehen bei CurseForge zuerst in die Pruefung.")


if __name__ == "__main__":
    main()
