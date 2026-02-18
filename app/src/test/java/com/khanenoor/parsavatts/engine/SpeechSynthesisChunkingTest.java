package com.khanenoor.parsavatts.engine;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SpeechSynthesisChunkingTest {

    @Test
    public void splitsPersianAndEnglishBySentenceBoundary() {
        List<String> chunks = SpeechSynthesis.splitIntoSynthesisChunks("سلام دنیا. hello world! خوبی؟");

        assertEquals(2, chunks.size());
        assertEquals("سلام دنیا.", chunks.get(0));
        assertEquals("hello world! خوبی؟", chunks.get(1));
    }

    @Test
    public void trimsWhitespaceAndSkipsEmptyChunks() {
        List<String> chunks = SpeechSynthesis.splitIntoSynthesisChunks("   سلام\n\n  دنیا  ");

        assertEquals(2, chunks.size());
        assertEquals("سلام", chunks.get(0));
        assertEquals("دنیا", chunks.get(1));
    }

    @Test
    public void doesNotSplitOnDotAfterNumberOrSinglePersianLetterAbbreviation() {
        List<String> chunks = SpeechSynthesis.splitIntoSynthesisChunks("نسخه 2.5 د. امروز رسید.");

        assertEquals(1, chunks.size());
        assertEquals("نسخه 2.5 د. امروز رسید.", chunks.get(0));
    }
}
