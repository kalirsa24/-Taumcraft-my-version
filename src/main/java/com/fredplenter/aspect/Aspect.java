package com.fredplenter.aspect;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Fredplenter's own aspect system.
 * <p>
 * This is an original set of "primal" aspects plus "compound" aspects built
 * out of two others, in the same general spirit as classical-element magic
 * systems used across the genre - but with Fredplenter's own names, colors
 * and combinations. Nothing here is copied from any other mod's code,
 * textures or data files.
 */
public enum Aspect {

    // --- Primal aspects -------------------------------------------------
    FLAMMA("flamma", 0xE0522B, null, null),   // fire
    UNDA("unda", 0x2B7FE0, null, null),       // water
    HUMUS("humus", 0x7A5230, null, null),     // earth
    FLATUS("flatus", 0xDCEBEF, null, null),   // air
    LUX("lux", 0xF5E67A, null, null),         // light
    TENEBRAE("tenebrae", 0x3A1E4D, null, null), // shadow

    // --- Compound aspects -------------------------------------------------
    VAPOR("vapor", 0xB9C7CE, FLAMMA, UNDA),      // steam: fire + water
    LIMUS("limus", 0x4B5A3A, UNDA, HUMUS),       // mud: water + earth
    CINIS("cinis", 0x8A8178, FLAMMA, HUMUS),     // ash: fire + earth
    PROCELLA("procella", 0x6E8FAF, FLATUS, UNDA), // storm: air + water
    PULVIS("pulvis", 0xC9B38C, FLATUS, HUMUS),   // dust: air + earth
    AESTUS("aestus", 0xE08A2B, FLAMMA, VAPOR),    // heat/glow: fire + steam
    VITA("vita", 0x5FBF5A, LUX, LIMUS),           // life: light + mud
    MORS("mors", 0x5A3A5A, TENEBRAE, CINIS),     // death: shadow + ash
    SOMNUS("somnus", 0x5A5A8A, TENEBRAE, PROCELLA), // sleep/dream: shadow + storm
    MOTUS("motus", 0xD9C23A, FLATUS, PULVIS);    // motion: air + dust

    private final String id;
    private final int color;
    private final Aspect parentA;
    private final Aspect parentB;

    Aspect(String id, int color, Aspect parentA, Aspect parentB) {
        this.id = id;
        this.color = color;
        this.parentA = parentA;
        this.parentB = parentB;
    }

    public String getId() {
        return id;
    }

    public ResourceLocation getRegistryName() {
        return ResourceLocation.fromNamespaceAndPath("fredplenter", id);
    }

    public String getTranslationKey() {
        return "aspect.fredplenter." + id;
    }

    public int getColor() {
        return color;
    }

    public boolean isPrimal() {
        return parentA == null && parentB == null;
    }

    public static Aspect byId(String id) {
        for (Aspect a : values()) if (a.id.equals(id)) return a;
        return null;
    }
    /** The compound made from a+b (order-independent), or null if none exists. */
    public static Aspect combine(Aspect a, Aspect b) {
        if (a == null || b == null || a == b) return null;
        for (Aspect c : values()) {
            if (c.parentA != null && ((c.parentA == a && c.parentB == b) || (c.parentA == b && c.parentB == a))) return c;
        }
        return null;
    }
    /**
     * Can two aspect tiles touch in a research link? They link when one is made
     * from the other, or when they share a component. Two different primals never
     * link directly - you must bridge them with a compound.
     */
    public boolean isLinkedTo(Aspect other) {
        if (other == null || other == this) return false;
        if (getComponents().contains(other) || other.getComponents().contains(this)) return true;
        for (Aspect c : getComponents()) if (other.getComponents().contains(c)) return true;
        return false;
    }

    public List<Aspect> getComponents() {
        List<Aspect> list = new ArrayList<>();
        if (parentA != null) list.add(parentA);
        if (parentB != null) list.add(parentB);
        return list;
    }
}
