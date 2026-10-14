# Architecture and maintainer guide

Fredplenter is a NeoForge mod for Minecraft 1.21.1. The server owns progression and validates every request that spends knowledge, completes research, changes a note, or modifies persistent player state. Client screens render synchronized state and send intent; they are not trusted to decide prices or rewards.

## Project map

| Location | Responsibility |
|---|---|
| `aspect/Aspect.java` | Aspect identifiers, display names, colors, and aspect-link relationships. |
| `aspect/AspectRecipe.java` | The canonical compound-aspect recipe catalogue used by both graph links and crafting. |
| `aspect/VanillaAspectMap.java`, `aspect/VanillaAspectRules.java` | Deterministic mappings for vanilla item identifiers; explicit path sets replace substring guesses. |
| `aspect/AspectTagResolver.java` | Looks up datapack tags for modded items and derives inherited aspects from crafting ingredients when enabled. |
| `research/Research.java` | Immutable research definition, method, costs, prerequisites, map location, and paper-only puzzle definition. |
| `research/ResearchRegistry.java` | The 32 registered research nodes, their prerequisite graph, categories, unlock methods, and costs. |
| `research/ArcaneProgress.java` | Server-side per-player balances, discoveries, completions, note reservations, and scan/reward state in `SavedData`. |
| `research/ResearchPuzzleSolver.java` | Finds a minimum-point non-repeating solution on the fixed hex board and validates submitted routes. |
| `research/ResearchPuzzleCosts.java` | Applies the Table Mastery reduction when computing the server-authoritative tile cost. |
| `research/AspectConversion.java`, `research/AspectCraftingRules.java` | Validate resource availability, research gates, conversion ratios, caps, and compound creation. |
| `item/` | The Codex, scanner, wand, research notes, inkwell, and writing tools. |
| `block/`, `block/entity/` | Arcane Table blocks/menu backing inventory and Aura Nodes with stored, regenerating vis. |
| `menu/ArcaneTableMenu.java` | Server-side table session, note-slot ownership, research start/completion validation, and inventory slots. |
| `client/screen/ArcaneTableScreen.java` | Research and aspect tabs, paper-research route interaction, and conversion confirmation UI. |
| `client/screen/ArcaneCodexScreen.java` | Research categories, dependency graph, node details, and unlock status. |
| `network/` | Typed payloads for client requests and server-to-client progress synchronization. |
| `gameplay/AuraAttunement.java` | Renewable knowledge from the nearest aura node for eligible players. |
| `gameplay/ResearchBonuses.java`, `loot/FisherCatchModifier.java` | Optional research-gated miner, warrior, fisher, and Create effects. |
| `spell/SpellEffects.java` | Wand spell effects, player-targeting policy, and crop-growth protection hooks. |
| `FredplenterConfig.java` | Server config definition and accessors for balance and optional gameplay behavior. |
| `AdminCommands.java` | Permission-level-2 operator commands for recovery and progression support. |

## Resource and progression model

Fredplenter has two separate resource concepts:

- **Knowledge** is the player's persistent, spendable research resource. It is stored by aspect, has a configurable per-aspect cap, and is earned through first scans, node study, and node attunement.
- **Vis** is a charge held by an Aura Node or wand. It powers spells and is distinct from knowledge; nodes and wands have separate capacities and regeneration/refill rules.

An ordinary discovery is recorded once per item/block identifier. Re-scanning an already-recorded target does not provide an unlimited repeat reward. Basic Observation improves the first discovery reward. Aura Survey enables slow renewable knowledge near an aura node; Node Attunement increases the search radius and Resonant Work shortens the interval. The nearest loaded node with a dominant aspect supplies one knowledge point, up to the configured knowledge cap.

Paper research and aspect research deliberately have different data and flows:

1. **Paper research** defines a three-aspect puzzle (start, required middle, end). The player creates a research note using the Codex, begins the study at an Arcane Table, traces a legal route, and learns the completed note.
2. **Aspect research** defines a cost and prerequisites. It is unlocked by spending the listed knowledge at the table; it has no puzzle path stored in its research definition.

The Codex uses a paper puzzle's start aspect as its node icon. An aspect-research node instead uses its first cost aspect. This keeps the map icon independent from puzzle-only data.

The Arcane Table's fixed puzzle board is a radius-two hexagon with 19 cells. Endpoints are fixed at `(-2, 0)` and `(2, 0)`. A valid route must use adjacent cells and linked aspects, visit both endpoints, include the required middle aspect, and never repeat an aspect or cell. `ResearchPuzzleSolver` searches candidate aspect sequences by length and then finds a matching board route; its reported tile cost is the number of intermediate aspects. Server-side completion validates the same constraints and deducts the cost only after all checks pass.

Compound creation requires the relevant research and component discovery. Conversion checks component balances, output capacity, and the configured knowledge cap before changing state. The UI's maximum-conversion action uses an arm-then-confirm interaction instead of immediately spending all available components.

## Research notes and multiplayer

- A pending note reservation is associated with the creating player's UUID and research id. The server refuses duplicate active reservations.
- An Arcane Table note slot only allows its owner to retrieve the note. Starting, completing, and learning the note are also owner-checked on the server.
- A completed note remains bound to its owner; other players cannot use it to learn the research.
- Table inventory is per table block, but research state and note reservations are per player.
- `/fredplenter recover-note` releases an orphaned reservation. It does not recreate a destroyed physical item; see the command reference below.

## Mapping aspects to items

### Vanilla mappings

Vanilla identifiers resolve through Java data in `VanillaAspectMap` and `VanillaAspectRules`. Specific vanilla path sets and explicit overrides are evaluated first. The fallback mapping is deterministic. Datapack tags do not override the vanilla mapping in the current resolver.

### Modded-item tags

Any datapack can add modded items to item tags such as `fredplenter:aspects/flamma`. The tag file path is:

```text
data/<datapack_namespace>/tags/item/aspects/<aspect_id>.json
```

Example:

```json
{
  "replace": false,
  "values": ["examplemod:crystal_shard"]
}
```

Supported aspect ids: `flamma`, `unda`, `humus`, `flatus`, `lux`, `tenebrae`, `vapor`, `limus`, `cinis`, `procella`, `pulvis`, `aestus`, `vita`, `mors`, `somnus`, and `motus`. An item can be included in multiple aspect tags.

### Recipe inheritance

After learning Aspect Almanac, scanning an unmapped modded crafting output can derive up to two aspects from its crafting recipes. The resolver examines recipe ingredients and votes using aspects that are directly mapped on ingredient alternatives. It requires at least two recognized ingredient groups, sorts ties deterministically by the aspect enum order, and limits the result to two aspects. It does not recursively infer ingredient mappings from other recipes, and it does not currently precompute every item at server startup. Explicit item tags take precedence over recipe inheritance.

## Server configuration

The config is `fredplenter-server.toml` in the world's `serverconfig` directory.

| Section/key | Default | Purpose |
|---|---:|---|
| `progression.knowledgeCap` | `100` | Maximum stored knowledge per aspect (range 1–100000). |
| `progression.passiveKnowledgeIntervalTicks` | `1200` | Attunement interval in ticks; 1200 ticks is one minute (range 20–1000000). |
| `progression.nodeAttunementRadius` | `10` | Search radius unlocked by Node Attunement (range 1–64); before that research, the base radius is 6. |
| `progression.auraNodeVisCap` | `24` | Maximum stored vis per primal aspect in a node (range 1–100000). |
| `progression.wandVisBaseCap` | `100` | Base wand capacity per primal aspect (range 1–100000); Wand Capacity doubles it. |
| `gameplay.bonusBranchesEnabled` | `true` | Enables/disables Miner, Warrior, Fisher, and optional Create bonus effects and research. |
| `gameplay.spellsAffectPlayers` | `false` | Allows harmful entity-targeting spells to target other players when enabled. |
| `gameplay.bonusesAffectPvP` | `false` | Allows Warrior Guard's damage reduction to apply to PvP damage. |
| `gameplay.minerBonusCap` | `20` | Maximum additional ore-drop chance in percentage points (range 0–100). |

Config accessors provide documented defaults if queried before NeoForge finishes loading the config. Balance changes should update the schema comments, this table, and any affected gameplay tests together.

## Operator commands

All commands require permission level 2 or higher.

| Command | Effect |
|---|---|
| `/fredplenter knowledge <player> <aspect> <amount>` | Set one knowledge balance, bounded by the configured cap. Aspect is an id such as `flamma`. |
| `/fredplenter research <player> <namespace:id>` | Grant a registered research. |
| `/fredplenter research <player> <namespace:id> revoke` | Revoke a registered research. |
| `/fredplenter reset <player>` | Reset all Fredplenter progression state for that player. |
| `/fredplenter recover-note <player> <namespace:id>` | Clear the reservation for a lost note; it does not issue a replacement note. |

Progression mutations send an updated progress snapshot to the target player. Commands only accept research ids already registered by this mod.

## Optional bonuses and compatibility

- The fishing modifier can duplicate fish catches only; it does not duplicate treasure, equipment, or other catch categories.
- Create crafting rewards are limited to one reward per player per distinct output item to prevent cheap/reversible recipes from becoming a repeatable farm.
- Miner chance is constrained by the server config cap.
- Warrior Fury excludes the Ender Dragon, Wither, and Warden from its damage bonus.
- PvP damage reduction and player spell targeting are disabled by default.
- Verdure sends candidate plant growth through NeoForge crop-growth pre/post events. Protection mods that listen to these events can cancel compatible growth actions; this is event interoperability, not a hard dependency on a specific claim mod.
- The Create research definitions remain registered while Create is absent; the UI reports the missing dependency and gameplay integration stays inactive.

## Interface layout

The Arcane Table uses a fixed logical size of 360 × 270 GUI pixels instead of the prior tall single-screen layout. Research and aspect crafting occupy separate tabs; the FAQ is reachable from either tab. The note slot is at menu coordinates `(41, 136)`, the player inventory begins at y=181, and the hotbar is at y=239. These constants are declared in `ArcaneTableMenu`; the screen uses the same table height and positions its content relative to the container origin. A visual check in the actual game is still recommended at GUI scale 4 and with translated text.

## Tests and build

Run from the repository root:

```powershell
.\gradlew.bat test build
```

```sh
./gradlew test build
```

The JUnit suite covers registered paper-puzzle solvability and route validation, aspect conversion and capacity rules, compound-research gates, recipe-inheritance scoring, knowledge caps, one-time Create rewards, and the Table Mastery tile discount. GitHub Actions runs tests and builds the mod on pushes and pull requests. A successful Gradle build writes the distributable JAR under `build/libs/`; generated releases should be distributed through GitHub Releases, not committed under `dist/`.

## Current scope and follow-up

Research definitions, compound recipes, and most vanilla item mappings are Java-registered rather than fully data-driven. Optional UI integrations (JEI/EMI and Jade/WTHIT), advancements, a Patchouli guide, randomized puzzle boards, and explicit adapters for particular claim plugins are not currently included. These should be added as separately testable integrations rather than presented as existing behavior.
