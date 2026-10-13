package com.fredplenter.item;

import com.fredplenter.block.entity.AuraNodeBlockEntity;
import com.fredplenter.network.NetworkHandler;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class AuraScannerItem extends Item {

    public AuraScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fredplenter.scanner_help"));
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

        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        boolean studied = ArcaneProgress.get(serverPlayer).markNodeStudied(
                player.getUUID(), level.dimension().location(), context.getClickedPos().asLong(), node.getScanAspects());
        if (studied) {
            serverPlayer.displayClientMessage(Component.translatable("message.fredplenter.node_scanned"), true);
            NetworkHandler.sendProgress(serverPlayer);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("message.fredplenter.already_studied"), true);
        }
        return InteractionResult.CONSUME;
    }
}
