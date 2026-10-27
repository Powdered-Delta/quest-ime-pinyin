package com.m0e_n00b.questpinyinprobe;

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
}
