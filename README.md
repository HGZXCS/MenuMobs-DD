# MenuMobs (Minecraft 1.20.1 Forge port)

Gimmick mod for Minecraft that renders the current player and a random mob on the
in-game main menu. Original 1.12.2 code by **superbas11**, credits to **bspkrs**
(original idea came from bspkrsCore). This repository is a **port to Minecraft
1.20.1 / Forge 47.4.x** using the official Mojang mappings.

## Features

* On the main menu (Title Screen) the currently logged-in player is shown on the
  right and a random living mob on the left (vanilla *and* modded mobs).
* Mobs slowly look toward your mouse cursor.
* Small on-screen buttons:
  * `+` (top-left, when a mob is shown): add the current mob to the blacklist.
  * `>` (next to it): show the next mob (or next random player).
* Mobs occasionally play their ambient sound at a configurable volume.
* Random appearance tweaks per mob (wool colour, villager profession, rabbit
  type, horse coat/armour, enderman carried block, random held items, ...).
* Fully configurable, see below.

## Configuration

The mod is client-side only. Open the mods list (Mods → Menu Mobs → Config) to use its
compact config screen, or edit `config/menumobs-client.toml` while the game is closed.

| Option | Default | Meaning |
|---|---|---|
| `showMainMenuMobs` | true | Master switch for the menu rendering. |
| `showOnlyPlayerModels` | false | Show only random player models (no mobs). |
| `mobSoundVolume` | 0.5 | 0.0–1.0 volume of the mob ambient sounds on the menu. |
| `fixedMob` | [] | If non-empty, only these entities are shown (entity registry ids, e.g. `minecraft:creeper`, or player names). |
| `blacklist` | [] | Entities never shown on the menu (adds to the built-in internal blacklist of problematic mobs). |
| `allowDebugOutput` | false | Extra log output about the shown entity. |
| `showMenuButtons` | true | Show the small top-left "blacklist / next entity" buttons on the main menu. |

## Building

Requirements: JDK 17+ (the wrapper is set up for Java 17 toolchains).

```
gradlew build
```

The finished jar is written to `build/libs/MenuMobsDD-1.20.1-<version>.jar` and can be
dropped into the `mods` folder of a Forge 1.20.1 (47.x) client.

## Notes / known differences vs the 1.12.2 original

* Rewritten for the modern render pipeline: entities are drawn through the vanilla
  `InventoryScreen` entity-preview helper (GuiGraphics + EntityRenderDispatcher) on the
  Forge `ScreenEvent.Render.Post` hook, i.e. right after the TitleScreen is drawn.
* To have a world to spawn entities in while on the title screen, the mod builds a fake
  `ClientLevel` and a fake packet listener. Because 1.20.1 levels need the data-driven
  registries, the first visit to the main menu loads them once from the vanilla data
  pack (a short one-time hitch); if that fails the feature disables itself for the
  session.
* Players shown on the menu are `RemotePlayer` instances with an injected `PlayerInfo`,
  so real skins/capes are shown when the profile can be resolved (textures are fetched
  on a background thread - a default skin is used until then). Player names in
  `fixedMob` are resolved through the Mojang session/name API, falling back to a random
  well-known player profile when offline.
* The old custom config GUI (entity picker screens) was replaced by a compact screen
  with the same six options (Forge 1.20.1 has no built-in ForgeConfigSpec screen).
* Integration with long gone 1.12-era mods (Millenaire, Thaumcraft 4, Twilight Forest,
  chickens, WorldStateCheckpoints) was removed; their ids were trimmed from the built-in
  blacklist where meaningless on 1.20.1.
* Custom main menus of *other* mods are only supported when they subclass the vanilla
  Title Screen.
* Only the mob plays occasional sounds (and only while it is a `Mob`); the entities are
  posed statically otherwise - they are not simulated.

## License

CC BY-NC-SA 3.0 (see `LICENSE`).
