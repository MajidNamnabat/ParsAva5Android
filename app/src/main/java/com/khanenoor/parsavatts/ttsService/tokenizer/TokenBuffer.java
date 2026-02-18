package com.khanenoor.parsavatts.ttsService.tokenizer;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable low-allocation token buffer for hot-path token detection.
 */
public final class TokenBuffer {
    private static final int DEFAULT_CAPACITY = 16;

    private TokenType[] types;
    private int[] starts;
    private int[] ends;
    private int size;

    public TokenBuffer() {
        this(DEFAULT_CAPACITY);
    }

    public TokenBuffer(int initialCapacity) {
        final int cap = Math.max(1, initialCapacity);
        types = new TokenType[cap];
        starts = new int[cap];
        ends = new int[cap];
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public void add(@NonNull TokenType type, int startCodeUnit, int endCodeUnitExclusive) {
        ensureCapacity(size + 1);
        types[size] = type;
        starts[size] = startCodeUnit;
        ends[size] = endCodeUnitExclusive;
        size++;
    }

    @NonNull
    public TokenType getType(int index) {
        checkIndex(index);
        return types[index];
    }

    public int getStartCodeUnit(int index) {
        checkIndex(index);
        return starts[index];
    }

    public int getEndCodeUnitExclusive(int index) {
        checkIndex(index);
        return ends[index];
    }

    @NonNull
    public List<DetectedToken> materialize(@NonNull CharSequence source) {
        if (size == 0) {
            return new ArrayList<DetectedToken>(0);
        }
        final List<DetectedToken> result = new ArrayList<DetectedToken>(size);
        for (int i = 0; i < size; i++) {
            result.add(DetectedToken.fromSource(types[i], source, starts[i], ends[i]));
        }
        return result;
    }

    private void ensureCapacity(int target) {
        if (target <= types.length) {
            return;
        }
        int next = types.length * 2;
        while (next < target) {
            next *= 2;
        }

        final TokenType[] nextTypes = new TokenType[next];
        final int[] nextStarts = new int[next];
        final int[] nextEnds = new int[next];

        System.arraycopy(types, 0, nextTypes, 0, size);
        System.arraycopy(starts, 0, nextStarts, 0, size);
        System.arraycopy(ends, 0, nextEnds, 0, size);

        types = nextTypes;
        starts = nextStarts;
        ends = nextEnds;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}
