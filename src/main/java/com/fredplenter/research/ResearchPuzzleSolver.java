package com.fredplenter.research;

import com.fredplenter.aspect.Aspect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/** Finds minimum-point, non-repeating solutions on the Arcane Table's fixed board. */
public final class ResearchPuzzleSolver {

    private static final HexPosition START = new HexPosition(-2, 0);
    private static final HexPosition END = new HexPosition(2, 0);
    private static final int MAX_CELLS = 19;

    private ResearchPuzzleSolver() {
    }

    public static Solution solve(Research research) {
        if (research == null || research.puzzle().size() != 3) {
            return null;
        }
        Aspect start = research.puzzle().getFirst();
        Aspect required = research.puzzle().get(1);
        Aspect end = research.puzzle().getLast();
        if (start == required || start == end || required == end) {
            return null;
        }

        Queue<List<Aspect>> candidates = new ArrayDeque<>();
        candidates.add(List.of(start));
        while (!candidates.isEmpty()) {
            List<Aspect> sequence = candidates.remove();
            if (sequence.size() >= 4
                    && sequence.contains(required)
                    && sequence.getLast().isLinkedTo(end)) {
                List<Aspect> complete = new ArrayList<>(sequence);
                complete.add(end);
                List<HexPosition> path = findPath(complete.size());
                if (path != null) {
                    return new Solution(List.copyOf(complete), path);
                }
            }
            if (sequence.size() >= MAX_CELLS - 1) {
                continue;
            }
            Aspect last = sequence.getLast();
            for (Aspect next : Aspect.values()) {
                if (next != end && !sequence.contains(next) && last.isLinkedTo(next)) {
                    List<Aspect> extended = new ArrayList<>(sequence);
                    extended.add(next);
                    candidates.add(List.copyOf(extended));
                }
            }
        }
        return null;
    }

    public static boolean isValidSequence(Research research, List<Aspect> aspects,
                                          List<HexPosition> path) {
        if (research == null || research.puzzle().size() != 3
                || aspects.size() != path.size()
                || aspects.size() < 5 || aspects.size() > MAX_CELLS
                || aspects.getFirst() != research.puzzle().getFirst()
                || aspects.getLast() != research.puzzle().getLast()
                || !aspects.subList(1, aspects.size() - 1).contains(research.puzzle().get(1))
                || !path.getFirst().equals(START)
                || !path.getLast().equals(END)
                || new HashSet<>(aspects).size() != aspects.size()) {
            return false;
        }

        Set<HexPosition> visited = new HashSet<>();
        for (int index = 0; index < path.size(); index++) {
            HexPosition cell = path.get(index);
            if (!cell.isOnBoard() || !visited.add(cell)
                    || index > 0 && !path.get(index - 1).isAdjacent(cell)
                    || index > 0 && !aspects.get(index - 1).isLinkedTo(aspects.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static List<HexPosition> findPath(int cellCount) {
        Queue<List<HexPosition>> candidates = new ArrayDeque<>();
        candidates.add(List.of(START));
        while (!candidates.isEmpty()) {
            List<HexPosition> path = candidates.remove();
            HexPosition last = path.getLast();
            if (path.size() == cellCount) {
                if (last.equals(END)) {
                    return path;
                }
                continue;
            }
            if (path.size() + lastDistance(last, END) > cellCount) {
                continue;
            }
            for (HexPosition next : boardCells()) {
                if (last.isAdjacent(next) && !path.contains(next)) {
                    List<HexPosition> extended = new ArrayList<>(path);
                    extended.add(next);
                    candidates.add(List.copyOf(extended));
                }
            }
        }
        return null;
    }

    private static int lastDistance(HexPosition first, HexPosition second) {
        int q = second.q() - first.q();
        int r = second.r() - first.r();
        return Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(q + r)));
    }

    private static List<HexPosition> boardCells() {
        List<HexPosition> cells = new ArrayList<>(MAX_CELLS);
        for (int q = -2; q <= 2; q++) {
            for (int r = -2; r <= 2; r++) {
                HexPosition cell = new HexPosition(q, r);
                if (cell.isOnBoard()) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    public record Solution(List<Aspect> aspects, List<HexPosition> path) {
        public int pointCost() {
            return aspects.size() - 2;
        }
    }
}
