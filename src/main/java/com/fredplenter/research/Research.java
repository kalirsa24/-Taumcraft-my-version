package com.fredplenter.research;

import com.fredplenter.aspect.AspectStack;
import com.fredplenter.aspect.Aspect;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A single node in this mod's own research tree.
 *
 * @param id           unique id, e.g. "fredplenter:basic_observation"
 * @param prerequisite ids of research that must already be learned (empty if root)
 * @param cost         aspect points spent for ASPECTS-method research (PAPER research is paid
 *                     with the aspect tiles placed on the Arcane Table instead)
 * @param puzzle       three anchor aspects for the table: start, required link, end
 * @param category     codex tab id ("foundations", "aura", "wand", "craft")
 * @param x            node position in the codex map, 0..1
 * @param y            node position in the codex map, 0..1
 */
public record Research(
        ResourceLocation id,
        List<ResourceLocation> prerequisite,
        List<AspectStack> cost,
        List<Aspect> puzzle,
        Method method,
        String category,
        float x,
        float y) {

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
