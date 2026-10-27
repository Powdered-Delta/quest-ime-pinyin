package com.m0e_n00b.questimepinyin;

/** Find only the active composition immediately before the mirror caret, never all matching words. */
public final class CompositionRange {
    private CompositionRange() { }
    public static int start(CharSequence text, int caret, String composition) {
        if (text == null || composition == null || composition.isEmpty()
                || caret < composition.length() || caret > text.length()) return -1;
        int begin = caret - composition.length();
        for (int i = 0; i < composition.length(); i++) {
            if (text.charAt(begin + i) != composition.charAt(i)) return -1;
        }
        return begin;
    }

    /** Finds the active composition range when the caret is inside it, not only at its end. */
    public static int containingStart(CharSequence text, int caret, String composition) {
        if (text == null || composition == null || composition.isEmpty()
                || caret < 0 || caret > text.length()) return -1;
        for (int begin = 0; begin + composition.length() <= text.length(); begin++) {
            int end = begin + composition.length();
            if (caret < begin || caret > end) continue;
            boolean match = true;
            for (int i = 0; i < composition.length(); i++) {
                if (text.charAt(begin + i) != composition.charAt(i)) { match = false; break; }
            }
            if (match) return begin;
        }
        return -1;
    }
}
