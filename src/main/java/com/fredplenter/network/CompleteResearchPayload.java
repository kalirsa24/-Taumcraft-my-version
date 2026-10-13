package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.menu.ArcaneTableMenu;
import com.fredplenter.research.HexPosition;
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

            ParsedSolution selected = parseSolution(payload.solution());
            if (selected == null || !menu.completeResearch(
                    player, payload.researchId(), selected.aspects(), selected.cells())) {
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

    private static ParsedSolution parseSolution(String value) {
        if (value.isBlank() || value.length() > 512) {
            return null;
        }
        List<Aspect> aspects = new ArrayList<>();
        List<HexPosition> cells = new ArrayList<>();
        for (String entry : value.split(";", -1)) {
            String[] parts = entry.split("@", -1);
            if (parts.length != 2) {
                return null;
            }
            Aspect aspect = Aspect.byId(parts[0]);
            String[] coordinates = parts[1].split(",", -1);
            if (aspect == null || coordinates.length != 2) {
                return null;
            }
            try {
                aspects.add(aspect);
                cells.add(new HexPosition(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return aspects.size() < 3 || aspects.size() > 19
                ? null : new ParsedSolution(List.copyOf(aspects), List.copyOf(cells));
    }

    private record ParsedSolution(List<Aspect> aspects, List<HexPosition> cells) {
    }
}
