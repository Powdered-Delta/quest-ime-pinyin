package com.m0e_n00b.questpinyinprobe;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

/** Fixed test mapping only. This is not a general Chinese conversion engine. */
public final class FixedPinyinSession {
    private static final int MAX_COMPOSITION = 64;
    private static final List<String> NI = Collections.unmodifiableList(Arrays.asList(
            "你", "呢", "泥", "尼", "拟", "逆", "倪", "腻", "妮", "霓",
            "昵", "溺", "匿", "睨", "旎", "鲵", "怩", "铌", "猊", "伲"));
    private static final List<String> HAO = Collections.unmodifiableList(Arrays.asList(
            "好", "号", "浩", "豪", "毫", "郝", "耗", "皓", "昊", "灏",
            "蒿", "嚎", "壕", "蚝", "镐", "濠", "薅", "貉", "颢", "嗥"));
    private final StringBuilder composition = new StringBuilder();

    /** Consumption is in raw composing coordinates, independently of completed spelling. */
    private static final class Candidate {
        final String text;
        final int consumedEnd;
        Candidate(String text, int consumedEnd) { this.text = text; this.consumedEnd = consumedEnd; }
    }

    public static final class Result {
        public final String committedText;
        public final String composition;
        public final List<String> candidates;
        public final boolean deleteBeforeCursor;
        public final boolean requestEditorAction;

        private Result(String committed, String composing, List<String> choices,
                       boolean delete, boolean action) {
            committedText = committed;
            composition = composing;
            candidates = choices;
            deleteBeforeCursor = delete;
            requestEditorAction = action;
        }
    }

    public String getComposition() {
        return composition.toString();
    }

    public List<String> getCandidates() {
        List<String> choices = new ArrayList<>();
        for (Candidate candidate : resolveCandidates()) choices.add(candidate.text);
        return Collections.unmodifiableList(choices);
    }

    private List<Candidate> resolveCandidates() {
        String raw = getComposition().toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder();
        int[] rawEnds = new int[raw.length()];
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) != '\'') {
                rawEnds[normalized.length()] = i + 1;
                normalized.append(raw.charAt(i));
            }
        }
        String query = normalized.toString();
        List<Candidate> choices = new ArrayList<>();
        if (query.isEmpty()) return choices;
        // The fixture follows the production engine's prefix/range contract. It remains
        // limited to ni/hao; completed predictions consume only letters actually typed.
        if (query.startsWith("nihao") || (query.length() > 2 && "nihao".startsWith(query))) {
            choices.add(new Candidate("你好", rawEnds[Math.min(5, query.length()) - 1]));
        }
        if (query.startsWith("ni") || "ni".startsWith(query)) {
            for (String text : NI) choices.add(new Candidate(text, rawEnds[Math.min(2, query.length()) - 1]));
        } else if (query.startsWith("hao") || "hao".startsWith(query)) {
            for (String text : HAO) choices.add(new Candidate(text, rawEnds[Math.min(3, query.length()) - 1]));
        }
        return choices;
    }

    public Result feed(String text) {
        if (text == null) throw new IllegalArgumentException("text must not be null");
        StringBuilder committed = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '\'') {
                if (composition.length() >= MAX_COMPOSITION) {
                    committed.append(composition);
                    composition.setLength(0);
                }
                composition.append(c);
            } else if (c == ' ') {
                committed.append(space().committedText);
            } else {
                committed.append(composition).append(c);
                composition.setLength(0);
            }
        }
        return result(committed.toString(), false, false);
    }

    public Result backspace() {
        if (composition.length() == 0) return result("", true, false);
        composition.deleteCharAt(composition.length() - 1);
        return result("", false, false);
    }

    public Result commitCandidate(int index) {
        List<Candidate> choices = resolveCandidates();
        if (index < 0 || index >= choices.size()) return result("", false, false);
        Candidate choice = choices.get(index);
        composition.delete(0, choice.consumedEnd);
        while (composition.length() > 0 && composition.charAt(0) == '\'') composition.deleteCharAt(0);
        return result(choice.text, false, false);
    }

    public Result space() {
        if (!getCandidates().isEmpty()) return commitCandidate(0);
        String committed = getComposition() + " ";
        composition.setLength(0);
        return result(committed, false, false);
    }

    public Result enter() {
        if (!getCandidates().isEmpty()) return commitCandidate(0);
        if (composition.length() == 0) return result("", false, true);
        String committed = getComposition();
        composition.setLength(0);
        return result(committed, false, false);
    }

    /** Discards pending input on focus/session changes; never emits stale text. */
    public Result reset() {
        composition.setLength(0);
        return result("", false, false);
    }

    private Result result(String committed, boolean delete, boolean action) {
        return new Result(committed, getComposition(), getCandidates(), delete, action);
    }
}
