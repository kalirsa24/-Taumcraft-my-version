package com.fredplenter.research;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectStack;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A node in Fredplenter's research tree.
 *
 * @param id           unique id, e.g. "fredplenter:basic_observation"
 * @param prerequisite ids of research that must already be learned (empty if root)
 * @param cost         aspect points spent by ASPECTS-method research
 * @param puzzle       start, required middle, and end for paper research; empty for aspect-cost research
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

    public Research {
        if (method == Method.PAPER && puzzle.size() != 3) {
            throw new IllegalArgumentException("Paper research must define a three-aspect puzzle: " + id);
        }
        if (method == Method.ASPECTS && cost.isEmpty()) {
            throw new IllegalArgumentException("Aspect research must define an aspect cost: " + id);
        }
        if (method == Method.ASPECTS && !puzzle.isEmpty()) {
            throw new IllegalArgumentException("Aspect research must not define an unused puzzle: " + id);
        }
    }

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

    public Aspect mapIconAspect() {
        return puzzle.isEmpty() ? cost.getFirst().aspect() : puzzle.getFirst();
    }
}
