package com.fredplenter.research;

/** Pure balance calculation for converting two component point balances into a compound aspect. */
public final class AspectConversion {

    private AspectConversion() {
    }

    public static int amount(int first, int second, int currentResult, int resultLimit,
                             boolean maximum) {
        int available = Math.min(Math.max(0, first), Math.max(0, second));
        available = Math.min(available, Math.max(0, resultLimit - currentResult));
        return maximum ? available : Math.min(1, available);
    }
}
