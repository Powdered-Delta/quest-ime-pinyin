package com.m0e_n00b.questimepinyin;

public final class CompositionRangeTest {
    public static void main(String[] args) {
        expect(0, "nihao", 5, "nihao");
        expect(6, "nihao nihao", 11, "nihao");
        expect(1, "你hao", 4, "hao");
        expect(-1, "nihao", 2, "nihao");
        expect(-1, "nihao", 7, "nihao");
        expect(-1, "hello", 5, "nihao");
        expect(-1, "你好", 2, "");
        expectContaining(0, "cesbi", 4, "cesbi");
        expectContaining(2, "xxcesbiyy", 5, "cesbi");
        expectContaining(0, "cesbi", 0, "cesbi");
        System.out.println("CompositionRangeTest: active-range cases passed");
    }
    private static void expect(int expected, String text, int caret, String composition) {
        if (CompositionRange.start(text, caret, composition) != expected) throw new AssertionError("Wrong composing range");
    }
    private static void expectContaining(int expected, String text, int caret, String composition) {
        if (CompositionRange.containingStart(text, caret, composition) != expected) throw new AssertionError("Wrong containing range");
    }
}
