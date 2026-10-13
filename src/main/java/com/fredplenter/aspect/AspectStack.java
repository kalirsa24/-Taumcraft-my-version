package com.fredplenter.aspect;

import net.minecraft.nbt.CompoundTag;

/**
 * A simple (aspect, amount) pair - the unit research costs and essentia
 * amounts are expressed in throughout Fredplenter.
 */
public record AspectStack(Aspect aspect, int amount) {

    public AspectStack withAmount(int newAmount) {
        return new AspectStack(aspect, newAmount);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("aspect", aspect.getId());
        tag.putInt("amount", amount);
        return tag;
    }

    public static AspectStack load(CompoundTag tag) {
        String id = tag.getString("aspect");
        int amount = tag.getInt("amount");
        for (Aspect aspect : Aspect.values()) {
            if (aspect.getId().equals(id)) {
                return new AspectStack(aspect, amount);
            }
        }
        return new AspectStack(Aspect.LUX, amount);
    }
}
