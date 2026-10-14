package com.fredplenter.aspect;

import com.fredplenter.Fredplenter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AspectTagResolver {

    private static final Map<Aspect, TagKey<Item>> TAGS = new EnumMap<>(Aspect.class);

    static {
        for (Aspect aspect : Aspect.values()) {
            TAGS.put(aspect, TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "aspects/" + aspect.getId())));
        }
    }

    private AspectTagResolver() {
    }

    public static List<AspectStack> aspectsFor(ItemStack stack, ServerLevel level, boolean recipeInheritance) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("minecraft".equals(itemId.getNamespace())) {
            return VanillaAspectMap.aspectsFor(itemId);
        }

        List<AspectStack> tagged = taggedAspects(stack);
        return tagged.isEmpty() && recipeInheritance ? inheritedRecipeAspects(itemId, level) : tagged;
    }

    private static List<AspectStack> taggedAspects(ItemStack stack) {
        List<AspectStack> result = new ArrayList<>();
        for (Map.Entry<Aspect, TagKey<Item>> entry : TAGS.entrySet()) {
            if (stack.is(entry.getValue())) {
                result.add(new AspectStack(entry.getKey(), 1));
            }
        }
        return List.copyOf(result);
    }

    private static List<AspectStack> inheritedRecipeAspects(ResourceLocation target, ServerLevel level) {
        for (RecipeHolder<CraftingRecipe> holder :
                level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            if (!target.equals(BuiltInRegistries.ITEM.getKey(
                    recipe.getResultItem(level.registryAccess()).getItem()))) {
                continue;
            }

            List<List<AspectStack>> ingredientAspects = new ArrayList<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredientAspects.add(aspectsForIngredient(ingredient));
            }
            List<AspectStack> inherited = deriveRecipeAspects(ingredientAspects);
            if (!inherited.isEmpty()) {
                return inherited;
            }
        }
        return List.of();
    }

    private static List<AspectStack> aspectsForIngredient(Ingredient ingredient) {
        List<AspectStack> result = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
                    List<AspectStack> aspects = taggedAspects(stack);
                    if (aspects.isEmpty()) {
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                        aspects = "minecraft".equals(id.getNamespace())
                                ? VanillaAspectMap.aspectsFor(id) : List.of();
                    }
                    result.addAll(aspects);
        }
        return List.copyOf(result);
    }

    public static List<AspectStack> deriveRecipeAspects(List<List<AspectStack>> ingredientAspects) {
        Map<Aspect, Integer> votes = new EnumMap<>(Aspect.class);
        int recognizedIngredients = 0;
        for (List<AspectStack> aspects : ingredientAspects) {
            if (aspects.isEmpty()) {
                continue;
            }
            votes.merge(aspects.getFirst().aspect(), 1, Integer::sum);
            recognizedIngredients++;
        }
        if (recognizedIngredients < 2 || votes.isEmpty()) {
            return List.of();
        }
        return votes.entrySet().stream()
                .sorted(Map.Entry.<Aspect, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().ordinal()))
                .limit(2)
                .map(entry -> new AspectStack(entry.getKey(), 1))
                .toList();
    }
}
