package com.m0e_n00b.questpinyinprobe;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.ExtractedText;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;

/** Version-pinned adapter. Host names below are verified against the recorded DEX. */
final class HostAdapter {
    private static final int LANGUAGE_MENU = 0x7f120092;
    private static final int SUGGESTION_VIEW = 0x7f090272;
    private static final String CHINESE_KEY = "quest-pinyin-probe.language";
    private final Context context;
    private final SharedPreferences preferences;
    private final AsyncPinyinSession session;
    private WeakReference<InputConnection> sessionDestination = new WeakReference<>(null);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Class<?> modelClass, keyClass, listenerClass, serviceClass, actionClass, eventClass;
    private final Constructor<?> modelConstructor;
    private final Method keyRelease, dispatch, keyDispatch, sendAction, stateDispatch;
    private final List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    private final Map<Object, WeakReference<Object>> addedMenuKeys = new WeakHashMap<>();
    private final Map<Object, String> changedIme = new WeakHashMap<>();
    private WeakReference<InputMethodService> service = new WeakReference<>(null);
    private WeakReference<ViewGroup> panel = new WeakReference<>(null);
    private final Map<View, Integer> savedVisibility = new WeakHashMap<>();
    private ViewGroup.LayoutParams savedSuggestionLayout;
    private int[] savedSuggestionPadding;
    private WeakReference<LinearLayout> bar = new WeakReference<>(null);
    private WeakReference<ViewGroup> suggestion = new WeakReference<>(null);
    private boolean selected, failed, updating, replayingLanguage;
    private boolean awaitingCompositionUpdate;
    private boolean editing;
    private int[] deferredSelection;
    private Runnable compositionTimeout;
    private WeakReference<InputConnection> unsupportedConnection = new WeakReference<>(null);
    private String lastUiDiagnostic = "";
    private boolean candidatesExpanded;
    private int visibleCandidateCount;
    private WeakReference<View> expandedCandidates = new WeakReference<>(null);
    private WeakReference<Spannable> underlinedMirror = new WeakReference<>(null);
    private final UnderlineSpan mirrorUnderline = new UnderlineSpan();
    private long generation;

    HostAdapter(Context context, ClassLoader loader, String moduleApk) throws Exception {
        this.context = context;
        preferences = context.getSharedPreferences("quest_pinyin_probe", Context.MODE_PRIVATE);
        selected = preferences.getBoolean("chinese_selected", false);
        modelClass = Class.forName("X.1gB", false, loader);
        keyClass = Class.forName("X.1gC", false, loader);
        listenerClass = Class.forName("X.1gM", false, loader);
        serviceClass = Class.forName("com.oculus.panelapp.keyboardv2.KeyboardInputMethodService", false, loader);
        actionClass = Class.forName("X.1hA", false, loader);
        eventClass = Class.forName("X.1gF", false, loader);
        Class<?> stateClass = Class.forName("X.1gL", false, loader);
        modelConstructor = modelClass.getDeclaredConstructor(Context.class,
                Class.forName("X.1gg", false, loader), Class.forName("X.1hU", false, loader),
                int.class, int.class, int.class, boolean.class, boolean.class);
        keyRelease = listenerClass.getDeclaredMethod("AEu", keyClass, String.class, boolean.class);
        dispatch = listenerClass.getDeclaredMethod("A0B", eventClass, Integer.class, String.class, String.class, boolean.class);
        keyDispatch = listenerClass.getDeclaredMethod("A0A", eventClass, Integer.class, String.class,
                String.class, String.class, float.class, float.class, boolean.class);
        sendAction = serviceClass.getDeclaredMethod("AKE", actionClass, String.class);
        stateDispatch = stateClass.getDeclaredMethod("A02", Class.forName("X.1gt", false, loader), stateClass);
        // Resolve all load-bearing members before installing any hook.
        requireField(modelClass, "A0P", ArrayList.class);
        requireField(modelClass, "A0R", List.class);
        requireField(modelClass, "A0M", String.class);
        requireField(modelClass, "A0E", int.class);
        requireField(keyClass, "A0b", List.class);
        requireField(keyClass, "A0Q", CharSequence.class);
        requireField(keyClass, "A0W", Integer.class);
        requireField(keyClass, "A0m", RectF.class);
        serviceClass.getDeclaredMethod("A8H");
        serviceClass.getDeclaredMethod("onStartInput", EditorInfo.class, boolean.class);
        session = new AsyncPinyinSession(new PinyinSession(new SqlitePinyinBackend(context, moduleApk)),
                Executors.newSingleThreadExecutor(task -> { Thread thread = new Thread(task, "QuestPinyinDictionary"); thread.setDaemon(true); return thread; }),
                task -> mainHandler.post(task), (result, hadComposition) -> {
                    try {
                        InputMethodService target = service.get();
                        if (failed || !selected || !allowed(target)
                                || sessionDestination.get() != target.getCurrentInputConnection()) return;
                        if (result.error != null) diagnostic("dictionary query failed: " + result.error);
                        apply(result, target, hadComposition);
                    } catch (Throwable failure) { fail(failure); }
                });
    }

    void install() throws Exception {
        try {
            hook(modelConstructor, new GuardedHook() {
                @Override void after(MethodHookParam p) throws Exception { modelCreated(p.thisObject); }
            });
            hook(keyRelease, new GuardedHook() {
                @Override void before(MethodHookParam p) throws Exception {
                    Object key = p.args[0];
                    if (key == null || (boolean) get(key, "A0d")) return;
                    Object model = get(key, "A0O");
                    if (model != null && (int) get(model, "A0E") == LANGUAGE_MENU && code(key) == -3) {
                        if (session.isPending() && !replayingLanguage) {
                            Object listener = p.thisObject;
                            Object[] arguments = p.args.clone();
                            InputMethodService active = service.get();
                            InputConnection destination = active == null ? null : active.getCurrentInputConnection();
                            p.setResult(null);
                            session.whenIdle(() -> {
                                try {
                                    InputMethodService target = service.get();
                                    if (failed || target == null || destination != target.getCurrentInputConnection()) return;
                                    replayingLanguage = true;
                                    keyRelease.invoke(listener, arguments);
                                } catch (Throwable failure) { fail(failure); }
                                finally { replayingLanguage = false; }
                            });
                            return;
                        }
                        WeakReference<Object> added = addedMenuKeys.get(model);
                        boolean chinese = added != null && key == added.get();
                        changeLanguage(chinese);
                        p.setObjectExtra(CHINESE_KEY, Boolean.TRUE);
                    }
                }
                @Override void after(MethodHookParam p) throws Exception {
                    if (p.getObjectExtra(CHINESE_KEY) != null) updateUi();
                }
            });
            GuardedHook textHook = new GuardedHook() {
                @Override void before(MethodHookParam p) throws Exception {
                    InputMethodService active = service.get();
                    if (!allowed(active) || !selected) return;
                    Object keyEvent = p.args[0];
                    int eventCode = (int) get(keyEvent, "A00");
                    int kind = (Integer) p.args[1];
                    String input = (String) p.args[3];
                    AsyncPinyinSession.Action action;
                    if (eventCode == -5) action = PinyinSession::backspace;
                    else if (eventCode == 1 || (kind == 1 && " ".equals(input))) action = PinyinSession::space;
                    else if (eventCode == -2) action = PinyinSession::enter;
                    else if (kind == 1 && input != null) action = model -> model.feed(input);
                    else return;
                    if (sessionDestination.get() != active.getCurrentInputConnection()) {
                        session.reset();
                        sessionDestination = new WeakReference<>(active.getCurrentInputConnection());
                    }
                    generation++;
                    candidatesExpanded = false;
                    p.setResult(null);
                    session.submit(action);
                    updateUi();
                }
            };
            // Intercept before the host's Latin autocorrection/buffer edits. The lower boundary
            // also catches direct command dispatch; skipping the upper original prevents duplicates.
            hook(keyDispatch, textHook);
            hook(dispatch, textHook);
            hook(serviceClass.getDeclaredMethod("onStartInput", EditorInfo.class, boolean.class), new GuardedHook() {
                @Override void after(MethodHookParam p) throws Exception {
                    resetSession(false);
                    service = new WeakReference<>((InputMethodService) p.thisObject);
                    unsupportedConnection.clear();
                    updateUi();
                }
            });
            hook(serviceClass.getDeclaredMethod("onStartInputView", EditorInfo.class, boolean.class), new GuardedHook() {
                @Override void after(MethodHookParam p) throws Exception {
                    service = new WeakReference<>((InputMethodService) p.thisObject);
                    updateUi();
                }
            });
            hook(serviceClass.getDeclaredMethod("onFinishInput"), new GuardedHook() {
                @Override void before(MethodHookParam p) throws Exception { resetSession(true); }
            });
            hook(serviceClass.getDeclaredMethod("onFinishInputView", boolean.class), new GuardedHook() {
                @Override void before(MethodHookParam p) throws Exception { resetSession(true); }
            });
            hook(serviceClass.getDeclaredMethod("onUpdateSelection", int.class, int.class,
                    int.class, int.class, int.class, int.class), new GuardedHook() {
                @Override void before(MethodHookParam p) {
                    if (p.thisObject != service.get() || !selected || session.getComposition().isEmpty()) return;
                    if (editing) {
                        deferredSelection = new int[6];
                        for (int i = 0; i < 6; i++) deferredSelection[i] = (int) p.args[i];
                        p.setObjectExtra("quest-pinyin-probe.stale-selection", Boolean.TRUE);
                        p.setResult(null);
                        return;
                    }
                    int start = (int) p.args[2], end = (int) p.args[3];
                    int composingStart = (int) p.args[4], composingEnd = (int) p.args[5];
                    boolean outsideComposition = composingStart >= 0
                            && (start < composingStart || end > composingEnd);
                    if (awaitingCompositionUpdate && start == end && !outsideComposition
                            && (composingStart < 0 || composingEnd - composingStart != session.getComposition().length())) {
                        // The host override finishes composing on mismatched lengths. A queued
                        // notification for an older key must not terminate our newer composition.
                        p.setObjectExtra("quest-pinyin-probe.stale-selection", Boolean.TRUE);
                        p.setResult(null);
                    }
                }
                @Override void after(MethodHookParam p) throws Exception {
                    if (p.getObjectExtra("quest-pinyin-probe.stale-selection") != null) return;
                    if (p.thisObject != service.get() || session.getComposition().isEmpty()) return;
                    int start = (int) p.args[2], end = (int) p.args[3];
                    int composingStart = (int) p.args[4], composingEnd = (int) p.args[5];
                    cancelCompositionWait();
                    if (composingStart < 0 || start < composingStart || end > composingEnd || start != end) {
                        resetSession(false);
                        updateUi();
                    }
                }
            });
            hook(stateDispatch, new GuardedHook() {
                @Override void after(MethodHookParam p) throws Exception { updateUi(); }
            });
            hook(serviceClass.getDeclaredMethod("onUpdateExtractedText", int.class, ExtractedText.class), new GuardedHook() {
                @Override void after(MethodHookParam p) throws Exception { updateUi(); }
            });
        } catch (Throwable error) {
            for (XC_MethodHook.Unhook h : hooks) h.unhook();
            throw new IllegalStateException("Hook installation incomplete", error);
        }
    }

    @SuppressWarnings("unchecked") private void modelCreated(Object model) throws Exception {
        int resource = (int) get(model, "A0E");
        if (resource == LANGUAGE_MENU) {
            List<Object> rows = (List<Object>) get(model, "A0P");
            if (rows.size() != 6) throw new IllegalStateException("Language menu shape");
            List<Object> first = (List<Object>) get(rows.get(0), "A06");
            List<Object> last = (List<Object>) get(rows.get(5), "A06");
            if (first.size() != 4 || last.size() != 1) throw new IllegalStateException("Language rows");
            Object english = first.get(0), cancel = last.get(0);
            if (code(english) != -3 || code(cancel) != -7) throw new IllegalStateException("Language keys");
            Object chinese = keyClass.getDeclaredConstructor().newInstance();
            for (Field field : keyClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;
                field.setAccessible(true);
                field.set(chinese, field.get(english));
            }
            set(chinese, "A0b", new ArrayList<>(Collections.singletonList(-3)));
            set(chinese, "A0Q", "中文（拼音）");
            set(chinese, "A0U", null);
            set(chinese, "A0P", null);
            set(chinese, "A0S", null);
            set(chinese, "A0T", "中文（拼音）");
            set(chinese, "A0V", "中文（拼音）");
            set(chinese, "A0I", Typeface.SANS_SERIF);
            set(chinese, "A0B", 0);
            set(chinese, "A07", get(cancel, "A07"));
            ((RectF) get(chinese, "A0m")).setEmpty();
            last.add(0, chinese);
            set(cancel, "A0B", 1);
            List<Object> cachedKeys = (List<Object>) get(model, "A0R");
            cachedKeys.clear(); // A03 rebuilds in row order, including controller navigation order.
            set(model, "A0X", null); // Neighbor hit-test indices must include the appended key.
            addedMenuKeys.put(model, new WeakReference<>(chinese));
        } else if (InputPolicy.supportsLayout(resource) && selected) {
            changedIme.put(model, (String) get(model, "A0M"));
            set(model, "A0M", "simple");
        }
    }

    private void changeLanguage(boolean chinese) throws Exception {
        InputMethodService active = service.get();
        String pending = session.getComposition();
        boolean committed = selected && !pending.isEmpty() && allowed(active);
        if (committed) send(active, "TEXT", pending);
        resetSession(!committed);
        selected = chinese;
        MainHook.log("language selected: chinese=" + chinese);
        preferences.edit().putBoolean("chinese_selected", chinese).apply();
        for (Map.Entry<Object, String> entry : changedIme.entrySet()) set(entry.getKey(), "A0M", entry.getValue());
        changedIme.clear();
        // The copied English key uses the host's own language transition. No new host enum is forged.
    }

    private void apply(PinyinSession.Result result, InputMethodService active, boolean hadComposition) throws Exception {
        final InputConnection destination = active.getCurrentInputConnection();
        if (destination == null) throw new IllegalStateException("Missing editor");
        final long revision = generation;
        EditorActions.emit(result, hadComposition, new EditorActions.Sink() {
            public void begin() {
                editing = true;
                deferredSelection = null;
                destination.beginBatchEdit();
            }
            public void send(String kind, String value) throws Exception { HostAdapter.this.send(active, kind, value); }
            public void end() {
                try { destination.endBatchEdit(); }
                finally {
                    editing = false;
                    final int[] selection = deferredSelection;
                    deferredSelection = null;
                    if (selection != null) mainHandler.post(() -> {
                        if (generation == revision && destination == active.getCurrentInputConnection()) {
                            active.onUpdateSelection(selection[0], selection[1], selection[2],
                                    selection[3], selection[4], selection[5]);
                        }
                    });
                }
            }
        });
        updateUi();
    }

    private void send(InputMethodService active, String kind, String value) throws Exception {
        Object action = null;
        for (Object candidate : actionClass.getEnumConstants()) {
            if (((Enum<?>) candidate).name().equals(kind)) action = candidate;
        }
        if (active == null || action == null) throw new IllegalStateException("Missing text destination");
        if (kind.equals("COMPOSE") || kind.equals("TEXT")) cancelCompositionWait();
        if (kind.equals("COMPOSE") && !value.isEmpty()) {
            awaitingCompositionUpdate = true;
            final long revision = generation;
            final InputConnection destination = active.getCurrentInputConnection();
            compositionTimeout = () -> {
                if (!awaitingCompositionUpdate || generation != revision
                        || destination != active.getCurrentInputConnection()) return;
                try {
                    // Bound stale-update suppression. Preserve the visible Latin preedit and
                    // return this editor to the original host if it cannot acknowledge composing.
                    resetSession(false);
                    unsupportedConnection = new WeakReference<>(destination);
                    if (destination != null) destination.finishComposingText();
                    updateUi();
                    MainHook.log("editor did not acknowledge composition; original input retained");
                } catch (Throwable failure) { fail(failure); }
            };
            mainHandler.postDelayed(compositionTimeout, 1000);
        }
        if (kind.equals("COMPOSE")) {
            // Preserve the pinned host's composing counters, but pass spans through the
            // existing connection instead of the String-only AKE boundary. One edit per action.
            set(active, "A00", value.length());
            StringBuilder hostComposition = (StringBuilder) get(active, "A0Q");
            hostComposition.setLength(0);
            hostComposition.append(value);
            SpannableString styled = new SpannableString(value);
            if (!value.isEmpty()) styled.setSpan(new UnderlineSpan(), 0, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            InputConnection connection = active.getCurrentInputConnection();
            if (connection == null || !connection.setComposingText(styled, 1)) {
                throw new IllegalStateException("Editor rejected composition");
            }
        } else sendAction.invoke(active, action, value);
    }

    /** Permit only ordinary text fields; unknown/null connections fail closed to the original host. */
    private boolean allowed(InputMethodService active) {
        if (failed || active == null || active.getCurrentInputConnection() == null) return false;
        if (active.getCurrentInputConnection() == unsupportedConnection.get()) return false;
        EditorInfo info = active.getCurrentInputEditorInfo();
        if (info == null) return false;
        return InputPolicy.allows(info.inputType);
    }

    private void resetSession(boolean clearEditor) throws Exception {
        if (clearEditor && !session.getComposition().isEmpty() && allowed(service.get())) send(service.get(), "COMPOSE", "");
        session.reset();
        sessionDestination.clear();
        candidatesExpanded = false;
        cancelCompositionWait();
        generation++;
        restoreSuggestion();
    }

    private void cancelCompositionWait() {
        awaitingCompositionUpdate = false;
        if (compositionTimeout != null) mainHandler.removeCallbacks(compositionTimeout);
        compositionTimeout = null;
    }

    private void updateUi() throws Exception {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> {
                try { if (!failed) updateUi(); } catch (Throwable failure) { fail(failure); }
            });
            return;
        }
        if (updating) return;
        updating = true;
        try {
            InputMethodService active = service.get();
            if (!selected || !allowed(active)) {
                EditorInfo info = active == null ? null : active.getCurrentInputEditorInfo();
                diagnostic("ui gate: selected=" + selected + " service=" + (active != null)
                        + " connection=" + (active != null && active.getCurrentInputConnection() != null)
                        + " inputType=" + (info == null ? "null" : Integer.toHexString(info.inputType))
                        + " unsupported=" + (active != null && active.getCurrentInputConnection() != null
                        && active.getCurrentInputConnection() == unsupportedConnection.get()));
                restoreSuggestion(); return;
            }
            Object value = serviceClass.getDeclaredMethod("A8H").invoke(active);
            if (!(value instanceof ViewGroup)) { diagnostic("ui gate: no panel"); return; }
            ViewGroup current = (ViewGroup) value;
            if (current != panel.get()) { restoreSuggestion(); panel = new WeakReference<>(current); }
            View keyboardView = current.findViewById(0x7f090267);
            Object currentModel = keyboardView == null ? null : get(keyboardView, "A0K");
            if (currentModel == null || !InputPolicy.supportsLayout((int) get(currentModel, "A0E"))) {
                diagnostic("ui gate: layout=" + (currentModel == null ? "null"
                        : Integer.toHexString((int) get(currentModel, "A0E"))));
                restoreSuggestion();
                return;
            }
            View found = current.findViewById(SUGGESTION_VIEW);
            if (!(found instanceof LinearLayout)) { diagnostic("ui gate: no suggestion container"); return; }
            diagnostic("ui ready: layout=" + Integer.toHexString((int) get(currentModel, "A0E")));
            LinearLayout owner = (LinearLayout) found;
            LinearLayout row = bar.get();
            if (row == null || row.getParent() != owner) {
                restoreSuggestion();
                row = new LinearLayout(owner.getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setFocusable(false);
                row.setBackgroundColor(Color.rgb(30, 34, 41));
                row.setPadding(0, 0, dp(6), 0);
                owner.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
                bar = new WeakReference<>(row);
                suggestion = new WeakReference<>(owner);
                savedSuggestionLayout = owner.getLayoutParams();
                savedSuggestionPadding = new int[] {owner.getPaddingLeft(), owner.getPaddingTop(),
                        owner.getPaddingRight(), owner.getPaddingBottom()};
                if (savedSuggestionLayout instanceof ViewGroup.MarginLayoutParams) {
                    LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
                            (ViewGroup.MarginLayoutParams) savedSuggestionLayout);
                    layout.leftMargin = 0;
                    layout.setMarginStart(0);
                    owner.setLayoutParams(layout);
                }
                owner.setPadding(0, owner.getPaddingTop(), owner.getPaddingRight(), owner.getPaddingBottom());
                savedVisibility.put(owner, owner.getVisibility());
                for (int i = 0; i < owner.getChildCount(); i++) {
                    View child = owner.getChildAt(i);
                    if (child != row) savedVisibility.put(child, child.getVisibility());
                }
            }
            for (View view : savedVisibility.keySet()) if (view != owner) view.setVisibility(View.GONE);
            owner.setVisibility(View.VISIBLE);
            row.setVisibility(View.VISIBLE);
            View keyboard = current.findViewById(0x7f090267);
            row.setMinimumWidth(keyboard != null && keyboard.getWidth() > 0 ? keyboard.getWidth() : dp(560));
            row.removeAllViews();
            LinearLayout choices = new LinearLayout(owner.getContext());
            choices.setOrientation(LinearLayout.HORIZONTAL);
            List<Button> measuredCandidates = new ArrayList<>();
            int[] candidateWidths = new int[session.getCandidates().size()];
            for (int i = 0; i < session.getCandidates().size(); i++) {
                Button candidate = candidateButton(owner.getContext(), i, active);
                candidate.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(dp(48), View.MeasureSpec.EXACTLY));
                candidateWidths[i] = candidate.getMeasuredWidth();
                measuredCandidates.add(candidate);
            }
            int stripWidth = (keyboard != null && keyboard.getWidth() > 0 ? keyboard.getWidth() : dp(560))
                    - row.getPaddingLeft() - row.getPaddingRight();
            visibleCandidateCount = CandidateFit.visibleCount(candidateWidths, Math.max(0, stripWidth), dp(4), dp(48));
            for (int i = 0; i < visibleCandidateCount; i++) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(candidateWidths[i], ViewGroup.LayoutParams.MATCH_PARENT);
                if (i > 0) params.setMarginStart(dp(4));
                choices.addView(measuredCandidates.get(i), params);
            }
            row.addView(choices, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            if (session.getError() != null && !session.isPending()) {
                Button retry = new Button(owner.getContext());
                retry.setText("词库不可用 · 重试");
                retry.setTextColor(Color.WHITE);
                retry.setFocusable(false);
                final long retryRevision = generation;
                final InputConnection retryDestination = active.getCurrentInputConnection();
                retry.setOnClickListener(v -> {
                    if (failed || retryRevision != generation || !selected || !allowed(active)
                            || retryDestination != active.getCurrentInputConnection()) return;
                    sessionDestination = new WeakReference<>(retryDestination);
                    session.submit(PinyinSession::refresh);
                    try { updateUi(); } catch (Throwable failure) { fail(failure); }
                });
                row.addView(retry, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            boolean hasOverflow = visibleCandidateCount < session.getCandidates().size();
            if (!hasOverflow) candidatesExpanded = false;
            Button expand = new Button(owner.getContext());
            expand.setText(candidatesExpanded ? "▲" : "▼");
            expand.setTextColor(Color.WHITE);
            expand.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(30, 34, 41)));
            expand.setContentDescription(candidatesExpanded ? "收起候选" : "展开候选");
            expand.setFocusable(false);
            expand.setFocusableInTouchMode(false);
            expand.setMinWidth(0);
            expand.setMinimumWidth(0);
            expand.setPadding(0, 0, 0, 0);
            expand.setEnabled(hasOverflow);
            expand.setOnClickListener(v -> {
                try { candidatesExpanded = !candidatesExpanded; updateUi(); }
                catch (Throwable failure) { fail(failure); }
            });
            if (hasOverflow) row.addView(expand, new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT));
            renderExpanded(keyboardView, active);
            updateMirrorUnderline(current);
        } finally { updating = false; }
    }

    private Button candidateButton(Context ui, int index, InputMethodService active) {
        Button button = new Button(ui);
        button.setText(session.getCandidates().get(index));
        final String candidateId = session.getCandidateId(index);
        button.setTextSize(20);
        button.setTextColor(Color.WHITE);
        GradientDrawable background = new GradientDrawable();
        background.setColor(index == 0 ? Color.rgb(55, 68, 87) : Color.rgb(30, 34, 41));
        background.setCornerRadius(dp(6));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.rgb(75, 88, 107)), background, null));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(dp(index == 0 ? 80 : 56));
        button.setMinimumWidth(dp(index == 0 ? 80 : 56));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setFocusable(false);
        button.setFocusableInTouchMode(false);
        final long revision = generation;
        final InputConnection destination = active.getCurrentInputConnection();
        button.setOnClickListener(v -> {
            try {
                InputMethodService target = service.get();
                if (revision != generation || !selected || !allowed(target)
                        || destination != target.getCurrentInputConnection()) return;
                generation++;
                candidatesExpanded = false;
                if (session.isPending()) return;
                sessionDestination = new WeakReference<>(destination);
                session.submit(model -> model.commitCandidate(candidateId));
                updateUi();
            } catch (Throwable failure) { fail(failure); }
        });
        return button;
    }

    private void renderExpanded(View keyboard, InputMethodService active) {
        removeExpanded();
        if (!candidatesExpanded || visibleCandidateCount >= session.getCandidates().size()
                || keyboard == null || !(keyboard.getParent() instanceof FrameLayout)) return;
        FrameLayout parent = (FrameLayout) keyboard.getParent();
        int width = keyboard.getWidth(), height = keyboard.getHeight();
        if (width <= 0 || height <= 0) return;
        ScrollView viewport = new ScrollView(parent.getContext());
        viewport.setFillViewport(true);
        viewport.setFocusable(false);
        viewport.setClickable(true);
        viewport.setBackgroundColor(Color.rgb(30, 34, 41));
        LinearLayout grid = new LinearLayout(parent.getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setBackgroundColor(Color.rgb(30, 34, 41));
        // Both surfaces start at the keyboard edge; the host strip's outer start
        // margin is removed only while our Chinese candidate UI owns it.
        grid.setPadding(0, dp(4), dp(6), dp(4));
        grid.setClickable(true);
        grid.setFocusable(false);
        int columns = Math.max(1, (width - grid.getPaddingLeft() - grid.getPaddingRight() + dp(4)) / (dp(84) + dp(4)));
        for (int start = visibleCandidateCount; start < session.getCandidates().size(); start += columns) {
            LinearLayout line = new LinearLayout(parent.getContext());
            line.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
            rowParams.bottomMargin = dp(4);
            grid.addView(line, rowParams);
            for (int col = 0; col < columns; col++) {
                int index = start + col;
                View cell = index < session.getCandidates().size()
                        ? candidateButton(parent.getContext(), index, active) : new View(parent.getContext());
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                if (col > 0) cellParams.setMarginStart(dp(4));
                line.addView(cell, cellParams);
            }
        }
        viewport.addView(grid, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // Explicit measured bounds avoid MATCH_PARENT being measured as wrap-content by this
        // wrap-content host FrameLayout. Cover all keys, without resizing the spatial surface.
        FrameLayout.LayoutParams bounds = new FrameLayout.LayoutParams(width, height);
        bounds.leftMargin = keyboard.getLeft();
        bounds.topMargin = keyboard.getTop();
        parent.addView(viewport, bounds);
        expandedCandidates = new WeakReference<>(viewport);
        diagnostic("expanded bounds=" + width + "x" + height + " hiddenFrom=" + visibleCandidateCount);
    }

    private void removeExpanded() {
        View expanded = expandedCandidates.get();
        if (expanded != null && expanded.getParent() instanceof ViewGroup) ((ViewGroup) expanded.getParent()).removeView(expanded);
        expandedCandidates.clear();
    }

    private void updateMirrorUnderline(ViewGroup current) {
        clearMirrorUnderline();
        View mirror = current.findViewById(0x7f09026e);
        if (!(mirror instanceof TextView)) return;
        TextView text = (TextView) mirror;
        if (!(text.getText() instanceof Spannable)) return;
        Spannable value = (Spannable) text.getText();
        int end = text.getSelectionEnd();
        int start = CompositionRange.start(value, end, session.getComposition());
        if (start >= 0) {
            value.setSpan(mirrorUnderline, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            underlinedMirror = new WeakReference<>(value);
        }
    }

    private void clearMirrorUnderline() {
        Spannable previous = underlinedMirror.get();
        if (previous != null) previous.removeSpan(mirrorUnderline);
        underlinedMirror.clear();
    }

    private void restoreSuggestion() {
        removeExpanded();
        clearMirrorUnderline();
        LinearLayout row = bar.get();
        if (row != null && row.getParent() instanceof ViewGroup) ((ViewGroup) row.getParent()).removeView(row);
        ViewGroup owner = suggestion.get();
        if (owner != null) {
            if (savedSuggestionLayout != null) owner.setLayoutParams(savedSuggestionLayout);
            if (savedSuggestionPadding != null) owner.setPadding(savedSuggestionPadding[0], savedSuggestionPadding[1],
                    savedSuggestionPadding[2], savedSuggestionPadding[3]);
        }
        savedSuggestionLayout = null;
        savedSuggestionPadding = null;
        for (Map.Entry<View, Integer> entry : savedVisibility.entrySet()) entry.getKey().setVisibility(entry.getValue());
        savedVisibility.clear();
        bar.clear();
        suggestion.clear();
    }

    private void fail(Throwable error) {
        if (failed) return;
        try { resetSession(true); } catch (Throwable ignored) { /* preserve host */ }
        failed = true;
        selected = false;
        session.close();
        for (Map.Entry<Object, String> entry : changedIme.entrySet()) {
            try { set(entry.getKey(), "A0M", entry.getValue()); } catch (Exception ignored) { }
        }
        for (Map.Entry<Object, WeakReference<Object>> entry : addedMenuKeys.entrySet()) {
            try {
                Object added = entry.getValue().get();
                List<?> rows = (List<?>) get(entry.getKey(), "A0P");
                List<?> last = (List<?>) get(rows.get(rows.size() - 1), "A06");
                last.remove(added);
                if (last.size() == 1) set(last.get(0), "A0B", 0);
                ((List<?>) get(entry.getKey(), "A0R")).clear();
                set(entry.getKey(), "A0X", null);
            } catch (Exception ignored) { }
        }
        addedMenuKeys.clear();
        try { restoreSuggestion(); } catch (Throwable ignored) { }
        MainHook.log("probe disabled for this process: " + error.getClass().getSimpleName());
    }

    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    private void diagnostic(String state) {
        if (!state.equals(lastUiDiagnostic)) { MainHook.log(state); lastUiDiagnostic = state; }
    }
    private static int code(Object key) throws Exception {
        List<?> values = (List<?>) get(key, "A0b");
        return values.isEmpty() ? 0 : ((Number) values.get(0)).intValue();
    }
    private static Object get(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value);
    }
    private static void requireField(Class<?> type, String name, Class<?> expected) throws Exception {
        if (!type.getDeclaredField(name).getType().equals(expected)) throw new IllegalStateException("Host shape mismatch");
    }
    private void hook(java.lang.reflect.Member member, XC_MethodHook callback) { hooks.add(XposedBridge.hookMethod(member, callback)); }
    private abstract class GuardedHook extends XC_MethodHook {
        void before(MethodHookParam p) throws Exception { }
        void after(MethodHookParam p) throws Exception { }
        @Override protected final void beforeHookedMethod(MethodHookParam p) {
            if (failed) return;
            try { before(p); } catch (Throwable failure) { fail(failure); }
        }
        @Override protected final void afterHookedMethod(MethodHookParam p) {
            if (failed || p.hasThrowable()) return;
            try { after(p); } catch (Throwable failure) { fail(failure); }
        }
    }
}
