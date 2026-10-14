package com.fredplenter.research;

import com.fredplenter.aspect.Aspect;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ResearchPuzzleCosts {

    private ResearchPuzzleCosts() {
    }

    public static int tileCost(int pathIndex, boolean tableMastery) {
        return tableMastery && pathIndex == 1 ? 0 : 1;
    }

    public static Map<Aspect, Integer> aspectCosts(List<Aspect> sequence, boolean tableMastery) {
        Map<Aspect, Integer> costs = new EnumMap<>(Aspect.class);
        for (int index = 1; index < sequence.size() - 1; index++) {
            int cost = tileCost(index, tableMastery);
            if (cost > 0) {
                costs.merge(sequence.get(index), cost, Integer::sum);
            }
        }
        return Map.copyOf(costs);
    }
}
