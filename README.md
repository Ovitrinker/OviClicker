# AutoClicker

Client-seitiger AutoClicker für Fabric mit drei Modi, eigenem Einstellungsbildschirm,
HUD-Anzeige und frei belegbaren Tasten. Mehrere Minecraft-Versionen aus einer
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

Die fertigen Jars liegen anschliessend unter `build/libs/1.0.0/`:

```
autoclicker-1.0.0+1.21.11.jar
autoclicker-1.0.0+26.1.2.jar
autoclicker-1.0.0+26.2.jar
```

Aktive Version im Entwicklungszustand wechseln:

```bash
./gradlew "Set active project to 1.21.11"
```

Testen im Spiel: `./gradlew :26.2.x:runClient`

## Bedienung

### Tasten

Alle drei Tasten stehen unter *Optionen → Steuerung → Tastenbelegung* in der Kategorie
**AutoClicker** und lassen sich dort frei ändern.

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
losgelassen, sobald der Mod abschaltet, ein Bildschirm aufgeht oder du die Welt verlässt.

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
* **HUD**: an/aus, Ecke, Abstand X und Y, Ausblenden im Zustand OFF

Änderungen im Bildschirm greifen erst mit **Speichern**. **Zurücksetzen** stellt die
Standardwerte her, **Abbrechen** verwirft die Änderungen.

### Zustand über Serverwechsel hinweg

Modus und Master-Schalter werden bei jeder Änderung sofort in die Konfigurationsdatei
geschrieben und beim Verlassen eines Servers nicht zurückgesetzt. Wer den Server verlässt und
wieder beitritt – oder den Client neu startet – findet den AutoClicker unverändert aktiv vor.
Lediglich der Intervall-Timer startet beim Betreten einer Welt frisch, damit direkt nach dem
Beitritt kein Klick-Stau entsteht.

## Konfiguration

`config/autoclicker.json`, geschrieben über die in Minecraft enthaltene GSON-Instanz.

Geschrieben wird atomar: zuerst `autoclicker.json.tmp`, dann wird die bisherige Datei als
`autoclicker.json.bak` gesichert und die temporäre Datei an ihre Stelle verschoben. Fehlende
oder defekte Felder fallen auf die Standardwerte zurück, eine unlesbare Datei verhindert den
Start des Clients nicht.

## Technik

* Einstiegspunkt `ClientModInitializer`, Tick-Logik in `ClientTickEvents.END_CLIENT_TICK`,
  kein eigener Thread
* Klick-Auslösung über Mixin-Invoker auf `Minecraft.startAttack()` und
  `Minecraft.startUseItem()`; zusätzlich gelesen werden `Minecraft.missTime` und
  `Player.getAttackStrengthScale(float)`
* Tastenaktionen über `KeyMapping.setDown(boolean)` plus `KeyMapping.click(key)` für
  Aktionen, die Klickzähler statt Haltezustand auswerten (z. B. Ablegen)
* Die Prüfung „nur bei gedrückter Angriffstaste" liest den Tastenzustand direkt bei GLFW
  (`InputConstants.isKeyDown` bzw. `glfwGetMouseButton`), nicht über `KeyMapping.isDown()` –
  sonst würde der Mod im Halte-Modus seinen eigenen simulierten Druck als Spielereingabe
  lesen und sich selbst am Leben halten
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

Die Kategorie der Tastenbelegungen ist seit 1.21.11 keine freie Zeichenkette mehr, sondern ein
`KeyMapping.Category` mit `Identifier`. Der daraus gebildete Übersetzungsschlüssel lautet
`key.category.autoclicker.main`; `key.categories.autoclicker` ist in den Sprachdateien als
Alias enthalten.

## Projektstruktur

```
src/main/java/ch/andrinzwicky/autoclicker/
  AutoClickerClient.java      Einstiegspunkt
  KeybindManager.java         Tastenbelegungen und deren Auswertung
  HudRenderer.java            HUD-Element
  compat/ClientCompat.java    versionsabhängige Screen-Zugriffe
  config/                     AutoClickerConfig, ConfigManager, HudCorner
  feature/                    ClickMode, AutoClickerEngine
  gui/                        AutoClickerScreen, DoubleSliderWidget
  mixin/                      MinecraftAccessor
src/main/resources/
  fabric.mod.json, autoclicker.mixins.json
  assets/autoclicker/lang/en_us.json, de_ch.json
```

Sprachdateien: Englisch (`en_us`) und Schweizer Deutsch (`de_ch`, durchgehend „ss“).
