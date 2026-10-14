package com.fredplenter.research;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectRecipe;
import com.fredplenter.aspect.AspectStack;
import com.fredplenter.registry.ModItems;
import com.fredplenter.item.InkwellItem;
import com.fredplenter.item.ResearchNoteItem;
import com.fredplenter.FredplenterConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ArcaneProgress extends SavedData {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArcaneProgress.class);
    private static final String DATA_NAME = "fredplenter_arcane_progress";
    public static final int MAX_ASPECT_POINTS = 100;
    private static final ResourceLocation COMPOUND_ASPECTS =
            ResourceLocation.fromNamespaceAndPath("fredplenter", "compound_aspects");
    private static final ResourceLocation ASPECT_MIXTURE =
            ResourceLocation.fromNamespaceAndPath("fredplenter", "aspect_mixture");
    private static final ResourceLocation NODE_HUNTING =
            ResourceLocation.fromNamespaceAndPath("fredplenter", "node_hunting");
    private static final ResourceLocation CAREFUL_SCRIBING =
            ResourceLocation.fromNamespaceAndPath("fredplenter", "careful_scribing");
    private static final ResourceLocation TABLE_MASTERY =
            ResourceLocation.fromNamespaceAndPath("fredplenter", "table_mastery");
    private static final ResourceLocation INK_AND_PAPER =
            ResourceLocation.fromNamespaceAndPath("fredplenter", "ink_and_paper");
    private static final Factory<ArcaneProgress> FACTORY =
            new Factory<>(ArcaneProgress::new, ArcaneProgress::load, DataFixTypes.LEVEL);

    private final Map<UUID, PlayerState> players = new HashMap<>();

    public static ArcaneProgress get(ServerPlayer player) {
        return player.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public int getAspectPoints(UUID playerId, Aspect aspect) {
        return state(playerId).aspects.getOrDefault(aspect, 0);
    }

    public Map<Aspect, Integer> getAspectSnapshot(UUID playerId) {
        return Map.copyOf(state(playerId).aspects);
    }

    public Set<ResourceLocation> getCompletedResearch(UUID playerId) {
        return Set.copyOf(state(playerId).completedResearch);
    }

    public Set<ResourceLocation> getStudiedItems(UUID playerId) {
        return Set.copyOf(state(playerId).studiedItems);
    }

    public boolean hasStudiedItem(UUID playerId, ResourceLocation itemId) {
        return state(playerId).studiedItems.contains(itemId);
    }

    public boolean markCreateItemCrafted(UUID playerId, ResourceLocation itemId) {
        PlayerState state = state(playerId);
        if (!state.createItemsCrafted.add(itemId)) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean markItemStudied(UUID playerId, ResourceLocation itemId, List<AspectStack> rewards) {
        return markItemStudied(playerId, itemId, rewards, false);
    }

    public boolean markItemStudied(UUID playerId, ResourceLocation itemId, List<AspectStack> rewards,
                                   boolean observationBonus) {
        PlayerState state = state(playerId);
        if (!state.studiedItems.add(itemId)) {
            return false;
        }
        for (int index = 0; index < rewards.size(); index++) {
            AspectStack reward = rewards.get(index);
            addPoints(state, reward.aspect(), reward.amount() + (observationBonus && index == 0 ? 1 : 0));
            state.discoveredAspects.add(reward.aspect());
        }
        ensurePrimals(state);
        setDirty();
        return true;
    }

    public boolean markNodeStudied(UUID playerId, ResourceLocation dimension, long packedPos, List<AspectStack> rewards) {
        PlayerState state = state(playerId);
        String nodeKey = dimension + ":" + Long.toUnsignedString(packedPos);
        if (!state.studiedNodes.add(nodeKey)) {
            return false;
        }
        int multiplier = state.completedResearch.contains(NODE_HUNTING) ? 2 : 1;
        for (AspectStack reward : rewards) {
            addPoints(state, reward.aspect(), reward.amount() * multiplier);
            state.discoveredAspects.add(reward.aspect());
        }
        ensurePrimals(state);
        setDirty();
        return true;
    }

    public void addAspectPoints(UUID playerId, Aspect aspect, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Aspect points must be positive");
        }
        PlayerState state = state(playerId);
        addPoints(state, aspect, amount);
        state.discoveredAspects.add(aspect);
        ensurePrimals(state);
        setDirty();
    }

    public int addRenewableKnowledge(UUID playerId, Aspect aspect, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Knowledge amount must be positive");
        }
        PlayerState state = state(playerId);
        int before = state.aspects.getOrDefault(aspect, 0);
        addPoints(state, aspect, amount);
        int added = state.aspects.getOrDefault(aspect, 0) - before;
        if (added > 0) {
            state.discoveredAspects.add(aspect);
            ensurePrimals(state);
            setDirty();
        }
        return added;
    }

    public boolean hasDiscovered(UUID playerId, Aspect aspect) {
        PlayerState state = state(playerId);
        ensurePrimals(state);
        return state.discoveredAspects.contains(aspect);
    }

    public Set<Aspect> getDiscovered(UUID playerId) {
        PlayerState state = state(playerId);
        ensurePrimals(state);
        return Set.copyOf(state.discoveredAspects);
    }

    public static void announceNewAspects(ServerPlayer player, Set<Aspect> before) {
        Set<Aspect> after = get(player).getDiscovered(player.getUUID());
        for (Aspect aspect : Aspect.values()) {
            if (after.contains(aspect) && !before.contains(aspect)) {
                player.displayClientMessage(Component.translatable("message.fredplenter.aspect_discovered",
                        Component.translatable(aspect.getTranslationKey())), false);
            }
        }
    }

    public int createAspect(UUID playerId, Aspect result, boolean createMaximum) {
        AspectRecipe recipe = result == null ? null : AspectRecipe.forResult(result);
        if (recipe == null) {
            return 0;
        }

        PlayerState state = state(playerId);
        ensurePrimals(state);
        if (!AspectCraftingRules.canCreate(recipe, state.completedResearch.contains(COMPOUND_ASPECTS),
                state.completedResearch.contains(ASPECT_MIXTURE))) {
            return 0;
        }
        if (!state.discoveredAspects.contains(recipe.first())
                || !state.discoveredAspects.contains(recipe.second())) {
            return 0;
        }

        int amount = AspectConversion.amount(
                state.aspects.getOrDefault(recipe.first(), 0),
                state.aspects.getOrDefault(recipe.second(), 0),
                state.aspects.getOrDefault(result, 0),
                FredplenterConfig.knowledgeCap(),
                createMaximum);
        if (amount <= 0) {
            return 0;
        }

        addPoints(state, recipe.first(), -amount);
        addPoints(state, recipe.second(), -amount);
        addPoints(state, result, amount);
        state.discoveredAspects.add(result);
        setDirty();
        return amount;
    }

    public boolean hasResearch(UUID playerId, ResourceLocation researchId) {
        return state(playerId).completedResearch.contains(researchId);
    }

    public void setAspectPoints(UUID playerId, Aspect aspect, int amount) {
        PlayerState state = state(playerId);
        state.aspects.put(aspect, Math.max(0, Math.min(FredplenterConfig.knowledgeCap(), amount)));
        if (amount > 0) {
            state.discoveredAspects.add(aspect);
        }
        ensurePrimals(state);
        setDirty();
    }

    public void resetPlayer(UUID playerId) {
        players.remove(playerId);
        setDirty();
    }

    public boolean setResearch(UUID playerId, ResourceLocation researchId, boolean learned) {
        if (ResearchRegistry.get(researchId) == null) {
            return false;
        }
        PlayerState state = state(playerId);
        boolean changed = learned
                ? state.completedResearch.add(researchId)
                : state.completedResearch.remove(researchId);
        if (!learned) {
            state.pendingResearchNotes.remove(researchId);
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean recoverResearchReservation(UUID playerId, ResourceLocation researchId) {
        PlayerState state = state(playerId);
        boolean recovered = state.pendingResearchNotes.remove(researchId);
        if (recovered) {
            setDirty();
        }
        return recovered;
    }

    public boolean canStartResearch(UUID playerId, Research research) {
        PlayerState state = state(playerId);
        if (!ResearchRegistry.isAvailable(research, state.completedResearch)) {
            return false;
        }
        for (AspectStack cost : research.cost()) {
            if (state.aspects.getOrDefault(cost.aspect(), 0) < cost.amount()) {
                return false;
            }
        }
        return true;
    }

    public boolean beginCodexResearch(ServerPlayer player, ResourceLocation researchId) {
        Research research = ResearchRegistry.get(researchId);
        if (research == null || !canStartResearch(player.getUUID(), research)) {
            return false;
        }

        PlayerState state = state(player.getUUID());
        if (research.method() == Research.Method.PAPER) {
            Inventory inventory = player.getInventory();
            boolean freeInk = state.completedResearch.contains(CAREFUL_SCRIBING)
                    && (hasItem(inventory, ModItems.SCRIBING_TOOLS.get())
                    || inventory.offhand.stream().anyMatch(stack -> stack.is(ModItems.SCRIBING_TOOLS.get())));
            if (state.pendingResearchNotes.contains(researchId)
                    || hasResearchNote(player, researchId)
                    || !hasItem(inventory, Items.PAPER) || !freeInk && !hasInk(inventory)) {
                return false;
            }
            if (!consumeOne(inventory, Items.PAPER)) {
                LOGGER.warn("Could not consume paper for research {} after validating player {}",
                        researchId, player.getUUID());
                return false;
            }
            if (!freeInk && !InkwellItem.consumeInk(player)) {
                ItemStack paper = new ItemStack(Items.PAPER);
                if (!inventory.add(paper)) {
                    player.drop(paper, false);
                }
                LOGGER.warn("Could not consume inkwell ink for research {} after validating player {}",
                        researchId, player.getUUID());
                return false;
            }
            ItemStack note = ModItems.RESEARCH_NOTE.get().createFor(researchId, player.getUUID());
            if (!inventory.add(note)) {
                player.drop(note, false);
            }
            state.pendingResearchNotes.add(researchId);
        } else {
            for (AspectStack cost : research.cost()) {
                if (state.aspects.getOrDefault(cost.aspect(), 0) < cost.amount()) {
                    return false;
                }
            }
            for (AspectStack cost : research.cost()) {
                addPoints(state, cost.aspect(), -cost.amount());
            }
            state.completedResearch.add(researchId);
        }
        setDirty();
        return true;
    }

    public boolean completeResearch(ServerPlayer player, ResourceLocation researchId,
                                    List<Aspect> solution, List<HexPosition> path) {
        Research research = ResearchRegistry.get(researchId);
        if (research == null || research.method() != Research.Method.PAPER
                || !ResearchPuzzleSolver.isValidSequence(research, solution, path)) {
            return false;
        }

        PlayerState state = state(player.getUUID());
        if (!ResearchRegistry.isAvailable(research, state.completedResearch)) {
            return false;
        }
        ensurePrimals(state);
        boolean tableMastery = state.completedResearch.contains(TABLE_MASTERY);
        for (int index = 0; index < solution.size(); index++) {
            Aspect aspect = solution.get(index);
            if (index > 0 && index < solution.size() - 1) {
                if (!state.discoveredAspects.contains(aspect)) {
                    return false;
                }
            }
        }

        Map<Aspect, Integer> placedCost = ResearchPuzzleCosts.aspectCosts(solution, tableMastery);
        for (Map.Entry<Aspect, Integer> cost : placedCost.entrySet()) {
            if (state.aspects.getOrDefault(cost.getKey(), 0) < cost.getValue()) {
                return false;
            }
        }
        placedCost.forEach((aspect, amount) -> addPoints(state, aspect, -amount));
        setDirty();
        return true;
    }

    public boolean learnFromNote(ServerPlayer player, ItemStack note) {
        UUID playerId = player.getUUID();
        ResourceLocation researchId = ResearchNoteItem.researchId(note);
        Research research = ResearchRegistry.get(researchId);
        boolean reservedNote = ResearchNoteItem.hasReservation(note);
        if (research == null || research.method() != Research.Method.PAPER
                || !ResearchNoteItem.isComplete(note)
                || !ResearchNoteItem.claimIfUnowned(note, playerId)
                || !ResearchNoteItem.isOwnedBy(note, playerId)) {
            return false;
        }
        PlayerState state = state(playerId);
        if (reservedNote && !state.pendingResearchNotes.contains(researchId)
                || !ResearchRegistry.isAvailable(research, state.completedResearch)) {
            return false;
        }
        boolean reclaimPaper = state.completedResearch.contains(INK_AND_PAPER);
        state.completedResearch.add(researchId);
        state.pendingResearchNotes.remove(researchId);
        if (reclaimPaper) {
            ItemStack paper = new ItemStack(Items.PAPER);
            if (!player.getInventory().add(paper)) {
                player.drop(paper, false);
            }
            player.sendSystemMessage(Component.translatable("message.fredplenter.paper_reclaimed"));
        }
        setDirty();
        return true;
    }

    public boolean reserveResearchNote(UUID playerId, ItemStack note) {
        ResourceLocation researchId = ResearchNoteItem.researchId(note);
        Research research = ResearchRegistry.get(researchId);
        if (research == null || research.method() != Research.Method.PAPER
                || !ResearchNoteItem.claimIfUnowned(note, playerId)
                || !ResearchNoteItem.isOwnedBy(note, playerId)) {
            return false;
        }
        PlayerState state = state(playerId);
        if (state.completedResearch.contains(researchId)) {
            return false;
        }
        state.pendingResearchNotes.add(researchId);
        ResearchNoteItem.markReserved(note);
        setDirty();
        return true;
    }

    public static boolean hasResearchNote(ServerPlayer player, ResourceLocation researchId) {
        Inventory inventory = player.getInventory();
        return inventory.items.stream().anyMatch(stack ->
                stack.is(ModItems.RESEARCH_NOTE.get())
                        && researchId.equals(ResearchNoteItem.researchId(stack)))
                || inventory.armor.stream().anyMatch(stack ->
                stack.is(ModItems.RESEARCH_NOTE.get())
                        && researchId.equals(ResearchNoteItem.researchId(stack)))
                || inventory.offhand.stream().anyMatch(stack ->
                stack.is(ModItems.RESEARCH_NOTE.get())
                        && researchId.equals(ResearchNoteItem.researchId(stack)));
    }

    public static boolean hasPendingResearchNote(ServerPlayer player, ResourceLocation researchId) {
        return get(player).state(player.getUUID()).pendingResearchNotes.contains(researchId);
    }

    private static boolean hasResearchNote(ItemStack stack, ResourceLocation researchId) {
        return stack.is(ModItems.RESEARCH_NOTE.get())
                && researchId.equals(ResearchNoteItem.researchId(stack));
    }

    public static boolean abandonResearchNote(ServerPlayer player, ItemStack note) {
        ResourceLocation researchId = ResearchNoteItem.researchId(note);
        if (researchId == null || !ResearchNoteItem.claimIfUnowned(note, player.getUUID())
                || !ResearchNoteItem.isOwnedBy(note, player.getUUID())) {
            return false;
        }
        ArcaneProgress progress = get(player);
        PlayerState state = progress.state(player.getUUID());
        if (ResearchNoteItem.hasReservation(note) && !state.pendingResearchNotes.contains(researchId)) {
            return false;
        }
        state.pendingResearchNotes.remove(researchId);
        progress.setDirty();
        note.shrink(1);
        return true;
    }

    public CompoundTag createClientSnapshot(UUID playerId, boolean fullSnapshot) {
        PlayerState state = state(playerId);
        CompoundTag snapshot = new CompoundTag();
        CompoundTag aspects = new CompoundTag();
        for (Aspect aspect : Aspect.values()) {
            aspects.putInt(aspect.getId(), state.aspects.getOrDefault(aspect, 0));
        }
        snapshot.put("Aspects", aspects);
        snapshot.put("CompletedResearch", stringList(state.completedResearch.stream().map(ResourceLocation::toString).toList()));
        if (fullSnapshot) {
            snapshot.put("StudiedItems", stringList(state.studiedItems.stream().map(ResourceLocation::toString).toList()));
        }
        snapshot.put("DiscoveredAspects",
                stringList(getDiscovered(playerId).stream().map(Aspect::getId).toList()));
        return snapshot;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag playerTags = new CompoundTag();
        for (Map.Entry<UUID, PlayerState> entry : players.entrySet()) {
            playerTags.put(entry.getKey().toString(), entry.getValue().save());
        }
        tag.put("Players", playerTags);
        return tag;
    }

    private static ArcaneProgress load(CompoundTag tag, HolderLookup.Provider registries) {
        ArcaneProgress progress = new ArcaneProgress();
        CompoundTag playerTags = tag.getCompound("Players");
        for (String key : playerTags.getAllKeys()) {
            try {
                progress.players.put(UUID.fromString(key), PlayerState.load(playerTags.getCompound(key)));
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Ignoring malformed Fredplenter player data key '{}'", key, exception);
            }
        }
        return progress;
    }

    private PlayerState state(UUID playerId) {
        PlayerState state = players.computeIfAbsent(playerId, ignored -> new PlayerState());
        ensurePrimals(state);
        return state;
    }

    private static void ensurePrimals(PlayerState state) {
        for (Aspect aspect : Aspect.values()) {
            if (aspect.isPrimal()) {
                state.discoveredAspects.add(aspect);
            }
        }
    }

    private static void addPoints(PlayerState state, Aspect aspect, int amount) {
        int current = state.aspects.getOrDefault(aspect, 0);
        state.aspects.put(aspect, Math.max(0,
                Math.min(FredplenterConfig.knowledgeCap(), current + amount)));
    }

    private static boolean hasItem(Inventory inventory, net.minecraft.world.item.Item item) {
        return inventory.items.stream().anyMatch(stack -> stack.is(item));
    }

    private static boolean hasInk(Inventory inventory) {
        return inventory.items.stream().anyMatch(InkwellItem::hasInk)
                || inventory.offhand.stream().anyMatch(InkwellItem::hasInk);
    }

    private static boolean consumeOne(Inventory inventory, net.minecraft.world.item.Item item) {
        for (ItemStack stack : inventory.items) {
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static ListTag stringList(List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    private static final class PlayerState {
        private final EnumMap<Aspect, Integer> aspects = new EnumMap<>(Aspect.class);
        private final Set<ResourceLocation> completedResearch = new HashSet<>();
        private final Set<ResourceLocation> pendingResearchNotes = new HashSet<>();
        private final Set<ResourceLocation> studiedItems = new HashSet<>();
        private final Set<ResourceLocation> createItemsCrafted = new HashSet<>();
        private final Set<String> studiedNodes = new HashSet<>();
        private final Set<Aspect> discoveredAspects = EnumSet.noneOf(Aspect.class);

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            CompoundTag aspectTag = new CompoundTag();
            for (Map.Entry<Aspect, Integer> entry : aspects.entrySet()) {
                aspectTag.putInt(entry.getKey().getId(), entry.getValue());
            }
            tag.put("Aspects", aspectTag);
            tag.put("CompletedResearch", stringList(completedResearch.stream().map(ResourceLocation::toString).toList()));
            tag.put("StudiedItems", stringList(studiedItems.stream().map(ResourceLocation::toString).toList()));
            tag.put("CreateItemsCrafted",
                    stringList(createItemsCrafted.stream().map(ResourceLocation::toString).toList()));
            tag.put("StudiedNodes", stringList(new ArrayList<>(studiedNodes)));
            tag.put("DiscoveredAspects",
                    stringList(discoveredAspects.stream().map(Aspect::getId).toList()));
            tag.put("PendingResearchNotes",
                    stringList(pendingResearchNotes.stream().map(ResourceLocation::toString).toList()));
            return tag;
        }

        private static PlayerState load(CompoundTag tag) {
            PlayerState state = new PlayerState();
            CompoundTag aspectTag = tag.getCompound("Aspects");
            for (Aspect aspect : Aspect.values()) {
                int amount = aspectTag.getInt(aspect.getId());
                if (amount > 0) {
                    state.aspects.put(aspect, Math.min(FredplenterConfig.knowledgeCap(), amount));
                }
            }
            readLocations(tag.getList("CompletedResearch", StringTag.TAG_STRING), state.completedResearch);
            readLocations(tag.getList("PendingResearchNotes", StringTag.TAG_STRING),
                    state.pendingResearchNotes);
            readLocations(tag.getList("StudiedItems", StringTag.TAG_STRING), state.studiedItems);
            readLocations(tag.getList("CreateItemsCrafted", StringTag.TAG_STRING), state.createItemsCrafted);
            ListTag discovered = tag.getList("DiscoveredAspects", StringTag.TAG_STRING);
            for (int index = 0; index < discovered.size(); index++) {
                Aspect aspect = Aspect.byId(discovered.getString(index));
                if (aspect != null) {
                    state.discoveredAspects.add(aspect);
                }
            }
            if (!tag.contains("DiscoveredAspects", CompoundTag.TAG_LIST)) {
                for (Map.Entry<Aspect, Integer> entry : state.aspects.entrySet()) {
                    if (entry.getValue() > 0) {
                        state.discoveredAspects.add(entry.getKey());
                    }
                }
            }
            ensurePrimals(state);
            ListTag nodes = tag.getList("StudiedNodes", StringTag.TAG_STRING);
            for (int i = 0; i < nodes.size(); i++) {
                state.studiedNodes.add(nodes.getString(i));
            }
            return state;
        }

        private static void readLocations(ListTag list, Set<ResourceLocation> target) {
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) {
                    target.add(id);
                }
            }
        }
    }
}
