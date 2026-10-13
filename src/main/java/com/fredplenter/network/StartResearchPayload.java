package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.menu.ArcaneTableMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StartResearchPayload(ResourceLocation researchId) implements CustomPacketPayload {

    public static final Type<StartResearchPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "start_research"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartResearchPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, StartResearchPayload::researchId,
                    StartResearchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartResearchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (ServerPacketRateLimiter.tryAcquire(
                    player, ServerPacketRateLimiter.Action.START_RESEARCH) > 0) {
                return;
            }
            boolean accepted = player.containerMenu instanceof ArcaneTableMenu menu
                    && menu.beginResearch(player, payload.researchId());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    player, new ResearchStartResultPayload(payload.researchId(), accepted));
            if (!accepted) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.fredplenter.research_start_failed"),
                        true);
                return;
            }
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.fredplenter.note_inserted"), true);
        });
    }
}
