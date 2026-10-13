package com.fredplenter.client;

import com.fredplenter.Fredplenter;
import com.fredplenter.client.screen.ArcaneTableScreen;
import com.fredplenter.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Fredplenter.MODID, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ARCANE_TABLE.get(), ArcaneTableScreen::new);
    }
}
