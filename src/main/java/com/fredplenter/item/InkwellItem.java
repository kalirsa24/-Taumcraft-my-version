package com.fredplenter.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class InkwellItem extends Item {

    public static final int CAPACITY = 8;

    public InkwellItem(Properties properties) {
        super(properties);
    }

    public static int ink(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return CAPACITY;
        }
        return Math.max(0, Math.min(CAPACITY, data.copyTag().getInt("Ink")));
    }

    public static boolean consumeInk(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (consumeInk(stack)) {
                player.getInventory().setChanged();
                return true;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (consumeInk(offhand)) {
            player.getInventory().setChanged();
            return true;
        }
        return false;
    }

    public static boolean hasInk(ItemStack stack) {
        return stack.is(com.fredplenter.registry.ModItems.INKWELL.get()) && ink(stack) > 0;
    }

    private static boolean consumeInk(ItemStack stack) {
        if (!hasInk(stack)) {
            return false;
        }
        setInk(stack, ink(stack) - 1);
        return true;
    }

    private static void setInk(ItemStack stack, int amount) {
        int boundedAmount = Math.max(0, Math.min(CAPACITY, amount));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("Ink", boundedAmount));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack inkwell = player.getItemInHand(hand);
        ItemStack inkSac = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!inkSac.is(net.minecraft.world.item.Items.INK_SAC)) {
            return InteractionResultHolder.pass(inkwell);
        }
        if (ink(inkwell) >= CAPACITY) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.fredplenter.inkwell_full"), true);
            }
            return InteractionResultHolder.fail(inkwell);
        }
        if (!level.isClientSide) {
            setInk(inkwell, CAPACITY);
            if (!player.getAbilities().instabuild) {
                inkSac.shrink(1);
            }
            player.getInventory().setChanged();
            player.displayClientMessage(Component.translatable("message.fredplenter.inkwell_refilled"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(inkwell, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fredplenter.inkwell_ink", ink(stack), CAPACITY));
        tooltip.add(Component.translatable("tooltip.fredplenter.inkwell_refill"));
    }
}
