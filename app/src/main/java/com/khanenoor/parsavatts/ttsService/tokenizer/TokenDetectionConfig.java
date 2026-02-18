package com.khanenoor.parsavatts.ttsService.tokenizer;

/**
 * Runtime knobs for token detection behavior.
 */
public final class TokenDetectionConfig {
    private final boolean includeWhitespaceTokens;

    private TokenDetectionConfig(boolean includeWhitespaceTokens) {
        this.includeWhitespaceTokens = includeWhitespaceTokens;
    }

    public boolean includeWhitespaceTokens() {
        return includeWhitespaceTokens;
    }

    public static TokenDetectionConfig defaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private boolean includeWhitespaceTokens = true;

        public Builder setIncludeWhitespaceTokens(boolean includeWhitespaceTokens) {
            this.includeWhitespaceTokens = includeWhitespaceTokens;
            return this;
        }

        public TokenDetectionConfig build() {
            return new TokenDetectionConfig(includeWhitespaceTokens);
        }
    }
}
