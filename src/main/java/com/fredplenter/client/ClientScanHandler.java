package com.fredplenter.client;

import com.fredplenter.Fredplenter;
import com.fredplenter.network.ScanItemPayload;
import com.fredplenter.network.RequestProgressPayload;
import com.fredplenter.registry.ModItems;
import com.fredplenter.research.ClientArcaneProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.UUID;

@EventBusSubscriber(modid = Fredplenter.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ClientScanHandler {

    private static ResourceLocation hoveredItem;
    private static AbstractContainerScreen<?> activeScreen;
    private static UUID requestedFor;
    private static long hoverStartedAt;
    private static long retryAfterMillis;
    private static long scanSentAtMillis;
    private static boolean scanSent;

    private ClientScanHandler() {
    }

    @SubscribeEvent
    public static void onContainerRender(ContainerScreenEvent.Render.Foreground event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !canScan(player)) {
            resetHover();
            activeScreen = null;
            return;
        }

        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (screen != activeScreen) {
            resetHover();
            activeScreen = screen;
        }
        if (!player.getUUID().equals(requestedFor)) {
            requestedFor = player.getUUID();
            PacketDistributor.sendToServer(new RequestProgressPayload());
        }
        double mouseX = event.getMouseX() - screen.getGuiLeft();
        double mouseY = event.getMouseY() - screen.getGuiTop();
        ItemStack target = null;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive()
                    && mouseX >= slot.x && mouseX < slot.x + 16
                    && mouseY >= slot.y && mouseY < slot.y + 16
                    && slot.hasItem()) {
                target = slot.getItem();
                break;
            }
        }

        if (target == null) {
            resetHover();
            return;
        }
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(target.getItem());
        if (ClientArcaneProgress.hasStudiedItem(itemId)) {
            resetHover();
            return;
        }

        if (!itemId.equals(hoveredItem)) {
            hoveredItem = itemId;
            hoverStartedAt = System.currentTimeMillis();
            scanSent = false;
        }
        long elapsed = System.currentTimeMillis() - hoverStartedAt;
        long now = System.currentTimeMillis();
        if (scanSent && now - scanSentAtMillis >= 5_000L) {
            scanSent = false;
            retryAfterMillis = now + 1_000L;
        }
        if (!scanSent && elapsed >= 2_000L && now >= retryAfterMillis) {
            PacketDistributor.sendToServer(new ScanItemPayload(itemId));
            scanSent = true;
            scanSentAtMillis = now;
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null || event.getItemStack().isEmpty()) {
            return;
        }
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (ClientArcaneProgress.hasStudiedItem(itemId)) {
            event.getToolTip().add(Component.translatable("tooltip.fredplenter.studied").withStyle(ChatFormatting.GREEN));
        } else if (itemId.equals(hoveredItem) && !scanSent) {
            long now = System.currentTimeMillis();
            long remaining = Math.max(
                    Math.max(0, 2_000L - (now - hoverStartedAt)),
                    Math.max(0, retryAfterMillis - now));
            event.getToolTip().add(Component.translatable(
                    "tooltip.fredplenter.scanning", String.format(Locale.ROOT, "%.1f", remaining / 1_000.0))
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    private static void resetHover() {
        hoveredItem = null;
        hoverStartedAt = 0;
        retryAfterMillis = 0;
        scanSentAtMillis = 0;
        scanSent = false;
    }

    public static void handleResult(ResourceLocation itemId, boolean accepted, int retryTicks) {
        if (!itemId.equals(hoveredItem)) {
            return;
        }
        if (accepted) {
            ClientArcaneProgress.markItemStudied(itemId);
            scanSent = true;
            return;
        }
        scanSent = false;
        scanSentAtMillis = 0;
        retryAfterMillis = System.currentTimeMillis() + Math.max(1, retryTicks) * 50L;
    }

    private static boolean canScan(Player player) {
        return player.getMainHandItem().is(ModItems.AURA_SCANNER.get())
                || player.getMainHandItem().is(ModItems.ARCANE_WAND.get());
    }
}
