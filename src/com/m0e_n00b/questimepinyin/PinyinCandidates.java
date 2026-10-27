/* Adapted from Wolvic-derived PinyinCandidates (MPL-2.0); see THIRD_PARTY.md. */
package com.m0e_n00b.questimepinyin;
import java.util.*;
/** Prefix lookup with explicit syllable boundaries and raw-coordinate consumption. */
public final class PinyinCandidates implements PinyinBackend {
    public static final class Word {
        public final String code, text;
        public Word(String code, String text) { this.code = code; this.text = text; }
    }
    public interface Words { List<Word> lookup(String prefix); }
    private final Words words;
    public PinyinCandidates(Words words) { this.words = words; }
    public List<PinyinCandidate> evaluate(String raw) {
        StringBuilder key = new StringBuilder();
        List<Integer> ends = new ArrayList<>(), boundaries = new ArrayList<>();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\'') boundaries.add(key.length());
            else { key.append(Character.toLowerCase(c)); ends.add(i + 1); }
        }
        if (key.length() == 0 || key.length() > 64) return Collections.emptyList();
        LinkedHashMap<String, PinyinCandidate> result = new LinkedHashMap<>();
        for (int end = key.length(); end > 0; end--) {
            String prefix = key.substring(0, end);
            for (Word word : words.lookup(prefix)) {
                String normalized = word.code.replace(" ", "").replace("'", "").toLowerCase(Locale.ROOT);
                if (!normalized.startsWith(prefix) || (end < key.length() && !normalized.equals(prefix))) continue;
                Set<Integer> wordBoundaries = new HashSet<>();
                int position = 0;
                for (int j = 0; j < word.code.length(); j++) {
                    char c = word.code.charAt(j);
                    if (c == ' ' || c == '\'') wordBoundaries.add(position); else position++;
                }
                boolean valid = true;
                for (int boundary : boundaries) if (boundary > 0 && boundary < end && !wordBoundaries.contains(boundary)) valid = false;
                if (!valid || word.text.isEmpty()) continue;
                PinyinCandidate choice = new PinyinCandidate(word.text, ends.get(end - 1));
                result.putIfAbsent(choice.id, choice);
                if (result.size() >= 128) return Collections.unmodifiableList(new ArrayList<>(result.values()));
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(result.values()));
    }
}
