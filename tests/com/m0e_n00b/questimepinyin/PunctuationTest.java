package com.m0e_n00b.questimepinyin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PunctuationTest {
    public static void main(String[] args) throws Exception {
        FixedPinyinSession session = new FixedPinyinSession();
        expect("，", session.feed(",").committedText);
        expect("！", session.feed("!").committedText);
        expect("！", session.feed("!", true).committedText);
        if (!session.feed("!", true).replacePrevious) throw new AssertionError("replacement flag missing");
        expect("。！？；：（）【】《》", ChinesePunctuation.convert(".!?;:()[]<>"));
        expect("a1+-/@_", ChinesePunctuation.convert("a1+-/@_"));

        session.reset();
        session.feed(",");
        PinyinSession.Result replacement = session.feed("!", true);
        List<String> actions = new ArrayList<>();
        EditorActions.emit(replacement, false, (kind, value) -> actions.add(kind + ":" + value));
        if (!actions.equals(Arrays.asList("REPLACE:！"))) throw new AssertionError(actions.toString());
        StringBuilder editor = new StringBuilder("，");
        editor.replace(editor.length() - 1, editor.length(), replacement.committedText);
        expect("！", editor.toString());

        session.reset();
        session.feed("ni");
        PinyinSession.Result composing = session.feed("!");
        if (!composing.committedText.equals("ni！") || composing.replacePrevious) {
            throw new AssertionError("punctuation must commit composition without replacement");
        }
        System.out.println("PunctuationTest: passed");
    }

    private static void expect(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
