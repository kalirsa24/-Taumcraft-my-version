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
    private static boolean scanSent;

    private ClientScanHandler() {
    }

    @SubscribeEvent
    public static void onContainerRender(ContainerScreenEvent.Render.Foreground event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !player.getMainHandItem().is(ModItems.AURA_SCANNER.get())) {
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
        if (!"minecraft".equals(itemId.getNamespace()) || ClientArcaneProgress.hasStudiedItem(itemId)) {
            resetHover();
            return;
        }

        if (!itemId.equals(hoveredItem)) {
            hoveredItem = itemId;
            hoverStartedAt = System.currentTimeMillis();
            scanSent = false;
        }
        long elapsed = System.currentTimeMillis() - hoverStartedAt;
        if (!scanSent && elapsed >= 2_000L) {
            PacketDistributor.sendToServer(new ScanItemPayload(itemId));
            scanSent = true;
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null || event.getItemStack().isEmpty()) {
            return;
        }
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if ("minecraft".equals(itemId.getNamespace())) {
            if (ClientArcaneProgress.hasStudiedItem(itemId)) {
                event.getToolTip().add(Component.translatable("tooltip.fredplenter.studied").withStyle(ChatFormatting.GREEN));
            } else if (itemId.equals(hoveredItem) && !scanSent) {
                long remaining = Math.max(0, 2_000L - (System.currentTimeMillis() - hoverStartedAt));
                event.getToolTip().add(Component.translatable(
                        "tooltip.fredplenter.scanning", String.format(Locale.ROOT, "%.1f", remaining / 1_000.0))
                        .withStyle(ChatFormatting.AQUA));
            }
        } else if (player.getMainHandItem().is(ModItems.AURA_SCANNER.get())) {
            event.getToolTip().add(Component.translatable("tooltip.fredplenter.unstudyable").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void resetHover() {
        hoveredItem = null;
        hoverStartedAt = 0;
        scanSent = false;
    }
}
