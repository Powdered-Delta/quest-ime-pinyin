package com.m0e_n00b.questimepinyin;

/** Emits host actions without making speculative edits to the current editor selection. */
public final class EditorActions {
    private EditorActions() { }

    public interface Sink {
        void send(String kind, String value) throws Exception;
        default void begin() throws Exception { }
        default void end() throws Exception { }
    }

    public static void emit(PinyinSession.Result result, boolean hadComposition, Sink sink)
            throws Exception {
        if (result.editorUnchanged) return;
        sink.begin();
        try {
        if (!result.committedText.isEmpty()) sink.send(result.replacePrevious ? "REPLACE" : "TEXT", result.committedText);
        // Empty-session backspace/action must not call setComposingText(""): it can
        // replace an editor selection before the actual command, causing a second deletion.
        if (!result.composition.isEmpty() || (hadComposition && result.committedText.isEmpty())) {
            sink.send("COMPOSE", result.composition);
        }
        if (result.deleteBeforeCursor) sink.send("COMMAND", "BACKSPACE");
        if (result.requestEditorAction) sink.send("COMMAND", "ACTION_KEY");
        } finally {
            sink.end();
        }
    }
}
