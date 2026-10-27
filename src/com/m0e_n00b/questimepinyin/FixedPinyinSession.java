package com.m0e_n00b.questimepinyin;
import java.util.*;
/** Deterministic fixture backend retained for regression tests only. */
public final class FixedPinyinSession extends PinyinSession {
    public FixedPinyinSession() { super(new Fixture()); }
    private static final class Fixture implements PinyinBackend {
    private static final List<String> NI = Collections.unmodifiableList(Arrays.asList(
            "你", "呢", "泥", "尼", "拟", "逆", "倪", "腻", "妮", "霓",
            "昵", "溺", "匿", "睨", "旎", "鲵", "怩", "铌", "猊", "伲"));
    private static final List<String> HAO = Collections.unmodifiableList(Arrays.asList(
            "好", "号", "浩", "豪", "毫", "郝", "耗", "皓", "昊", "灏",
            "蒿", "嚎", "壕", "蚝", "镐", "濠", "薅", "貉", "颢", "嗥"));
    public List<PinyinCandidate> evaluate(String composition) {
        String raw = composition.toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder();
        int[] rawEnds = new int[raw.length()];
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) != '\'') {
                rawEnds[normalized.length()] = i + 1;
                normalized.append(raw.charAt(i));
            }
        }
        String query = normalized.toString();
        List<PinyinCandidate> choices = new ArrayList<>();
        if (query.isEmpty()) return choices;
        // The fixture follows the production engine's prefix/range contract. It remains
        // limited to ni/hao; completed predictions consume only letters actually typed.
        if (query.startsWith("nihao") || (query.length() > 2 && "nihao".startsWith(query))) {
            choices.add(new PinyinCandidate("你好", rawEnds[Math.min(5, query.length()) - 1]));
        }
        if (query.startsWith("ni") || "ni".startsWith(query)) {
            for (String text : NI) choices.add(new PinyinCandidate(text, rawEnds[Math.min(2, query.length()) - 1]));
        } else if (query.startsWith("hao") || "hao".startsWith(query)) {
            for (String text : HAO) choices.add(new PinyinCandidate(text, rawEnds[Math.min(3, query.length()) - 1]));
        }
        return choices;
    }

    }
}
