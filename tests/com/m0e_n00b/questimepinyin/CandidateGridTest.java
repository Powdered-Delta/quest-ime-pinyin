package com.m0e_n00b.questimepinyin;

import java.util.Arrays;
import java.util.List;

public final class CandidateGridTest {
    public static void main(String[] args) {
        List<List<Integer>> rows = CandidateGrid.pack(new int[] {50, 180, 60, 70, 220}, 0, 200, 8, 84);
        expect(rows, Arrays.asList(Arrays.asList(0), Arrays.asList(1), Arrays.asList(2, 3), Arrays.asList(4)));
        rows = CandidateGrid.pack(new int[] {500}, 0, 200, 8, 84);
        expect(rows, Arrays.asList(Arrays.asList(0)));
        System.out.println("CandidateGridTest: variable-width packing passed");
    }

    private static void expect(List<List<Integer>> actual, List<List<Integer>> expected) {
        if (!actual.equals(expected)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
