package com.m0e_n00b.questimepinyin;

public final class InputPolicyTest {
    public static void main(String[] args) {
        int[] blocked = {0, 2, 3, 4, 0x12, 0x21, 0x81, 0x91, 0xd1, 0xe1, 0x80081};
        for (int type : blocked) {
            if (InputPolicy.allows(type)) throw new AssertionError("Protected field accepted: " + type);
        }
        int[] ordinary = {1, 0xa1, 0x20001, 0x80001, 0x200a1, 0x11, 0x80011};
        for (int type : ordinary) {
            if (!InputPolicy.allows(type)) throw new AssertionError("Ordinary text rejected: " + type);
        }
        int[] textLayouts = {0x7f12007e, 0x7f120000, 0x7f1200b4, 0x7f120082};
        for (int resource : textLayouts) {
            if (!InputPolicy.supportsLayout(resource)) throw new AssertionError("Text layout rejected");
        }
        int[] otherLayouts = {0, 0x7f120098, 0x7f120092, 0x7f120096, 0x7f120091};
        for (int resource : otherLayouts) {
            if (InputPolicy.supportsLayout(resource)) throw new AssertionError("Non-text layout accepted");
        }
        System.out.println("InputPolicyTest: " + (blocked.length + ordinary.length
                + textLayouts.length + otherLayouts.length) + " field/layout policies passed");
    }
}
