package com.khanenoor.parsavatts.ttsService.tokenizer;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class EspeakLikeTokenDetectorTest {

    @Test
    public void detectsMixedPersianLatinNumberPunctuationAndEmoji() {
        EspeakLikeTokenDetector detector = new EspeakLikeTokenDetector();
        List<DetectedToken> tokens = detector.detect("سلام hello ۱۲۳، test😊!");

        assertEquals(TokenType.WORD_PERSIAN, tokens.get(0).getType());
        assertEquals("سلام", tokens.get(0).getText());
        assertEquals(TokenType.WHITESPACE, tokens.get(1).getType());
        assertEquals(TokenType.WORD_LATIN, tokens.get(2).getType());
        assertEquals(TokenType.WHITESPACE, tokens.get(3).getType());
        assertEquals(TokenType.NUMBER, tokens.get(4).getType());
        assertEquals(TokenType.PUNCT, tokens.get(5).getType());
        assertEquals(TokenType.WHITESPACE, tokens.get(6).getType());
        assertEquals(TokenType.WORD_LATIN, tokens.get(7).getType());
        assertEquals(TokenType.EMOJI, tokens.get(8).getType());
        assertEquals(TokenType.PUNCT, tokens.get(9).getType());
    }

    @Test
    public void splitsOnScriptBoundaryWithoutWhitespace() {
        EspeakLikeTokenDetector detector = new EspeakLikeTokenDetector();
        List<DetectedToken> tokens = detector.detect("abcسلام");

        assertEquals(2, tokens.size());
        assertEquals(TokenType.WORD_LATIN, tokens.get(0).getType());
        assertEquals("abc", tokens.get(0).getText());
        assertEquals(TokenType.WORD_PERSIAN, tokens.get(1).getType());
        assertEquals("سلام", tokens.get(1).getText());
    }

    @Test
    public void collapsesWhitespaceWhenDisabled() {
        EspeakLikeTokenDetector detector = new EspeakLikeTokenDetector(
                new TokenDetectionConfig.Builder().setIncludeWhitespaceTokens(false).build());

        List<DetectedToken> tokens = detector.detect("a   b");

        assertEquals(2, tokens.size());
        assertEquals(TokenType.WORD_LATIN, tokens.get(0).getType());
        assertEquals(TokenType.WORD_LATIN, tokens.get(1).getType());
    }

    @Test
    public void keepsNumericJoinersOnlyBetweenDigits() {
        EspeakLikeTokenDetector detector = new EspeakLikeTokenDetector();

        List<DetectedToken> tokens = detector.detect("12,345-67 / 89-");

        assertEquals(TokenType.NUMBER, tokens.get(0).getType());
        assertEquals("12,345-67", tokens.get(0).getText());
        assertEquals(TokenType.WHITESPACE, tokens.get(1).getType());
        assertEquals(TokenType.PUNCT, tokens.get(2).getType());
        assertEquals(TokenType.WHITESPACE, tokens.get(3).getType());
        assertEquals(TokenType.NUMBER, tokens.get(4).getType());
        assertEquals("89", tokens.get(4).getText());
        assertEquals(TokenType.PUNCT, tokens.get(5).getType());
    }

    @Test
    public void treatsZwjEmojiSequenceAsSingleEmojiToken() {
        EspeakLikeTokenDetector detector = new EspeakLikeTokenDetector();

        List<DetectedToken> tokens = detector.detect("👩‍💻 سلام");

        assertEquals(TokenType.EMOJI, tokens.get(0).getType());
        assertEquals("👩‍💻", tokens.get(0).getText());
        assertEquals(TokenType.WHITESPACE, tokens.get(1).getType());
        assertEquals(TokenType.WORD_PERSIAN, tokens.get(2).getType());
    }

    @Test
    public void groupsRepeatedPunctuationIntoSingleToken() {
        EspeakLikeTokenDetector detector = new EspeakLikeTokenDetector();

        List<DetectedToken> tokens = detector.detect("سلام؟!");

        assertEquals(2, tokens.size());
        assertEquals(TokenType.WORD_PERSIAN, tokens.get(0).getType());
        assertEquals(TokenType.PUNCT, tokens.get(1).getType());
        assertEquals("؟!", tokens.get(1).getText());
    }

    @Test
    public void detectIntoReusesBufferAcrossCalls() {
        EspeakLikeTokenDetector detector = new EspeakLikeTokenDetector();
        TokenBuffer buffer = new TokenBuffer();

        detector.detectInto("abc", buffer);
        assertEquals(1, buffer.size());
        assertEquals(TokenType.WORD_LATIN, buffer.getType(0));

        detector.detectInto("۱۲٫۳۴", buffer);
        assertEquals(1, buffer.size());
        assertEquals(TokenType.NUMBER, buffer.getType(0));

        List<DetectedToken> secondRunTokens = buffer.materialize("۱۲٫۳۴");
        assertEquals("۱۲٫۳۴", secondRunTokens.get(0).getText());
    }
}
