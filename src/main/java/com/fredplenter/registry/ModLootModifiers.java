package com.fredplenter.registry;

import com.fredplenter.Fredplenter;
import com.fredplenter.loot.FisherCatchModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Fredplenter.MODID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<FisherCatchModifier>> FISHER_CATCH =
            LOOT_MODIFIERS.register("fisher_catch", FisherCatchModifier.CODEC);

    private ModLootModifiers() {
    }
}
