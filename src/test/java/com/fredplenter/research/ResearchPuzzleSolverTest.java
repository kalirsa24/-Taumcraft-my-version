package com.fredplenter.research;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchPuzzleSolverTest {

    @Test
    void paperResearchHasSolvablePuzzlesAndAspectResearchHasNoPuzzleData() {
        assertEquals(32, ResearchRegistry.all().size());
        for (Research research : ResearchRegistry.all().values()) {
            assertNotNull(research.mapIconAspect(), research.id().toString());
            if (research.method() == Research.Method.ASPECTS) {
                assertTrue(research.puzzle().isEmpty(), research.id().toString());
                continue;
            }

            ResearchPuzzleSolver.Solution solution = ResearchPuzzleSolver.solve(research);
            assertNotNull(solution, research.id().toString());
            assertTrue(ResearchPuzzleSolver.isValidSequence(
                    research, solution.aspects(), solution.path()), research.id().toString());
            assertTrue(solution.pointCost() > 0, research.id().toString());
        }
    }
}
