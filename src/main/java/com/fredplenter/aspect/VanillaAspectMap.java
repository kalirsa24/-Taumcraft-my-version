package com.fredplenter.aspect;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class VanillaAspectMap {
    private VanillaAspectMap() {
    }

    public static List<AspectStack> aspectsFor(ResourceLocation itemId) {
        return VanillaAspectRules.aspectsFor(itemId.getNamespace(), itemId.getPath());
    }
}
