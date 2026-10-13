package com.fredplenter.research;

public record HexPosition(int q, int r) {

    public boolean isAdjacent(HexPosition other) {
        int deltaQ = other.q - q;
        int deltaR = other.r - r;
        return Math.max(Math.abs(deltaQ), Math.max(Math.abs(deltaR), Math.abs(deltaQ + deltaR))) == 1;
    }

    public boolean isOnBoard() {
        return Math.abs(q) <= 2 && Math.abs(r) <= 2 && Math.abs(q + r) <= 2;
    }
}
