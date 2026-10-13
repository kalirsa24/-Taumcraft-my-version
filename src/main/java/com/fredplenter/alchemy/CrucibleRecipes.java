package com.fredplenter.alchemy;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Crucible mixtures. Every requirement uses only aspects that the vanilla item
 * table actually produces, so each recipe can be reached by dissolving ordinary
 * items: feathers, coal, bones, sand, water buckets and so on.
 */
public final class CrucibleRecipes {

    private static final ResourceLocation ALCHEMY = id("crucible_alchemy");
    private static final ResourceLocation TRANSMUTATION = id("transmutation");

    private static final List<CrucibleRecipe> RECIPES = List.of(
            new CrucibleRecipe(ALCHEMY,
                    List.of(new AspectStack(Aspect.FLATUS, 2), new AspectStack(Aspect.PROCELLA, 1)),
                    new ItemStack(Items.PAPER)),
            new CrucibleRecipe(ALCHEMY,
                    List.of(new AspectStack(Aspect.TENEBRAE, 2), new AspectStack(Aspect.MORS, 1)),
                    new ItemStack(Items.INK_SAC)),
            new CrucibleRecipe(ALCHEMY,
                    List.of(new AspectStack(Aspect.FLAMMA, 2), new AspectStack(Aspect.AESTUS, 1)),
                    new ItemStack(Items.FIRE_CHARGE, 2)),
            new CrucibleRecipe(ALCHEMY,
                    List.of(new AspectStack(Aspect.HUMUS, 2), new AspectStack(Aspect.MOTUS, 1)),
                    new ItemStack(Items.IRON_INGOT)),
            new CrucibleRecipe(TRANSMUTATION,
                    List.of(new AspectStack(Aspect.FLAMMA, 2), new AspectStack(Aspect.PROCELLA, 2)),
                    new ItemStack(Items.GUNPOWDER, 2)),
            new CrucibleRecipe(TRANSMUTATION,
                    List.of(new AspectStack(Aspect.LUX, 3), new AspectStack(Aspect.AESTUS, 1)),
                    new ItemStack(Items.GLOWSTONE_DUST, 2)),
            new CrucibleRecipe(TRANSMUTATION,
                    List.of(new AspectStack(Aspect.VITA, 2), new AspectStack(Aspect.LUX, 2)),
                    new ItemStack(Items.GOLDEN_APPLE)),
            new CrucibleRecipe(TRANSMUTATION,
                    List.of(new AspectStack(Aspect.TENEBRAE, 2), new AspectStack(Aspect.MORS, 2)),
                    new ItemStack(Items.ENDER_PEARL)),
            new CrucibleRecipe(TRANSMUTATION,
                    List.of(new AspectStack(Aspect.UNDA, 2), new AspectStack(Aspect.LIMUS, 2)),
                    new ItemStack(Items.SLIME_BALL)));

    private CrucibleRecipes() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, path);
    }

    /** The first recipe the contents satisfy whose research is learned, or {@code null}. */
    public static CrucibleRecipe match(Map<Aspect, Integer> contents, Predicate<ResourceLocation> learned) {
        for (CrucibleRecipe recipe : RECIPES) {
            if (learned.test(recipe.research()) && satisfies(contents, recipe.requires())) {
                return recipe;
            }
        }
        return null;
    }

    /** True when the contents match some recipe, learned or not. Used to hint at locked mixtures. */
    public static boolean anyMatch(Map<Aspect, Integer> contents) {
        for (CrucibleRecipe recipe : RECIPES) {
            if (satisfies(contents, recipe.requires())) {
                return true;
            }
        }
        return false;
    }

    public static boolean satisfies(Map<Aspect, Integer> contents, List<AspectStack> requires) {
        for (AspectStack need : requires) {
            if (contents.getOrDefault(need.aspect(), 0) < need.amount()) {
                return false;
            }
        }
        return true;
    }
}
