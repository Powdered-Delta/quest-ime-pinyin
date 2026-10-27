package com.m0e_n00b.questpinyinprobe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class EditorActionsTest {
    public static void main(String[] args) throws Exception {
        FixedPinyinSession session = new FixedPinyinSession();
        List<String> actions = new ArrayList<>();
        EditorActions.Sink recorder = (kind, value) -> actions.add(kind + ":" + value);
        EditorActions.emit(session.backspace(), false, recorder);
        expect(actions, "COMMAND:BACKSPACE");

        // A selection is deleted exactly once by BACKSPACE. An earlier COMPOSE("")
        // would delete CD first and then delete b, which was the reviewed regression.
        final StringBuilder document = new StringBuilder("abCD");
        final int[] selection = {2, 4};
        EditorActions.emit(session.backspace(), false, (kind, value) -> {
            if (kind.equals("COMPOSE")) {
                document.replace(selection[0], selection[1], value);
                selection[1] = selection[0] + value.length();
            } else if (value.equals("BACKSPACE")) {
                if (selection[0] != selection[1]) document.delete(selection[0], selection[1]);
                else if (selection[0] > 0) document.deleteCharAt(selection[0] - 1);
            }
        });
        if (!document.toString().equals("ab")) throw new AssertionError("Double deletion: " + document);

        actions.clear();
        EditorActions.emit(session.enter(), false, recorder);
        expect(actions, "COMMAND:ACTION_KEY");
        actions.clear();
        session.feed("n");
        EditorActions.emit(session.backspace(), true, recorder);
        expect(actions, "COMPOSE:");
        actions.clear();
        session.feed("nihao");
        EditorActions.emit(session.commitCandidate(0), true, recorder);
        expect(actions, "TEXT:你好");
        actions.clear();
        EditorActions.emit(session.commitCandidate(0), false, recorder);
        expect(actions);
        actions.clear();
        EditorActions.emit(session.feed("n"), false, recorder);
        expect(actions, "COMPOSE:n");
        session.reset();
        session.feed("nihao");
        actions.clear();
        EditorActions.emit(session.commitCandidate(1), true, recorder);
        expect(actions, "TEXT:你", "COMPOSE:hao");
        // Model the editor's composing range, not just emitted strings. A partial choice
        // must leave hao replaceable, and expose only the final state at batch completion.
        session.reset();
        session.feed("nihao");
        final StringBuilder text = new StringBuilder("nihao");
        final int[] composing = {0, 5};
        final boolean[] batching = {false};
        final List<String> notifications = new ArrayList<>();
        EditorActions.Sink editor = new EditorActions.Sink() {
            public void begin() { batching[0] = true; }
            public void send(String kind, String value) {
                if (!batching[0]) throw new AssertionError("Unbatched editor edit");
                int start = composing[0] < 0 ? text.length() : composing[0];
                int end = composing[1] < 0 ? text.length() : composing[1];
                text.replace(start, end, value);
                if (kind.equals("TEXT")) { composing[0] = -1; composing[1] = -1; }
                else { composing[0] = start; composing[1] = start + value.length(); }
            }
            public void end() {
                batching[0] = false;
                notifications.add(text + ":" + composing[0] + ":" + composing[1]);
            }
        };
        EditorActions.emit(session.commitCandidate(4), true, editor);
        expect(notifications, "尼hao:1:4");
        if (!session.getCandidates().get(0).equals("好")) throw new AssertionError("Missing remaining candidates");
        EditorActions.emit(session.commitCandidate(0), true, editor);
        expect(notifications, "尼hao:1:4", "尼好:-1:-1");
        final boolean[] ended = {false};
        try {
            EditorActions.emit(session.feed("n"), false, new EditorActions.Sink() {
                public void send(String kind, String value) throws Exception { throw new Exception("fixture"); }
                public void end() { ended[0] = true; }
            });
            throw new AssertionError("Expected editor failure");
        } catch (Exception expected) {
            if (!ended[0]) throw new AssertionError("Unclosed editor batch");
        }
        System.out.println("EditorActionsTest: 12 emission, batch and selection regressions passed");
    }

    private static void expect(List<String> actual, String... expected) {
        if (!actual.equals(Arrays.asList(expected))) throw new AssertionError(actual.toString());
    }
}
