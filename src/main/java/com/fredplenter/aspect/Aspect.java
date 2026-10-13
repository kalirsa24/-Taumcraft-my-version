package com.fredplenter.aspect;

import com.fredplenter.Fredplenter;
import net.minecraft.resources.ResourceLocation;

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
    FLAMMA("flamma", 0xE0522B),   // fire
    UNDA("unda", 0x2B7FE0),       // water
    HUMUS("humus", 0x7A5230),     // earth
    FLATUS("flatus", 0xDCEBEF),   // air
    LUX("lux", 0xF5E67A),         // light
    TENEBRAE("tenebrae", 0x3A1E4D), // shadow

    // --- Compound aspects -------------------------------------------------
    VAPOR("vapor", 0xB9C7CE),      // steam: fire + water
    LIMUS("limus", 0x4B5A3A),      // mud: water + earth
    CINIS("cinis", 0x8A8178),      // ash: fire + earth
    PROCELLA("procella", 0x6E8FAF), // storm: air + water
    PULVIS("pulvis", 0xC9B38C),    // dust: air + earth
    AESTUS("aestus", 0xE08A2B),    // heat/glow: fire + steam
    VITA("vita", 0x5FBF5A),        // life: light + mud
    MORS("mors", 0x5A3A5A),       // death: shadow + ash
    SOMNUS("somnus", 0x5A5A8A),   // sleep/dream: shadow + storm
    MOTUS("motus", 0xD9C23A);     // motion: air + dust

    private final String id;
    private final int color;

    Aspect(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public ResourceLocation getRegistryName() {
        return ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, id);
    }

    public String getTranslationKey() {
        return "aspect.fredplenter." + id;
    }

    public int getColor() {
        return color;
    }

    public boolean isPrimal() {
        return AspectRecipe.forResult(this) == null;
    }

    public static Aspect byId(String id) {
        for (Aspect aspect : values()) {
            if (aspect.id.equals(id)) {
                return aspect;
            }
        }
        return null;
    }

    public boolean isLinkedTo(Aspect other) {
        if (other == null || other == this) {
            return false;
        }
        List<Aspect> components = getComponents();
        List<Aspect> otherComponents = other.getComponents();
        return components.contains(other)
                || otherComponents.contains(this)
                || components.stream().anyMatch(otherComponents::contains);
    }

    public List<Aspect> getComponents() {
        AspectRecipe recipe = AspectRecipe.forResult(this);
        return recipe == null ? List.of() : recipe.components();
    }
}
