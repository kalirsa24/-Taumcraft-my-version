package com.fredplenter.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Stirring Rod. Right-clicking a heated crucible with it brews the mixture;
 * every stir wears the rod down, so a crucible is never a free machine.
 */
public class StirringRodItem extends Item {

    public StirringRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fredplenter.stirring_rod_help"));
    }
}
