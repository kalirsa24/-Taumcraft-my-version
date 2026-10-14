package com.fredplenter.research;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.aspect.AspectRecipe;
import com.fredplenter.aspect.AspectStack;
import com.fredplenter.aspect.AspectTagResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRulesTest {

    @Test
    void compoundResearchUnlocksOnlyPrimalComponentRecipes() {
        for (AspectRecipe recipe : AspectRecipe.all()) {
            boolean usesOnlyPrimals = recipe.first().isPrimal() && recipe.second().isPrimal();
            assertEquals(usesOnlyPrimals, AspectCraftingRules.canCreate(recipe, true, false),
                    recipe.result().getId());
            assertFalse(AspectCraftingRules.canCreate(recipe, false, true), recipe.result().getId());
            assertTrue(AspectCraftingRules.canCreate(recipe, true, true), recipe.result().getId());
        }
    }

    @Test
    void renewableKnowledgeHonorsThePerAspectCap() {
        ArcaneProgress progress = new ArcaneProgress();
        UUID playerId = UUID.randomUUID();

        assertEquals(99, progress.addRenewableKnowledge(playerId, Aspect.MOTUS, 99));
        assertEquals(1, progress.addRenewableKnowledge(playerId, Aspect.MOTUS, 10));
        assertEquals(0, progress.addRenewableKnowledge(playerId, Aspect.MOTUS, 1));
        assertEquals(ArcaneProgress.MAX_ASPECT_POINTS,
                progress.getAspectPoints(playerId, Aspect.MOTUS));
    }

    @Test
    void createCraftingRewardCanOnlyBeClaimedOncePerOutputItem() {
        ArcaneProgress progress = new ArcaneProgress();
        UUID playerId = UUID.randomUUID();
        var output = net.minecraft.resources.ResourceLocation.withDefaultNamespace("andesite_alloy");

        assertTrue(progress.markCreateItemCrafted(playerId, output));
        assertFalse(progress.markCreateItemCrafted(playerId, output));
        assertTrue(progress.markCreateItemCrafted(playerId,
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("shaft")));
    }

    @Test
    void tableMasteryWaivesOnlyTheFirstIntermediateTile() {
        assertEquals(1, ResearchPuzzleCosts.tileCost(1, false));
        assertEquals(0, ResearchPuzzleCosts.tileCost(1, true));
        assertEquals(1, ResearchPuzzleCosts.tileCost(2, true));
        assertEquals(1, ResearchPuzzleCosts.tileCost(3, true));
    }

    @Test
    void aspectResearchUnlocksAndPaysCompoundCreation() {
        ArcaneProgress progress = new ArcaneProgress();
        UUID playerId = UUID.randomUUID();
        var flammaItem = net.minecraft.resources.ResourceLocation.withDefaultNamespace("blaze_rod");
        var undaItem = net.minecraft.resources.ResourceLocation.withDefaultNamespace("water_bucket");
        progress.markItemStudied(playerId, flammaItem, List.of(new AspectStack(Aspect.FLAMMA, 4)));
        progress.markItemStudied(playerId, undaItem, List.of(new AspectStack(Aspect.UNDA, 3)));

        assertEquals(0, progress.createAspect(playerId, Aspect.VAPOR, true));
        var compoundAspects = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                "fredplenter", "compound_aspects");
        assertTrue(progress.setResearch(playerId, compoundAspects, true));
        assertEquals(3, progress.createAspect(playerId, Aspect.VAPOR, true));
        assertEquals(1, progress.getAspectPoints(playerId, Aspect.FLAMMA));
        assertEquals(0, progress.getAspectPoints(playerId, Aspect.UNDA));
        assertEquals(3, progress.getAspectPoints(playerId, Aspect.VAPOR));
    }

    @Test
    void completedPuzzleCostMapIncludesOnlyIntermediateAspectTiles() {
        Research research = ResearchRegistry.get(net.minecraft.resources.ResourceLocation
                .fromNamespaceAndPath("fredplenter", "basic_observation"));
        ResearchPuzzleSolver.Solution solution = ResearchPuzzleSolver.solve(research);

        var costs = ResearchPuzzleCosts.aspectCosts(solution.aspects(), false);
        assertEquals(solution.pointCost(), costs.values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(ResearchPuzzleSolver.isValidSequence(research, solution.aspects(), solution.path()));

        var masteredCosts = ResearchPuzzleCosts.aspectCosts(solution.aspects(), true);
        assertEquals(solution.pointCost() - 1,
                masteredCosts.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void recipeInheritanceNeedsMappedIngredientsAndPicksTheTwoMostCommonAspects() {
        var derived = AspectTagResolver.deriveRecipeAspects(List.of(
                List.of(new AspectStack(Aspect.HUMUS, 1)),
                List.of(new AspectStack(Aspect.FLAMMA, 1)),
                List.of(new AspectStack(Aspect.HUMUS, 1))));
        assertEquals(List.of(new AspectStack(Aspect.HUMUS, 1), new AspectStack(Aspect.FLAMMA, 1)), derived);
        assertTrue(AspectTagResolver.deriveRecipeAspects(List.of(
                List.of(new AspectStack(Aspect.HUMUS, 1)), List.of())).isEmpty());
    }
}
