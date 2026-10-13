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
        if (event.getItemStack().is(ModItems.ARCANE_CODEX.get())) {
            Minecraft.getInstance().execute(ClientHooks::openCodex);
        } else if (event.getItemStack().is(ModItems.RESEARCH_NOTE.get())) {
            var stack = event.getItemStack();
            var researchId = ResearchNoteItem.researchId(stack);
            if (researchId != null) {
                boolean complete = ResearchNoteItem.isComplete(stack);
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(
                        new ResearchNoteScreen(researchId, complete)));
            }
        }
    }
}
