package com.fredplenter.research;

import com.fredplenter.aspect.AspectStack;
import com.fredplenter.aspect.Aspect;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A single node in the Thaumonomicon's research tree.
 *
 * @param id           unique id, e.g. "fredplenter:basic_observation"
 * @param prerequisite ids of research that must already be learned (empty if root)
 * @param cost         the aspects spent when the research is completed
 * @param puzzle       the ordered aspect path to solve at the Arcane Table
 */
public record Research(
        ResourceLocation id,
        List<ResourceLocation> prerequisite,
        List<AspectStack> cost,
        List<Aspect> puzzle,
        Method method) {

    public enum Method {
        PAPER,
        ASPECTS
    }

    public String getTranslationKey() {
        return "research." + id.getNamespace() + "." + id.getPath() + ".title";
    }

    public String getDescriptionKey() {
        return "research." + id.getNamespace() + "." + id.getPath() + ".desc";
    }
}
