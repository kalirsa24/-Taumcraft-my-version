package com.fredplenter.item;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.block.entity.AuraNodeBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class ArcaneWandItem extends Item {

    public static final int CAPACITY_PER_ASPECT = 100;
    private static final int USE_COOLDOWN_TICKS = 5;

    public ArcaneWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof AuraNodeBlockEntity node)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        ItemStack wand = context.getItemInHand();
        CompoundTag stored = getStored(wand);
        int drained = 0;
        boolean hasCapacity = false;
        boolean nodeHasVis = false;
        for (Aspect aspect : Aspect.values()) {
            if (!aspect.isPrimal()) {
                continue;
            }
            int current = Math.max(0, Math.min(CAPACITY_PER_ASPECT, stored.getInt(aspect.getId())));
            hasCapacity |= current < CAPACITY_PER_ASPECT;
            nodeHasVis |= node.getVis(aspect) > 0;
            if (current >= CAPACITY_PER_ASPECT) {
                continue;
            }
            int amount = node.extract(aspect, 1);
            if (amount > 0) {
                stored.putInt(aspect.getId(), current + amount);
                drained += amount;
            }
        }

        if (drained > 0) {
            wand.set(DataComponents.CUSTOM_DATA, CustomData.of(stored));
            player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
            level.playSound(null, context.getClickedPos(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.7F, 1.2F);
            player.displayClientMessage(Component.translatable("message.fredplenter.wand_drained", drained), true);
            return InteractionResult.CONSUME;
        }
        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        String message = !hasCapacity ? "message.fredplenter.wand_full"
                : nodeHasVis ? "message.fredplenter.wand_cannot_hold_node_aspects"
                : "message.fredplenter.node_empty";
        player.displayClientMessage(Component.translatable(message), true);
        return InteractionResult.CONSUME;
    }

    public static int stored(ItemStack stack, Aspect aspect) {
        return Math.max(0, Math.min(CAPACITY_PER_ASPECT, getStored(stack).getInt(aspect.getId())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int total = 0;
        for (Aspect aspect : Aspect.values()) {
            if (!aspect.isPrimal()) {
                continue;
            }
            int amount = stored(stack, aspect);
            total += amount;
        }
        int capacity = CAPACITY_PER_ASPECT * 6;
        int fillPercent = total * 100 / capacity;
        tooltip.add(Component.translatable("tooltip.fredplenter.wand_capacity", total, capacity, fillPercent));
        tooltip.add(Component.translatable("tooltip.fredplenter.wand_shift_hint"));
        tooltip.add(Component.translatable("tooltip.fredplenter.wand_scan_help"));
    }

    private static CompoundTag getStored(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
}
