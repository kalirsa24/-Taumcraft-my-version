# Fredplenter

Fredplenter is an original magic and research mod for Minecraft 1.21.1 and NeoForge. It builds its own aspect vocabulary, research progression, scanning tools, aura nodes, and two-block Arcane Table. The project is inspired by the broad idea of magical research in Minecraft; its code, names, recipes, descriptions, models, and textures are original.

This README is both the player guide and a technical map of the current implementation. It documents the source files, the responsibilities of their main functions, the client/server message flows, the saved data, the localization setup, and the latest Arcane Table recipe-panel update.

## Contents

- [At a glance](#at-a-glance)
- [Player guide](#player-guide)
- [Aspect system and recipes](#aspect-system-and-recipes)
- [How aspect crafting works internally](#how-aspect-crafting-works-internally)
- [Arcane Table layout and controls](#arcane-table-layout-and-controls)
- [Research, notes, and the Codex](#research-notes-and-the-codex)
- [Scanning, aura nodes, and the wand](#scanning-aura-nodes-and-the-wand)
- [Saved progress and multiplayer](#saved-progress-and-multiplayer)
- [Code map](#code-map)
- [Recent implementation changes](#recent-implementation-changes)
- [Localization](#localization)
- [Dependencies and build](#dependencies-and-build)
- [Current limitations](#current-limitations)
- [License](#license)

## At a glance

| Area | Current implementation |
|---|---|
| Minecraft | 1.21.1 |
| Mod loader | NeoForge 21.1.x; project build targets 21.1.72 |
| Java | Java 21 |
| Mod id | `fredplenter` |
| Aspect count | 16: six primal and ten compound |
| Compound recipes | One shared in-code recipe catalogue |
| Aspect point maximum | 1,000,000 per aspect per player |
| Research nodes | 14 definitions in `ResearchRegistry` |
| Languages shipped | English (`en_us`) and Russian (`ru_ru`) |
| Server authority | Aspect creation, scanning rewards, note completion, and research progress are validated server-side |
| Extra runtime libraries | None declared in `build.gradle` beyond Minecraft/NeoForge-provided APIs |

The project uses NeoForge's deferred registries and custom network payloads. The client draws screens and sends requests; the server owns authoritative research progress and aspect balances. Player knowledge is stored separately for each UUID in world `SavedData`.

## Player guide

### 1. Make an Aspect Codex

Craft an Aspect Codex from a book, feather, and ink sac using the recipe in `src/main/resources/data/fredplenter/recipe/arcane_codex.json`. Right-click the Codex in the air to open the research map.

The map shows research nodes and their prerequisites. Select a node to inspect its translated title, description, research method, aspect cost, and prerequisites. The category rail filters the map, and the search box narrows the visible research list.

### 2. Get aspects

Hold an Aura Scanner or Resonance Wand in the main hand and hover over a vanilla item in an open inventory or container for about two seconds. That item type can reward aspects once per player. The Scanner can also study an Aura Node by using it on the node block.

The Resonance Wand can draw primal vis from an Aura Node. Stored wand vis and research aspect points are related to the same aspect vocabulary, but they are different storage systems: wand charges live on that wand item, while research points belong to the player's world-saved research state.

### 3. Create a research note

For a paper-method research:

1. Hold the Codex and open its research map.
2. Choose an available paper-method research.
3. Have one paper and an Inkwell with at least one ink charge in the inventory or offhand.
4. Press the research button. The server checks the Codex, prerequisites, materials, and available state; on success it consumes one paper and one ink charge and creates a note carrying that research id.
5. Put the note in the Arcane Table's note slot.

Scribing Tools are currently optional. The Inkwell is crafted from a glass bottle and ink sac. To refill it, hold the Inkwell in one hand and an ink sac in the other, then use the Inkwell. Its capacity is eight charges.

### 4. Solve the Arcane Table path

Select the matching research on the table and press **Begin**. The path has a fixed start, required middle aspect, and fixed end. Click the start cell, select a discovered aspect in the left aspect list, and click an adjacent empty hex to place that aspect. Continue with adjacent cells, making sure each next aspect is linked to the previous one. Pass through the required middle aspect and click the end anchor. The anchors are four hex steps apart, so even the shortest valid route contains five cells total: the two anchors and at least three placed aspect tiles.

The client checks the route for useful immediate feedback, but the server repeats the important checks before it accepts anything. Points are deducted only after the full path passes validation. Every intermediate tile costs one point of the aspect placed there; the start and end anchors cost nothing.

After successful completion, take the note out of the table, right-click it, and press **Learn** on its note screen. Learning is a separate server-validated step so a crafted or forged item cannot bypass research prerequisites.

Open **FAQ** on the right side of the table for an in-game, scrollable guide. It includes a small diagram of the example `Flatus → Procella → Unda` route, plus instructions for notes, paths, aspect recipes, and discovery.

### 5. Create compound aspects

Below the table UI is a grid of compound-aspect recipes. Each cell displays its two components and the result, with current point counts. Select a recipe and press **Create** to make one compound point. Shift-click a recipe cell, or use the Create button while holding Shift, to ask the server to create as many as the current balances and result capacity allow.

Cells are highlighted when both ingredients are known and the components have points available. A result does not need to be discovered beforehand: creating it for the first time unlocks it. Hovering a cell explains its recipe and identifies undiscovered components or missing points. The client highlight is only a preview; the server makes the final decision.

## Aspect system and recipes

The aspect enum in `src/main/java/com/fredplenter/aspect/Aspect.java` defines stable ids, colors, translation-key helpers, primal classification, and links between aspects. It does not define component pairs. The recipe catalogue in `src/main/java/com/fredplenter/aspect/AspectRecipe.java` is the single source of truth for compound-aspect inputs and outputs.

### Primal aspects

Primal aspects have no `AspectRecipe` result entry and are therefore treated as primal by `Aspect.isPrimal()`:

| Aspect id | Russian display name | English display name |
|---|---|---|
| `flamma` | Фламма (Огонь) | Flamma |
| `unda` | Унда (Вода) | Unda |
| `humus` | Хумус (Земля) | Humus |
| `flatus` | Флатус (Воздух) | Flatus |
| `lux` | Люкс (Свет) | Lux |
| `tenebrae` | Тенебрэ (Тьма) | Tenebrae |

The display names are resources, not strings hard-coded into the screen. See [Localization](#localization).

### Compound-aspect recipe catalogue

The order shown here is the order used by the recipe grid. Component order is not significant for matching: either component may be passed first.

| Result | Component 1 | Component 2 | Example |
|---|---|---|---|
| Vapor (`vapor`) | Flamma | Unda | Fire + Water = Steam |
| Limus (`limus`) | Unda | Humus | Water + Earth = Mud |
| Cinis (`cinis`) | Flamma | Humus | Fire + Earth = Ash |
| Procella (`procella`) | Flatus | Unda | Air + Water = Storm |
| Pulvis (`pulvis`) | Flatus | Humus | Air + Earth = Dust |
| Aestus (`aestus`) | Flamma | Vapor | Fire + Steam = Heat |
| Vita (`vita`) | Lux | Limus | Light + Mud = Life |
| Mors (`mors`) | Tenebrae | Cinis | Shadow + Ash = Death |
| Somnus (`somnus`) | Tenebrae | Procella | Shadow + Storm = Sleep |
| Motus (`motus`) | Flatus | Pulvis | Air + Dust = Motion |

The exact machine-readable source is the `RECIPES` list in `AspectRecipe.java`. `AspectRecipe.forResult(result)` performs result-to-recipe lookup; `AspectRecipe.forComponents(first, second)` searches inputs in either order; `matches` compares the inputs; `components()` returns the two inputs. The code derives compound classification, component lists, and pairwise links from these records instead of duplicating component pairs in the enum.

## How aspect crafting works internally

### Client-side selection and display

`ArcaneTableScreen` draws the recipe panel in `drawAspectRecipePanel`. It enumerates `AspectRecipe.all()`, so displayed recipe order and recipe data come from the same catalogue used by logic. Each recipe cell shows component icons, a result icon, and the three point values. Unknown component counts are shown as `?`; an undiscovered result is shown as `0` because first-time crafting is allowed.

`findRecipeAt` maps mouse coordinates back to a recipe cell. Clicking a cell selects its result; Shift-clicking a currently usable cell requests maximum creation immediately. The separate **Create** button requests one by default and maximum when Shift is held. `canCreate` is only a client-side convenience for selecting/highlighting/enabling controls: it checks whether both components are discovered, both input balances are positive, and the result is below the point cap. The result itself may still be undiscovered.

`renderRecipeTooltip` obtains the same `AspectRecipe` and shows:

- the localized result name;
- the formula;
- current input and result counts;
- which component is undiscovered or has no point available; and
- the Shift-click instruction when the recipe is currently usable.

The cell-rendering and hit-test coordinates intentionally share the same panel geometry. This alignment is important: the hit area must match the pixels drawn under the cursor.

### Request payload

When the player requests a creation, `ArcaneTableScreen.createSelectedAspect` sends `CreateAspectPayload(resultId, createMaximum)`. It sends the requested result id and one boolean; it does not send a client-computed cost, player balance, or claimed output count.

`NetworkHandler.registerPayloads` registers the payload as a play-to-server NeoForge custom payload. `CreateAspectPayload.handle` queues work on the game thread, requires a `ServerPlayer`, and requires the player's open menu to be an `ArcaneTableMenu`. It resolves the requested id against the `Aspect` enum and calls `ArcaneProgress.createAspect`.

### Authoritative server calculation

`ArcaneProgress.createAspect` independently:

1. Finds the recipe for the requested result in `AspectRecipe`.
2. Rejects a null, primal, or otherwise recipe-less result.
3. Retrieves that player's `PlayerState` and ensures primal aspects are known.
4. Requires both recipe components to be in that player's discovered-aspect set. The result does not need to be discovered yet.
5. Calculates the amount available as the smaller of the two component balances.
6. Caps that amount by the remaining capacity of the result, up to `MAX_ASPECT_POINTS` (1,000,000).
7. For a regular request, creates one if at least one is available. For a maximum request, creates the calculated maximum.
8. Deducts the same amount from each component, adds that amount to the result, marks the result discovered, marks the SavedData dirty, and returns the amount created.

For example, if a recipe has 7 component-A points, 3 component-B points, and room for at least 3 result points, a maximum request produces 3 result points and subtracts 3 from each input. The result is unlocked on this first creation. It does not subtract one input and then discover the other is insufficient: both balances are read and checked before either deduction.

When the server finishes, the payload sends a localized action-bar success/failure message and calls `NetworkHandler.sendProgress`. That lightweight update is delivered through `ProgressSyncPayload`; the client applies it in `ClientArcaneProgress`, increments its revision, and the open screen rebuilds its displayed points and cell availability. This is why the recipe panel is not allowed to update its own point counts optimistically.

### Single crafting request

The table uses `CreateAspectPayload` and `ArcaneProgress.createAspect` as the sole server-side aspect-crafting path. The payload identifies the selected result and whether to craft the maximum; the server resolves its recipe, checks discovered components and balances, performs deductions, and unlocks the result on first creation. There is no separate pair-based combination payload or method.

## Arcane Table layout and controls

### Screen geometry

The screen implementation is `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java`; its menu and actual inventory slot positions are in `src/main/java/com/fredplenter/menu/ArcaneTableMenu.java`.

| Element | Coordinate/value | Purpose |
|---|---:|---|
| Screen width | 360 px | Keeps room for the aspect list and parchment workspace |
| Screen height | 410 px | Adds room for the recipe panel and a gap before inventory |
| Aspect/sidebar width | 90 px | Aspect balances and note slot |
| Main work area | Remaining width after 90 px sidebar and margins | Research map or hex puzzle |
| Recipe panel top | y = 170 | Begins beneath the original tabletop workspace |
| Recipe panel height | 72 px | Heading plus two rows of five recipe cells |
| Inventory top | y = 260 | Recipe panel bottom (242) plus 18 px of spacing |
| Player main-inventory first row | menu y = 274 | 14 px inset below the inventory heading |
| Player hotbar | menu y = 332 | Three inventory rows plus normal row spacing |
| Research-note slot | menu x = 41, y = 146 | Dedicated server-backed table inventory slot |

`ArcaneTableMenu` owns the slot coordinates because it controls click and transfer behavior. `ArcaneTableScreen` uses the same `INVENTORY_TOP` value for the inventory frame and heading. The screen height and menu slot offsets are deliberately coordinated; changing only the drawing coordinates would make visual slots and clickable slots disagree.

The wooden-looking frame, parchment, inset panels, grid cells, hexagons, borders, icons, and small FAQ path diagram are drawn with Minecraft `GuiGraphics` fill/text calls and existing aspect colors/marks. No separate recipe-grid image file is currently required. The Arcane Table block itself has its own block texture under `src/main/resources/assets/fredplenter/textures/block/arcane_table.png`.

### Research and recipe screen states

The table UI has three principal content states:

1. **No selected research note:** shows a prompt to place a Codex-created note and the aspect-crafting hint in the main area. Recipe cells remain available below.
2. **A note is in the slot, no puzzle active:** displays the research map and selected-research details. The Begin button starts the selected paper research if it matches the note and is currently available.
3. **Puzzle active:** displays the hex board and research path. Reset clears the in-progress local path; Back cancels the active local puzzle state. Recipe creation controls are not built while the puzzle is active.

The **FAQ** button is added in `rebuildButtons` and opens `ArcaneTableFaqScreen`, preserving the existing table screen as its parent. FAQ uses translated text, scroll clipping, a scrollbar, and a small generated route diagram. Closing the FAQ returns to the same table session and keeps the table state intact.

### Recipe cell styling and feedback

The recipe row contains all ten compound recipes in a five-column, two-row arrangement. A result currently creatable by the local preview receives a green border; the selected recipe receives a gold highlight; the hovered recipe receives a lighter border. Component icons are bright when discovered and stocked, and the result icon brightens when it is discovered or can be created from the current component balances. Undiscovered result counts display as zero; unknown component counts remain hidden.

These visual states are advisory. A stale client snapshot, a modified packet, or a second player action cannot make the server accept insufficient or undiscovered inputs: `ArcaneProgress.createAspect` rechecks state at the moment of the operation.

## Research, notes, and the Codex

### Research record

`Research` is an immutable record containing an id, prerequisites, aspect costs, a three-aspect puzzle definition, method, category, and normalized map coordinates. Its title and description keys are derived from its id. `ResearchRegistry` is the single in-code registry used by the Codex, table UI, and server validation.

`ResearchRegistry.isAvailable` returns true only when the research is not already complete and every prerequisite id is complete. Costs are checked separately when starting an ASPECTS-method research.

### Registered research

The map positions below are normalized values in the Codex definition (`x`, `y`), not fixed screen pixels.

| Research id | Method | Prerequisites | Aspect cost, if immediate | Puzzle start → required middle → end |
|---|---|---|---|---|
| `basic_observation` | Paper | None | — | Flatus → Procella → Unda |
| `aspect_almanac` | Paper | `basic_observation` | — | Humus → Vita → Lux |
| `compound_aspects` | Paper | `aspect_almanac` | — | Flamma → Vapor → Unda |
| `careful_scribing` | Aspects | `basic_observation` | 2 Flatus + 2 Lux | Flatus → Motus → Lux |
| `aspect_mixture` | Aspects | `compound_aspects` | 3 Limus + 3 Cinis | Humus → Limus → Unda |
| `aura_survey` | Paper | `basic_observation` | — | Unda → Vapor → Flamma |
| `node_hunting` | Aspects | `aura_survey` | 3 Lux + 2 Procella | Lux → Vita → Humus |
| `node_attunement` | Paper | `node_hunting`, `aspect_mixture` | — | Tenebrae → Somnus → Flatus |
| `wand_attunement` | Aspects | `aura_survey` | 3 Flatus + 3 Motus + 3 Lux | Flatus → Motus → Lux |
| `wand_capacity` | Aspects | `wand_attunement` | 4 Pulvis + 4 Humus | Humus → Pulvis → Flatus |
| `deep_resonance` | Paper | `wand_capacity`, `node_attunement` | — | Flamma → Aestus → Lux |
| `resonant_work` | Aspects | `wand_attunement`, `aspect_almanac` | 4 Tenebrae + 4 Somnus + 4 Flatus | Tenebrae → Somnus → Flatus |
| `ink_and_paper` | Paper | `careful_scribing` | — | Unda → Limus → Humus |
| `table_mastery` | Aspects | `ink_and_paper`, `resonant_work` | 5 Cinis + 5 Motus | Flamma → Cinis → Humus |

The exact canonical values are in the static registrations in `ResearchRegistry.java`. This table is an explanatory copy: when adding or changing research, update the Java registration and the matching language entries as well.

### Paper-method research lifecycle

1. The client opens `ArcaneCodexScreen` and sends `RequestProgressPayload` so its view can use the latest learned research and balances.
2. The player selects a research and presses the Codex button. The client sends `BeginCodexResearchPayload` with only the research id.
3. The server requires the Codex in the player's inventory, resolves the id using `ResearchRegistry`, and calls `ArcaneProgress.beginCodexResearch`.
4. `beginCodexResearch` verifies availability. For PAPER it also verifies paper and ink, consumes one of each, creates a `ResearchNoteItem` stack with its research id and `Complete=false`, and adds it to the inventory (dropping it if the inventory cannot accept it).
5. The player inserts the note into the Arcane Table menu. The slot only accepts this registered research-note item.
6. The player begins and solves the table path. Completion marks the note's custom data as complete after server validation.
7. Right-clicking a note opens `ResearchNoteScreen`. Only a completed note gets an enabled **Learn** action. That action sends `LearnNotePayload`; the server rechecks the research and its prerequisites before saving completion.

An unfinished note is therefore not itself proof of a solved puzzle. Its per-stack data has `ResearchId` and `Complete`; the server verifies the item's id and completion state at each relevant step.

### Hex-board validation

`HexPosition` represents an axial hex coordinate `(q, r)`. It considers a coordinate on the board when `|q| <= 2`, `|r| <= 2`, and `|q + r| <= 2`; adjacency uses the three axial-distance components and requires distance exactly one. The current honeycomb contains 19 cells.

`ArcaneTableScreen.selectHex` manages the interactive path:

- Start is fixed at `(-2, 0)` and must be clicked first.
- End is fixed at `(2, 0)` and may be selected only if the prior cell is adjacent, the required middle aspect has appeared, and the last placed aspect links to the end aspect.
- An intermediate hex must be adjacent to the preceding one.
- A discovered sidebar aspect is selected as the aspect for the next intermediate cell.
- `Aspect.isLinkedTo` checks the relationship derived from the central recipe catalogue.
- A local point check prevents obviously unaffordable clicks.
- Clicking the most recent placed intermediate cell removes that last cell; Reset clears the route.
- On reaching a valid end, the client encodes entries as `aspectId@q,r`, separated by semicolons, and sends `CompleteResearchPayload`.

`CompleteResearchPayload.parseSolution` rejects blank or oversized input, malformed entries, unknown aspect ids, non-integer coordinates, and a route outside the allowed 3-to-19-entry range. The server-side `ArcaneProgress.completeResearch` then verifies:

1. the research exists and uses the PAPER method;
2. the registered puzzle has the expected three anchors;
3. the submitted aspect and coordinate lists have equal lengths;
4. first and last aspects match the puzzle's fixed ends;
5. the required middle aspect is included;
6. first and last coordinates match the fixed board anchors;
7. every cell is on the board and no coordinate is repeated;
8. every pair of sequential cells is adjacent;
9. every intermediate aspect is discovered;
10. every sequential aspect pair is linked; and
11. the player has enough points for every intermediate occurrence of each aspect.

Only after those checks does the server deduct the aggregated per-aspect intermediate-tile costs and mark SavedData dirty. The endpoints are not charged. Invalid submissions do not consume aspect points or complete the note.

## Scanning, aura nodes, and the wand

### Vanilla item scanning

`ClientScanHandler` observes the foreground render of open container screens. If the player holds the Aura Scanner or Resonance Wand in the main hand, it identifies the item stack under the pointer. A vanilla item id that has not already been studied starts a two-second timer. After the timer, the client sends `ScanItemPayload`; the server responds with `ScanResultPayload`, and the client retries if an acknowledgement is rejected or times out.

The server does not trust a client claim that it scanned something. `ScanItemPayload.handle` checks that the sender is a server player, that a scanner or wand is in the main hand, that the id uses the `minecraft` namespace, and that the specified item is actually present in an active open menu. Scans have a server-side 40-tick cooldown. `ArcaneProgress.markItemStudied` makes the reward one-time per player, adds the aspect points, records the discovered aspects, and marks SavedData dirty. Successful scans send a lightweight progress update rather than the complete item history.

`VanillaAspectMap.aspectsFor` uses an ordered set of explicit vanilla item paths and stable one-point rewards, plus fixed overrides for special cases such as lava buckets, water buckets, and bowls. Unmatched vanilla items use a stable fallback; non-vanilla ids receive no mapping. This is a lightweight playable mapping, not a curated compatibility database.

### Aura nodes and the Aura Scanner

Aura nodes are block entities with their own persistent `Vis` compound and initialization flag. On server ticks they initialize a deterministic selection of primal vis amounts from the node position and regenerate vis periodically, up to 24 per primal type. `getScanAspects` returns the node's present aspect types as research rewards.

Using the Aura Scanner on a node calls `ArcaneProgress.markNodeStudied` with dimension id and packed block position. This creates a per-player unique node key, so a player cannot repeatedly claim the same node scan. Scanning is server-side and the client receives a progress update on success.

Aura Nodes do not drop a placeable item when broken in Survival. This prevents moving a node to a new position to claim repeat scan rewards; Aura Node items remain available in Creative. Existing placed nodes and their saved data are unchanged.

The node block's generation configuration is under `src/main/resources/data/fredplenter/worldgen/` and its biome modifier is under `src/main/resources/data/fredplenter/neoforge/biome_modifier/`.

### Resonance Wand

The Wand transfers primal vis from the clicked Aura Node into the held wand, up to 100 units of each primal aspect (600 total). Stored values are custom item data and therefore travel with the item. The node is debited by `AuraNodeBlockEntity.extract`; wand and node data are updated on the server. The client tooltip shows total capacity, average fill, and scanning hints; the client tooltip handler can list the individual stored primal charges while Shift is held.

The wand has three vanilla-resource crafting recipes: the standard, amethyst/copper, and quartz/blaze-powder variants. Their data files are `arcane_wand.json`, `arcane_wand_amethyst.json`, and `arcane_wand_quartz.json`.

## Saved progress and multiplayer

### Server-side storage

`ArcaneProgress` extends Minecraft `SavedData`. `ArcaneProgress.get(ServerPlayer)` retrieves the single world-level data file named `fredplenter_arcane_progress` from the overworld's `DataStorage`. Inside it, each player is keyed by UUID and has an independent `PlayerState`.

Each `PlayerState` contains:

- `aspects`: an `EnumMap<Aspect, Integer>` of research points;
- `discoveredAspects`: an `EnumSet<Aspect>` of aspects the player knows;
- `completedResearch`: ids of learned research;
- `studiedItems`: vanilla item ids already scanned by that player; and
- `studiedNodes`: dimension-and-position keys already scanned by that player.

`PlayerState.save/load` serializes these values to NBT. Invalid UUID or resource-location strings are ignored rather than crashing a world load. Aspect balances load with bounds, and point mutations cap balances at `MAX_ASPECT_POINTS`.

For older worlds without a `DiscoveredAspects` list, load migration treats aspects with positive saved point totals as discovered. Primal aspects are always seeded as known. Thus the new discovered-aspect state can be added without erasing aspect knowledge already represented by older point balances.

### Client snapshot

`ArcaneProgress.createClientSnapshot` produces a lightweight NBT snapshot containing aspect amounts, discovered aspect ids, and completed research, or a full snapshot that also includes studied item ids. Studied node ids remain server-side. `NetworkHandler.sendProgress` sends lightweight updates after frequent changes; `sendFullProgress` is used at login, respawn, dimension change, and table opening.

`ClientArcaneProgress.load` replaces authoritative display fields and merges studied-item ids only when a full snapshot arrives; successful scan acknowledgements add the individual newly studied id. It notifies screens by incrementing its revision number. Client state is a display cache, not the authority.

All serverbound gameplay payloads use a shared per-player tick-based rate limiter, cleared on logout and respawn. Text identifiers in `CreateAspectPayload` are also length-bounded during decoding.

The use of UUID-keyed server data keeps two players' research points and discoveries distinct, including when both players use the same Arcane Table. Container/menu validity and server-side checks are also applied to table requests; this project is designed for dedicated-server execution as well as single-player integrated servers.

## Code map

Paths below are relative to this repository. Names in the “main responsibilities” column identify representative methods and data; they are not a promise that a class contains no other helpers.

### Mod entry point, registries, and game content

| File | Main responsibilities |
|---|---|
| `src/main/java/com/fredplenter/Fredplenter.java` | `@Mod("fredplenter")` entry point. Registers item, block, block-item, block-entity, menu, creative-tab, and payload registrations on the mod event bus. |
| `src/main/java/com/fredplenter/registry/ModItems.java` | Deferred registration for Codex, Wand, Scanner, Cache, Scribing Tools, Inkwell, and Research Note. |
| `src/main/java/com/fredplenter/registry/ModBlocks.java` | Deferred registration for the main Arcane Table half, decorative extension, Aura Node, and their placeable block items. |
| `src/main/java/com/fredplenter/registry/ModBlockEntities.java` | Block-entity type registrations. |
| `src/main/java/com/fredplenter/registry/ModMenuTypes.java` | Arcane Table menu type and client menu construction. |
| `src/main/java/com/fredplenter/registry/ModCreativeTab.java` | Creative tab and its displayed mod items. |
| `src/main/resources/META-INF/neoforge.mods.toml` | Mod metadata and required Minecraft/NeoForge version ranges. |
| `src/main/resources/data/fredplenter/recipe/` | Vanilla-resource crafting recipes for mod items. |
| `src/main/resources/assets/fredplenter/models/`, `blockstates/`, `textures/` | Item/block models, block-state models, and original pixel-art assets. |

### Aspect and research domain

| File | Main responsibilities |
|---|---|
| `src/main/java/com/fredplenter/aspect/Aspect.java` | Sixteen enum values, ids, colors, translation keys, primal classification via `AspectRecipe.forResult`, `combine`, `isLinkedTo`, and recipe-derived `getComponents`. |
| `src/main/java/com/fredplenter/aspect/AspectRecipe.java` | Canonical result-and-two-components recipe catalogue; `all`, `forResult`, `forComponents`, `matches`, `components`. |
| `src/main/java/com/fredplenter/aspect/AspectStack.java` | Aspect-plus-amount value used for research costs and scanned rewards, including NBT conversion. |
| `src/main/java/com/fredplenter/aspect/VanillaAspectMap.java` | Resource-location adapter for the explicit vanilla-item scan rules. |
| `src/main/java/com/fredplenter/aspect/VanillaAspectRules.java` | Ordered vanilla scan rules, explicit item overrides, and stable aspect amounts. |
| `src/main/java/com/fredplenter/research/Research.java` | Immutable research data: prerequisites, costs, method, puzzle, category, and normalized Codex position. |
| `src/main/java/com/fredplenter/research/ResearchRegistry.java` | All 14 research definitions and shared lookup/availability logic. |
| `src/main/java/com/fredplenter/research/HexPosition.java` | Hex axial-coordinate adjacency and legal-board bounds. |
| `src/main/java/com/fredplenter/research/ArcaneProgress.java` | Authoritative per-world/per-player SavedData, discovery, points, research completion, scans, puzzle validation, and aspect creation. |
| `src/main/java/com/fredplenter/research/ClientArcaneProgress.java` | Client-only progress snapshot/cache and revision tracking for GUI refresh. |

### Client screens and interaction

| File | Main responsibilities |
|---|---|
| `src/main/java/com/fredplenter/client/screen/ArcaneCodexScreen.java` | Cosmic research map, categories, search, selected research details, and Codex research request. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java` | 360×410 table screen, aspect sidebar, recipe grid, recipe hit-testing, tooltips, create button, hex puzzle, map, FAQ navigation, and screen-side message requests. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableFaqScreen.java` | Scrollable translated FAQ, clipping/scrollbar, and generated example path illustration. |
| `src/main/java/com/fredplenter/client/screen/ResearchNoteScreen.java` | Note details, route summary, learned/unfinished state, and Learn/Back actions. |
| `src/main/java/com/fredplenter/client/ClientHooks.java` | Opens the Codex screen and right-clicked research-note screen; asks the server for a fresh progress snapshot. |
| `src/main/java/com/fredplenter/client/ClientScanHandler.java` | Hover timer, scan acknowledgements/retries, and scanner/wand tooltip hints. |
| `src/main/java/com/fredplenter/client/ClientSetup.java` | Client-only screen and rendering setup. |
| `src/main/java/com/fredplenter/client/WandTooltipHandler.java` | Detailed wand contents tooltip behavior. |

### Blocks, items, and block entities

| File | Main responsibilities |
|---|---|
| `src/main/java/com/fredplenter/block/ArcaneTableBlock.java` | Main table half; opens menu, sends current progress, and drops stored contents when removed. |
| `src/main/java/com/fredplenter/block/ArcaneTableExtensionBlock.java` | Decorative second half and paired-block behavior. |
| `src/main/java/com/fredplenter/block/AuraNodeBlock.java` | Aura Node interaction, block entity ticking, light, and world representation. |
| `src/main/java/com/fredplenter/block/entity/ArcaneTableBlockEntity.java` | Persistent one-slot table inventory and link to the extension half. |
| `src/main/java/com/fredplenter/block/entity/AuraNodeBlockEntity.java` | Per-node primal vis storage, initialization, extraction, regeneration, scan rewards, and NBT persistence. |
| `src/main/java/com/fredplenter/item/ArcaneCodexItem.java` | Forms an Arcane Table by using the Codex on two adjacent crafting tables, with permissions and placement-event checks. |
| `src/main/java/com/fredplenter/item/ArcaneWandItem.java` | Transfers primal vis from nodes into item custom data and shows capacity/status tooltips. |
| `src/main/java/com/fredplenter/item/AuraScannerItem.java` | Scans aura-node block entities and awards their aspects once per player/node. |
| `src/main/java/com/fredplenter/item/ResearchNoteItem.java` | Stores research id and completion flag in item custom data; exposes translated tooltip details. |
| `src/main/java/com/fredplenter/item/InkwellItem.java` | Eight-charge ink storage, refill action, ink checks/consumption, and tooltip. |
| `src/main/java/com/fredplenter/item/ScribingToolsItem.java` | Optional writing-tool item and tooltip. |
| `src/main/java/com/fredplenter/item/AspectCacheItem.java` | Creative/test item that grants 100 points of each aspect and is consumed. |
| `src/main/java/com/fredplenter/item/ModItemCheck.java` | Checks whether an item id is present in an open menu for server-side scan validation. |
| `src/main/java/com/fredplenter/menu/ArcaneTableMenu.java` | Backing one-slot table menu, player slots, note validation, server begin/complete flow, transfer logic, and distance validity. |

### Network layer

| File | Direction and purpose |
|---|---|
| `src/main/java/com/fredplenter/network/NetworkHandler.java` | Registers all custom payload codecs/directions and sends player progress snapshots. |
| `src/main/java/com/fredplenter/network/RequestProgressPayload.java` | Client → server request for the caller's progress snapshot. |
| `src/main/java/com/fredplenter/network/ProgressSyncPayload.java` | Server → client NBT snapshot; loads into `ClientArcaneProgress`. |
| `src/main/java/com/fredplenter/network/ScanResultPayload.java` | Server → client scan acknowledgement and retry delay. |
| `src/main/java/com/fredplenter/network/ServerPacketRateLimiter.java` | Per-player tick-based limit for serverbound gameplay payloads; cleared on disconnect/respawn. |
| `src/main/java/com/fredplenter/network/BeginCodexResearchPayload.java` | Client → server request to begin Codex research. |
| `src/main/java/com/fredplenter/network/StartResearchPayload.java` | Client → server request to start solving a matching table note. |
| `src/main/java/com/fredplenter/network/CompleteResearchPayload.java` | Client → server serialized puzzle path; parses then invokes server validation. |
| `src/main/java/com/fredplenter/network/ResearchStartResultPayload.java` | Server → client acceptance/refusal of table puzzle start. |
| `src/main/java/com/fredplenter/network/ResearchResultPayload.java` | Server → client success/failure of puzzle completion. |
| `src/main/java/com/fredplenter/network/LearnNotePayload.java` | Client → server request to learn from a completed note. |
| `src/main/java/com/fredplenter/network/ScanItemPayload.java` | Client → server vanilla-item scan request; the server verifies tool and open-menu item. |
| `src/main/java/com/fredplenter/network/CreateAspectPayload.java` | Client → server request containing one result id and the normal/max mode; server looks up its recipe. |

### Main localization and data resources

| Path | Contents |
|---|---|
| `src/main/resources/assets/fredplenter/lang/en_us.json` | English names, gameplay messages, UI labels, FAQ text, and research titles/descriptions. |
| `src/main/resources/assets/fredplenter/lang/ru_ru.json` | Russian equivalents for the same keys. |
| `src/main/resources/data/fredplenter/recipe/` | Codex, Inkwell, Scanner, Scribing Tools, and three Wand recipe definitions. |
| `src/main/resources/data/fredplenter/worldgen/` | Configured and placed Aura Node world-generation features. |
| `src/main/resources/data/fredplenter/neoforge/biome_modifier/` | Biome modifier that adds Aura Node generation. |
| `src/main/resources/data/fredplenter/loot_table/blocks/` | Arcane Table and Aura Node loot definitions. |

## Recent implementation changes

This section records the changes that were made to implement the latest written update request, plus the related table-guide/localization work already present in this project.

### Centralized compound recipes

- **Added** `src/main/java/com/fredplenter/aspect/AspectRecipe.java`. The ten `(result, first component, second component)` triples now live together in `RECIPES`; `BY_RESULT` supports direct result lookup.
- **Changed** `src/main/java/com/fredplenter/aspect/Aspect.java`. Component fields were removed from each enum constant. `isPrimal()` asks whether the recipe catalogue has a result entry. `getComponents()` obtains the component list through `AspectRecipe.forResult`, and `isLinkedTo()` uses recipe-derived components rather than a second recipe table. The unused pair-to-result `combine()` helper was removed with the legacy crafting flow.
- **Connected** the table grid, cell tooltip, server craft logic, and aspect-link test to this shared representation. Recipe definitions are not repeated in the GUI or in `ArcaneProgress`.

### Server-authoritative result-based crafting

- **Added** `src/main/java/com/fredplenter/network/CreateAspectPayload.java`. The payload contains a result aspect id and a boolean `createMaximum`; the client does not submit a recipe, claimed input costs, balance, or output amount.
- **Registered** the payload in `src/main/java/com/fredplenter/network/NetworkHandler.java` as play-to-server.
- **Added** `ArcaneProgress.createAspect(UUID, Aspect, boolean)` in `src/main/java/com/fredplenter/research/ArcaneProgress.java`. It retrieves the recipe from the catalogue, validates the player's discovery and balances for both ingredients (not the result), calculates the safe amount from component balances and result capacity, applies equal input deductions/output addition, marks the result discovered on first creation, and marks SavedData dirty only on a successful craft.
- **Added** `MAX_ASPECT_POINTS` as a public constant for the screen's advisory availability indicator. The server also enforces this cap itself.
- **Removed** the obsolete `CombineAspectsPayload` and `ArcaneProgress.combineAspects` pair-based operation after confirming no active UI calls it. `CreateAspectPayload` is now the only aspect-crafting request.
- Removed the unused `ArcaneProgress.startResearch` paper-consumption helper after confirming there were no callers; actual note creation remains in `beginCodexResearch`, and starting a table puzzle remains in `ArcaneTableMenu.beginResearch`.

### Crafting and scan-mapping corrections

- Fixed first-time compound aspect creation on both sides. The client enables a recipe when its components are discovered and stocked, without requiring the result to be known. The server repeats the component checks and discovers the result only after successfully transferring points. A new result displays `0` points instead of `?`; the obsolete result-locked tooltip was removed.
- Corrected `VanillaAspectMap` substring collisions: `ice` now matches the complete path or ice names with a path boundary, `light` is no longer a broad fragment that captures colored blocks, and `axe` only matches the `_axe` suffix.
- Updated Russian and English FAQ text with the actual recipe-panel creation controls and an explanation that a valid start-to-end research route needs at least five cells total (three placed tiles). Removed localization strings used only by the deleted pair-crafting request.

### Recipe grid and controls

- **Changed** `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java`: removed the UI's two-input selection path in favor of selecting a recipe result; added a 10-cell recipe grid, visual counts, selected/hovered/available states, component/result icons, localized recipe tooltips, and a Create button.
- A standard Create request asks for one output. Shift-clicking an available recipe cell or using Create while Shift is held asks for the maximum.
- Recipe availability in the GUI is only an early visual check; creation is always recalculated by `ArcaneProgress` on the server.
- The recipe hit-test was aligned with the drawn grid so pointer coordinates target the visible cell.
- Unknown component point counts are displayed as `?`; an undiscovered result displays `0`, and recipe tooltips describe undiscovered components or missing component points.

### Increased table and inventory layout

- Increased the Arcane Table screen height from 320 to 410 pixels while retaining a 360-pixel width.
- Added a 72-pixel recipe panel at screen y=170, followed by an 18-pixel gap; the inventory frame and heading now start at y=260.
- Changed `ArcaneTableMenu` player inventory rows to start at y=274 and hotbar to y=332. Screen and menu coordinates are separate responsibilities and must remain coordinated.
- The table note slot remains at x=41, y=146; the new recipe panel does not overlap it.

### Table FAQ and display readability

- Added `src/main/java/com/fredplenter/client/screen/ArcaneTableFaqScreen.java`, a separate FAQ screen with wheel scrolling, clipping, a scrollbar, translated explanatory sections, and an in-game route diagram.
- Added a right-side FAQ button in `ArcaneTableScreen`; returning from FAQ restores the same parent screen instead of closing the table.
- Removed the old long list of all aspect recipes from the tiny central workspace. The main table view now shows a short instruction and the persistent recipe panel carries recipes.
- Simplified puzzle feedback to short status messages and a separate required-middle label, avoiding wrapped status text overlapping other controls.
- Added the matching FAQ, recipe-panel, recipe-state, creation-result, and validation-message translations to both locale files.
- Updated the player guide above and the current gameplay description here so the instructions match actual input rules and server validation.

### Translation coverage and compatibility

- Kept UI strings in translation resources and used `Component.translatable` in the UI rather than hard-coding Russian or English screen text.
- The shipped `ru_ru` and `en_us` resources have matching key sets. Minecraft selects the active resource according to the user's language setting; the mod itself does not add a separate language selector.
- Existing `SavedData` without a `DiscoveredAspects` list is migrated by inferring discovery from positive saved aspect balances. This is implemented in `ArcaneProgress.PlayerState.load`.
- Language resource updates also include direct message keys used by node descriptions and aspect crafting outcomes, not only visible screen labels.

### Current source line map

The following are the current line locations for the primary implementation points described above. They identify where to start reading the source; they are not a patch-format diff, and line numbers can shift if later edits are made.

| Source file | Current lines | What is implemented there |
|---|---:|---|
| `src/main/java/com/fredplenter/aspect/AspectRecipe.java` | 7-42 | `AspectRecipe` record, canonical ten-entry list, result index, component lookup, order-independent match, component accessor. |
| `src/main/java/com/fredplenter/aspect/Aspect.java` | 62-91 | Primal classification from catalogue, link relationship, and `getComponents` delegate to catalogue data. |
| `src/main/java/com/fredplenter/research/ArcaneProgress.java` | 119-146 | Result-based `createAspect`; server-side component discovery and point checks, amount calculation, point deductions/grant, result discovery, and dirty marking. |
| `src/main/java/com/fredplenter/research/ArcaneProgress.java` | 200-249 | Research-path server validation and charging of intermediate cells after every validation succeeds. |
| `src/main/java/com/fredplenter/research/ArcaneProgress.java` | 265-276, 350-404 | Client snapshot and persistent `PlayerState` data, including old-save discovered-aspect migration. |
| `src/main/java/com/fredplenter/network/CreateAspectPayload.java` | 15-51 | Result-id/max-mode codec, server-player/open-menu checks, authoritative progress call, feedback, and progress resynchronization. |
| `src/main/java/com/fredplenter/network/NetworkHandler.java` | 19-61 | Payload registration, including `CreateAspectPayload` as a play-to-server message. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java` | 28-34 | Screen dimensions, recipe-panel position/height, inventory-gap calculation, and hex-board drawing constants. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java` | 116-181 | Rebuilding GUI buttons, availability state, Create action and Shift behavior, FAQ button. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java` | 284-330 | Creation request, recipe-cell selection, Shift-click handling, aspect selection, and mouse routing. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java` | 377-394 | Inventory frame and heading placement using the shared `INVENTORY_TOP` value. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java` | 429-562 | Recipe grid drawing, point labels, aligned hit-testing, local availability preview, and missing-component tooltips. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java` | 628-638 | Shorter path details and required-middle feedback. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableScreen.java` | 836-860 | Brief no-note crafting instructions rather than the old crowded, full recipe text dump. |
| `src/main/java/com/fredplenter/menu/ArcaneTableMenu.java` | 28-31, 75-84 | Note slot constants and matching player inventory/hotbar slot coordinates. |
| `src/main/java/com/fredplenter/client/screen/ArcaneTableFaqScreen.java` | 11-151 | FAQ sections, screen sizing, clipping, generated path diagram, scrollbar and mouse-wheel scrolling. |
| `src/main/resources/assets/fredplenter/lang/en_us.json` | Search for `recipe_panel`, `faq_title`, and `aspect_created` | English recipe-panel, FAQ, and server-feedback strings. |
| `src/main/resources/assets/fredplenter/lang/ru_ru.json` | Search for `recipe_panel`, `faq_title`, and `aspect_created` | Matching Russian recipe-panel, FAQ, and server-feedback strings. |

## Localization

All player-visible interface text, aspect names, research names/descriptions, item names, and feedback should be added to both:

- `src/main/resources/assets/fredplenter/lang/en_us.json`
- `src/main/resources/assets/fredplenter/lang/ru_ru.json`

Code constructs a translatable component using a key such as `gui.fredplenter.recipe_panel`; the active Minecraft language resource resolves that key. The project currently ships English and Russian. If a future locale is selected without a Fredplenter locale file, Minecraft's own language fallback behavior applies; this README does not claim that all languages have dedicated translations.

When adding a translatable string:

1. Add the identical key to both locale JSON files.
2. Translate the value into the corresponding language; keep `%s` placeholders in the same order and count as the code expects.
3. Use `Component.translatable`/`Component.translatable(key, args...)` in Java instead of hard-coded display text.
4. Validate both JSON files and compare their key sets.
5. Verify the full key is present in both resources; JSON can be valid while a key is missing.

## Dependencies and build

### Build/runtime dependencies

The dependency and build configuration is in `build.gradle`, `gradle.properties`, `settings.gradle`, and the checked-in Gradle wrapper:

- Java toolchain: **21**.
- Minecraft: **1.21.1**.
- NeoForge: **21.1.72** in `gradle.properties`; mod metadata permits the declared NeoForge range there.
- NeoForge Gradle ModDev plugin: **2.0.78** in `build.gradle`.
- Parchment mappings: **2024.11.17** for Minecraft 1.21.1.
- Gradle wrapper: **8.10.2**.
- Maven repositories: Maven Central, Parchment, and NeoForge releases.
- No extra third-party Java runtime dependency is declared in the `dependencies` block. Minecraft and NeoForge APIs supply the game/loader classes.

`src/main/resources/META-INF/neoforge.mods.toml` declares Minecraft and NeoForge as required runtime dependencies. The versions there are filtered from `gradle.properties` during `processResources`.

### Build and launch

From the project root in PowerShell:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

The development server launch is configured in `build.gradle` and can be run with:

```powershell
.\gradlew.bat runServer
```

The normal build JAR is written to `build/libs/`. A separately copied downloadable JAR is kept at `dist/fredplenter-0.0.1.jar`; after building, copy the freshly built JAR into `dist/` if the downloadable copy should match it.

There are currently no Java test sources (`compileTestJava` and `test` report `NO-SOURCE` during the Gradle build). A successful `build` verifies compilation, resource processing, and packaging; it is not a substitute for a multiplayer or in-game manual test.

## Current limitations

- Item scanning currently maps vanilla item identifiers only. Modded items are reported as not studyable.
- Vanilla item mappings are deterministic string-family heuristics, not an exhaustive curated mapping.
- The recipe set is fixed in Java code; it is centralized, but it is not yet loaded from datapacks.
- Only `en_us` and `ru_ru` translations are shipped by this project.
- Scribing Tools exist but are optional for creating paper research notes.
- The research tree is a starter progression, not a complete content catalog.
- Essentia jars/transport, infusion altar, golems, and corruption/taint are deferred systems.
- The current GUI supports player feedback and visually marks likely-available recipes, but the server remains the final authority for every state-changing operation.

## License

Licensed under the [MIT License](LICENSE).
