package com.khanenoor.parsavatts.ttsService.tokenizer;

/**
 * Token categories for eSpeak-like mixed-text detection.
 */
public enum TokenType {
    WORD_LATIN,
    WORD_PERSIAN,
    NUMBER,
    PUNCT,
    EMOJI,
    SYMBOL,
    WHITESPACE,
    UNKNOWN
}
