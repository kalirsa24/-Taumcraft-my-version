package com.fredplenter.registry;

import com.fredplenter.Fredplenter;
import com.fredplenter.item.ArcaneCodexItem;
import com.fredplenter.item.ArcaneWandItem;
import com.fredplenter.item.AspectCacheItem;
import com.fredplenter.item.AuraScannerItem;
import com.fredplenter.item.ResearchNoteItem;
import com.fredplenter.item.ScribingToolsItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fredplenter.MODID);

    public static final DeferredItem<ArcaneCodexItem> ARCANE_CODEX = ITEMS.registerItem(
            "arcane_codex",
            ArcaneCodexItem::new,
            new Item.Properties().stacksTo(1));

    public static final DeferredItem<ArcaneWandItem> ARCANE_WAND = ITEMS.registerItem(
            "arcane_wand", ArcaneWandItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<AuraScannerItem> AURA_SCANNER = ITEMS.registerItem(
            "aura_scanner", AuraScannerItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<AspectCacheItem> ASPECT_CACHE = ITEMS.registerItem(
            "aspect_cache", AspectCacheItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<ScribingToolsItem> SCRIBING_TOOLS = ITEMS.registerItem(
            "scribing_tools", ScribingToolsItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<ResearchNoteItem> RESEARCH_NOTE = ITEMS.registerItem(
            "research_note", ResearchNoteItem::new);

    private ModItems() {
    }
}
