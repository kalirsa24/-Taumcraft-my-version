package com.fredplenter.research;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectStack;
import com.fredplenter.registry.ModItems;
import com.fredplenter.item.InkwellItem;
import com.fredplenter.item.ResearchNoteItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
    private static final int MAX_ASPECT_POINTS = 1_000_000;
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

    public boolean markItemStudied(UUID playerId, ResourceLocation itemId, List<AspectStack> rewards) {
        PlayerState state = state(playerId);
        if (!state.studiedItems.add(itemId)) {
            return false;
        }
        for (AspectStack reward : rewards) {
            addPoints(state, reward.aspect(), reward.amount());
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
        for (AspectStack reward : rewards) {
            addPoints(state, reward.aspect(), reward.amount());
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

    public boolean combineAspects(UUID playerId, Aspect first, Aspect second) {
        Aspect result = Aspect.combine(first, second);
        if (result == null) {
            return false;
        }
        PlayerState state = state(playerId);
        ensurePrimals(state);
        if (!state.discoveredAspects.contains(first) || !state.discoveredAspects.contains(second)
                || state.aspects.getOrDefault(first, 0) < 1
                || state.aspects.getOrDefault(second, 0) < 1) {
            return false;
        }
        addPoints(state, first, -1);
        addPoints(state, second, -1);
        addPoints(state, result, 1);
        state.discoveredAspects.add(result);
        setDirty();
        return true;
    }

    public boolean hasResearch(UUID playerId, ResourceLocation researchId) {
        return state(playerId).completedResearch.contains(researchId);
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
            if (!hasItem(inventory, Items.PAPER) || !hasInk(inventory)) {
                return false;
            }
            consumeOne(inventory, Items.PAPER);
            if (!InkwellItem.consumeInk(player)) {
                throw new IllegalStateException("Research inkwell ran out of ink after validation");
            }
            ItemStack note = ModItems.RESEARCH_NOTE.get().createFor(researchId);
            if (!inventory.add(note)) {
                player.drop(note, false);
            }
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
                || research.puzzle().size() != 3
                || solution.size() != path.size()
                || solution.size() < 3
                || solution.size() > 19
                || solution.getFirst() != research.puzzle().getFirst()
                || solution.getLast() != research.puzzle().getLast()
                || !solution.contains(research.puzzle().get(1))
                || !path.getFirst().equals(new HexPosition(-2, 0))
                || !path.getLast().equals(new HexPosition(2, 0))) {
            return false;
        }

        PlayerState state = state(player.getUUID());
        if (!ResearchRegistry.isAvailable(research, state.completedResearch)) {
            return false;
        }
        ensurePrimals(state);
        Map<Aspect, Integer> placedCost = new EnumMap<>(Aspect.class);
        Set<HexPosition> visited = new HashSet<>();
        for (int index = 0; index < solution.size(); index++) {
            HexPosition cell = path.get(index);
            Aspect aspect = solution.get(index);
            if (!cell.isOnBoard() || !visited.add(cell)
                    || index > 0 && !path.get(index - 1).isAdjacent(cell)) {
                return false;
            }
            if (index > 0 && index < solution.size() - 1) {
                if (!state.discoveredAspects.contains(aspect)) {
                    return false;
                }
                placedCost.merge(aspect, 1, Integer::sum);
            }
            if (index > 0 && !solution.get(index - 1).isLinkedTo(aspect)) {
                return false;
            }
        }

        for (Map.Entry<Aspect, Integer> cost : placedCost.entrySet()) {
            if (state.aspects.getOrDefault(cost.getKey(), 0) < cost.getValue()) {
                return false;
            }
        }
        placedCost.forEach((aspect, amount) -> addPoints(state, aspect, -amount));
        setDirty();
        return true;
    }

    public boolean learnFromNote(UUID playerId, ResourceLocation researchId) {
        Research research = ResearchRegistry.get(researchId);
        if (research == null) {
            return false;
        }
        PlayerState state = state(playerId);
        if (!ResearchRegistry.isAvailable(research, state.completedResearch)) {
            return false;
        }
        state.completedResearch.add(researchId);
        setDirty();
        return true;
    }

    public CompoundTag createClientSnapshot(UUID playerId) {
        PlayerState state = state(playerId);
        CompoundTag snapshot = new CompoundTag();
        CompoundTag aspects = new CompoundTag();
        for (Aspect aspect : Aspect.values()) {
            aspects.putInt(aspect.getId(), state.aspects.getOrDefault(aspect, 0));
        }
        snapshot.put("Aspects", aspects);
        snapshot.put("CompletedResearch", stringList(state.completedResearch.stream().map(ResourceLocation::toString).toList()));
        snapshot.put("StudiedItems", stringList(state.studiedItems.stream().map(ResourceLocation::toString).toList()));
        snapshot.put("StudiedNodes", stringList(new ArrayList<>(state.studiedNodes)));
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
        state.aspects.put(aspect, Math.max(0, Math.min(MAX_ASPECT_POINTS, current + amount)));
    }

    private static boolean hasItem(Inventory inventory, net.minecraft.world.item.Item item) {
        return inventory.items.stream().anyMatch(stack -> stack.is(item));
    }

    private static boolean hasInk(Inventory inventory) {
        return inventory.items.stream().anyMatch(InkwellItem::hasInk)
                || inventory.offhand.stream().anyMatch(InkwellItem::hasInk);
    }

    private static void consumeOne(Inventory inventory, net.minecraft.world.item.Item item) {
        for (ItemStack stack : inventory.items) {
            if (stack.is(item)) {
                stack.shrink(1);
                return;
            }
        }
        throw new IllegalStateException("Required research paper disappeared during completion");
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
        private final Set<ResourceLocation> studiedItems = new HashSet<>();
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
            tag.put("StudiedNodes", stringList(new ArrayList<>(studiedNodes)));
            tag.put("DiscoveredAspects",
                    stringList(discoveredAspects.stream().map(Aspect::getId).toList()));
            return tag;
        }

        private static PlayerState load(CompoundTag tag) {
            PlayerState state = new PlayerState();
            CompoundTag aspectTag = tag.getCompound("Aspects");
            for (Aspect aspect : Aspect.values()) {
                int amount = aspectTag.getInt(aspect.getId());
                if (amount > 0) {
                    state.aspects.put(aspect, Math.min(MAX_ASPECT_POINTS, amount));
                }
            }
            readLocations(tag.getList("CompletedResearch", StringTag.TAG_STRING), state.completedResearch);
            readLocations(tag.getList("StudiedItems", StringTag.TAG_STRING), state.studiedItems);
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
