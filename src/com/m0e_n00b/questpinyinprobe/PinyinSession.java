package com.m0e_n00b.questpinyinprobe;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/** Worker-confined composition with interchangeable candidate sources. */
public class PinyinSession {
    private static final int MAX_COMPOSITION = 64;
    private final StringBuilder composition = new StringBuilder();
    private final PinyinBackend backend;
    private List<PinyinCandidate> choices = Collections.emptyList();
    public PinyinSession(PinyinBackend backend) { this.backend = backend; }

    public static final class Result {
        public final String committedText;
        public final String composition;
        public final List<String> candidates;
        public final List<PinyinCandidate> candidateDetails;
        public final boolean deleteBeforeCursor;
        public final boolean requestEditorAction;
        public final String error;

        private Result(String committed, String composing, List<String> choices,
                       boolean delete, boolean action, String error, List<PinyinCandidate> candidateDetails) {
            committedText = committed;
            composition = composing;
            candidates = choices;
            deleteBeforeCursor = delete;
            requestEditorAction = action;
            this.error = error;
            this.candidateDetails = Collections.unmodifiableList(new ArrayList<>(candidateDetails));
        }
    }

    public String getComposition() {
        return composition.toString();
    }

    public List<String> getCandidates() {
        List<String> texts = new ArrayList<>();
        for (PinyinCandidate choice : choices) texts.add(choice.text);
        return Collections.unmodifiableList(texts);
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
                refresh();
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

    public Result commitCandidate(String id) {
        for (int i = 0; i < choices.size(); i++) if (choices.get(i).id.equals(id)) return commitCandidate(i);
        return result("", false, false);
    }

    public Result commitCandidate(int index) {
        List<PinyinCandidate> choices = this.choices;
        if (index < 0 || index >= choices.size()) return result("", false, false);
        PinyinCandidate choice = choices.get(index);
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
        choices = Collections.emptyList();
        return result("", false, false);
    }

    public Result refresh() { return result("", false, false); }
    public void close() { backend.close(); }

    private Result result(String committed, boolean delete, boolean action) {
        String error = null;
        try { choices = composition.length() == 0 ? Collections.emptyList() : backend.evaluate(getComposition()); }
        catch (RuntimeException failure) { choices = Collections.emptyList(); error = failure.getClass().getSimpleName(); }
        return new Result(committed, getComposition(), getCandidates(), delete, action, error, choices);
    }
}
