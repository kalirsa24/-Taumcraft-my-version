package com.fredplenter.client;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.item.ArcaneWandItem;
import com.fredplenter.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Fredplenter.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class WandTooltipHandler {

    private WandTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.ARCANE_WAND.get()) || !Screen.hasShiftDown()) {
            return;
        }
        event.getToolTip().removeIf(line -> line.getString().equals(
                Component.translatable("tooltip.fredplenter.wand_shift_hint").getString()));
        for (Aspect aspect : Aspect.values()) {
            if (aspect.isPrimal()) {
                event.getToolTip().add(Component.translatable("tooltip.fredplenter.wand_aspect_charge",
                        Component.translatable(aspect.getTranslationKey())
                                .withStyle(style -> style.withColor(aspect.getColor())),
                        ArcaneWandItem.stored(stack, aspect), ArcaneWandItem.CAPACITY_PER_ASPECT));
            }
        }
    }
}
