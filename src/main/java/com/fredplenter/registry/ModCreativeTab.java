package com.fredplenter.registry;

import com.fredplenter.Fredplenter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Fredplenter.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fredplenter.main"))
                    .icon(() -> new ItemStack(ModItems.ARCANE_CODEX.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ARCANE_CODEX.get());
                        output.accept(ModItems.ARCANE_WAND.get());
                        output.accept(ModItems.AURA_SCANNER.get());
                        output.accept(ModItems.SCRIBING_TOOLS.get());
                        output.accept(ModItems.INKWELL.get());
                        output.accept(ModItems.ASPECT_CACHE.get());
                        output.accept(ModBlocks.ARCANE_TABLE_ITEM.get());
                        output.accept(ModBlocks.AURA_NODE_ITEM.get());
                    })
                    .build());

    private ModCreativeTab() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
