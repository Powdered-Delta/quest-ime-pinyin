package com.m0e_n00b.questimepinyin;

public final class CandidateFitTest {
    public static void main(String[] args) {
        check(0, new int[]{}, 400, 4, 48);
        check(3, new int[]{50, 120, 70}, 248, 4, 48); // All fit: no arrow reservation.
        check(1, new int[]{50, 120, 70}, 200, 4, 48); // Overflow forces space for arrow.
        check(7, new int[]{40, 40, 40, 40, 40, 40, 40, 100}, 360, 4, 48);
        check(0, new int[]{90, 40}, 100, 4, 48); // Never skip the first wider candidate.
        check(1, new int[]{80}, 80, 4, 48);
        check(0, new int[]{80}, 0, 4, 48);
        check(1, new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE}, Integer.MAX_VALUE, 0, 0);
        System.out.println("CandidateFitTest: 8 natural-width partition cases passed");
    }
    private static void check(int expected, int[] widths, int available, int gap, int arrow) {
        int actual = CandidateFit.visibleCount(widths, available, gap, arrow);
        if (actual != expected) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
