package com.khanenoor.parsavatts.ttsService.tokenizer;

import androidx.annotation.NonNull;

/**
 * Immutable token record for mixed-script preprocessing.
 */
public final class DetectedToken {
    @NonNull
    private final TokenType type;
    @NonNull
    private final String text;
    private final int startCodeUnit;
    private final int endCodeUnitExclusive;

    public DetectedToken(@NonNull TokenType type,
                         @NonNull String text,
                         int startCodeUnit,
                         int endCodeUnitExclusive) {
        this.type = type;
        this.text = text;
        this.startCodeUnit = startCodeUnit;
        this.endCodeUnitExclusive = endCodeUnitExclusive;
    }

    @NonNull
    public static DetectedToken fromSource(@NonNull TokenType type,
                                           @NonNull CharSequence source,
                                           int startCodeUnit,
                                           int endCodeUnitExclusive) {
        return new DetectedToken(
                type,
                source.subSequence(startCodeUnit, endCodeUnitExclusive).toString(),
                startCodeUnit,
                endCodeUnitExclusive);
    }

    @NonNull
    public TokenType getType() {
        return type;
    }

    @NonNull
    public String getText() {
        return text;
    }

    public int getStartCodeUnit() {
        return startCodeUnit;
    }

    public int getEndCodeUnitExclusive() {
        return endCodeUnitExclusive;
    }
}
