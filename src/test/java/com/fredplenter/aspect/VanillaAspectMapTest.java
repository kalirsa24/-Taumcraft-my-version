package com.fredplenter.aspect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VanillaAspectMapTest {

    @Test
    void lavaBucketUsesItsExplicitFireOverride() {
        assertEquals(
                List.of(new AspectStack(Aspect.FLAMMA, 2), new AspectStack(Aspect.AESTUS, 1)),
                VanillaAspectRules.aspectsFor("minecraft", "lava_bucket"));
    }

    @Test
    void bowlDoesNotMatchTheBowRule() {
        List<AspectStack> aspects = VanillaAspectRules.aspectsFor("minecraft", "bowl");

        assertEquals(List.of(new AspectStack(Aspect.HUMUS, 1), new AspectStack(Aspect.VITA, 1)), aspects);
        assertFalse(aspects.stream().anyMatch(stack -> stack.aspect() == Aspect.MOTUS));
    }

    @Test
    void unmappedItemAmountsAreStable() {
        List<AspectStack> first = VanillaAspectRules.aspectsFor("minecraft", "stick");
        List<AspectStack> second = VanillaAspectRules.aspectsFor("minecraft", "stick");

        assertEquals(first, second);
        assertEquals(
                List.of(new AspectStack(Aspect.FLATUS, 1), new AspectStack(Aspect.PROCELLA, 1)),
                first);
    }

    @Test
    void nonVanillaItemsDoNotGrantVanillaAspects() {
        assertEquals(List.of(), VanillaAspectRules.aspectsFor("example", "item"));
    }
}
