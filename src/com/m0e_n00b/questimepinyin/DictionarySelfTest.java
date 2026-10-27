package com.m0e_n00b.questimepinyin;
import android.content.Context;
import android.util.Log;
import java.util.List;
/** Explicit diagnostic using constant fixtures only; never reads or edits the user's field. */
final class DictionarySelfTest {
    interface Completion { void finish(boolean passed, int check); }
    static void run(Context context, String moduleApk, Completion completion) {
        SqlitePinyinBackend backend = new SqlitePinyinBackend(context, moduleApk);
        try { backend.evaluate("nihao"); throw new AssertionError("Missing UI guard"); }
        catch (IllegalStateException expected) { /* guarded before any disk/SQL */ }
        new Thread(() -> {
            int step = 0;
            try {
                step = 1; List<PinyinCandidate> choices = backend.evaluate("nihao");
                require(choices.get(0).text.equals("你好") && choices.get(0).consumedEnd == 5);
                step = 2; require(backend.evaluate("woshi").get(0).text.equals("我是"));
                step = 3; require(backend.evaluate("shurufa").get(0).text.equals("输入法"));
                step = 4; require(backend.evaluate("h").stream().anyMatch(c -> c.text.equals("好") && c.consumedEnd == 1));
                step = 5; require(backend.evaluate("nih").stream().anyMatch(c -> c.text.equals("尼") && c.consumedEnd == 2));
                PinyinSession session = new PinyinSession(backend);
                step = 6; session.feed("nih"); require(session.commitCandidate(session.getCandidates().indexOf("尼")).committedText.equals("尼"));
                step = 7; require(session.getComposition().equals("h") && session.getCandidates().contains("好"));
                step = 8; require(session.commitCandidate(session.getCandidates().indexOf("好")).committedText.equals("好") && session.getComposition().isEmpty());
                step = 9; choices = backend.evaluate("xi'an");
                require(choices.stream().anyMatch(c -> c.text.equals("西安") && c.consumedEnd == 5));
                require(choices.stream().noneMatch(c -> c.text.equals("先") && c.consumedEnd == 5));
                Log.i("QuestPinyinDictionary", "self-test passed; checks=9; UI-thread guard verified");
                completion.finish(true, step);
            } catch (Throwable failure) {
                Log.e("QuestPinyinDictionary", "self-test failed; check=" + step + "; type=" + failure.getClass().getSimpleName());
                completion.finish(false, step);
            } finally { backend.close(); }
        }, "QuestPinyinSelfTest").start();
    }
    private static void require(boolean value) { if (!value) throw new AssertionError("Dictionary fixture"); }
}
