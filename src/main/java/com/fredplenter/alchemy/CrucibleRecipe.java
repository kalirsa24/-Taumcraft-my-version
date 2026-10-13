package com.fredplenter.alchemy;

import com.fredplenter.aspect.AspectStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A mixture the Alchemical Crucible can brew.
 *
 * @param research   research a player must learn before this mixture brews for them
 * @param requires   aspects that must be present in the crucible; they are spent on brewing
 * @param result     the item produced
 */
public record CrucibleRecipe(ResourceLocation research, List<AspectStack> requires, ItemStack result) {
}
