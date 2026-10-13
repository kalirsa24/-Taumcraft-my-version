package com.fredplenter.client;

import com.fredplenter.Fredplenter;
import com.fredplenter.client.screen.ArcaneCodexScreen;
import com.fredplenter.client.screen.ResearchNoteScreen;
import com.fredplenter.item.ResearchNoteItem;
import com.fredplenter.network.RequestProgressPayload;
import com.fredplenter.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Fredplenter.MODID, value = Dist.CLIENT)
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openCodex() {
        PacketDistributor.sendToServer(new RequestProgressPayload());
        Minecraft.getInstance().setScreen(new ArcaneCodexScreen());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        var stack = event.getItemStack();
        if (stack.is(ModItems.ARCANE_CODEX.get())) {
            Minecraft.getInstance().execute(ClientHooks::openCodex);
        } else if (stack.is(ModItems.RESEARCH_NOTE.get())) {
            // A note opens its own parchment screen - never the Codex map.
            var id = ResearchNoteItem.researchId(stack);
            boolean complete = ResearchNoteItem.isComplete(stack);
            if (id != null) {
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new ResearchNoteScreen(id, complete)));
            }
        }
    }
}
