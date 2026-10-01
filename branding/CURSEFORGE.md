# CurseForge veröffentlichen

Das Projekt selbst muss **von Hand** im Autoren-Konto angelegt werden â€“ die Upload-API
kann Dateien hochladen, aber keine Projekte erstellen. Anzulegen unter
<https://authors.curseforge.com/#/projects/create/choose-game>, Spiel *Minecraft*.
DafÃ¼r bereit liegen:

| Feld | Quelle |
|---|---|
| Name | `OviClicker` |
| Summary | Block `SUMMARY` in `branding/curseforge-description.md` |
| Description | alles ab `DESCRIPTION` in derselben Datei (CurseForge verlangt Englisch) |
| Project License | MIT |
| Logo Image | `branding/oviclicker-logo.png` (512Ã—512, CurseForge verlangt mindestens 400Ã—400 im VerhÃ¤ltnis 1:1) |

Danach die drei Jars hochladen. Von Hand geht das Ã¼ber den Reiter *Files*, automatisch
Ã¼ber das Skript:

```bash
export CURSEFORGE_TOKEN=...     # Konto-Einstellungen -> API Tokens
./gradlew :1.21.11:buildAndCollect :26.1.x:buildAndCollect :26.2.x:buildAndCollect :26.3.x:buildAndCollect
python tools/publish_curseforge.py --project-id 123456 --dry-run
python tools/publish_curseforge.py --project-id 123456
```

Das Skript lÃ¤dt jedes Jar mit den Minecraft-Versionen hoch, die in
`stonecutter.properties.toml` unter `mod.mc_releases` stehen, ergÃ¤nzt den Modloader
`Fabric` und die passende Java-Version und nimmt den Text aus
`branding/curseforge-changelog.md` als Changelog. Die numerischen Versions-IDs holt es
zur Laufzeit von der API, damit hier keine veraltete Liste gepflegt werden muss. Ist die
Projekt-ID einmal bekannt, kann sie oben im Skript in `PROJECT_ID` fest eingetragen
werden.

Jede hochgeladene Datei geht bei CurseForge zuerst in die PrÃ¼fung und ist erst danach
Ã¶ffentlich sichtbar.

Beschreibung der API:
<https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api>
