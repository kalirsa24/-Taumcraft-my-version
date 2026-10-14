package com.fredplenter.research;

import com.fredplenter.aspect.AspectRecipe;

public final class AspectCraftingRules {

    private AspectCraftingRules() {
    }

    public static boolean canCreate(AspectRecipe recipe, boolean compoundResearch, boolean mixtureResearch) {
        return recipe != null && compoundResearch
                && (recipe.first().isPrimal() && recipe.second().isPrimal() || mixtureResearch);
    }
}
