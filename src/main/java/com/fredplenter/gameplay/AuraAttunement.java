package com.fredplenter.gameplay;

import com.fredplenter.Fredplenter;
import com.fredplenter.FredplenterConfig;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.block.entity.AuraNodeBlockEntity;
import com.fredplenter.network.NetworkHandler;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Fredplenter.MODID)
public final class AuraAttunement {

    private static final ResourceLocation AURA_SURVEY = id("aura_survey");
    private static final ResourceLocation NODE_ATTUNEMENT = id("node_attunement");
    private static final ResourceLocation RESONANT_WORK = id("resonant_work");
    private static final int BASE_RADIUS = 6;

    private AuraAttunement() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, path);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ArcaneProgress progress = ArcaneProgress.get(player);
        if (!progress.hasResearch(player.getUUID(), AURA_SURVEY)) {
            return;
        }
        int baseInterval = FredplenterConfig.passiveKnowledgeIntervalTicks();
        int interval = progress.hasResearch(player.getUUID(), RESONANT_WORK)
                ? Math.max(1, baseInterval / 2) : baseInterval;
        if (player.tickCount % interval != 0) {
            return;
        }

        int radius = progress.hasResearch(player.getUUID(), NODE_ATTUNEMENT)
                ? FredplenterConfig.nodeAttunementRadius() : BASE_RADIUS;
        BlockPos origin = player.blockPosition();
        AuraNodeBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (!player.level().hasChunkAt(pos)
                    || !(player.level().getBlockEntity(pos) instanceof AuraNodeBlockEntity node)) {
                continue;
            }
            Aspect aspect = node.dominantAspect();
            if (aspect == null) {
                continue;
            }
            double distance = pos.distToCenterSqr(player.position());
            if (distance < nearestDistance) {
                nearest = node;
                nearestDistance = distance;
            }
        }
        if (nearest == null) {
            return;
        }

        Aspect aspect = nearest.dominantAspect();
        if (aspect == null || progress.addRenewableKnowledge(player.getUUID(), aspect, 1) == 0) {
            return;
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.fredplenter.aura_attunement",
                net.minecraft.network.chat.Component.translatable(aspect.getTranslationKey())), true);
        NetworkHandler.sendProgress(player);
    }
}
