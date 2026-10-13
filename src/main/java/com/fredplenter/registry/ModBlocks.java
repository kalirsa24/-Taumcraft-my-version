package com.fredplenter.registry;

import com.fredplenter.Fredplenter;
import com.fredplenter.block.AuraNodeBlock;
import com.fredplenter.block.ArcaneTableBlock;
import com.fredplenter.block.ArcaneTableExtensionBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Fredplenter.MODID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(Fredplenter.MODID);

    public static final DeferredBlock<ArcaneTableBlock> ARCANE_TABLE = BLOCKS.register(
            "arcane_table",
            () -> new ArcaneTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.BLOCK)
                    .noOcclusion()));

    public static final DeferredBlock<ArcaneTableExtensionBlock> ARCANE_TABLE_EXTENSION = BLOCKS.register(
            "arcane_table_extension",
            () -> new ArcaneTableExtensionBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.BLOCK)
                    .noOcclusion()
                    .noLootTable()));

    public static final DeferredBlock<AuraNodeBlock> AURA_NODE = BLOCKS.register(
            "aura_node",
            () -> new AuraNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.3f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> ARCANE_TABLE_ITEM = BLOCK_ITEMS.registerSimpleBlockItem(ARCANE_TABLE);
    public static final DeferredItem<BlockItem> AURA_NODE_ITEM = BLOCK_ITEMS.registerSimpleBlockItem(AURA_NODE);

    private ModBlocks() {
    }
}
