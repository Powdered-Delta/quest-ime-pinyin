package com.m0e_n00b.questimepinyin;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Local diagnostic editor: no network, persistence, history or entered-text logs. */
public final class ProbeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 24, 24, 24);
        TextView title = new TextView(this);
        title.setText("Quest IME Pinyin 验证");
        title.setTextSize(24);
        content.addView(title);
        TextView instruction = new TextView(this);
        instruction.setText("从系统键盘语言菜单选择中文（拼音），输入 nihao，再选择候选“你好”。");
        content.addView(instruction);
        Button selfTest = new Button(this);
        selfTest.setText("运行真实词库自检");
        selfTest.setOnClickListener(v -> DictionarySelfTest.run(this, getApplicationInfo().sourceDir,
                (passed, check) -> runOnUiThread(() -> instruction.setText(passed ? "真实词库自检通过（9 项）" : "真实词库自检失败：检查 " + check))));
        content.addView(selfTest);
        if (getIntent().getBooleanExtra("dictionary_self_test", false)) selfTest.post(selfTest::performClick);
        EditText editor = new EditText(this);
        editor.setId(0x1001001);
        editor.setHint("在这里验证输入");
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setSingleLine(true);
        content.addView(editor, new LinearLayout.LayoutParams(-1, 72));
        TextView result = new TextView(this);
        result.setTextSize(18);
        result.setText("TEXT= | COMPOSING=-1,-1");
        content.addView(result);
        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void afterTextChanged(Editable value) {
                result.setText("TEXT=" + value + " | COMPOSING="
                        + BaseInputConnection.getComposingSpanStart(value) + ","
                        + BaseInputConnection.getComposingSpanEnd(value));
            }
        });
        Button show = new Button(this);
        show.setText("打开系统键盘");
        show.setOnClickListener(v -> {
            editor.requestFocus();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT);
        });
        content.addView(show);
        Button clear = new Button(this);
        clear.setText("清空测试文本");
        clear.setOnClickListener(v -> editor.setText(""));
        content.addView(clear);
        setContentView(content);
        editor.requestFocus();
        editor.postDelayed(() -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT), 300);
    }
}
