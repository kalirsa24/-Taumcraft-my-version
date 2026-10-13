package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.client.ClientScanHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ScanResultPayload(ResourceLocation itemId, boolean accepted, int retryTicks)
        implements CustomPacketPayload {

    public static final Type<ScanResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "scan_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, ScanResultPayload::itemId,
                    ByteBufCodecs.BOOL, ScanResultPayload::accepted,
                    ByteBufCodecs.VAR_INT, ScanResultPayload::retryTicks,
                    ScanResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScanResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientScanHandler.handleResult(payload.itemId(), payload.accepted(), payload.retryTicks()));
    }
}
