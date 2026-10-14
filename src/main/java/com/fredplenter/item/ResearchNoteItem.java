package com.fredplenter.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class ResearchNoteItem extends Item {

    public ResearchNoteItem(Properties properties) {
        super(properties);
    }

    public ItemStack createFor(ResourceLocation researchId, UUID owner) {
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = new CompoundTag();
        tag.putString("ResearchId", researchId.toString());
        tag.putString("Owner", owner.toString());
        tag.putBoolean("Complete", false);
        tag.putBoolean("Reserved", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static UUID owner(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        try {
            return UUID.fromString(data.copyTag().getString("Owner"));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static boolean isOwnedBy(ItemStack stack, UUID playerId) {
        UUID owner = owner(stack);
        return owner == null || owner.equals(playerId);
    }

    public static boolean claimIfUnowned(ItemStack stack, UUID playerId) {
        UUID owner = owner(stack);
        if (owner != null) {
            return owner.equals(playerId);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString("Owner", playerId.toString()));
        return true;
    }

    public static ResourceLocation researchId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        return ResourceLocation.tryParse(data.copyTag().getString("ResearchId"));
    }

    public static boolean isComplete(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean("Complete");
    }

    public static boolean hasReservation(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean("Reserved");
    }

    public static void markReserved(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("Reserved", true));
    }

    public static void markComplete(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("Complete", true));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation researchId = researchId(stack);
        if (researchId != null) {
            tooltip.add(Component.translatable(
                    "research." + researchId.getNamespace() + "." + researchId.getPath() + ".title"));
        }
        tooltip.add(Component.translatable(isComplete(stack)
                ? "tooltip.fredplenter.note_ready"
                : "tooltip.fredplenter.note_unfinished"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && com.fredplenter.research.ArcaneProgress.abandonResearchNote(serverPlayer, stack)) {
            player.displayClientMessage(Component.translatable("message.fredplenter.note_abandoned"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
