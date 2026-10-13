package com.fredplenter.aspect;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VanillaAspectMap {

    private VanillaAspectMap() {
    }

    public static List<AspectStack> aspectsFor(ResourceLocation itemId) {
        if (!"minecraft".equals(itemId.getNamespace())) {
            return List.of();
        }

        String path = itemId.getPath().toLowerCase(Locale.ROOT);
        Aspect primary = primaryAspect(path);
        Aspect secondary = secondaryAspect(path, primary);
        int primaryAmount = 1 + Math.floorMod(path.hashCode(), 3);

        List<AspectStack> result = new ArrayList<>();
        result.add(new AspectStack(primary, primaryAmount));
        if (secondary != primary) {
            int secondaryAmount = 1 + Math.floorMod(path.hashCode() / 7, 2);
            result.add(new AspectStack(secondary, secondaryAmount));
        }
        return List.copyOf(result);
    }

    private static Aspect primaryAspect(String path) {
        if (contains(path, "water", "bucket", "ice", "snow", "fish", "kelp", "coral", "sponge", "slime")) {
            return Aspect.UNDA;
        }
        if (contains(path, "wood", "log", "plank", "sapling", "seed", "crop", "flower", "wheat", "bread", "apple", "meat", "beef", "pork", "chicken", "carrot", "potato", "beetroot", "honey", "egg", "mushroom")) {
            return Aspect.HUMUS;
        }
        if (contains(path, "torch", "lantern", "glow", "light", "sea_lantern", "shroomlight", "glowstone", "amethyst")) {
            return Aspect.LUX;
        }
        if (contains(path, "flint", "fire", "lava", "blaze", "magma", "coal", "charcoal", "campfire", "furnace")) {
            return Aspect.FLAMMA;
        }
        if (contains(path, "ender", "phantom", "soul", "wither", "skull", "obsidian", "sculk", "spider_eye", "rotten", "bone", "wool", "ink_sac")) {
            return Aspect.TENEBRAE;
        }
        if (contains(path, "feather", "elytra", "wind", "banner", "sail", "string", "arrow", "rocket", "boat", "minecart", "rail")) {
            return Aspect.FLATUS;
        }
        if (contains(path, "sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots", "shield", "bow", "crossbow", "trident", "clock", "compass", "piston", "door", "button", "lever", "redstone", "minecart")) {
            return Aspect.MOTUS;
        }
        if (contains(path, "stone", "dirt", "sand", "gravel", "ore", "ingot", "nugget", "raw_", "brick", "deepslate", "granite", "diorite", "andesite", "copper", "iron", "gold", "diamond", "emerald", "netherite")) {
            return Aspect.HUMUS;
        }
        return Aspect.FLATUS;
    }

    private static Aspect secondaryAspect(String path, Aspect primary) {
        if (contains(path, "sword", "pickaxe", "axe", "shovel", "hoe", "rail", "minecart", "boat", "door", "piston", "clock", "compass")) {
            return Aspect.MOTUS;
        }
        if (contains(path, "food", "bread", "apple", "meat", "fish", "stew", "soup", "honey", "potion")) {
            return Aspect.VITA;
        }
        if (contains(path, "torch", "lantern", "glow", "fire", "lava", "blaze")) {
            return Aspect.LUX;
        }
        return switch (primary) {
            case FLAMMA -> Aspect.AESTUS;
            case UNDA -> Aspect.VAPOR;
            case HUMUS -> Aspect.LIMUS;
            case FLATUS -> Aspect.PROCELLA;
            case LUX -> Aspect.VITA;
            case TENEBRAE -> Aspect.MORS;
            default -> Aspect.SOMNUS;
        };
    }

    private static boolean contains(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
