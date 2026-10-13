package com.fredplenter.aspect;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VanillaAspectRules {

    private static final Map<String, List<AspectStack>> OVERRIDES = Map.of(
            "lava_bucket", List.of(new AspectStack(Aspect.FLAMMA, 2), new AspectStack(Aspect.AESTUS, 1)),
            "bowl", List.of(new AspectStack(Aspect.HUMUS, 1), new AspectStack(Aspect.VITA, 1)),
            "water_bucket", List.of(new AspectStack(Aspect.UNDA, 2), new AspectStack(Aspect.VAPOR, 1)));

    private static final List<Rule> RULES = List.of(
            new Rule(Set.of(
                    "ice", "packed_ice", "blue_ice", "snow", "snow_block", "snowball", "water_bucket",
                    "cod", "salmon", "tropical_fish", "pufferfish", "kelp", "dried_kelp", "seagrass",
                    "sponge", "wet_sponge", "turtle_helmet"), List.of(),
                    Aspect.UNDA, Aspect.VAPOR),
            new Rule(Set.of(
                    "flint_and_steel", "fire_charge", "lava_bucket", "blaze_rod", "blaze_powder",
                    "magma_block", "coal", "charcoal", "campfire", "soul_campfire", "furnace",
                    "blast_furnace", "smoker"), List.of(),
                    Aspect.FLAMMA, Aspect.AESTUS),
            new Rule(Set.of(
                    "ender_pearl", "ender_eye", "phantom_membrane", "soul_sand", "soul_soil",
                    "wither_skeleton_skull", "obsidian", "spider_eye", "rotten_flesh", "bone", "ink_sac",
                    "sculk", "sculk_sensor", "sculk_catalyst", "sculk_shrieker"), List.of(),
                    Aspect.TENEBRAE, Aspect.MORS),
            new Rule(Set.of(
                    "feather", "elytra", "breeze_rod", "string", "arrow", "spectral_arrow",
                    "firework_rocket", "oak_boat", "spruce_boat", "birch_boat", "jungle_boat",
                    "acacia_boat", "dark_oak_boat", "mangrove_boat", "cherry_boat",
                    "oak_chest_boat", "spruce_chest_boat", "birch_chest_boat", "jungle_chest_boat",
                    "acacia_chest_boat", "dark_oak_chest_boat", "minecart", "chest_minecart",
                    "furnace_minecart", "hopper_minecart", "tnt_minecart"), List.of("rail"),
                    Aspect.FLATUS, Aspect.PROCELLA),
            new Rule(Set.of(
                    "wooden_sword", "wooden_pickaxe", "wooden_axe", "wooden_shovel", "wooden_hoe",
                    "stone_sword", "stone_pickaxe", "stone_axe", "stone_shovel", "stone_hoe",
                    "iron_sword", "iron_pickaxe", "iron_axe", "iron_shovel", "iron_hoe",
                    "golden_sword", "golden_pickaxe", "golden_axe", "golden_shovel", "golden_hoe",
                    "diamond_sword", "diamond_pickaxe", "diamond_axe", "diamond_shovel", "diamond_hoe",
                    "netherite_sword", "netherite_pickaxe", "netherite_axe", "netherite_shovel",
                    "netherite_hoe", "bow", "crossbow", "trident", "shield", "clock", "compass",
                    "piston", "sticky_piston", "lever", "redstone", "redstone_torch", "observer",
                    "dispenser", "dropper", "hopper"), List.of(),
                    Aspect.MOTUS, Aspect.PROCELLA),
            new Rule(Set.of(
                    "torch", "soul_torch", "lantern", "soul_lantern", "glowstone", "glow_berries",
                    "sea_lantern", "shroomlight", "amethyst_shard", "amethyst_cluster"), List.of(),
                    Aspect.LUX, Aspect.VITA),
            new Rule(Set.of(
                    "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
                    "mangrove_log", "cherry_log", "oak_planks", "spruce_planks",
                    "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks",
                    "mangrove_planks", "cherry_planks", "oak_sapling", "spruce_sapling",
                    "birch_sapling", "jungle_sapling", "acacia_sapling", "dark_oak_sapling",
                    "mangrove_propagule", "cherry_sapling", "wheat", "wheat_seeds",
                    "carrot", "potato", "beetroot", "beetroot_seeds", "apple", "bread", "beef",
                    "cooked_beef", "porkchop", "cooked_porkchop", "chicken", "cooked_chicken",
                    "egg", "honey_bottle", "red_mushroom", "brown_mushroom"), List.of(),
                    Aspect.HUMUS, Aspect.VITA),
            new Rule(Set.of(
                    "stone", "cobblestone", "dirt", "sand", "gravel", "iron_ore", "gold_ore",
                    "copper_ore", "diamond_ore", "emerald_ore", "iron_ingot", "gold_ingot",
                    "copper_ingot", "iron_nugget", "gold_nugget", "raw_iron", "raw_gold",
                    "raw_copper", "brick", "deepslate", "granite", "diorite", "andesite",
                    "diamond", "emerald", "netherite_ingot"), List.of(),
                    Aspect.HUMUS, Aspect.LIMUS));

    private VanillaAspectRules() {
    }

    public static List<AspectStack> aspectsFor(String namespace, String path) {
        if (!"minecraft".equals(namespace)) {
            return List.of();
        }
        List<AspectStack> override = OVERRIDES.get(path);
        if (override != null) {
            return override;
        }
        for (Rule rule : RULES) {
            if (rule.matches(path)) {
                return List.of(new AspectStack(rule.primary(), 1), new AspectStack(rule.secondary(), 1));
            }
        }
        return List.of(new AspectStack(Aspect.FLATUS, 1), new AspectStack(Aspect.PROCELLA, 1));
    }

    private record Rule(Set<String> paths, List<String> prefixes, Aspect primary, Aspect secondary) {
        private boolean matches(String path) {
            return paths.contains(path) || prefixes.stream().anyMatch(path::startsWith);
        }
    }
}
