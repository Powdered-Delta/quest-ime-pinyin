package com.m0e_n00b.questimepinyin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Packs variable-width expanded candidates into rows without clipping wide words. */
final class CandidateGrid {
    private CandidateGrid() { }

    static List<List<Integer>> pack(int[] widths, int start, int availableWidth, int gap, int minimumCellWidth) {
        if (widths == null) throw new IllegalArgumentException("widths must not be null");
        if (start < 0 || start > widths.length) throw new IllegalArgumentException("invalid start");
        if (availableWidth <= 0 || gap < 0 || minimumCellWidth <= 0) {
            throw new IllegalArgumentException("invalid grid dimensions");
        }
        List<List<Integer>> rows = new ArrayList<>();
        List<Integer> row = new ArrayList<>();
        int used = 0;
        for (int index = start; index < widths.length; index++) {
            int width = Math.max(minimumCellWidth, Math.min(widths[index], availableWidth));
            int next = row.isEmpty() ? width : used + gap + width;
            if (!row.isEmpty() && next > availableWidth) {
                rows.add(Collections.unmodifiableList(row));
                row = new ArrayList<>();
                used = 0;
                next = width;
            }
            row.add(index);
            used = next;
        }
        if (!row.isEmpty()) rows.add(Collections.unmodifiableList(row));
        return Collections.unmodifiableList(rows);
    }
}
