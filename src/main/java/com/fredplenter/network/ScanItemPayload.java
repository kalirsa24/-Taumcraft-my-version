package com.fredplenter.network;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectTagResolver;
import com.fredplenter.item.ModItemCheck;
import com.fredplenter.registry.ModItems;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;

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
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            int retryTicks = ServerPacketRateLimiter.tryAcquire(
                    player, ServerPacketRateLimiter.Action.SCAN_ITEM);
            if (retryTicks > 0) {
                sendResult(player, payload.itemId(), false, retryTicks);
                return;
            }

            if (!(player.getMainHandItem().is(ModItems.AURA_SCANNER.get())
                    || player.getMainHandItem().is(ModItems.ARCANE_WAND.get()))
                    || !BuiltInRegistries.ITEM.containsKey(payload.itemId())
                    || BuiltInRegistries.ITEM.get(payload.itemId()) == Items.AIR
                    || !ModItemCheck.isPresentInOpenMenu(player, payload.itemId())) {
                sendResult(player, payload.itemId(), false,
                        ServerPacketRateLimiter.Action.SCAN_ITEM.intervalTicks());
                return;
            }
            ArcaneProgress progress = ArcaneProgress.get(player);
            java.util.List<com.fredplenter.aspect.AspectStack> rewards = AspectTagResolver.aspectsFor(
                    new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(payload.itemId())),
                    player.serverLevel(), progress.hasResearch(player.getUUID(),
                            ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "aspect_almanac")));
            if (rewards.isEmpty()) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.fredplenter.no_aspects_mapped"),
                        true);
                sendResult(player, payload.itemId(), false, 0);
                return;
            }
            Set<Aspect> before = progress.getDiscovered(player.getUUID());
            boolean newlyStudied = progress.markItemStudied(
                    player.getUUID(), payload.itemId(), rewards,
                    progress.hasResearch(player.getUUID(),
                            ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "basic_observation")));
            boolean studied = newlyStudied || progress.hasStudiedItem(player.getUUID(), payload.itemId());
            if (newlyStudied) {
                ArcaneProgress.announceNewAspects(player, before);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.fredplenter.item_scanned"), true);
                NetworkHandler.sendProgress(player);
            }
            sendResult(player, payload.itemId(), studied, 0);
        });
    }

    private static void sendResult(ServerPlayer player, ResourceLocation itemId, boolean accepted, int retryTicks) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, new ScanResultPayload(itemId, accepted, retryTicks));
    }
}
