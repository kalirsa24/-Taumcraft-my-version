package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestProgressPayload() implements CustomPacketPayload {

    public static final Type<RequestProgressPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "request_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestProgressPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestProgressPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestProgressPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                NetworkHandler.sendProgress(player);
            }
        });
    }
}
