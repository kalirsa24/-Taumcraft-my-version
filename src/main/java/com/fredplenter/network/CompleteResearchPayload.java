package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.menu.ArcaneTableMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public record CompleteResearchPayload(ResourceLocation researchId, String solution) implements CustomPacketPayload {

    public static final Type<CompleteResearchPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "complete_research"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompleteResearchPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, CompleteResearchPayload::researchId,
                    ByteBufCodecs.STRING_UTF8, CompleteResearchPayload::solution,
                    CompleteResearchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompleteResearchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof ArcaneTableMenu menu)) {
                return;
            }

            List<Aspect> selected = parseSolution(payload.solution());
            if (selected == null || !menu.completeResearch(player, payload.researchId(), selected)) {
                PacketDistributor.sendToPlayer(player, new ResearchResultPayload(payload.researchId(), false));
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.fredplenter.research_failed"), true);
            } else {
                PacketDistributor.sendToPlayer(player, new ResearchResultPayload(payload.researchId(), true));
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.fredplenter.note_completed"), true);
            }
            NetworkHandler.sendProgress(player);
        });
    }

    private static List<Aspect> parseSolution(String value) {
        if (value.isBlank() || value.length() > 128) {
            return null;
        }
        List<Aspect> result = new ArrayList<>();
        for (String id : value.split(",", -1)) {
            Aspect found = null;
            for (Aspect aspect : Aspect.values()) {
                if (aspect.getId().equals(id)) {
                    found = aspect;
                    break;
                }
            }
            if (found == null) {
                return null;
            }
            result.add(found);
        }
        return List.copyOf(result);
    }
}
