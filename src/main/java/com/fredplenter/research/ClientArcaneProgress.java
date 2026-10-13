package com.fredplenter.research;

import com.fredplenter.aspect.Aspect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ClientArcaneProgress {

    private static final EnumMap<Aspect, Integer> ASPECTS = new EnumMap<>(Aspect.class);
    private static final Set<ResourceLocation> COMPLETED = new HashSet<>();
    private static final Set<ResourceLocation> STUDIED_ITEMS = new HashSet<>();
    private static ResourceLocation lastResearchResultId;
    private static boolean lastResearchResultSucceeded;
    private static ResourceLocation lastResearchStartId;
    private static boolean lastResearchStartAccepted;
    private static int revision;

    private ClientArcaneProgress() {
    }

    public static void load(CompoundTag tag) {
        ASPECTS.clear();
        COMPLETED.clear();
        STUDIED_ITEMS.clear();

        CompoundTag aspects = tag.getCompound("Aspects");
        for (Aspect aspect : Aspect.values()) {
            int amount = aspects.getInt(aspect.getId());
            if (amount > 0) {
                ASPECTS.put(aspect, amount);
            }
        }
        readLocations(tag.getList("CompletedResearch", StringTag.TAG_STRING), COMPLETED);
        readLocations(tag.getList("StudiedItems", StringTag.TAG_STRING), STUDIED_ITEMS);
        revision++;
    }

    public static int getAspectPoints(Aspect aspect) {
        return ASPECTS.getOrDefault(aspect, 0);
    }

    public static Map<Aspect, Integer> getAspectSnapshot() {
        return Map.copyOf(ASPECTS);
    }

    public static boolean hasResearch(ResourceLocation id) {
        return COMPLETED.contains(id);
    }

    public static Set<ResourceLocation> getCompletedResearch() {
        return Set.copyOf(COMPLETED);
    }

    public static boolean hasStudiedItem(ResourceLocation id) {
        return STUDIED_ITEMS.contains(id);
    }

    public static int revision() {
        return revision;
    }

    public static void setResearchResult(ResourceLocation researchId, boolean succeeded) {
        lastResearchResultId = researchId;
        lastResearchResultSucceeded = succeeded;
        revision++;
    }

    public static void clearResearchResult() {
        lastResearchResultId = null;
        lastResearchResultSucceeded = false;
        lastResearchStartId = null;
        lastResearchStartAccepted = false;
    }

    public static ResourceLocation lastResearchResultId() {
        return lastResearchResultId;
    }

    public static boolean lastResearchResultSucceeded() {
        return lastResearchResultSucceeded;
    }

    public static void setResearchStartResult(ResourceLocation researchId, boolean accepted) {
        lastResearchStartId = researchId;
        lastResearchStartAccepted = accepted;
        revision++;
    }

    public static ResourceLocation lastResearchStartId() {
        return lastResearchStartId;
    }

    public static boolean lastResearchStartAccepted() {
        return lastResearchStartAccepted;
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
