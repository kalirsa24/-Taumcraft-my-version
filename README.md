# Fredplenter

Fredplenter is an original magic-and-research mod for Minecraft 1.21.1 and NeoForge 21.1.x. It is inspired by Thaumcraft, is not affiliated with its creators, and does not reuse its code or assets.

## Features

- Six primal aspects and ten compound aspects.
- A Codex research map, player-bound research notes, and an Arcane Table with a route puzzle.
- Aura nodes that store regenerating vis, a scanner, and a wand with research-unlocked spells.
- Optional Miner, Warrior, Fisher, and Create research bonuses; server operators can disable these.
- English and Russian localization.

## Install

1. Install Java 21 and NeoForge for Minecraft 1.21.1.
2. Download the Fredplenter JAR from the project's [GitHub Releases](https://github.com/kalirsa24/fredplenter-arcane-research/releases).
3. Put the JAR in the Minecraft instance's `mods` folder.
4. Launch the matching NeoForge profile. For a multiplayer server, install the same mod build on the server and clients.

No extra runtime library is required. Use only releases built for the same Minecraft and NeoForge versions shown above.

## Start playing

1. Craft the Aspect Codex and right-click it to open the research map.
2. Hold the Aura Scanner or Resonance Wand in your main hand and hover an item with aspects in an open inventory for about two seconds. Shift-right-click a block with the scanner to study it. Scanning an aura node reveals its current vis.
3. Complete available research in the Codex. Paper research consumes paper and ink to create a player-bound note; aspect research spends the listed knowledge immediately.
4. Put an unfinished note in the Arcane Table. Begin the matching study, connect the displayed aspects through adjacent hexes without repeating an aspect, and finish at the end anchor. Take the completed note and use it to learn the research.
5. Learn **Compound Aspects** to combine components into compound knowledge. Learn **Aura Survey** to slowly gather renewable knowledge near nodes.

Player knowledge and wand/node vis are separate resources. Research costs knowledge; wand spells spend vis. **Basic Observation** increases first-scan rewards, and **Aspect Almanac** enables mapped crafting ingredients to pass their aspects to unmapped modded crafting outputs.

## Modpack support

Modpack authors can assign aspects to modded items with datapack item tags. Add an item id to `data/<pack_namespace>/tags/item/aspects/<aspect>.json`; for example:

```json
{
  "replace": false,
  "values": ["examplemod:crystal_shard"]
}
```

Supported aspect ids are `flamma`, `unda`, `humus`, `flatus`, `lux`, `tenebrae`, `vapor`, `limus`, `cinis`, `procella`, `pulvis`, `aestus`, `vita`, `mors`, `somnus`, and `motus`. An item can belong to more than one tag. See [the architecture guide](docs/ARCHITECTURE.md) for recipe inheritance, config options, commands, and implementation details.

Vanilla item mappings are defined by the mod and take precedence over datapack tags. Tags are intended for modded items; after Aspect Almanac, an unmapped modded crafting output can inherit aspects from mapped ingredients.

## Server operators

The server config is `fredplenter-server.toml` in the world's `serverconfig` directory. It includes per-aspect knowledge and vis caps, passive node knowledge interval and radius, optional bonus-branch enablement and strength, and PvP/spell targeting settings.

Permission-level-2 command examples:

```text
/fredplenter knowledge <player> flamma 25
/fredplenter research <player> fredplenter:basic_observation
/fredplenter research <player> fredplenter:basic_observation revoke
/fredplenter recover-note <player> fredplenter:basic_observation
/fredplenter reset <player>
```

Commands can grant/revoke registered research, set an aspect balance, reset all Fredplenter progress, or release a lost-note reservation.

## Project links

- [Technical architecture and validation](docs/ARCHITECTURE.md)
- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
- [MIT License](LICENSE)

## Build from source

Requires Java 21. On Windows run `.\gradlew.bat test build`; on Linux/macOS run `./gradlew test build`. The distributable JAR is written to `build/libs/`. Automated tests run on GitHub Actions for pushes and pull requests.
