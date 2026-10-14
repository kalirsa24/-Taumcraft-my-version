package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.item.ResearchNoteItem;
import com.fredplenter.research.ArcaneProgress;
import com.fredplenter.research.Research;
import com.fredplenter.research.ResearchRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LearnNotePayload(ResourceLocation researchId) implements CustomPacketPayload {

    public static final Type<LearnNotePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "learn_note"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LearnNotePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, LearnNotePayload::researchId,
                    LearnNotePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LearnNotePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (ServerPacketRateLimiter.tryAcquire(
                    player, ServerPacketRateLimiter.Action.LEARN_NOTE) > 0) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof ResearchNoteItem
                        && ResearchNoteItem.isComplete(stack)
                        && payload.researchId().equals(ResearchNoteItem.researchId(stack))) {
                    if (ArcaneProgress.get(player).learnFromNote(player, stack)) {
                        stack.shrink(1);
                        Research research = ResearchRegistry.get(payload.researchId());
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.fredplenter.research_learned",
                                net.minecraft.network.chat.Component.translatable(
                                        research.getTranslationKey())), true);
                    } else {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                ResearchNoteItem.isOwnedBy(stack, player.getUUID())
                                        ? "message.fredplenter.note_unusable"
                                        : "message.fredplenter.note_not_owner"), true);
                    }
                    NetworkHandler.sendProgress(player);
                    return;
                }
            }
        });
    }
}
