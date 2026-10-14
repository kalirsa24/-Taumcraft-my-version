# Changelog

## Unreleased

### Research reliability and progression

- Validated all 32 registered research definitions against the aspect-link graph and hex board; repeated aspects and repeated cells are rejected.
- Bound paper research notes to their creating player, reserved one note per research, and restricted completion, retrieval, and learning to its owner.
- Replaced Shift-click mass conversion with a separate arm-then-confirm interaction.
- Capped research knowledge at 100 per aspect and distinguished it from node/wand vis in player-facing text.
- Added renewable knowledge from aura nodes after Aura Survey, with radius and rate improvements from Node Attunement and Resonant Work.
- Made research unlocks functional: compound creation gates, improved node-study rewards, wand charging/capacity, lower spell costs, scribing ink savings, paper recovery, and the Table Mastery tile discount.
- Limited the fishing bonus to fish items, preventing duplication of treasure or equipment.
- Limited Create Motus rewards to the first manual craft of each distinct Create output by a player.

### Server operations, mapping, and balance

- Added a server configuration for knowledge and vis caps, node-attunement rate and radius, bonus branches, miner bonus limits, PvP damage reduction, and player targeting by spells.
- Added permission-level-2 commands to set a player's aspect knowledge, grant or revoke registered research, reset progression, and clear a lost-note reservation.
- Added datapack item-tag mappings for modded items and optional recipe-based aspect inheritance after Aspect Almanac; explicit tags take precedence over inherited mappings.
- Replaced rail-name prefix matching with explicit vanilla item identifiers.
- Made first-scan rewards and Aspect Almanac recipe inheritance unlock through research, excluded the Ender Dragon, Wither, and Warden from Warrior Fury, and routed Verdure growth through NeoForge crop-growth events.
- Kept all bonus research definitions registered when Create is absent so the Codex graph does not shift between installations.

### Interface, tests, and documentation

- Reworked the Arcane Table into Research and Aspects tabs and reduced its logical height to 270 pixels.
- Removed unused puzzle definitions from aspect-cost research; only paper research carries a route puzzle, while Codex map icons use the first aspect cost where needed.
- Added tests for aspect conversion, puzzle tile cost calculation, recipe-aspect inheritance, and paper-only puzzle data alongside existing progression coverage.
- Replaced the old session-style README with a player installation and gameplay guide and expanded `docs/ARCHITECTURE.md` with resource flow, server settings, commands, mapping precedence, multiplayer boundaries, and explicitly unimplemented integrations.

### Verification and repository workflow

- Added unit coverage for puzzle solutions, conversion limits, research gates, renewable knowledge caps, one-time Create rewards, and Table Mastery costs.
- Added GitHub Actions CI to run Gradle tests and package the mod on pushes and pull requests.
