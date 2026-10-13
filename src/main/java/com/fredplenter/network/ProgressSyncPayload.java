package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.research.ClientArcaneProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ProgressSyncPayload(CompoundTag data, boolean fullSnapshot) implements CustomPacketPayload {

    public static final Type<ProgressSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "progress_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, ProgressSyncPayload::data,
                    ByteBufCodecs.BOOL, ProgressSyncPayload::fullSnapshot,
                    ProgressSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProgressSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientArcaneProgress.load(payload.data(), payload.fullSnapshot()));
    }
}
