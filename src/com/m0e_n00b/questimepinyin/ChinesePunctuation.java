package com.m0e_n00b.questimepinyin;

/** Converts common punctuation in Chinese mode while preserving technical symbols. */
final class ChinesePunctuation {
    private ChinesePunctuation() { }

    static String convert(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            switch (value.charAt(i)) {
                case ',': result.append('\uFF0C'); break;
                case '.': result.append('\u3002'); break;
                case '!': result.append('\uFF01'); break;
                case '?': result.append('\uFF1F'); break;
                case ';': result.append('\uFF1B'); break;
                case ':': result.append('\uFF1A'); break;
                case '(': result.append('\uFF08'); break;
                case ')': result.append('\uFF09'); break;
                case '[': result.append('\u3010'); break;
                case ']': result.append('\u3011'); break;
                case '<': result.append('\u300A'); break;
                case '>': result.append('\u300B'); break;
                default: result.append(value.charAt(i));
            }
        }
        return result.toString();
    }
}
