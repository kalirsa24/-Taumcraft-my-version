package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.menu.ArcaneTableMenu;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CombineAspectsPayload(String firstId, String secondId) implements CustomPacketPayload {

    public static final Type<CombineAspectsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "combine_aspects"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombineAspectsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CombineAspectsPayload::firstId,
                    ByteBufCodecs.STRING_UTF8, CombineAspectsPayload::secondId,
                    CombineAspectsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CombineAspectsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof ArcaneTableMenu)) {
                return;
            }
            Aspect first = Aspect.byId(payload.firstId());
            Aspect second = Aspect.byId(payload.secondId());
            boolean combined = first != null && second != null
                    && ArcaneProgress.get(player).combineAspects(player.getUUID(), first, second);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    combined ? "message.fredplenter.aspect_combined"
                            : "message.fredplenter.aspect_combine_failed",
                    combined
                            ? net.minecraft.network.chat.Component.translatable(Aspect.combine(first, second).getTranslationKey())
                            : net.minecraft.network.chat.Component.empty()), true);
            NetworkHandler.sendProgress(player);
        });
    }
}
