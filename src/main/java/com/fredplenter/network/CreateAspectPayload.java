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

public record CreateAspectPayload(String resultId, boolean createMaximum) implements CustomPacketPayload {

    public static final Type<CreateAspectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "create_aspect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreateAspectPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(64), CreateAspectPayload::resultId,
                    ByteBufCodecs.BOOL, CreateAspectPayload::createMaximum,
                    CreateAspectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CreateAspectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (ServerPacketRateLimiter.tryAcquire(
                    player, ServerPacketRateLimiter.Action.CREATE_ASPECT) > 0
                    || !(player.containerMenu instanceof ArcaneTableMenu)) {
                return;
            }
            Aspect result = Aspect.byId(payload.resultId());
            int created = ArcaneProgress.get(player)
                    .createAspect(player.getUUID(), result, payload.createMaximum());
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    created > 0 ? "message.fredplenter.aspect_created"
                            : "message.fredplenter.aspect_creation_failed",
                    created > 0
                            ? net.minecraft.network.chat.Component.translatable(result.getTranslationKey())
                            : net.minecraft.network.chat.Component.empty(),
                    created), true);
            NetworkHandler.sendProgress(player);
        });
    }
}
