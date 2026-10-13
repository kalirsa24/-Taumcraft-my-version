package com.fredplenter.item;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.network.NetworkHandler;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class AspectCacheItem extends Item {

    public AspectCacheItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.fredplenter.cache_help"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ArcaneProgress progress = ArcaneProgress.get(serverPlayer);
            for (Aspect aspect : Aspect.values()) {
                progress.addAspectPoints(player.getUUID(), aspect, 100);
            }
            stack.shrink(1);
            NetworkHandler.sendProgress(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
