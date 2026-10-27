package com.m0e_n00b.questimepinyin;

/** Android inputType policy kept free of Android runtime dependencies for JVM verification. */
public final class InputPolicy {
    private InputPolicy() { }

    public static boolean allows(int inputType) {
        if ((inputType & 0x0f) != 1) return false; // Only TYPE_CLASS_TEXT.
        switch (inputType & 0x0ff0) {
            case 0x20: // email
            case 0x80: // password
            case 0x90: // visible password
            case 0xd0: // web email
            case 0xe0: // web password
                return false;
            default:
                return true;
        }
    }

    /** Resource IDs belong to the fingerprint-gated VrShell version, not arbitrary APKs. */
    public static boolean supportsLayout(int resource) {
        return resource == 0x7f12007e // English text QWERTY
                || resource == 0x7f120000 // Browser address/search bar
                || resource == 0x7f1200b4 // Generic URI text
                || resource == 0x7f120082; // Simple English text
    }
}
