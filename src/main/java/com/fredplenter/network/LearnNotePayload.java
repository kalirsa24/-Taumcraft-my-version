package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.item.ResearchNoteItem;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Learn from a COMPLETED note held in either hand. Sent by the note's own screen, never by the Codex. */
public record LearnNotePayload(ResourceLocation researchId) implements CustomPacketPayload {
    public static final Type<LearnNotePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "learn_note"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LearnNotePayload> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, LearnNotePayload::researchId, LearnNotePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LearnNotePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            for (var hand : net.minecraft.world.InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof ResearchNoteItem
                        && ResearchNoteItem.isComplete(stack)
                        && payload.researchId().equals(ResearchNoteItem.researchId(stack))) {
                    if (ArcaneProgress.get(player).learnFromNote(player.getUUID(), payload.researchId())) {
                        stack.shrink(1);
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.fredplenter.research_learned",
                                net.minecraft.network.chat.Component.translatable(
                                        "research." + payload.researchId().getNamespace() + "." + payload.researchId().getPath() + ".title")), true);
                    } else {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.fredplenter.note_unusable"), true);
                    }
                    NetworkHandler.sendProgress(player);
                    return;
                }
            }
        });
    }
}
