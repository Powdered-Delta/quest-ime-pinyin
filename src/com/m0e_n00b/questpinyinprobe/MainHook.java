package com.m0e_n00b.questpinyinprobe;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.FileInputStream;
import java.security.MessageDigest;

/** Legacy API is intentional: Vector 2.2 supports it; no resource hooks are used. */
public final class MainHook implements IXposedHookLoadPackage {
    static final String TARGET = "com.oculus.vrshell";
    static final String APK_SHA256 = "9c2f1c503031f3adb21964463c8c89c13b790b45c5375ccbd3ed01c5b5c17ed4";
    private static boolean installed;

    @Override public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam load) throws Throwable {
        if (!TARGET.equals(load.packageName) || !TARGET.equals(load.processName)) return;
        XposedBridge.hookMethod(Application.class.getDeclaredMethod("attach", Context.class),
                new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (installed) return;
                        installed = true;
                        try {
                            Context context = ((Context) p.args[0]).getApplicationContext();
                            if (context == null) context = (Context) p.args[0];
                            PackageInfo info = context.getPackageManager().getPackageInfo(TARGET, 0);
                            if (info.getLongVersionCode() != 1045683657L
                                    || !APK_SHA256.equals(hash(context.getApplicationInfo().sourceDir))) {
                                log("unsupported host; hooks not installed");
                                return;
                            }
                            HostAdapter adapter = new HostAdapter(context, load.classLoader);
                            adapter.install();
                            log("hooks installed; fixed-mapping probe; host fingerprint matched");
                        } catch (Throwable failure) {
                            // Do not print exception messages: they can contain editor text.
                            log("initialization failed: " + failure.getClass().getSimpleName());
                        }
                    }
                });
    }

    private static String hash(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(path)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder text = new StringBuilder();
        for (byte value : digest.digest()) text.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return text.toString();
    }

    static void log(String message) { XposedBridge.log("QuestPinyinProbe: " + message); }
}
