package com.fredplenter.item;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.VanillaAspectMap;
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

/**
 * Aura Scanner.
 * <ul>
 *     <li>Right-click an aura node: study it once and print the vis left inside.</li>
 *     <li>Shift + right-click a vanilla block: study that block type once.</li>
 *     <li>Hover a vanilla item in an open inventory while holding it: study the item type.</li>
 * </ul>
 */
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

    /**
     * Shift + right-click on a block. Shift is required so that scanning never
     * replaces normal block interaction such as opening a chest.
     */
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
        if (!"minecraft".equals(blockId.getNamespace())) {
            serverPlayer.displayClientMessage(Component.translatable("message.fredplenter.block_unstudyable"), true);
            return InteractionResult.CONSUME;
        }
        // Blocks and items share one studied set, so a block and its item form are studied together.
        ArcaneProgress progress = ArcaneProgress.get(serverPlayer);
        Set<Aspect> before = progress.getDiscovered(serverPlayer.getUUID());
        boolean studied = progress.markItemStudied(
                serverPlayer.getUUID(), blockId, VanillaAspectMap.aspectsFor(blockId));
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
