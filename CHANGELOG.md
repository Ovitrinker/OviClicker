# Changelog

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
