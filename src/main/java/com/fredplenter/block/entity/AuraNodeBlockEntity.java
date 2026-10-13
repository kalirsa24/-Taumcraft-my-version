package com.fredplenter.block.entity;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectStack;
import com.fredplenter.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AuraNodeBlockEntity extends BlockEntity {

    private static final int MAX_VIS = 24;
    private static final int REGEN_INTERVAL = 600;
    private final EnumMap<Aspect, Integer> vis = new EnumMap<>(Aspect.class);
    private boolean initialized;

    public AuraNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AURA_NODE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AuraNodeBlockEntity node) {
        if (!node.initialized) {
            node.initializeVis(level, pos);
        }
        if (level.getGameTime() % REGEN_INTERVAL == Math.floorMod(pos.asLong(), REGEN_INTERVAL)) {
            node.regenerate();
        }
    }

    public int extract(Aspect aspect, int amount) {
        int current = vis.getOrDefault(aspect, 0);
        int drained = Math.min(Math.max(0, amount), current);
        if (drained > 0) {
            vis.put(aspect, current - drained);
            setChanged();
        }
        return drained;
    }

    public List<AspectStack> getScanAspects() {
        List<AspectStack> aspects = new ArrayList<>();
        for (Map.Entry<Aspect, Integer> entry : vis.entrySet()) {
            aspects.add(new AspectStack(entry.getKey(), 1));
        }
        return List.copyOf(aspects);
    }

    public int getVis(Aspect aspect) {
        return vis.getOrDefault(aspect, 0);
    }

    public Component describe() {
        return Component.translatable("message.fredplenter.node_vis", vis.size());
    }

    private void initializeVis(Level level, BlockPos pos) {
        initialized = true;
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(pos.asLong());
        Aspect[] aspects = java.util.Arrays.stream(Aspect.values())
                .filter(Aspect::isPrimal)
                .toArray(Aspect[]::new);
        int count = 3 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            Aspect aspect = aspects[random.nextInt(aspects.length)];
            vis.put(aspect, 8 + random.nextInt(9));
        }
        setChanged();
    }

    private void regenerate() {
        boolean changed = false;
        for (Map.Entry<Aspect, Integer> entry : vis.entrySet()) {
            if (entry.getValue() < MAX_VIS) {
                entry.setValue(entry.getValue() + 1);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag storedVis = new CompoundTag();
        for (Map.Entry<Aspect, Integer> entry : vis.entrySet()) {
            storedVis.putInt(entry.getKey().getId(), entry.getValue());
        }
        tag.put("Vis", storedVis);
        tag.putBoolean("Initialized", initialized);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        vis.clear();
        CompoundTag storedVis = tag.getCompound("Vis");
        for (Aspect aspect : Aspect.values()) {
            int amount = storedVis.getInt(aspect.getId());
            if (amount > 0) {
                vis.put(aspect, Math.min(MAX_VIS, amount));
            }
        }
        initialized = tag.getBoolean("Initialized");
    }
}
