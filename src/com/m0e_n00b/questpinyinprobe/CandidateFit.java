package com.m0e_n00b.questpinyinprobe;

/** Same two-pass natural-width rule as SearchCandidateStrip: reserve an arrow only on overflow. */
public final class CandidateFit {
    private CandidateFit() { }
    public static int visibleCount(int[] widths, int available, int gap, int arrow) {
        if (available < 0 || gap < 0 || arrow < 0) throw new IllegalArgumentException("Negative geometry");
        for (int width : widths) if (width < 0) throw new IllegalArgumentException("Negative candidate width");
        int all = fit(widths, available, gap, 0);
        return all == widths.length ? all : fit(widths, available, gap, (long) arrow + gap);
    }
    private static int fit(int[] widths, int available, int gap, long reserved) {
        long used = 0;
        int count = 0;
        for (int width : widths) {
            long next = used + width + (count == 0 ? 0 : gap);
            if (next + reserved > available) break;
            used = next;
            count++;
        }
        return count;
    }
}
