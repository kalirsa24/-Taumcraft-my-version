package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.research.ClientArcaneProgress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchStartResultPayload(ResourceLocation researchId, boolean accepted) implements CustomPacketPayload {

    public static final Type<ResearchStartResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "research_start_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchStartResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, ResearchStartResultPayload::researchId,
                    ByteBufCodecs.BOOL, ResearchStartResultPayload::accepted,
                    ResearchStartResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchStartResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientArcaneProgress.setResearchStartResult(payload.researchId(), payload.accepted()));
    }
}
