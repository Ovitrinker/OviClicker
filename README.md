# OviClicker

Client-seitiger Auto-Klicker für Fabric mit drei Modi, automatischem Essen, eigenem
Einstellungsbildschirm, HUD-Anzeige und frei belegbaren Tasten. Mehrere Minecraft-Versionen aus einer
Codebasis, verwaltet mit [Stonecutter](https://stonecutter.kikugie.dev).

Autor: **Ovitrinker** · Lizenz: **MIT**

## Versionsmatrix

| Build-Knoten | Kompiliert gegen | Deckt ab | Fabric API | Java | Mappings |
|---|---|---|---|---|---|
| `1.21.11` | 1.21.11 | 1.21.11 | 0.141.6+1.21.11 | 21 | Mojang (auf obfuskierter Version) |
| `26.1.x` | 26.1.2 | 26.1, 26.1.1, 26.1.2 | 0.155.2+26.1.2 | 25 | Mojang (unobfuskiert) |
| `26.2.x` | 26.2 | 26.2 | 0.157.0+26.2 | 25 | Mojang (unobfuskiert) |

Fabric Loader ab 0.17, empfohlen 0.19.3. Die Fabric API wird zur Laufzeit benötigt.

**Warum kein Yarn?** 1.21.11 ist die letzte obfuskierte Minecraft-Version. Ab 26.1 ist das
Spiel unobfuskiert, Yarn und Intermediary werden von Fabric nicht mehr gepflegt. Damit eine
einzige Codebasis alle Zielversionen bedienen kann, wird durchgehend mit den offiziellen
Mojang-Mappings kompiliert – auch für 1.21.11. Loom remappt die Mixin-Annotationen des
1.21.11-Jars beim Bauen automatisch nach Intermediary.

Zwischen 1.21.11 und 26.1 existiert keine weitere Version; Mojang ist mit 26.1 auf eine
Jahres-Versionierung umgestiegen.

## Bauen

Voraussetzung: JDK 21 oder neuer (Gradle lädt fehlende JDKs über den Foojay-Resolver nach).

```bash
# Eine Version
./gradlew :26.2.x:build

# Alle Versionen bauen und die Jars einsammeln
./gradlew :1.21.11:buildAndCollect :26.1.x:buildAndCollect :26.2.x:buildAndCollect
```

Die fertigen Jars liegen anschliessend unter `build/libs/1.1.0/`:

```
oviclicker-1.1.0+1.21.11.jar
oviclicker-1.1.0+26.1.2.jar
oviclicker-1.1.0+26.2.jar
```

Aktive Version im Entwicklungszustand wechseln:

```bash
./gradlew "Set active project to 1.21.11"
```

Testen im Spiel: `./gradlew :26.2.x:runClient`

## Bedienung

### Tasten

Alle drei Tasten stehen unter *Optionen → Steuerung → Tastenbelegung* in der Kategorie
**OviClicker** und lassen sich dort frei ändern.

| Aktion | Standard |
|---|---|
| Einstellungen öffnen | `$` (Schweizer Layout) |
| Ein- und ausschalten (Master-Toggle) | Rechte Umschalttaste |
| Modus weiterschalten | nicht belegt |

GLFW meldet Tasten immer in der Belegung des US-Layouts. Die Schweizer `$`-Taste liegt auf
Scancode `0x2B`, also auf der ISO-Taste links von der Eingabetaste, im US-Layout der
Backslash. Der verwendete GLFW-Code ist deshalb `GLFW_KEY_BACKSLASH` (92).

Der Einstellungsbildschirm öffnet nur, wenn kein anderer Bildschirm offen ist. Während einer
Texteingabe (Chat, Schilder, Amboss) reicht Minecraft Tastendrücke gar nicht erst an
Tastenbelegungen weiter.

### Modi

| Modus | Verhalten |
|---|---|
| `OFF` | Nichts. |
| `AUTOATTACK` | Löst einen Linksklick aus, sobald das Fadenkreuz auf einer Entity liegt (`EntityHitResult`). |
| `TIMER` | Löst einen Linksklick in festem Intervall aus, Standard 10 Sekunden. |

### Aktion

Welche Taste ausgelöst wird, ist **pro Modus** einstellbar (Knopf „Aktion" im Bildschirm):

| Aktion | Was passiert |
|---|---|
| Linksklick (Angriff) | Angriff bzw. Block abbauen, über `Minecraft.startAttack()` |
| Rechtsklick (Benutzen) | Gegenstand oder Block benutzen, über `Minecraft.startUseItem()` |
| Springen, Vorwärts, Rückwärts, Nach links, Nach rechts, Schleichen, Sprinten, Ablegen | setzt die jeweilige Vanilla-Tastenbelegung |

Alle Tastenaktionen benutzen die Belegung, die du in den Steuerungs-Optionen gesetzt hast –
belegst du „Vorwärts" auf `Z`, drückt der Mod `Z`.

* **Gedrückt halten statt antippen** – die Taste bleibt gedrückt, solange alle Bedingungen
  erfüllt sind (Autolauf, Dauerabbau). Das Intervall wird dabei ignoriert.
* **Druckdauer** (1 – 20 Ticks) – wie lange eine angetippte Taste unten bleibt. Für Springen
  oder Ablegen reicht 1, für manches braucht es mehr.

Es ist immer höchstens eine Taste gleichzeitig gedrückt, und sie wird zuverlässig
losgelassen, sobald der Mod abschaltet, das Spiel anhält oder du die Welt verlässt.

### Automatisch essen (AutoEat)

Fällt der Hunger unter die eingestellte Schwelle (Standard 6 Keulen), unterbricht der Mod
das Klicken und isst, bis die Hungerleiste wieder voll ist. Danach läuft der OviClicker von
selbst weiter. Solange gegessen wird, zeigt das HUD „· isst“.

* **Nur gutes Essen.** Gegessen wird nur, was einen Nährwert hat und beim Verzehr keinen
  schädlichen Statuseffekt auslöst. Verfaultes Fleisch, Spinnenauge, giftige Kartoffel,
  Kugelfisch und rohes Huhn fallen dadurch von selbst weg – geprüft werden die
  Verzehr-Effekte des Gegenstands, nicht eine feste Liste, deshalb greift das auch bei
  Essen aus anderen Mods.
* Zusätzlich gesperrt sind Chorusfrucht (teleportiert) und verdächtiger Eintopf
  (unbekannter Effekt). Die beiden goldenen Äpfel haben je eine eigene Option und sind
  standardmässig gesperrt: **Goldene Äpfel erlauben** und **Verzauberte goldene Äpfel
  erlauben** lassen sich unabhängig voneinander einschalten.
* **Essen aus dem Inventar holen** – ist in der Hotbar nichts Essbares mehr, legt der Mod
  Nachschub aus dem Inventar dorthin. Das ist derselbe Tausch, den eine Hotbar-Taste im
  offenen Inventar auslöst. Ist ein Platz frei, wird dieser genommen und der Rest des
  Stapels bleibt danach dort liegen; ist die Hotbar voll, weicht der gewählte Gegenstand
  vorübergehend ins Inventar und kommt nach dem Essen an seinen Platz zurück. Bei einem
  offenen fremden Behälter (Truhe, Ofen) wird nicht umgelegt.
* Gewählt wird der nahrhafteste Gegenstand, der noch vollständig in die Hungerleiste passt;
  passt keiner hinein, der schwächste – so verfällt möglichst wenig Nährwert.
* Der Spieler hat Vorrang: benutzt er selbst gerade einen Gegenstand, fängt der Mod gar
  nicht erst an, und wechselt er während des Essens den Hotbar-Platz, bricht der Vorgang ab.
* AutoEat hängt am Master-Schalter, nicht am Modus: es arbeitet auch im Modus `OFF`.

### Offene Bildschirme und Fensterwechsel

Der OviClicker läuft weiter, wenn ein Bildschirm offen ist – Esc-Menü, Inventar, Chat, der
eigene Einstellungsbildschirm – und auch dann, wenn du in ein anderes Fenster wechselst.

Minecraft überspringt in diesen Fällen seine eigene Tastenverarbeitung und setzt zusätzlich
`missTime` auf 10000, was jeden Angriff blockiert. Der Mod stösst Angriff, Benutzen und
Ablegen deshalb selbst genau so an, wie es Minecraft täte, und führt die Angriffs-Sperrzeit
selbst weiter (`OviClickerEngine.trackMissTime`). Bewegungstasten wirken ohnehin, weil der
Spieler den gehaltenen Zustand direkt ausliest.

Zwei Grenzen bleiben:

* **Einzelspieler**: Minecraft hält dort das ganze Spiel an, sobald ein Bildschirm offen ist.
  Dann tickt weder Welt noch Server – der OviClicker pausiert mit und setzt seinen Timer neu
  an. Auf einem Server (auch bei „Für LAN öffnen") läuft alles weiter. Für den Fensterwechsel
  hilft im Einzelspieler `Optionen → *Pause bei Fokusverlust* → AUS`: dann geht beim
  Alt-Tab gar kein Bildschirm auf und das Spiel läuft samt OviClicker weiter.
* **Nur bei gedrückter Angriffstaste**: der Zustand wird direkt bei GLFW abgefragt. Verliert
  das Fenster den Fokus, meldet GLFW die Taste als losgelassen, und die Bedingung greift.

### Einstellungen

* **Klicks pro Sekunde** (AUTOATTACK, 0.1 – 20) und **Intervall** (TIMER, 0.05 – 300 s) –
  getrennt pro Modus
* **Jitter** in Prozent – zufällige Abweichung des Intervalls nach oben und unten, ebenfalls
  getrennt pro Modus
* **Nur bei gedrückter Angriffstaste**
* **Angriffs-Cooldown respektieren** – wartet, bis `getAttackStrengthScale` wieder 1.0 ist
  (greift nur bei der Aktion Linksklick)
* **Nur mit Waffe in der Hand** – Schwert, Axt, Dreizack oder Keule
* **Maximale Reichweite** für AUTOATTACK (1 – 6 Blöcke)
* **Entity-Blacklist**: Spieler, Dorfbewohner, gezähmte Tiere, friedliche Tiere
* **AutoEat**: automatisch essen an/aus, Schwelle in Hungerkeulen (1 – 9), Essen aus dem
  Inventar holen, goldene Äpfel erlauben, verzauberte goldene Äpfel erlauben
* **HUD**: an/aus, Ecke, Abstand X und Y, Ausblenden im Zustand OFF

Änderungen im Bildschirm greifen erst mit **Speichern**. **Zurücksetzen** stellt die
Standardwerte her, **Abbrechen** verwirft die Änderungen.

### Zustand über Serverwechsel hinweg

Modus und Master-Schalter werden bei jeder Änderung sofort in die Konfigurationsdatei
geschrieben und beim Verlassen eines Servers nicht zurückgesetzt. Wer den Server verlässt und
wieder beitritt – oder den Client neu startet – findet den OviClicker unverändert aktiv vor.
Lediglich der Intervall-Timer startet beim Betreten einer Welt frisch, damit direkt nach dem
Beitritt kein Klick-Stau entsteht.

## Konfiguration

`config/oviclicker.json`, geschrieben über die in Minecraft enthaltene GSON-Instanz.

Geschrieben wird atomar: zuerst `oviclicker.json.tmp`, dann wird die bisherige Datei als
`oviclicker.json.bak` gesichert und die temporäre Datei an ihre Stelle verschoben. Fehlende
oder defekte Felder fallen auf die Standardwerte zurück, eine unlesbare Datei verhindert den
Start des Clients nicht.

## Technik

* Einstiegspunkt `ClientModInitializer`, Tick-Logik in `ClientTickEvents.END_CLIENT_TICK`,
  kein eigener Thread
* Klick-Auslösung über Mixin-Invoker auf `Minecraft.startAttack()`,
  `Minecraft.startUseItem()` und `Minecraft.continueAttack(boolean)`; gelesen werden
  `Minecraft.rightClickDelay` und `Player.getAttackStrengthScale(float)`,
  gelesen und geschrieben wird `Minecraft.missTime`
* Tastenaktionen über `KeyMapping.setDown(boolean)` plus `KeyMapping.click(key)` für
  Aktionen, die Klickzähler statt Haltezustand auswerten (z. B. Ablegen)
* Die Prüfung „nur bei gedrückter Angriffstaste" liest den Tastenzustand direkt bei GLFW
  (`InputConstants.isKeyDown` bzw. `glfwGetMouseButton`), nicht über `KeyMapping.isDown()` –
  sonst würde der Mod im Halte-Modus seinen eigenen simulierten Druck als Spielereingabe
  lesen und sich selbst am Leben halten
* AutoEat isst über `MultiPlayerGameMode.useItem(player, hand)` und hält dabei die Taste
  „Benutzen“ gedrückt, weil Minecraft das Essen sonst im nächsten Tick abbricht. Bewusst
  nicht über `Minecraft.startUseItem()`: der würde zuerst den anvisierten Block bedienen
  und statt zu essen eine Truhe öffnen. Das Umlegen von Essen läuft über
  `handleContainerInput(…, SWAP, …)`, also über ein reguläres Klick-Paket
* Keine Reflection auf verschleierte Namen, keine gefälschten Netzwerkpakete, kein Umgehen von
  Serverlogik – der Mod simuliert ausschliesslich lokale Eingaben
* Null-Prüfung auf `client.player`, `client.level` und `client.gameMode` in jedem Tick
* Der Einstellungsbildschirm nutzt ausschliesslich das Vanilla-Screen-API, damit die
  Multiversion-Builds an keiner Fremdabhängigkeit scheitern

### Versionsunterschiede im Code

Alle Unterschiede sind über Stonecutter-Kommentare gelöst:

| Bereich | 1.21.11 | 26.1.x | 26.2 |
|---|---|---|---|
| Keybind-Modul | `fabric-key-binding-api-v1` / `KeyBindingHelper` | `fabric-key-mapping-api-v1` / `KeyMappingHelper` | wie 26.1 |
| Keybind-Kategorie | `KeyMapping.Category` | gleich | gleich |
| HUD und Screen | `GuiGraphics.drawString(…)` | `GuiGraphicsExtractor.text(…)` | wie 26.1 |
| Screen öffnen | `Minecraft.setScreen` | gleich | `Minecraft.gui.setScreen` |
| Inventar-Klick | `handleInventoryMouseClick(…, ClickType.SWAP, …)` | `handleContainerInput(…, ContainerInput.SWAP, …)` | wie 26.1 |

Die Kategorie der Tastenbelegungen ist seit 1.21.11 keine freie Zeichenkette mehr, sondern ein
`KeyMapping.Category` mit `Identifier`. Der daraus gebildete Übersetzungsschlüssel lautet
`key.category.oviclicker.main`; `key.categories.oviclicker` ist in den Sprachdateien als
Alias enthalten.

## Projektstruktur

```
src/main/java/ch/ovitrinker/oviclicker/
  OviClickerClient.java      Einstiegspunkt
  KeybindManager.java         Tastenbelegungen und deren Auswertung
  HudRenderer.java            HUD-Element
  compat/ClientCompat.java    versionsabhängige Screen-Zugriffe
  compat/ContainerCompat.java versionsabhängiger Inventar-Klick
  config/                     OviClickerConfig, ConfigManager, HudCorner
  feature/                    ClickMode, ClickAction, InputSimulator, OviClickerEngine
  feature/AutoEatHandler.java automatisches Essen
  feature/FoodFilter.java     Bewertung, welches Essen gut ist
  gui/                        OviClickerScreen, DoubleSliderWidget
  mixin/                      MinecraftAccessor
src/main/resources/
  fabric.mod.json, oviclicker.mixins.json
  assets/oviclicker/icon.png       Mod-Icon, 128x128
  assets/oviclicker/lang/en_us.json, de_ch.json
tools/make_icon.py                 erzeugt Icon und CurseForge-Logo
tools/publish_curseforge.py        laedt die Jars auf CurseForge
branding/oviclicker-logo.png       Projektbild fuer CurseForge, 512x512
branding/curseforge-description.md Projekttext (englisch, wie CurseForge es verlangt)
branding/curseforge-changelog.md   Changelog-Text des laufenden Release
CHANGELOG.md                       Aenderungen je Version
```

Sprachdateien: Englisch (`en_us`) und Schweizer Deutsch (`de_ch`, durchgehend „ss“).

## Veröffentlichen

### CurseForge

Projektseite: *(wird hier eingetragen, sobald das Projekt angelegt ist)*

Das Projekt selbst muss **von Hand** im Autoren-Konto angelegt werden – die Upload-API
kann Dateien hochladen, aber keine Projekte erstellen. Anzulegen unter
<https://authors.curseforge.com/#/projects/create/choose-game>, Spiel *Minecraft*.
Dafür bereit liegen:

| Feld | Quelle |
|---|---|
| Name | `OviClicker` |
| Summary | Block `SUMMARY` in `branding/curseforge-description.md` |
| Description | alles ab `DESCRIPTION` in derselben Datei (CurseForge verlangt Englisch) |
| Project License | MIT |
| Logo Image | `branding/oviclicker-logo.png` (512×512, CurseForge verlangt mindestens 400×400 im Verhältnis 1:1) |

Danach die drei Jars hochladen. Von Hand geht das über den Reiter *Files*, automatisch
über das Skript:

```bash
export CURSEFORGE_TOKEN=...     # Konto-Einstellungen -> API Tokens
./gradlew :1.21.11:buildAndCollect :26.1.x:buildAndCollect :26.2.x:buildAndCollect
python tools/publish_curseforge.py --project-id 123456 --dry-run
python tools/publish_curseforge.py --project-id 123456
```

Das Skript lädt jedes Jar mit den Minecraft-Versionen hoch, die in
`stonecutter.properties.toml` unter `mod.mc_releases` stehen, ergänzt den Modloader
`Fabric` und die passende Java-Version und nimmt den Text aus
`branding/curseforge-changelog.md` als Changelog. Die numerischen Versions-IDs holt es
zur Laufzeit von der API, damit hier keine veraltete Liste gepflegt werden muss. Ist die
Projekt-ID einmal bekannt, kann sie oben im Skript in `PROJECT_ID` fest eingetragen
werden.

Jede hochgeladene Datei geht bei CurseForge zuerst in die Prüfung und ist erst danach
öffentlich sichtbar.

Beschreibung der API:
<https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api>

### mods.ovitrinker.ch

Die eigene Download-Seite liest die Jars aus `Desktop\Mods\<Version>\`. Nach dem Bauen
die Jars dorthin kopieren und in `C:\dev\mods.ovitrinker.ch` `python build.py --upload`
ausführen.

## Icon

`assets/oviclicker/icon.png` (128×128, im Jar) und `branding/oviclicker-logo.png`
(512×512, Projektbild für CurseForge) werden nicht von Hand gezeichnet, sondern beide aus
derselben Zeichnung von `tools/make_icon.py` erzeugt (Pillow, mit Supersampling). Der Look folgt
mods.ovitrinker.ch: schwarzer Grund mit Terminal-Raster, grüner Mauszeiger, hellblaue
Klick-Wellen – grün für Inhalt, hellblau für alles Klickbare.

```bash
python tools/make_icon.py
```
