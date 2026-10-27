/* Wolvic-derived lookup, MPL-2.0. See THIRD_PARTY.md and assets/ime/notices/. */
package com.m0e_n00b.questpinyinprobe;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
/** Lazy module-APK asset extraction and read-only SQLite lookup on a single worker. */
final class SqlitePinyinBackend implements PinyinBackend {
    static final String SHA = "920f621c690355d33ed6b9732bb7fa3e297b36c379d6b352ef3643be43b83635";
    private final Context context;
    private final String moduleApk;
    private SQLiteDatabase database;
    private final PinyinCandidates evaluator = new PinyinCandidates(this::lookup);
    private final Map<String, List<PinyinCandidates.Word>> cache = new LinkedHashMap<String, List<PinyinCandidates.Word>>(128, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, List<PinyinCandidates.Word>> item) { return size() > 128; }
    };
    SqlitePinyinBackend(Context context, String moduleApk) { this.context = context; this.moduleApk = moduleApk; }
    public List<PinyinCandidate> evaluate(String raw) {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("Dictionary on UI thread");
        return evaluator.evaluate(raw);
    }
    private List<PinyinCandidates.Word> lookup(String key) {
        List<PinyinCandidates.Word> found = cache.get(key);
        if (found != null) return found;
        if (database == null) database = open();
        List<PinyinCandidates.Word> rows = new ArrayList<>();
        try (Cursor cursor = database.rawQuery("SELECT display,candidates FROM keymaps WHERE keymap=? ORDER BY _id LIMIT 128", new String[]{key})) {
            while (cursor.moveToNext()) for (int i = 0; i < 2; i++) if (!cursor.isNull(i)) add(rows, key, cursor.getString(i));
        }
        try (Cursor cursor = database.rawQuery("SELECT displaycode,display FROM autocorrect WHERE inputcode=? ORDER BY _id LIMIT 128", new String[]{key})) {
            while (cursor.moveToNext()) add(rows, cursor.getString(0), cursor.getString(1));
        }
        if (rows.isEmpty()) try (Cursor cursor = database.rawQuery("SELECT displaycode,display FROM autocorrect WHERE inputcode LIKE ? ORDER BY length(inputcode),_id LIMIT 32", new String[]{key + "%"})) {
            while (cursor.moveToNext()) add(rows, cursor.getString(0), cursor.getString(1));
        }
        cache.put(key, rows);
        return rows;
    }
    private static void add(List<PinyinCandidates.Word> rows, String code, String values) {
        if (code == null || values == null) return;
        for (String text : values.split("\\|")) if (!text.isEmpty()) rows.add(new PinyinCandidates.Word(code, text));
    }
    private SQLiteDatabase open() {
        File folder = new File(context.getFilesDir(), "quest_ime_pinyin");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IllegalStateException("Dictionary directory");
        File file = new File(folder, "pinyin-" + SHA + ".db");
        try {
            if (!file.isFile() || !SHA.equals(hash(file))) {
                File temporary = new File(folder, "pinyin.tmp");
                try {
                    if (moduleApk == null) throw new IOException("Module path unavailable");
                    try (ZipFile apk = new ZipFile(moduleApk)) {
                        ZipEntry entry = apk.getEntry("assets/ime/google_pinyin.db");
                        if (entry == null || entry.getSize() != 3023872) throw new IOException("Dictionary asset missing");
                        try (InputStream input = apk.getInputStream(entry); FileOutputStream output = new FileOutputStream(temporary)) {
                            byte[] buffer = new byte[32768]; int count;
                            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                            output.getFD().sync();
                        }
                    }
                    if (!SHA.equals(hash(temporary))) throw new IOException("Dictionary checksum");
                    if (!temporary.renameTo(file)) throw new IOException("Dictionary install");
                } finally { if (temporary.exists()) temporary.delete(); }
            }
            SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            try (Cursor check = db.rawQuery("PRAGMA quick_check", null)) {
                if (!check.moveToFirst() || !"ok".equals(check.getString(0))) { db.close(); throw new IOException("Dictionary integrity"); }
            }
            android.util.Log.i("QuestPinyinDictionary", "dictionary ready; sha256=" + SHA + "; worker initialized");
            return db;
        } catch (IOException failure) { throw new IllegalStateException("Dictionary initialization", failure); }
    }
    private static String hash(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new FileInputStream(file)) { byte[] buffer = new byte[32768]; int count; while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count); }
            StringBuilder result = new StringBuilder(); for (byte b : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", b & 255)); return result.toString();
        } catch (java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }
    public void close() { if (database != null) { database.close(); database = null; } cache.clear(); }
}
