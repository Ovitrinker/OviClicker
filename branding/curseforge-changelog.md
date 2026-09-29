**OviClicker 1.3.0**

* Support for **Minecraft 26.3**.
* **Freecam compatibility**: with Freecam (xolt) enabled, AUTOATTACK keeps attacking. The
  mod then aims from the player itself, because Freecam blocks the regular attack. Single
  attacks on entities only; holding, block breaking and right-click stay blocked while
  Freecam is active.

**OviClicker 1.1.0**

* New name and a new icon. This project was previously developed under the name
  "AutoClicker"; the mod id is now `oviclicker`.
* **Auto eat**: when hunger drops below the configured threshold, the mod pauses clicking
  and eats. Only food without harmful effects is used, golden apples and enchanted golden
  apples each have their own opt-in setting, and food can be pulled up from the inventory
  when the hotbar runs out.
* The config file is now `config/oviclicker.json`. Settings from the previous
  `config/autoclicker.json` are **not** carried over and start at their defaults.

Built for Minecraft 1.21.11, 26.1–26.1.2, 26.2 and 26.3. Client-side only, requires Fabric API.
