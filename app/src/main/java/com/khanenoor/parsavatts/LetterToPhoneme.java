package com.khanenoor.parsavatts;

import java.util.HashMap;
import java.util.Map;

public class LetterToPhoneme {

    // 1) نگاشت حروف فارسی به فونم‌های (تقریباً) استاندارد برای fa در eSpeak/Piper
    // خروجی‌ها رشته‌اند؛ برای برخی حروف چند فونم داریم (مثل چ = "t ʃ")
    public static final Map<Character, String> FA_LETTER_TO_PHONEME = new HashMap<>();

    // 2) نگاشت اعراب/علائم عربی-فارسی به فونم‌ها/رفتارها
    public static final Map<Character, String> FA_DIACRITIC_TO_PHONEME = new HashMap<>();

    static {
        // --- Consonants ---
        FA_LETTER_TO_PHONEME.put('ا', "");      // خودِ ا معمولاً حامل واکه است؛ وابسته به زمینه
        FA_LETTER_TO_PHONEME.put('آ', "ɒ");     // آ = /ɒː/ (تقریبی)
        FA_LETTER_TO_PHONEME.put('ب', "b");
        FA_LETTER_TO_PHONEME.put('پ', "p");
        FA_LETTER_TO_PHONEME.put('ت', "t");
        FA_LETTER_TO_PHONEME.put('ث', "s");
        FA_LETTER_TO_PHONEME.put('ج', "d ʒ");  // /dʒ/
        FA_LETTER_TO_PHONEME.put('چ', "t ʃ");  // /tʃ/
        FA_LETTER_TO_PHONEME.put('ح', "h");    // در بسیاری از سامانه‌ها مثل /h/
        FA_LETTER_TO_PHONEME.put('خ', "x");    // /x/
        FA_LETTER_TO_PHONEME.put('د', "d");
        FA_LETTER_TO_PHONEME.put('ذ', "z");
        FA_LETTER_TO_PHONEME.put('ر', "r");
        FA_LETTER_TO_PHONEME.put('ز', "z");
        FA_LETTER_TO_PHONEME.put('ژ', "ʒ");    // /ʒ/
        FA_LETTER_TO_PHONEME.put('س', "s");
        FA_LETTER_TO_PHONEME.put('ش', "ʃ");    // /ʃ/
        FA_LETTER_TO_PHONEME.put('ص', "s");
        FA_LETTER_TO_PHONEME.put('ض', "z");
        FA_LETTER_TO_PHONEME.put('ط', "t");
        FA_LETTER_TO_PHONEME.put('ظ', "z");
        FA_LETTER_TO_PHONEME.put('ع', "");     // اغلب در فارسی امروزی بی‌صدا/حذف می‌شود
        FA_LETTER_TO_PHONEME.put('غ', "ɣ");    // /ɣ/
        FA_LETTER_TO_PHONEME.put('ف', "f");
        FA_LETTER_TO_PHONEME.put('ق', "q");    // برخی لهجه‌ها نزدیک ɣ؛ اینجا q گذاشتیم
        FA_LETTER_TO_PHONEME.put('ک', "k");
        FA_LETTER_TO_PHONEME.put('گ', "g");
        FA_LETTER_TO_PHONEME.put('ل', "l");
        FA_LETTER_TO_PHONEME.put('م', "m");
        FA_LETTER_TO_PHONEME.put('ن', "n");
        FA_LETTER_TO_PHONEME.put('ه', "h");
        FA_LETTER_TO_PHONEME.put('و', "v");    // و هم می‌تواند /v/ باشد هم واکه (o/u)؛ اینجا پایه v
        FA_LETTER_TO_PHONEME.put('ی', "j");    // ی هم می‌تواند /j/ یا واکه i باشد؛ اینجا پایه j

        // عربی رایج در متن فارسی
        FA_LETTER_TO_PHONEME.put('ي', "j");
        FA_LETTER_TO_PHONEME.put('ك', "k");
        FA_LETTER_TO_PHONEME.put('ة', "h");    // معمولاً /e/ یا /h/ بسته به بافت؛ اینجا ساده
        FA_LETTER_TO_PHONEME.put('ؤ', "v");    // ساده‌سازی
        FA_LETTER_TO_PHONEME.put('ئ', "j");    // ساده‌سازی

        // --- Common vowel carriers (context-dependent) ---
        // بهتر است این‌ها را با Rule جداگانه مدیریت کنید:
        // ا/و/ی بسته به جایگاه، می‌توانند واکه بلند بدهند.
        // مثال‌های پیشنهادی برای Rule:
        // - "او" -> ʔ? + u (معمولاً u)
        // - "ای" -> i
        // - "آ" -> ɒ

        // --- Diacritics / marks ---
        FA_DIACRITIC_TO_PHONEME.put('\u064E', "æ");  // َ  fatha -> æ (تقریبی)
        FA_DIACRITIC_TO_PHONEME.put('\u0650', "e");  // ِ  kasra -> e
        FA_DIACRITIC_TO_PHONEME.put('\u064F', "o");  // ُ  damma -> o (گاهی نزدیک u/o)
        FA_DIACRITIC_TO_PHONEME.put('\u0652', "");   // ْ  sukun -> بدون واکه
        FA_DIACRITIC_TO_PHONEME.put('\u0651', "ː");  // ّ  shadda -> تشدید (این را باید به معنی تکرار همخوان پیاده کنید)
        FA_DIACRITIC_TO_PHONEME.put('\u0670', "ɒ");  // ٰ  dagger alif (تقریبی)
        FA_DIACRITIC_TO_PHONEME.put('\u064B', "n");  // ً  tanwin fath -> n (در فارسی کم)
        FA_DIACRITIC_TO_PHONEME.put('\u064C', "n");  // ٌ
        FA_DIACRITIC_TO_PHONEME.put('\u064D', "n");  // ٍ

        // فاصله و نیم‌فاصله
        FA_LETTER_TO_PHONEME.put(' ', " ");
        FA_LETTER_TO_PHONEME.put('\u200C', " "); // ZWNJ
    }

    /**
     * تبدیل ساده‌ی کاراکتری (بدون قواعد بافتی):
     * - حروف را نگاشت می‌کند
     * - اعراب را به واکه‌های کوتاه نگاشت می‌کند
     * - خروجی را با فاصله جدا می‌کند (مناسب برای tokenization بعدی)
     */
    public static String roughPhonemize(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            String p = FA_DIACRITIC_TO_PHONEME.get(ch);
            if (p != null) {
                // شَدّه را فقط علامت گذاشتیم؛ بهتر: همخوان قبلی را دوباره تکرار کنید
                appendPh(out, p);
                continue;
            }

            p = FA_LETTER_TO_PHONEME.get(ch);
            if (p != null) {
                appendPh(out, p);
                continue;
            }

            // نادیده گرفتن علائم رایج
            if (isPunctuation(ch)) continue;

            // اگر ناشناخته بود، رد می‌کنیم یا می‌توانید نگه دارید
            // appendPh(out, String.valueOf(ch));
        }
        return out.toString().trim().replaceAll("\\s+", " ");
    }

    private static void appendPh(StringBuilder sb, String ph) {
        if (ph == null || ph.isEmpty()) return;
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') sb.append(' ');
        sb.append(ph);
        if (!ph.endsWith(" ")) sb.append(' ');
    }

    private static boolean isPunctuation(char ch) {
        return ".,!?؛:،«»()[]{}\"'ـ…".indexOf(ch) >= 0;
    }
}
