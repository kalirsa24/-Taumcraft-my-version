package com.fredplenter.research;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectStack;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Research definitions shared by the Codex, Arcane Table, and server validation.
 */
public final class ResearchRegistry {

    private static final Map<ResourceLocation, Research> RESEARCH = new LinkedHashMap<>();

    private ResearchRegistry() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, path);
    }

    private static void register(String path, String category, float x, float y, Research.Method method,
                                 List<ResourceLocation> prereq, List<Aspect> puzzle, AspectStack... cost) {
        ResourceLocation rl = id(path);
        RESEARCH.put(rl, new Research(rl, prereq, List.of(cost), puzzle, method, category, x, y));
    }

    static {
        register("basic_observation", "foundations", 0.30f, 0.62f, Research.Method.PAPER, List.of(),
                List.of(Aspect.FLATUS, Aspect.PROCELLA, Aspect.UNDA));
        register("aspect_almanac", "foundations", 0.30f, 0.34f, Research.Method.PAPER,
                List.of(id("basic_observation")), List.of(Aspect.HUMUS, Aspect.VITA, Aspect.LUX));
        register("compound_aspects", "foundations", 0.46f, 0.20f, Research.Method.PAPER,
                List.of(id("aspect_almanac")), List.of(Aspect.FLAMMA, Aspect.VAPOR, Aspect.UNDA));
        register("careful_scribing", "foundations", 0.14f, 0.48f, Research.Method.ASPECTS,
                List.of(id("basic_observation")), List.of(Aspect.FLATUS, Aspect.MOTUS, Aspect.LUX),
                new AspectStack(Aspect.FLATUS, 2), new AspectStack(Aspect.LUX, 2));
        register("aspect_mixture", "foundations", 0.62f, 0.20f, Research.Method.ASPECTS,
                List.of(id("compound_aspects")), List.of(Aspect.HUMUS, Aspect.LIMUS, Aspect.UNDA),
                new AspectStack(Aspect.LIMUS, 3), new AspectStack(Aspect.CINIS, 3));

        register("aura_survey", "aura", 0.46f, 0.48f, Research.Method.PAPER,
                List.of(id("basic_observation")),
                List.of(Aspect.UNDA, Aspect.VAPOR, Aspect.FLAMMA));
        register("node_hunting", "aura", 0.62f, 0.48f, Research.Method.ASPECTS,
                List.of(id("aura_survey")), List.of(Aspect.LUX, Aspect.VITA, Aspect.HUMUS),
                new AspectStack(Aspect.LUX, 3), new AspectStack(Aspect.PROCELLA, 2));
        register("node_attunement", "aura", 0.78f, 0.36f, Research.Method.PAPER,
                List.of(id("node_hunting"), id("aspect_mixture")),
                List.of(Aspect.TENEBRAE, Aspect.SOMNUS, Aspect.FLATUS));

        register("wand_attunement", "wand", 0.62f, 0.74f, Research.Method.ASPECTS,
                List.of(id("aura_survey")),
                List.of(Aspect.FLATUS, Aspect.MOTUS, Aspect.LUX),
                new AspectStack(Aspect.FLATUS, 3), new AspectStack(Aspect.MOTUS, 3),
                new AspectStack(Aspect.LUX, 3));
        register("wand_capacity", "wand", 0.78f, 0.66f, Research.Method.ASPECTS,
                List.of(id("wand_attunement")), List.of(Aspect.HUMUS, Aspect.PULVIS, Aspect.FLATUS),
                new AspectStack(Aspect.PULVIS, 4), new AspectStack(Aspect.HUMUS, 4));
        register("deep_resonance", "wand", 0.86f, 0.84f, Research.Method.PAPER,
                List.of(id("wand_capacity"), id("node_attunement")),
                List.of(Aspect.FLAMMA, Aspect.AESTUS, Aspect.LUX));
        register("focal_casting", "wand", 0.62f, 0.92f, Research.Method.ASPECTS,
                List.of(id("wand_attunement")),
                List.of(Aspect.FLATUS, Aspect.FLAMMA, Aspect.LUX),
                new AspectStack(Aspect.FLATUS, 2), new AspectStack(Aspect.LUX, 2),
                new AspectStack(Aspect.MOTUS, 2));
        register("verdant_weaving", "wand", 0.46f, 0.94f, Research.Method.ASPECTS,
                List.of(id("focal_casting")),
                List.of(Aspect.HUMUS, Aspect.PULVIS, Aspect.VITA),
                new AspectStack(Aspect.HUMUS, 3), new AspectStack(Aspect.PULVIS, 2));
        register("dread_weaving", "wand", 0.78f, 0.92f, Research.Method.ASPECTS,
                List.of(id("focal_casting")),
                List.of(Aspect.TENEBRAE, Aspect.MORS, Aspect.SOMNUS),
                new AspectStack(Aspect.TENEBRAE, 3), new AspectStack(Aspect.MORS, 2));

        register("resonant_work", "craft", 0.46f, 0.82f, Research.Method.ASPECTS,
                List.of(id("wand_attunement"), id("aspect_almanac")),
                List.of(Aspect.TENEBRAE, Aspect.SOMNUS, Aspect.FLATUS),
                new AspectStack(Aspect.TENEBRAE, 4), new AspectStack(Aspect.SOMNUS, 4),
                new AspectStack(Aspect.FLATUS, 4));
        register("ink_and_paper", "craft", 0.14f, 0.74f, Research.Method.PAPER,
                List.of(id("careful_scribing")), List.of(Aspect.UNDA, Aspect.LIMUS, Aspect.HUMUS));
        register("table_mastery", "craft", 0.30f, 0.86f, Research.Method.ASPECTS,
                List.of(id("ink_and_paper"), id("resonant_work")),
                List.of(Aspect.FLAMMA, Aspect.CINIS, Aspect.HUMUS),
                new AspectStack(Aspect.CINIS, 5), new AspectStack(Aspect.MOTUS, 5));
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
