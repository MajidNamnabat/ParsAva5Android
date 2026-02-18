package com.khanenoor.parsavatts.ttsService.tokenizer;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * eSpeak-like mixed-text tokenizer for DualTts routing.
 */
public class EspeakLikeTokenDetector {

    enum CharClass {
        LATIN_LETTER,
        PERSIAN_LETTER,
        DIGIT,
        NUMBER_JOINER,
        WHITESPACE,
        PUNCT,
        EMOJI,
        SYMBOL,
        UNKNOWN
    }

    @NonNull
    private final TokenDetectionConfig config;

    public EspeakLikeTokenDetector() {
        this(TokenDetectionConfig.defaults());
    }

    public EspeakLikeTokenDetector(@NonNull TokenDetectionConfig config) {
        this.config = config;
    }

    /**
     * Low-allocation API for hot synthesis paths.
     */
    public void detectInto(@NonNull CharSequence input, @NonNull TokenBuffer outBuffer) {
        outBuffer.clear();
        final int length = input.length();
        if (length == 0) {
            return;
        }

        int i = 0;
        while (i < length) {
            final int cp = Character.codePointAt(input, i);
            final CharClass cls = classifyCodePoint(cp);

            if (cls == CharClass.WHITESPACE) {
                final int end = consumeWhitespace(input, i);
                if (config.includeWhitespaceTokens()) {
                    outBuffer.add(TokenType.WHITESPACE, i, end);
                }
                i = end;
                continue;
            }

            if (cls == CharClass.DIGIT) {
                final int end = consumeNumber(input, i);
                outBuffer.add(TokenType.NUMBER, i, end);
                i = end;
                continue;
            }

            if (cls == CharClass.LATIN_LETTER || cls == CharClass.PERSIAN_LETTER) {
                final int end = consumeWord(input, i, cls);
                outBuffer.add(cls == CharClass.LATIN_LETTER ? TokenType.WORD_LATIN : TokenType.WORD_PERSIAN, i, end);
                i = end;
                continue;
            }

            if (cls == CharClass.EMOJI) {
                final int end = consumeEmojiCluster(input, i);
                outBuffer.add(TokenType.EMOJI, i, end);
                i = end;
                continue;
            }

            if (cls == CharClass.PUNCT) {
                final int end = consumePunctuationCluster(input, i);
                outBuffer.add(TokenType.PUNCT, i, end);
                i = end;
                continue;
            }

            if (cls == CharClass.SYMBOL) {
                final int end = consumeSymbolCluster(input, i);
                outBuffer.add(TokenType.SYMBOL, i, end);
                i = end;
                continue;
            }

            final int next = i + Character.charCount(cp);
            outBuffer.add(TokenType.UNKNOWN, i, next);
            i = next;
        }
    }

    /**
     * Convenience API for code paths that need immutable token objects.
     */
    @NonNull
    public List<DetectedToken> detect(@NonNull CharSequence input) {
        final TokenBuffer buffer = new TokenBuffer(8);
        detectInto(input, buffer);
        return buffer.materialize(input);
    }

    private int consumeWhitespace(@NonNull CharSequence input, int start) {
        int i = start;
        while (i < input.length()) {
            final int cp = Character.codePointAt(input, i);
            if (!Character.isWhitespace(cp)) {
                break;
            }
            i += Character.charCount(cp);
        }
        return i;
    }

    private int consumeNumber(@NonNull CharSequence input, int start) {
        int i = start;
        final int length = input.length();
        while (i < length) {
            final int cp = Character.codePointAt(input, i);
            if (isDecimalDigit(cp)) {
                i += Character.charCount(cp);
                continue;
            }
            if (isNumberJoiner(cp) && hasDigitBeforeAndAfter(input, i)) {
                i += Character.charCount(cp);
                continue;
            }
            break;
        }
        return i;
    }

    private int consumeWord(@NonNull CharSequence input, int start, @NonNull CharClass baseClass) {
        int i = start;
        final int length = input.length();

        while (i < length) {
            final int cp = Character.codePointAt(input, i);
            final CharClass currentClass = classifyCodePoint(cp);

            if (shouldForceScriptBoundary(baseClass, currentClass)) {
                break;
            }

            if (currentClass == baseClass || isWordMark(cp) || isInternalWordJoiner(cp)) {
                i += Character.charCount(cp);
                continue;
            }
            break;
        }
        return i;
    }

    private int consumeEmojiCluster(@NonNull CharSequence input, int start) {
        int i = start;
        final int length = input.length();
        boolean allowNextEmoji = false;

        while (i < length) {
            final int cp = Character.codePointAt(input, i);
            if (isEmojiLike(cp) || isEmojiModifier(cp)) {
                final int count = Character.charCount(cp);
                i += count;
                allowNextEmoji = (cp == 0x200D);
                continue;
            }
            if (allowNextEmoji) {
                final CharClass nextClass = classifyCodePoint(cp);
                if (nextClass == CharClass.EMOJI) {
                    i += Character.charCount(cp);
                    allowNextEmoji = false;
                    continue;
                }
            }
            break;
        }
        return i;
    }

    private int consumePunctuationCluster(@NonNull CharSequence input, int start) {
        int i = start;
        final int length = input.length();
        while (i < length) {
            final int cp = Character.codePointAt(input, i);
            if (!isPunctuation(cp)) {
                break;
            }
            i += Character.charCount(cp);
        }
        return i;
    }

    private int consumeSymbolCluster(@NonNull CharSequence input, int start) {
        int i = start;
        final int length = input.length();
        while (i < length) {
            final int cp = Character.codePointAt(input, i);
            if (!isSymbol(cp)) {
                break;
            }
            i += Character.charCount(cp);
        }
        return i;
    }

    private boolean hasDigitBeforeAndAfter(@NonNull CharSequence input, int index) {
        final int before = findPreviousCodePoint(input, index);
        final int after = findNextCodePoint(input, index);
        return before >= 0 && after >= 0 && isDecimalDigit(before) && isDecimalDigit(after);
    }

    private int findPreviousCodePoint(@NonNull CharSequence input, int currentIndex) {
        if (currentIndex <= 0) {
            return -1;
        }
        final int prevIndex = Character.offsetByCodePoints(input, currentIndex, -1);
        return Character.codePointAt(input, prevIndex);
    }

    private int findNextCodePoint(@NonNull CharSequence input, int currentIndex) {
        final int cp = Character.codePointAt(input, currentIndex);
        final int nextIndex = currentIndex + Character.charCount(cp);
        if (nextIndex >= input.length()) {
            return -1;
        }
        return Character.codePointAt(input, nextIndex);
    }

    private boolean isWordMark(int codePoint) {
        final int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    private boolean isInternalWordJoiner(int codePoint) {
        return codePoint == '\'' || codePoint == 0x2019 || codePoint == '-' || codePoint == 0x0640;
    }

    static CharClass classifyCodePoint(int codePoint) {
        if (Character.isWhitespace(codePoint)) {
            return CharClass.WHITESPACE;
        }
        if (isPersianLetter(codePoint)) {
            return CharClass.PERSIAN_LETTER;
        }
        if (isLatinLetter(codePoint)) {
            return CharClass.LATIN_LETTER;
        }
        if (isDecimalDigit(codePoint)) {
            return CharClass.DIGIT;
        }
        if (isNumberJoiner(codePoint)) {
            return CharClass.NUMBER_JOINER;
        }
        if (isEmojiLike(codePoint)) {
            return CharClass.EMOJI;
        }
        if (isPunctuation(codePoint)) {
            return CharClass.PUNCT;
        }
        if (isSymbol(codePoint)) {
            return CharClass.SYMBOL;
        }
        return CharClass.UNKNOWN;
    }

    static boolean shouldForceScriptBoundary(@NonNull CharClass previousClass,
                                             @NonNull CharClass currentClass) {
        return (previousClass == CharClass.LATIN_LETTER && currentClass == CharClass.PERSIAN_LETTER)
                || (previousClass == CharClass.PERSIAN_LETTER && currentClass == CharClass.LATIN_LETTER);
    }

    static boolean isNumberJoiner(int codePoint) {
        return codePoint == '.'
                || codePoint == ','
                || codePoint == '/'
                || codePoint == '-'
                || codePoint == 0x066B
                || codePoint == 0x066C;
    }

    static boolean isDecimalDigit(int codePoint) {
        return Character.getType(codePoint) == Character.DECIMAL_DIGIT_NUMBER;
    }

    static boolean isLatinLetter(int codePoint) {
        if (!Character.isLetter(codePoint)) {
            return false;
        }
        final Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.BASIC_LATIN
                || block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT
                || block == Character.UnicodeBlock.LATIN_EXTENDED_A
                || block == Character.UnicodeBlock.LATIN_EXTENDED_B
                || block == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL;
    }

    static boolean isPersianLetter(int codePoint) {
        if (!Character.isLetter(codePoint)) {
            return false;
        }
        final Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.ARABIC
                || block == Character.UnicodeBlock.ARABIC_SUPPLEMENT
                || block == Character.UnicodeBlock.ARABIC_EXTENDED_A
                || block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A
                || block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B;
    }

    static boolean isPunctuation(int codePoint) {
        final int type = Character.getType(codePoint);
        if (type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION) {
            return true;
        }

        return codePoint == 0x060C
                || codePoint == 0x061B
                || codePoint == 0x061F;
    }

    static boolean isSymbol(int codePoint) {
        final int type = Character.getType(codePoint);
        return type == Character.MATH_SYMBOL
                || type == Character.CURRENCY_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.OTHER_SYMBOL;
    }

    static boolean isEmojiLike(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || codePoint == 0x200D
                || codePoint == 0xFE0F;
    }

    static boolean isEmojiModifier(int codePoint) {
        return codePoint >= 0x1F3FB && codePoint <= 0x1F3FF;
    }
}
