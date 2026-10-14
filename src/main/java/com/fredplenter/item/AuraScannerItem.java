package com.fredplenter.item;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectTagResolver;
import com.fredplenter.block.entity.AuraNodeBlockEntity;
import com.fredplenter.network.NetworkHandler;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;

public class AuraScannerItem extends Item {

    public AuraScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fredplenter.scanner_help"));
        tooltip.add(Component.translatable("tooltip.fredplenter.scanner_block_help"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node)) {
            return scanWorldBlock(context);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ArcaneProgress progress = ArcaneProgress.get(serverPlayer);
        Set<Aspect> before = progress.getDiscovered(player.getUUID());
        boolean studied = progress.markNodeStudied(
                player.getUUID(), level.dimension().location(), pos.asLong(), node.getScanAspects());
        ArcaneProgress.announceNewAspects(serverPlayer, before);
        serverPlayer.sendSystemMessage(node.describe());
        Aspect dominant = node.dominantAspect();
        if (dominant != null) {
            serverPlayer.sendSystemMessage(Component.translatable("message.fredplenter.node_dominant",
                    Component.translatable(dominant.getTranslationKey())));
        }
        if (studied) {
            serverPlayer.displayClientMessage(Component.translatable("message.fredplenter.node_scanned"), true);
            NetworkHandler.sendProgress(serverPlayer);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("message.fredplenter.already_studied"), true);
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult scanWorldBlock(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (state.isAir()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        ArcaneProgress progress = ArcaneProgress.get(serverPlayer);
        List<com.fredplenter.aspect.AspectStack> rewards = AspectTagResolver.aspectsFor(
                new ItemStack(Item.byBlock(state.getBlock())), serverPlayer.serverLevel(),
                progress.hasResearch(serverPlayer.getUUID(),
                        ResourceLocation.fromNamespaceAndPath(com.fredplenter.Fredplenter.MODID,
                                "aspect_almanac")));
        if (rewards.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("message.fredplenter.no_aspects_mapped"), true);
            return InteractionResult.CONSUME;
        }
        Set<Aspect> before = progress.getDiscovered(serverPlayer.getUUID());
        boolean studied = progress.markItemStudied(
                serverPlayer.getUUID(), blockId, rewards,
                progress.hasResearch(serverPlayer.getUUID(),
                        ResourceLocation.fromNamespaceAndPath(com.fredplenter.Fredplenter.MODID,
                                "basic_observation")));
        ArcaneProgress.announceNewAspects(serverPlayer, before);
        if (studied) {
            serverPlayer.displayClientMessage(Component.translatable("message.fredplenter.item_scanned"), true);
            NetworkHandler.sendProgress(serverPlayer);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("message.fredplenter.already_studied"), true);
        }
        return InteractionResult.CONSUME;
    }
}
