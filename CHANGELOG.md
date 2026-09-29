# Changelog

## 1.3.0 – 2026-09-29

* Unterstützung für **Minecraft 26.3** (neuer Build-Knoten `26.3.x`, Fabric API
  0.161.0+26.3). Minecraft hat mit 26.3 GLFW durch SDL3 ersetzt: die physische Tasten- und
  Mausabfrage, die Standardtasten und die Aufrufe zum Schwingen und Ablegen sind dafür
  angepasst.
* **Freecam-Kompatibilität**: Ist Freecam (xolt) aktiv, greift AUTOATTACK weiterhin an. Der
  Mod zielt dann selbst vom Spieler aus, weil Freecam den normalen Angriff blockiert. Nur
  Einzel-Angriffe auf Wesen; Halten, Block-Abbau und Rechtsklick bleiben unter Freecam
  blockiert.

## 1.2.0 – 2026-09-08

* Neuer Erweiterungspunkt für andere Mods: `ScreenExtension`/`ScreenExtensions` lassen sich
  eigene Sektionen in den Einstellungsbildschirm einhängen (im selben scrollbaren Bereich wie
  die eigenen Optionen), `HudLineProvider`/`HudExtensions` lassen sich zusätzliche Zeilen im
  HUD anzeigen. Ohne installierte Erweiterung ändert sich am Verhalten nichts.

## 1.1.0 – 2026-09-03

* Der Mod heisst neu **OviClicker** (vorher „AutoClicker"). Umbenannt sind die Mod-ID
  (`oviclicker`), der Anzeigename, das Java-Paket, alle Klassen, Assets und
  Übersetzungsschlüssel.
* Neues Mod-Icon, erzeugt von `tools/make_icon.py`.
* **AutoEat**: Fällt der Hunger unter die eingestellte Schwelle, unterbricht der Mod das
  Klicken und isst. Nur Nahrung ohne schädliche Verzehr-Effekte, mit eigenen Optionen für
  goldene und verzauberte goldene Äpfel und mit Nachschub aus dem Inventar.
* Die Konfigurationsdatei heisst neu `config/oviclicker.json`. Die alte
  `config/autoclicker.json` wird **nicht** übernommen, die Einstellungen starten bei den
  Standardwerten.

## 1.0.0

* Erste Fassung: Modi OFF, AUTOATTACK und TIMER, frei wählbare Aktion, eigener
  Einstellungsbildschirm, HUD-Anzeige, belegbare Tasten.
* Ein Codestand für Minecraft 1.21.11, 26.1–26.1.2 und 26.2, verwaltet mit Stonecutter.
