package com.fredplenter.network;

import net.neoforged.bus.api.IEventBus;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {

    private NetworkHandler() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NetworkHandler::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                RequestProgressPayload.TYPE,
                RequestProgressPayload.STREAM_CODEC,
                RequestProgressPayload::handle);
        registrar.playToServer(
                BeginCodexResearchPayload.TYPE,
                BeginCodexResearchPayload.STREAM_CODEC,
                BeginCodexResearchPayload::handle);
        registrar.playToServer(
                CompleteResearchPayload.TYPE,
                CompleteResearchPayload.STREAM_CODEC,
                CompleteResearchPayload::handle);
        registrar.playToServer(
                StartResearchPayload.TYPE,
                StartResearchPayload.STREAM_CODEC,
                StartResearchPayload::handle);
        registrar.playToClient(
                ResearchResultPayload.TYPE,
                ResearchResultPayload.STREAM_CODEC,
                ResearchResultPayload::handle);
        registrar.playToClient(
                ResearchStartResultPayload.TYPE,
                ResearchStartResultPayload.STREAM_CODEC,
                ResearchStartResultPayload::handle);
        registrar.playToServer(
                ScanItemPayload.TYPE,
                ScanItemPayload.STREAM_CODEC,
                ScanItemPayload::handle);
        registrar.playToClient(
                ProgressSyncPayload.TYPE,
                ProgressSyncPayload.STREAM_CODEC,
                ProgressSyncPayload::handle);
    }

    public static void sendProgress(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player, new ProgressSyncPayload(ArcaneProgress.get(player).createClientSnapshot(player.getUUID())));
    }
}
