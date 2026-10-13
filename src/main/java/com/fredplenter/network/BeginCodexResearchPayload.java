package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.research.ArcaneProgress;
import com.fredplenter.research.Research;
import com.fredplenter.research.ResearchRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BeginCodexResearchPayload(ResourceLocation researchId) implements CustomPacketPayload {

    public static final Type<BeginCodexResearchPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "begin_codex_research"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BeginCodexResearchPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, BeginCodexResearchPayload::researchId,
                    BeginCodexResearchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BeginCodexResearchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !player.getInventory().contains(stack -> stack.is(
                    com.fredplenter.registry.ModItems.ARCANE_CODEX.get()))) {
                return;
            }

            Research research = ResearchRegistry.get(payload.researchId());
            boolean accepted = research != null
                    && ArcaneProgress.get(player).beginCodexResearch(player, payload.researchId());
            PacketDistributor.sendToPlayer(player,
                    new ResearchStartResultPayload(payload.researchId(), accepted));
            if (accepted && research.method() == Research.Method.PAPER) {
                player.displayClientMessage(
                        Component.translatable("message.fredplenter.paper_research_started"),
                        true);
            } else if (accepted) {
                player.displayClientMessage(
                        Component.translatable("message.fredplenter.aspect_research_learned",
                                Component.translatable(research.getTranslationKey())),
                        true);
            } else if (!accepted) {
                player.displayClientMessage(
                        Component.translatable("message.fredplenter.research_start_failed"),
                        true);
            }
            NetworkHandler.sendProgress(player);
        });
    }
}
