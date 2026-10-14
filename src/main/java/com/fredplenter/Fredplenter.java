package com.fredplenter;

import com.fredplenter.network.NetworkHandler;
import com.fredplenter.registry.ModBlockEntities;
import com.fredplenter.registry.ModBlocks;
import com.fredplenter.registry.ModCreativeTab;
import com.fredplenter.registry.ModItems;
import com.fredplenter.registry.ModLootModifiers;
import com.fredplenter.registry.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fredplenter - an original, open-source Thaumonomicon-inspired magic mod.
 * <p>
 * Nothing in this project reuses code, assets or data from any other mod.
 * The aspect system, the research tree, the Arcane Table block/menu and the
 * "fuse two crafting tables with a book" ritual are this project's own
 * implementation of a familiar genre idea - the same way many Minecraft
 * magic mods share a family resemblance without sharing code.
 */
@Mod(Fredplenter.MODID)
public class Fredplenter {

    public static final String MODID = "fredplenter";

    public Fredplenter(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);
        ModCreativeTab.register(modEventBus);

        NetworkHandler.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(NetworkHandler::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(NetworkHandler::onPlayerRespawned);
        NeoForge.EVENT_BUS.addListener(NetworkHandler::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(NetworkHandler::onPlayerLoggedOut);
    }
}
