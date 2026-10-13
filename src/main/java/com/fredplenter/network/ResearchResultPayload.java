package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.research.ClientArcaneProgress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchResultPayload(ResourceLocation researchId, boolean success) implements CustomPacketPayload {

    public static final Type<ResearchResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "research_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, ResearchResultPayload::researchId,
                    ByteBufCodecs.BOOL, ResearchResultPayload::success,
                    ResearchResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientArcaneProgress.setResearchResult(payload.researchId(), payload.success()));
    }
}
