# Publishing on CurseForge

The project itself has to be created **by hand** in the author account – the upload API can
upload files but cannot create projects. Create it at
<https://authors.curseforge.com/#/projects/create/choose-game>, game *Minecraft*.
Prepared for this:

| Field | Source |
|---|---|
| Name | `OviClicker` |
| Summary | `SUMMARY` block in `branding/curseforge-description.md` |
| Description | everything from `DESCRIPTION` on in the same file |
| Project License | MIT |
| Logo Image | `branding/oviclicker-logo.png` (512×512, CurseForge requires at least 400×400 at a 1:1 ratio) |

Then upload the jars. By hand via the *Files* tab, or automatically with the script:

```bash
export CURSEFORGE_TOKEN=...     # account settings -> API Tokens
./gradlew :1.21.11:buildAndCollect :26.1.x:buildAndCollect :26.2.x:buildAndCollect :26.3.x:buildAndCollect
python tools/publish_curseforge.py --project-id 123456 --dry-run
python tools/publish_curseforge.py --project-id 123456
```

The script uploads each jar with the Minecraft versions listed under `mod.mc_releases` in
`stonecutter.properties.toml`, adds the `Fabric` mod loader and the matching Java version, and
uses the text from `branding/curseforge-changelog.md` as the changelog. It fetches the numeric
version IDs from the API at runtime, so no outdated list has to be maintained here. Once the
project ID is known, it can be hard-coded in `PROJECT_ID` at the top of the script.

Every uploaded file goes through CurseForge review first and only becomes public afterwards.

API documentation:
<https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api>
