<!--
Text fuer die CurseForge-Projektseite. CurseForge verlangt eine englische, ausreichend
ausfuehrliche Beschreibung, deshalb ist diese Datei als einzige im Projekt auf Englisch.
Alles hier Beschriebene steht so auch in der README - keine Angabe darf ueber das
hinausgehen, was der Mod wirklich kann.

Der Block "SUMMARY" gehoert ins Feld "Summary", alles ab "DESCRIPTION" in die
Beschreibung.
-->

## SUMMARY

Client-side auto clicker for Fabric: auto attack, timed clicks, any key you like, plus
automatic eating. One build for 1.21.11, 26.1 and 26.2.

## DESCRIPTION

# OviClicker

A client-side auto clicker for Fabric. It holds or taps a key for you, either whenever
your crosshair is on a mob or on a fixed timer, and it can keep your hunger bar topped up
while it works. Everything is configured in its own settings screen — no config file
editing required.

The mod only simulates local input. It does not send crafted packets, does not touch
server logic and does not use reflection on obfuscated names.

## Modes

| Mode | What it does |
|---|---|
| **Off** | Nothing. |
| **Auto attack** | Triggers a click as soon as your crosshair is on an entity. |
| **Timer** | Triggers a click on a fixed interval, 10 seconds by default. |

Rate and jitter are configured **per mode**: 0.1–20 clicks per second for auto attack,
0.05–300 seconds for the timer, plus a jitter percentage that randomly varies the interval
in both directions.

## Any key, not just the mouse

The key that gets triggered is a setting, not a fixed left click:

* Left click (attack), right click (use)
* Jump, walk forward, walk backward, strafe left, strafe right, sneak, sprint, drop item

Actions use the key bindings from your own controls options, so if you moved "forward" to
`Z`, the mod presses `Z`. You can choose between tapping (1–20 ticks per press) and simply
holding the key down, which is what you want for auto walking or continuous mining. At most
one key is held at a time, and it is always released when the mod switches off, the game
pauses or you leave the world.

## Filters

* Only while the attack key is held
* Respect the attack cooldown, so charged attacks are not wasted
* Only with a weapon in hand — sword, axe, trident or mace
* Maximum reach for auto attack, 1–6 blocks
* Entity blacklist: never attack players, villagers, tamed animals or passive animals

## Automatic eating

When your hunger drops below the configured threshold (1–9 haunches, 6 by default), the mod
stops clicking and eats until the bar is full again, then carries on by itself.

* Only food that has nutrition and applies no harmful effect when eaten. Rotten flesh,
  spider eyes, poisonous potatoes, pufferfish and raw chicken drop out on their own,
  because the mod checks the item's actual consume effects rather than a fixed list — so
  food from other mods is handled too.
* Chorus fruit and suspicious stew are excluded as well. Golden apples and enchanted golden
  apples have one opt-in setting each and are off by default.
* Optionally refills the hotbar from your inventory when it runs out of food, using the same
  swap a hotbar key press would perform. Nothing is moved while another container is open.
* It picks the most nourishing item that still fits into the hunger bar, so as little
  nutrition as possible is wasted.
* Your own actions win: if you are using an item yourself the mod does not start, and if you
  switch hotbar slot while it eats, it stops.

## HUD and controls

A small HUD shows the current mode and whether the mod is eating. Corner, X and Y offset
and "hide while off" are configurable.

Three key bindings, all rebindable under Options → Controls:

| Action | Default |
|---|---|
| Open settings | `$` (Swiss keyboard layout) |
| Master toggle | Right shift |
| Cycle mode | unbound |

Mode and master toggle are written to disk immediately and survive leaving a server,
joining another one and restarting the client.

## Open screens and alt-tabbing

The mod keeps running while a screen is open — pause menu, inventory, chat — and while
Minecraft is in the background. Two limits remain: in single player Minecraft pauses the
whole game whenever a screen is open, so the mod pauses with it (on a server, including
"Open to LAN", everything keeps running), and the "only while the attack key is held"
option reads the physical key state, which the operating system reports as released once
the window loses focus.

## Requirements

* Fabric Loader 0.17 or newer
* Fabric API
* Client-side only — nothing needs to be installed on the server

Supported: Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2 and 26.2, all from a single code base.

Settings live in `config/oviclicker.json` and are written atomically, so a crash mid-save
cannot leave you with an unreadable config.

## Please note

Automated clicking is not allowed on every server. Check the rules of the servers you play
on before you use this mod there.

Source code and issue tracker:
https://github.com/Ovitrinker/Autoklicker-1.21.11---26.2 · MIT licensed.
