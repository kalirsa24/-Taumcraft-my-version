package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.VanillaAspectMap;
import com.fredplenter.item.ModItemCheck;
import com.fredplenter.registry.ModItems;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ScanItemPayload(ResourceLocation itemId) implements CustomPacketPayload {

    public static final Type<ScanItemPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "scan_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanItemPayload> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, ScanItemPayload::itemId, ScanItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScanItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.getMainHandItem().is(ModItems.AURA_SCANNER.get())
                    || player.getMainHandItem().is(ModItems.ARCANE_WAND.get()))
                    || !"minecraft".equals(payload.itemId().getNamespace())
                    || !ModItemCheck.isPresentInOpenMenu(player, payload.itemId())) {
                return;
            }

            if (!ArcaneProgress.get(player).markItemStudied(
                    player.getUUID(), payload.itemId(), VanillaAspectMap.aspectsFor(payload.itemId()))) {
                return;
            }
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.fredplenter.item_scanned"), true);
            NetworkHandler.sendProgress(player);
        });
    }
}
