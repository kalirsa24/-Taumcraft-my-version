package com.fredplenter.item;

import com.fredplenter.network.NetworkHandler;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchNoteItem extends Item {

    public ResearchNoteItem(Properties properties) {
        super(properties);
    }

    public ItemStack createFor(ResourceLocation researchId) {
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = new CompoundTag();
        tag.putString("ResearchId", researchId.toString());
        tag.putBoolean("Complete", false);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
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
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ResourceLocation researchId = researchId(stack);
            if (!isComplete(stack)) {
                player.displayClientMessage(Component.translatable("message.fredplenter.note_needs_table"), true);
            } else if (researchId != null
                    && ArcaneProgress.get(serverPlayer).learnFromNote(player.getUUID(), researchId)) {
                stack.shrink(1);
                player.displayClientMessage(
                        Component.translatable("message.fredplenter.research_learned",
                                Component.translatable("research." + researchId.getNamespace() + "." + researchId.getPath() + ".title")),
                        true);
                NetworkHandler.sendProgress(serverPlayer);
            } else {
                player.displayClientMessage(Component.translatable("message.fredplenter.note_unusable"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
