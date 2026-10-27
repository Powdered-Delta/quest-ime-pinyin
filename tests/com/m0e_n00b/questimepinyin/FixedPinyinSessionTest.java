package com.m0e_n00b.questimepinyin;

public final class FixedPinyinSessionTest {
    private static int checks;

    public static void main(String[] args) {
        FixedPinyinSession session = new FixedPinyinSession();
        for (String letter : new String[] {"n", "i", "h", "a", "o"}) {
            equal("", session.feed(letter).committedText);
        }
        equal("nihao", session.getComposition());
        equal("你好", session.getCandidates().get(0));
        FixedPinyinSession.Result snapshot = session.feed("");
        equal("你好", session.commitCandidate(0).committedText);
        equal("", session.getComposition());
        equal("", session.commitCandidate(0).committedText);
        equal("nihao", snapshot.composition);
        equal("你好", snapshot.candidates.get(0));

        session.feed("nihao");
        equal("niha", session.backspace().composition);
        equal("你好", session.getCandidates().get(0));
        session.feed("o");
        equal("你好", session.space().committedText);
        equal(" ", session.space().committedText);
        yes(session.backspace().deleteBeforeCursor);

        session.feed("NIHAO");
        equal("NIHAO", session.enter().committedText);
        yes(session.enter().requestEditorAction);
        session.feed("other");
        equal("other ", session.space().committedText);
        session.feed("raw");
        equal("raw", session.enter().committedText);
        session.feed("nihao");
        equal("", session.reset().committedText);
        equal("", session.commitCandidate(0).committedText);
        equal("", session.getComposition());

        session.feed("nihao");
        equal("nihao！", session.feed("!").committedText);
        equal("你好", session.feed("nihao ").committedText);
        equal("你好", session.feed("你好").committedText);
        equal("", session.getComposition());
        session.feed("nihao");
        equal("", session.commitCandidate(-1).committedText);
        equal("", session.commitCandidate(session.getCandidates().size()).committedText);
        equal("nihao", session.getComposition());
        equal(21, session.getCandidates().size());
        equal(session.getCandidates().size(), new java.util.HashSet<>(session.getCandidates()).size());
        equal("你", session.commitCandidate(1).committedText);
        equal("hao", session.getComposition());
        equal("好", session.getCandidates().get(0));
        equal("hao", session.enter().committedText);
        equal("", session.getComposition());
        session.feed("nihao");
        equal("你", session.commitCandidate(1).committedText);
        equal("hao", session.getComposition());
        equal("好", session.commitCandidate(0).committedText);
        equal("", session.getComposition());
        session.feed("nibao");
        session.moveCursor(3);
        session.backspace();
        session.moveCursor(2);
        session.feed("h");
        equal("nihao", session.getComposition());
        equal("你好", session.getCandidates().get(0));
        session.reset();
        session.feed("ni");
        equal(20, session.getCandidates().size());
        equal("泥", session.commitCandidate(2).committedText);
        equal("", session.getComposition());
        session.feed("nihao");
        equal("伲", session.commitCandidate(20).committedText);
        equal("hao", session.getComposition());
        equal(20, session.getCandidates().size());
        equal(session.getCandidates().size(), new java.util.HashSet<>(session.getCandidates()).size());
        equal("嗥", session.commitCandidate(19).committedText);
        equal("", session.getComposition());
        session.feed("nih");
        equal("你好", session.getCandidates().get(0));
        equal("尼", session.getCandidates().get(4));
        equal("尼", session.commitCandidate(4).committedText);
        equal("h", session.getComposition());
        equal("好", session.getCandidates().get(0));
        equal(20, session.getCandidates().size());
        equal("ha", session.feed("a").composition);
        equal("好", session.getCandidates().get(0));
        equal("h", session.backspace().composition);
        equal("好", session.commitCandidate(0).committedText);
        equal("", session.getComposition());
        session.feed("NI'H");
        equal("你好", session.getCandidates().get(0));
        equal("尼", session.commitCandidate(4).committedText);
        equal("H", session.getComposition());
        equal("好", session.commitCandidate(0).committedText);
        equal("", session.getComposition());
        session.feed("ni'hao");
        equal("你", session.commitCandidate(1).committedText);
        equal("hao", session.getComposition());
        equal("好", session.commitCandidate(0).committedText);
        session.feed("niha");
        equal("你好", session.commitCandidate(0).committedText);
        equal("", session.getComposition());
        session.feed("n");
        equal("你", session.getCandidates().get(0));
        equal("你", session.commitCandidate(0).committedText);
        equal("", session.getComposition());
        session.feed("nix");
        equal("尼", session.commitCandidate(3).committedText);
        equal("x", session.getComposition());
        yes(session.getCandidates().isEmpty());
        session.reset();
        for (int i = 0; i < 64; i++) session.feed("a");
        equal(64, session.feed("b").committedText.length());
        equal("b", session.getComposition());
        System.out.println("FixedPinyinSessionTest: " + checks + " checks passed");
    }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    private static void yes(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Condition failed");
    }
}
