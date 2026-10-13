package com.fredplenter.registry;

import com.fredplenter.Fredplenter;
import com.fredplenter.block.entity.AuraNodeBlockEntity;
import com.fredplenter.block.entity.ArcaneTableBlockEntity;
import com.fredplenter.block.entity.CrucibleBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, Fredplenter.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneTableBlockEntity>> ARCANE_TABLE = BLOCK_ENTITIES.register(
            "arcane_table",
            () -> BlockEntityType.Builder.of(ArcaneTableBlockEntity::new, ModBlocks.ARCANE_TABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AuraNodeBlockEntity>> AURA_NODE = BLOCK_ENTITIES.register(
            "aura_node",
            () -> BlockEntityType.Builder.of(AuraNodeBlockEntity::new, ModBlocks.AURA_NODE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> CRUCIBLE = BLOCK_ENTITIES.register(
            "crucible",
            () -> BlockEntityType.Builder.of(CrucibleBlockEntity::new, ModBlocks.CRUCIBLE.get()).build(null));

    private ModBlockEntities() {
    }
}
