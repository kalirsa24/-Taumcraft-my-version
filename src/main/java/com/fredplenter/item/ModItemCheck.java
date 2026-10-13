package com.fredplenter.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

public final class ModItemCheck {

    private ModItemCheck() {
    }

    public static boolean isPresentInOpenMenu(ServerPlayer player, ResourceLocation itemId) {
        for (Slot slot : player.containerMenu.slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && itemId.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }
}
