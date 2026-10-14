package com.fredplenter.research;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AspectConversionTest {

    @Test
    void singleConversionConsumesAtMostOneBalancedPoint() {
        assertEquals(1, AspectConversion.amount(8, 3, 0, 100, false));
    }

    @Test
    void maximumConversionIsBoundedByBothInputsAndResultCapacity() {
        assertEquals(2, AspectConversion.amount(8, 5, 8, 10, true));
    }

    @Test
    void maximumConversionNeverCreatesNegativeOrOverCapacityAmounts() {
        assertEquals(0, AspectConversion.amount(-1, 5, 0, 10, true));
        assertEquals(0, AspectConversion.amount(5, 5, 10, 10, true));
    }
}
