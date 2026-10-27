package com.m0e_n00b.questimepinyin;
import java.util.*;
public final class PinyinCandidatesTest {
    private static int checks;
    private static void yes(boolean value) { checks++; if (!value) throw new AssertionError("Candidate contract " + checks); }
    public static void main(String[] args) {
        List<PinyinCandidates.Word> words = Arrays.asList(
            new PinyinCandidates.Word("ni hao", "你好"), new PinyinCandidates.Word("ni", "你"),
            new PinyinCandidates.Word("ni", "尼"), new PinyinCandidates.Word("hao", "好"),
            new PinyinCandidates.Word("xian", "先"), new PinyinCandidates.Word("xi an", "西安"),
            new PinyinCandidates.Word("wo shi", "我是"), new PinyinCandidates.Word("shu ru fa", "输入法"));
        PinyinCandidates backend = new PinyinCandidates(key -> {
            List<PinyinCandidates.Word> found = new ArrayList<>();
            for (PinyinCandidates.Word word : words) if (word.code.replace(" ", "").startsWith(key)) found.add(word);
            return found;
        });
        List<PinyinCandidate> full = backend.evaluate("nihao");
        yes(full.get(0).text.equals("你好") && full.get(0).consumedEnd == 5);
        yes(full.stream().anyMatch(c -> c.text.equals("尼") && c.consumedEnd == 2));
        yes(full.stream().noneMatch(c -> c.text.equals("你好") && c.consumedEnd != 5));
        yes(backend.evaluate("h").get(0).text.equals("好"));
        yes(backend.evaluate("ha").get(0).consumedEnd == 2);
        yes(backend.evaluate("nih").get(0).consumedEnd == 3);
        yes(backend.evaluate("NI'HAO").get(0).consumedEnd == 6);
        List<PinyinCandidate> separated = backend.evaluate("xi'an");
        yes(separated.get(0).text.equals("西安") && separated.get(0).consumedEnd == 5);
        yes(separated.stream().noneMatch(c -> c.text.equals("先")));
        yes(backend.evaluate("woshi").get(0).text.equals("我是"));
        yes(backend.evaluate("shurufa").get(0).text.equals("输入法"));
        PinyinSession session = new PinyinSession(backend);
        session.feed("nih");
        int choice = session.getCandidates().indexOf("尼");
        yes(choice >= 0);
        yes(session.commitCandidate(choice).committedText.equals("尼"));
        yes(session.getComposition().equals("h") && session.getCandidates().contains("好"));
        yes(session.space().committedText.equals("好") && session.getComposition().isEmpty());
        session.feed("ni'zzz");
        choice = session.getCandidates().indexOf("你");
        yes(session.commitCandidate(choice).committedText.equals("你"));
        yes(session.getComposition().equals("zzz"));
        yes(backend.evaluate("'").isEmpty());
        PinyinSession broken = new PinyinSession(raw -> { throw new IllegalStateException("fixture"); });
        PinyinSession.Result failed = broken.feed("nihao");
        yes(failed.error != null && failed.composition.equals("nihao") && failed.candidates.isEmpty());
        yes(broken.enter().committedText.equals("nihao"));
        session.reset();
        PinyinSession.Result published = session.feed("nihao");
        PinyinCandidate ni = published.candidateDetails.stream().filter(c -> c.text.equals("尼")).findFirst().get();
        yes(ni.consumedEnd == 2);
        yes(session.commitCandidate(ni.id).composition.equals("hao"));
        yes(session.commitCandidate("expired-id").committedText.isEmpty());
        System.out.println("PinyinCandidatesTest: " + checks + " backend/range cases passed");
    }
}
