package com.fredplenter.aspect;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record AspectRecipe(Aspect result, Aspect first, Aspect second) {

    private static final List<AspectRecipe> RECIPES = List.of(
            new AspectRecipe(Aspect.VAPOR, Aspect.FLAMMA, Aspect.UNDA),
            new AspectRecipe(Aspect.LIMUS, Aspect.UNDA, Aspect.HUMUS),
            new AspectRecipe(Aspect.CINIS, Aspect.FLAMMA, Aspect.HUMUS),
            new AspectRecipe(Aspect.PROCELLA, Aspect.FLATUS, Aspect.UNDA),
            new AspectRecipe(Aspect.PULVIS, Aspect.FLATUS, Aspect.HUMUS),
            new AspectRecipe(Aspect.AESTUS, Aspect.FLAMMA, Aspect.VAPOR),
            new AspectRecipe(Aspect.VITA, Aspect.LUX, Aspect.LIMUS),
            new AspectRecipe(Aspect.MORS, Aspect.TENEBRAE, Aspect.CINIS),
            new AspectRecipe(Aspect.SOMNUS, Aspect.TENEBRAE, Aspect.PROCELLA),
            new AspectRecipe(Aspect.MOTUS, Aspect.FLATUS, Aspect.PULVIS)
    );
    private static final Map<Aspect, AspectRecipe> BY_RESULT = RECIPES.stream()
            .collect(Collectors.toUnmodifiableMap(AspectRecipe::result, recipe -> recipe));

    public static List<AspectRecipe> all() {
        return RECIPES;
    }

    public static AspectRecipe forResult(Aspect result) {
        return BY_RESULT.get(result);
    }

    public static AspectRecipe forComponents(Aspect first, Aspect second) {
        return RECIPES.stream()
                .filter(recipe -> recipe.matches(first, second))
                .findFirst()
                .orElse(null);
    }

    public boolean matches(Aspect first, Aspect second) {
        return this.first == first && this.second == second
                || this.first == second && this.second == first;
    }

    public List<Aspect> components() {
        return List.of(first, second);
    }
}
