package com.fredplenter.spell;

import com.fredplenter.Fredplenter;
import com.fredplenter.aspect.Aspect;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Predicate;

/**
 * Spells a Resonance Wand can cast.
 * <p>
 * Every spell is paid for with one primal aspect stored in the wand, and it
 * stays locked until the research listed here is learned in the Codex. The
 * actual effects live in {@link SpellEffects}.
 */
public enum Spell {

    GUST("gust", Aspect.FLATUS, 2, "focal_casting"),
    PYRE("pyre", Aspect.FLAMMA, 2, "focal_casting"),
    TIDE("tide", Aspect.UNDA, 2, "focal_casting"),
    RADIANCE("radiance", Aspect.LUX, 3, "focal_casting"),
    VERDURE("verdure", Aspect.HUMUS, 3, "verdant_weaving"),
    DREAD("dread", Aspect.TENEBRAE, 3, "dread_weaving");

    private final String id;
    private final Aspect aspect;
    private final int cost;
    private final ResourceLocation research;

    Spell(String id, Aspect aspect, int cost, String researchPath) {
        this.id = id;
        this.aspect = aspect;
        this.cost = cost;
        this.research = ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, researchPath);
    }

    public String getId() {
        return id;
    }

    public Aspect getAspect() {
        return aspect;
    }

    /** Amount of {@link #getAspect()} drained from the wand per cast. */
    public int getCost() {
        return cost;
    }

    public ResourceLocation getResearch() {
        return research;
    }

    public String getTranslationKey() {
        return "spell.fredplenter." + id;
    }

    public String getDescriptionKey() {
        return "spell.fredplenter." + id + ".desc";
    }

    public boolean isUnlocked(Predicate<ResourceLocation> learned) {
        return learned.test(research);
    }

    public static Spell byId(String id) {
        for (Spell spell : values()) {
            if (spell.id.equals(id)) {
                return spell;
            }
        }
        return null;
    }

    public static Spell firstUnlocked(Predicate<ResourceLocation> learned) {
        return nextUnlocked(learned, null);
    }

    /**
     * The next unlocked spell after {@code current}, wrapping around. Returns
     * {@code current} itself when it is the only unlocked spell, and {@code null}
     * when nothing is unlocked.
     */
    public static Spell nextUnlocked(Predicate<ResourceLocation> learned, Spell current) {
        Spell[] all = values();
        int start = current == null ? 0 : current.ordinal() + 1;
        for (int offset = 0; offset < all.length; offset++) {
            Spell candidate = all[(start + offset) % all.length];
            if (candidate.isUnlocked(learned)) {
                return candidate;
            }
        }
        return null;
    }
}
