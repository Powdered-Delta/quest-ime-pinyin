package com.m0e_n00b.questimepinyin;
/** A candidate consumes a prefix in RAW composition coordinates, never Han character count. */
public final class PinyinCandidate {
    public final String id, text;
    public final int consumedEnd;
    public PinyinCandidate(String text, int consumedEnd) {
        if (text.isEmpty() || consumedEnd <= 0) throw new IllegalArgumentException("Invalid candidate");
        this.text = text;
        this.consumedEnd = consumedEnd;
        this.id = consumedEnd + ":" + text;
    }
}
