package com.fredplenter.loot;

import com.fredplenter.Fredplenter;
import com.fredplenter.FredplenterConfig;
import com.fredplenter.research.ArcaneProgress;
import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.function.Supplier;

public final class FisherCatchModifier extends LootModifier {
    public static final Supplier<MapCodec<FisherCatchModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, FisherCatchModifier::new)));
    private static final ResourceLocation RESEARCH =
            ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "fisher_catch");

    public FisherCatchModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!FredplenterConfig.bonusBranchesEnabled()
                || !(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof FishingHook hook)
                || !(hook.getPlayerOwner() instanceof ServerPlayer player)
                || !ArcaneProgress.get(player).hasResearch(player.getUUID(), RESEARCH)
                || loot.isEmpty() || context.getRandom().nextInt(10) != 0) {
            return loot;
        }
        int originalSize = loot.size();
        for (int index = 0; index < originalSize; index++) {
            if (!loot.get(index).isEmpty() && loot.get(index).is(ItemTags.FISHES)) {
                loot.add(loot.get(index).copy());
            }
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
