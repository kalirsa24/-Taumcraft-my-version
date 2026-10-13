package com.fredplenter.block.entity;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectStack;
import com.fredplenter.aspect.VanillaAspectMap;
import com.fredplenter.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The liquid-free "solution" inside a crucible: a pool of loose aspects built by
 * dissolving items. Nothing here is an item, so breaking the crucible loses the
 * mixture, which is the intended cost of a failed brew.
 */
public class CrucibleBlockEntity extends BlockEntity {

    public static final int MAX_PER_ASPECT = 48;
    private final EnumMap<Aspect, Integer> contents = new EnumMap<>(Aspect.class);

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    /** Dissolves one item into its aspects. Returns false for items with no aspect table entry. */
    public boolean dissolve(ItemStack stack) {
        List<AspectStack> aspects = VanillaAspectMap.aspectsFor(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (aspects.isEmpty()) {
            return false;
        }
        for (AspectStack aspect : aspects) {
            int current = contents.getOrDefault(aspect.aspect(), 0);
            contents.put(aspect.aspect(), Math.min(MAX_PER_ASPECT, current + aspect.amount()));
        }
        setChanged();
        return true;
    }

    /** Spends the aspects of a brewed recipe. Aspects that reach zero are removed. */
    public void consume(List<AspectStack> requirements) {
        for (AspectStack need : requirements) {
            int left = contents.getOrDefault(need.aspect(), 0) - need.amount();
            if (left > 0) {
                contents.put(need.aspect(), left);
            } else {
                contents.remove(need.aspect());
            }
        }
        setChanged();
    }

    public Map<Aspect, Integer> getContents() {
        return new EnumMap<>(contents);
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    /** Readable summary of the mixture, e.g. "Flamma 2, Humus 1". */
    public Component describe() {
        if (contents.isEmpty()) {
            return Component.translatable("message.fredplenter.crucible_empty");
        }
        MutableComponent summary = Component.empty();
        boolean first = true;
        for (Map.Entry<Aspect, Integer> entry : contents.entrySet()) {
            if (!first) {
                summary.append(", ");
            }
            summary.append(Component.translatable(entry.getKey().getTranslationKey()))
                    .append(" " + entry.getValue());
            first = false;
        }
        return Component.translatable("message.fredplenter.crucible_contents", summary);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag stored = new CompoundTag();
        for (Map.Entry<Aspect, Integer> entry : contents.entrySet()) {
            stored.putInt(entry.getKey().getId(), entry.getValue());
        }
        tag.put("Contents", stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.clear();
        CompoundTag stored = tag.getCompound("Contents");
        for (Aspect aspect : Aspect.values()) {
            int amount = stored.getInt(aspect.getId());
            if (amount > 0) {
                contents.put(aspect, Math.min(MAX_PER_ASPECT, amount));
            }
        }
    }
}
