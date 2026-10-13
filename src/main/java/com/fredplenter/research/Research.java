package com.fredplenter.research;

import com.fredplenter.aspect.AspectStack;
import com.fredplenter.aspect.Aspect;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A node in Fredplenter's research tree.
 *
 * @param id           unique id, e.g. "fredplenter:basic_observation"
 * @param prerequisite ids of research that must already be learned (empty if root)
 * @param cost         aspect points spent by ASPECTS-method research
 * @param puzzle       start, required middle, and end aspects for the table puzzle
 * @param category     Codex category id
 * @param x            normalized position on the Codex map
 * @param y            normalized position on the Codex map
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
