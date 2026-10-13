package com.fredplenter.research;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectStack;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The initial research tree. Definitions stay centralized so the book,
 * table and server-side validation use the same prerequisites and puzzle.
 */
public final class ResearchRegistry {

    private static final Map<ResourceLocation, Research> RESEARCH = new LinkedHashMap<>();

    private ResearchRegistry() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, path);
    }

    private static void register(String path, Research.Method method, List<ResourceLocation> prereq,
                                 List<Aspect> puzzle, AspectStack... cost) {
        ResourceLocation rl = id(path);
        RESEARCH.put(rl, new Research(rl, prereq, List.of(cost), puzzle, method));
    }

    static {
        register("basic_observation", Research.Method.PAPER, List.of(),
                List.of(Aspect.FLATUS, Aspect.PROCELLA, Aspect.UNDA));

        register("aura_survey", Research.Method.PAPER, List.of(id("basic_observation")),
                List.of(Aspect.UNDA, Aspect.VAPOR, Aspect.FLAMMA));

        register("wand_attunement", Research.Method.ASPECTS, List.of(id("aura_survey")),
                List.of(Aspect.FLATUS, Aspect.MOTUS, Aspect.LUX),
                new AspectStack(Aspect.FLATUS, 3), new AspectStack(Aspect.MOTUS, 3),
                new AspectStack(Aspect.LUX, 3));

        register("aspect_almanac", Research.Method.PAPER, List.of(id("basic_observation")),
                List.of(Aspect.HUMUS, Aspect.VITA, Aspect.LUX));

        register("resonant_work", Research.Method.ASPECTS, List.of(id("wand_attunement"), id("aspect_almanac")),
                List.of(Aspect.TENEBRAE, Aspect.SOMNUS, Aspect.FLATUS),
                new AspectStack(Aspect.TENEBRAE, 4), new AspectStack(Aspect.SOMNUS, 4),
                new AspectStack(Aspect.FLATUS, 4));
    }

    public static Map<ResourceLocation, Research> all() {
        return java.util.Collections.unmodifiableMap(RESEARCH);
    }

    public static Research get(ResourceLocation id) {
        return RESEARCH.get(id);
    }

    /**
     * A research is available to be learned if it isn't learned yet and all
     * of its prerequisites are already learned.
     */
    public static boolean isAvailable(Research research, java.util.Set<ResourceLocation> learned) {
        if (learned.contains(research.id())) return false;
        return learned.containsAll(research.prerequisite());
    }
}
