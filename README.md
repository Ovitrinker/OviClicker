# OviClicker

Client-side auto-clicker for Fabric with three modes, automatic eating, its own settings screen,
a HUD and freely rebindable keys. Several Minecraft versions from a single codebase, managed with
[Stonecutter](https://stonecutter.kikugie.dev).

Author: **Ovitrinker** · License: **MIT** · Download: [mods.ovitrinker.ch/oviclicker](https://mods.ovitrinker.ch/oviclicker/)

## Version matrix

| Build node | Compiled against | Covers | Fabric API | Java | Mappings |
|---|---|---|---|---|---|
| `1.21.11` | 1.21.11 | 1.21.11 | 0.141.6+1.21.11 | 21 | Mojang (on an obfuscated version) |
| `26.1.x` | 26.1.2 | 26.1, 26.1.1, 26.1.2 | 0.155.2+26.1.2 | 25 | Mojang (unobfuscated) |
| `26.2.x` | 26.2 | 26.2 | 0.157.0+26.2 | 25 | Mojang (unobfuscated) |
| `26.3.x` | 26.3 | 26.3 | 0.161.0+26.3 | 25 | Mojang (unobfuscated) |

Fabric Loader 0.17 or newer, 0.19.3 recommended. Fabric API is required at runtime.

**Why no Yarn?** 1.21.11 is the last obfuscated Minecraft version. From 26.1 on the game is
unobfuscated, and Fabric no longer maintains Yarn and Intermediary. So that a single codebase can
serve every target version, everything is compiled against the official Mojang mappings – including
1.21.11. Loom automatically remaps the mixin annotations of the 1.21.11 jar to Intermediary at
build time.

There is no other version between 1.21.11 and 26.1; with 26.1 Mojang switched to year-based
versioning.

## Building

Requirement: JDK 21 or newer (Gradle fetches missing JDKs via the Foojay resolver).

```bash
# One version
./gradlew :26.2.x:build

# Build all versions and collect the jars
./gradlew :1.21.11:buildAndCollect :26.1.x:buildAndCollect :26.2.x:buildAndCollect :26.3.x:buildAndCollect
```

The finished jars end up in `build/libs/1.3.0/`:

```
oviclicker-1.3.0+1.21.11.jar
oviclicker-1.3.0+26.1.2.jar
oviclicker-1.3.0+26.2.jar
oviclicker-1.3.0+26.3.jar
```

Switch the active version in the development workspace:

```bash
./gradlew "Set active project to 1.21.11"
```

Test in game: `./gradlew :26.3.x:runClient`

## Usage

### Keys

All three keys are listed under *Options → Controls → Key Binds* in the **OviClicker** category
and can be changed freely there.

| Action | Default |
|---|---|
| Open settings | `$` (Swiss layout) |
| Turn on and off (master toggle) | Right Shift |
| Cycle mode | unbound |

Up to 26.2, GLFW always reports keys in the US layout. The Swiss `$` key sits on scancode `0x2B`,
the ISO key left of Enter, which is backslash in the US layout. The GLFW code used is therefore
`GLFW_KEY_BACKSLASH` (92). From 26.3 on, Minecraft uses SDL3 and works with scancodes.

The settings screen only opens when no other screen is open. While typing (chat, signs, anvil),
Minecraft doesn't pass key presses to key binds at all.

### Modes

| Mode | Behaviour |
|---|---|
| `OFF` | Nothing. |
| `AUTOATTACK` | Triggers a left click as soon as the crosshair is on an entity (`EntityHitResult`). |
| `TIMER` | Triggers a left click at a fixed interval, 10 seconds by default. |

### Action

Which key is triggered can be set **per mode** ("Action" button in the screen):

| Action | What happens |
|---|---|
| Left click (attack) | Attack or break a block, via `Minecraft.startAttack()` |
| Right click (use) | Use an item or block, via `Minecraft.startUseItem()` |
| Jump, Forward, Back, Left, Right, Sneak, Sprint, Drop | presses the corresponding vanilla key bind |

All key actions use the binding you set in the controls options – if you bind "Forward" to `Z`,
the mod presses `Z`.

* **Hold instead of tap** – the key stays pressed as long as all conditions are met (auto-walk,
  continuous mining). The interval is ignored.
* **Press duration** (1 – 20 ticks) – how long a tapped key stays down. 1 is enough for jumping or
  dropping; some things need more.

At most one key is pressed at a time, and it is reliably released as soon as the mod turns off,
the game pauses or you leave the world.

### Automatic eating (AutoEat)

When hunger drops below the configured threshold (6 haunches by default), the mod pauses clicking
and eats until the hunger bar is full again. Then OviClicker resumes on its own. While eating, the
HUD shows "· eating".

* **Good food only.** Only items with nutritional value and no harmful status effect on
  consumption are eaten. Rotten flesh, spider eye, poisonous potato, pufferfish and raw chicken
  drop out automatically – the item's consumption effects are checked rather than a fixed list,
  so this also works for food from other mods.
* Chorus fruit (teleports) and suspicious stew (unknown effect) are blocked as well. The two golden
  apples each have their own option and are blocked by default: **Allow golden apples** and
  **Allow enchanted golden apples** can be enabled independently.
* **Take food from the inventory** – if there is nothing edible left in the hotbar, the mod moves
  supplies there from the inventory. This is the same swap a hotbar key triggers in the open
  inventory. If a slot is free, that one is used and the rest of the stack stays there afterwards;
  if the hotbar is full, the selected item temporarily moves to the inventory and returns to its
  slot after eating. Nothing is moved while a foreign container (chest, furnace) is open.
* The most nutritious item that still fits completely into the hunger bar is chosen; if none fits,
  the weakest one – so as little nutrition as possible is wasted.
* The player has priority: if they are using an item themselves, the mod doesn't start, and if
  they switch the hotbar slot while eating, the process is cancelled.
* AutoEat depends on the master switch, not on the mode: it also works in `OFF` mode.

### Open screens and window switching

OviClicker keeps running while a screen is open – pause menu, inventory, chat, its own settings
screen – and also when you switch to another window.

In these cases Minecraft skips its own key handling and additionally sets `missTime` to 10000,
which blocks every attack. The mod therefore triggers attack, use and drop itself exactly the way
Minecraft would, and keeps track of the attack cooldown itself (`OviClickerEngine.trackMissTime`).
Movement keys work anyway, because the player reads the held state directly.

Two limits remain:

* **Singleplayer**: Minecraft pauses the whole game there as soon as a screen is open. Then neither
  world nor server ticks – OviClicker pauses along with it and restarts its timer. On a server
  (including "Open to LAN") everything keeps running. For window switching in singleplayer,
  `Options → Pause on Lost Focus → OFF` helps: then no screen opens on Alt-Tab and the game, along
  with OviClicker, keeps running.
* **Only while the attack key is held**: the state is polled directly from the window system. If
  the window loses focus, the key is reported as released and the condition applies.

### Settings

* **Clicks per second** (AUTOATTACK, 0.1 – 20) and **interval** (TIMER, 0.05 – 300 s) – separate
  per mode
* **Jitter** in percent – random deviation of the interval up and down, also separate per mode
* **Only while the attack key is held**
* **Respect attack cooldown** – waits until `getAttackStrengthScale` is back at 1.0 (only applies
  to the left-click action)
* **Only with a weapon in hand** – sword, axe, trident or mace
* **Maximum reach** for AUTOATTACK (1 – 6 blocks)
* **Entity blacklist**: players, villagers, tamed animals, passive animals
* **AutoEat**: eat automatically on/off, threshold in haunches (1 – 9), take food from the
  inventory, allow golden apples, allow enchanted golden apples
* **HUD**: on/off, corner, X and Y offset, hide while OFF

Changes in the screen only take effect on **Save**. **Reset** restores the defaults, **Cancel**
discards the changes.

### State across server switches

Mode and master switch are written to the config file immediately on every change and are not
reset when leaving a server. Leave the server and rejoin – or restart the client – and OviClicker
is still active as before. Only the interval timer starts fresh when entering a world, so there is
no burst of clicks right after joining.

## Configuration

`config/oviclicker.json`, written with the GSON instance bundled with Minecraft.

Writes are atomic: first `oviclicker.json.tmp`, then the previous file is backed up as
`oviclicker.json.bak` and the temporary file is moved into place. Missing or broken fields fall
back to the defaults; an unreadable file does not prevent the client from starting.

## Technical details

* Entry point `ClientModInitializer`, tick logic in `ClientTickEvents.END_CLIENT_TICK`, no extra
  thread
* Clicks are triggered via mixin invokers on `Minecraft.startAttack()`,
  `Minecraft.startUseItem()` and `Minecraft.continueAttack(boolean)`; `Minecraft.rightClickDelay`
  and `Player.getAttackStrengthScale(float)` are read, `Minecraft.missTime` is read and written
* Key actions via `KeyMapping.setDown(boolean)` plus `KeyMapping.click(key)` for actions that
  evaluate click counters instead of held state (e.g. drop)
* The "only while the attack key is held" check reads the key state directly from the window
  system (`InputConstants.isKeyDown` or the mouse button), not via `KeyMapping.isDown()` –
  otherwise the mod would read its own simulated press as player input in hold mode and keep
  itself alive
* AutoEat eats via `MultiPlayerGameMode.useItem(player, hand)` and holds the "Use" key while doing
  so, because Minecraft would otherwise cancel eating in the next tick. Deliberately not via
  `Minecraft.startUseItem()`: that would interact with the targeted block first and open a chest
  instead of eating. Moving food uses `handleContainerInput(…, SWAP, …)`, i.e. a regular click
  packet
* No reflection on obfuscated names, no forged network packets, no bypassing of server logic – the
  mod only simulates local input
* Null checks on `client.player`, `client.level` and `client.gameMode` every tick
* The settings screen only uses the vanilla screen API, so the multi-version builds don't depend on
  any third-party library
* Freecam (xolt) is detected via reflection; while it is active, AUTOATTACK aims from the player
  and attacks via `gameMode.attack()` + `swing()`, because Freecam blocks `startAttack()`

### Version differences in the code

All differences are handled with Stonecutter comments:

| Area | 1.21.11 | 26.1.x | 26.2 | 26.3 |
|---|---|---|---|---|
| Key bind module | `fabric-key-binding-api-v1` / `KeyBindingHelper` | `fabric-key-mapping-api-v1` / `KeyMappingHelper` | like 26.1 | like 26.1 |
| Key bind category | `KeyMapping.Category` | same | same | same |
| HUD and screen | `GuiGraphics.drawString(…)` | `GuiGraphicsExtractor.text(…)` | like 26.1 | like 26.1 |
| Open screen | `Minecraft.setScreen` | same | `Minecraft.gui.setScreen` | like 26.2 |
| Inventory click | `handleInventoryMouseClick(…, ClickType.SWAP, …)` | `handleContainerInput(…, ContainerInput.SWAP, …)` | like 26.1 | like 26.1 |
| Window system | GLFW (`GLFW.glfwGetMouseButton`, key codes) | same | same | SDL3 (`SDLMouse`, scancodes) |
| Swing / drop | `swing(hand)` / `player.drop(false)` | same | same | `swing(hand, animation, false)` / `gameMode.dropItem(…)` |

Since 1.21.11 the key bind category is no longer a free-form string but a `KeyMapping.Category`
with an `Identifier`. The resulting translation key is `key.category.oviclicker.main`;
`key.categories.oviclicker` is included in the language files as an alias.

## Project structure

```
src/main/java/ch/ovitrinker/oviclicker/
  OviClickerClient.java       entry point
  KeybindManager.java         key binds and their handling
  HudRenderer.java            HUD element
  HudExtensions.java,
  HudLineProvider.java        extension point for extra HUD lines
  compat/ClientCompat.java    version-dependent screen and input access
  compat/ContainerCompat.java version-dependent inventory click
  compat/FreecamCompat.java   Freecam detection
  config/                     OviClickerConfig, ConfigManager, HudCorner
  feature/                    ClickMode, ClickAction, InputSimulator, OviClickerEngine
  feature/AutoEatHandler.java automatic eating
  feature/FoodFilter.java     decides which food is good
  gui/                        OviClickerScreen, DoubleSliderWidget,
                              ScreenExtension(s), ExtensionApi
  mixin/                      MinecraftAccessor
src/main/resources/
  fabric.mod.json, oviclicker.mixins.json
  assets/oviclicker/icon.png       mod icon, 128x128
  assets/oviclicker/lang/en_us.json, de_ch.json
tools/make_icon.py                 generates the icon and CurseForge logo
tools/publish_curseforge.py        uploads the jars to CurseForge
branding/                          logo, project texts and CurseForge guide
CHANGELOG.md                       changes per version
```

Languages: English (`en_us`) and Swiss German (`de_ch`).

## Download

Ready-made jars for all supported versions are available at
[mods.ovitrinker.ch/oviclicker](https://mods.ovitrinker.ch/oviclicker/). Changes per version are
listed in the [CHANGELOG](CHANGELOG.md).

## Icon

`assets/oviclicker/icon.png` (128×128, in the jar) and `branding/oviclicker-logo.png` (512×512,
CurseForge project image) are not drawn by hand; both are generated from the same drawing by
`tools/make_icon.py` (Pillow, with supersampling). The look follows mods.ovitrinker.ch: black
background with a terminal grid, green mouse cursor, light-blue click waves – green for content,
light blue for everything clickable.

```bash
python tools/make_icon.py
```
