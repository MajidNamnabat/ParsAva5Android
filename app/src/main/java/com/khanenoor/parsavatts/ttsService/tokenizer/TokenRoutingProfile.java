package com.khanenoor.parsavatts.ttsService.tokenizer;

import androidx.annotation.NonNull;

/**
 * Aggregated token metadata for low-latency TTS routing decisions.
 */
public final class TokenRoutingProfile {
    private static final TokenRoutingProfile EMPTY = new TokenRoutingProfile(0, 0, 0, 0, 0, 0);
    private final int totalTokens;
    private final int persianWordTokens;
    private final int latinWordTokens;
    private final int numberTokens;
    private final int emojiTokens;
    private final int punctuationTokens;

    private TokenRoutingProfile(int totalTokens,
                                int persianWordTokens,
                                int latinWordTokens,
                                int numberTokens,
                                int emojiTokens,
                                int punctuationTokens) {
        this.totalTokens = totalTokens;
        this.persianWordTokens = persianWordTokens;
        this.latinWordTokens = latinWordTokens;
        this.numberTokens = numberTokens;
        this.emojiTokens = emojiTokens;
        this.punctuationTokens = punctuationTokens;
    }

    @NonNull
    public static TokenRoutingProfile from(@NonNull TokenBuffer buffer) {
        if (buffer.size() == 0) {
            return empty();
        }
        int persian = 0;
        int latin = 0;
        int numbers = 0;
        int emoji = 0;
        int punct = 0;

        for (int i = 0; i < buffer.size(); i++) {
            final TokenType type = buffer.getType(i);
            if (type == TokenType.WORD_PERSIAN) {
                persian++;
            } else if (type == TokenType.WORD_LATIN) {
                latin++;
            } else if (type == TokenType.NUMBER) {
                numbers++;
            } else if (type == TokenType.EMOJI) {
                emoji++;
            } else if (type == TokenType.PUNCT) {
                punct++;
            }
        }

        return new TokenRoutingProfile(buffer.size(), persian, latin, numbers, emoji, punct);
    }

    @NonNull
    public static TokenRoutingProfile empty() {
        return EMPTY;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public int getPersianWordTokens() {
        return persianWordTokens;
    }

    public int getLatinWordTokens() {
        return latinWordTokens;
    }

    public int getNumberTokens() {
        return numberTokens;
    }

    public int getEmojiTokens() {
        return emojiTokens;
    }

    public int getPunctuationTokens() {
        return punctuationTokens;
    }

    public boolean hasPersianWords() {
        return persianWordTokens > 0;
    }

    public boolean hasLatinWords() {
        return latinWordTokens > 0;
    }

    public boolean isMixedScriptWords() {
        return hasPersianWords() && hasLatinWords();
    }

    @NonNull
    public String toCompactLogString() {
        return "tokens=" + totalTokens
                + " fa=" + persianWordTokens
                + " en=" + latinWordTokens
                + " num=" + numberTokens
                + " emoji=" + emojiTokens
                + " punct=" + punctuationTokens
                + " mixed=" + isMixedScriptWords();
    }
}
