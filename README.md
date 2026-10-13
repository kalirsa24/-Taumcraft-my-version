# Fredplenter

An original magic mod for NeoForge and Minecraft 1.21.1. Its research-and-aspect gameplay takes inspiration from classic thaumaturgy mods, while all code, names, descriptions, research content, and textures in this project are original. No Thaumcraft files or assets are included.

## Current gameplay

- **Aspect Codex** — craft it from a book, feather, and ink sac. Right-click it in the air to open the purple, starfield research map; select a node to see its description, prerequisites, method, and costs. A paper research consumes one paper and one inkwell charge to create an unfinished note. An aspect research spends its listed aspects and is learned immediately.
- **Arcane Table** — use the Codex on two adjacent vanilla crafting tables to form the two-block table. Place an unfinished research note in the table's note slot, select its matching topic, and connect the fixed start and end anchors across the hex board. The chain must pass through the required middle aspect; every adjacent pair must be related, and each placed tile must be discovered and have one point available. The server validates the complete route and only then spends the points. Remove the completed note, right-click it to open its own screen, and press **Learn**.
- **Research writing** — paper research requires one paper and an Inkwell with ink; Scribing Tools are optional. Craft an Inkwell from a glass bottle and ink sac; craft Scribing Tools from a feather and ink sac. Hold the Inkwell in one hand and an ink sac in the other, then right-click to refill it (8 charges). Each paper note spends one charge.
- **Aura Scanner and Resonance Wand** — hold either tool in your main hand and hover over a vanilla item in an open inventory or chest for two seconds. Each vanilla item type can be studied once per player, even if it appears in multiple stacks; modded items show “Unstudyable!”.
- **Aura nodes and Resonance Wand** — nodes generate during overworld world generation at exposed surface height, about one attempt per 24 chunks. Nodes provide the six primal vis types. Right-clicking a node with the Wand stores up to 100 of each type (600 total); hold Shift for the six individual counts. Nodes slowly regenerate. Right-clicking a node with the Aura Scanner also prints the vis still left in the node in chat, together with its dominant aspect. Nodes emit faint primal-coloured motes and an occasional chime.
- **Scanning the world** — hold Shift and right-click a vanilla block with the Aura Scanner to study its block type once, using the same aspect table as items. Blocks and items share one studied list. Holding Shift keeps normal block use (chests, doors) unaffected.
- **Discovery messages** — when a scan first reveals a compound aspect, the chat shows “New aspect discovered”.
- **Codex hints** — research tooltips list any wand spell the entry unlocks.
- **Wand spells** — the Resonance Wand casts the selected spell on right-click (anywhere that is not a node or an interactive block). Each spell spends stored vis of one primal aspect and does nothing if no valid target is in range, so the vis is only spent on success. Shift + right-click cycles through the spells you have learned:
  - **Gust** (Flatus, 2) — pushes creatures in a cone in front of you. Unlocked by *Focal Casting*.
  - **Pyre** (Flamma, 2) — sets the aimed creature on fire for 5 seconds. Unlocked by *Focal Casting*.
  - **Tide** (Unda, 2) — slows the aimed creature and puts out its fire. Unlocked by *Focal Casting*.
  - **Radiance** (Lux, 3) — heals you by two hearts and grants regeneration. Unlocked by *Focal Casting*.
  - **Verdure** (Humus, 3) — speeds up up to twelve nearby plants. Unlocked by *Verdant Weaving*.
  - **Dread** (Tenebrae, 3) — weakens and blinds the aimed creature. Unlocked by *Dread Weaving*.
- **Arcane Table display** — the 360-pixel-wide window gives the research parchment room to display its map and puzzle without crowding the six primal vis counters. The left panel has one dedicated slot for a Codex-generated research note; ordinary paper and scribing tools are not accepted. The aspect panel lists all 16 aspects and shows compound recipes in tooltips. Select two known aspects and use **Combine** to spend one point of each and gain one point of their compound aspect. The in-screen guide explains the table's purpose, how to obtain a note, and how to solve its path.
- **Aspect Cache** — creative-tab test item. Right-click once to add 100 points of every current aspect to the player; the item is consumed.
- **Research tree** — 17 entries across Foundations, Aura, Wand, and Crafting. The Wand branch now continues from *Wand Attunement* into *Focal Casting*, which leads to *Verdant Weaving* and *Dread Weaving*.
- **Progress and aspects** — currently 16 original aspects; primal aspects start known, while compound aspects are discovered by scanning items or combining their recipe. Aspect points, discovered aspects, and learned research are stored per player in world SavedData; existing worlds infer previously earned aspects from their saved positive point totals.
- **Original starter assets** — small pixel-art textures for the Codex, scanner, cache, scribing tools, notes, table, and aura node. The Resonance Wand uses Minecraft’s vanilla stick appearance.
- **Vanilla-resource recipes** — the Codex uses a book, feather, and ink sac. The Inkwell uses a glass bottle and ink sac. The Resonance Wand has three shaped recipes using vanilla resources: gold, copper/amethyst, or quartz/blaze powder.

The vanilla-item aspect assignments are deterministic and grouped by item-name families. They are a playable starter mapping, not a hand-authored Thaumcraft compatibility database.

## Deferred systems

Essentia jars and transport, infusion altar, golems, and corruption/taint are intentionally left for later phases. The current research tree is a starter progression rather than a complete reproduction of another mod’s full research catalog.

## Build and run

Requirements: Java 21. The included Gradle wrapper downloads Gradle 8.10.2 when needed.

From this directory:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

The NeoForge version is configured in `gradle.properties`; the Parchment mapping version is in `build.gradle`. The built mod JAR is written under `build/libs/`. The tested `0.0.1` JAR is also included in `dist/` for direct download.

## License

Licensed under the [MIT License](LICENSE).
