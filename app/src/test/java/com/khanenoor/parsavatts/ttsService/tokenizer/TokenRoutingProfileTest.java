package com.khanenoor.parsavatts.ttsService.tokenizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TokenRoutingProfileTest {

    @Test
    public void countsMixedScriptAndSpecialTokenTypes() {
        EspeakLikeTokenDetector detector = new EspeakLikeTokenDetector();
        TokenBuffer buffer = new TokenBuffer();

        detector.detectInto("سلام hello ۱۲۳؟ 😊", buffer);
        TokenRoutingProfile profile = TokenRoutingProfile.from(buffer);

        assertEquals(buffer.size(), profile.getTotalTokens());
        assertEquals(1, profile.getPersianWordTokens());
        assertEquals(1, profile.getLatinWordTokens());
        assertEquals(1, profile.getNumberTokens());
        assertEquals(1, profile.getPunctuationTokens());
        assertEquals(1, profile.getEmojiTokens());
        assertTrue(profile.hasPersianWords());
        assertTrue(profile.hasLatinWords());
        assertTrue(profile.isMixedScriptWords());
    }

    @Test
    public void reportsSingleScriptWhenOnlyPersianWordsArePresent() {
        EspeakLikeTokenDetector detector = new EspeakLikeTokenDetector();
        TokenBuffer buffer = new TokenBuffer();

        detector.detectInto("سلام دنیا", buffer);
        TokenRoutingProfile profile = TokenRoutingProfile.from(buffer);

        assertEquals(2, profile.getPersianWordTokens());
        assertEquals(0, profile.getLatinWordTokens());
        assertTrue(profile.hasPersianWords());
        assertFalse(profile.hasLatinWords());
        assertFalse(profile.isMixedScriptWords());
    }

    @Test
    public void emptyProfileIsZeroedAndStable() {
        TokenRoutingProfile first = TokenRoutingProfile.empty();
        TokenRoutingProfile second = TokenRoutingProfile.empty();

        assertTrue(first == second);
        assertEquals(0, first.getTotalTokens());
        assertEquals(0, first.getPersianWordTokens());
        assertEquals(0, first.getLatinWordTokens());
        assertEquals(0, first.getNumberTokens());
        assertEquals(0, first.getEmojiTokens());
        assertEquals(0, first.getPunctuationTokens());
        assertFalse(first.hasPersianWords());
        assertFalse(first.hasLatinWords());
        assertFalse(first.isMixedScriptWords());
    }
}
