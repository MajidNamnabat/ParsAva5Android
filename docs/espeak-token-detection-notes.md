# eSpeak-ng token detection notes (English, Persian, numbers, punctuation, emoji)

## Scope
- This note explains the eSpeak-ng text-front-end algorithm that performs token detection before phoneme generation.
- Focus: low-latency behavior relevant for TalkBack speech synchronization.

## Where detection happens in eSpeak-ng
- `src/libespeak-ng/readclause.c`
  - Splits input stream into clauses/sentences and classifies punctuation boundaries.
- `src/libespeak-ng/translate.c`
  - Main translation loop scans Unicode code points, segments words/tokens, and dispatches token types.
- `src/libespeak-ng/tr_languages.c`
  - Language-specific character/property tables that decide script/category handling per translator.
- `src/libespeak-ng/numbers.c`
  - Number token expansion (cardinal/ordinal/year/time-style paths).
- `src/libespeak-ng/dictionary.c`
  - Lexicon lookup and fallback rules for detected word tokens.

## Practical algorithm (tokenism level)
- **Normalize and classify**
  - Read Unicode code points.
  - Assign each code point to classes such as letter/script, digit, whitespace, punctuation, symbol/emoji.
- **Build token runs**
  - Consecutive compatible classes are grouped into one token:
    - letter/script runs for words,
    - digit runs (including separators) for numbers,
    - punctuation marks as pause/control tokens,
    - symbol/emoji as symbol tokens.
- **Script and language routing**
  - Mixed-script text is split by script boundaries (for example Latin vs Arabic-script segments).
  - Translator/language tables choose pronunciation path per run.
- **Dictionary + rules**
  - Word tokens first attempt dictionary lookup.
  - If not found, grapheme-to-phoneme rules apply.
- **Number expansion**
  - Numeric tokens are expanded to spoken forms before phoneme output.
- **Punctuation and pause timing**
  - Comma/full-stop/question-mark and similar marks inject prosodic boundaries and pauses.
- **Emoji/symbol handling**
  - Symbols/emoji are either named (if mapped) or spoken via fallback symbol strategy.

## Why this matters for accessibility performance
- Early token class detection reduces mispronunciation churn when TalkBack focus moves rapidly.
- Clause-level punctuation timing helps maintain navigational rhythm for blind users.
- Script-split routing avoids blending English and Persian pronunciation in one run.
- Immediate token-to-chunk streaming lowers perceived latency during focus navigation.

## Mapping this to ParsAva5Android (current project)
- Persian synthesis is **ONNX-only** (`elnaz.onnx`); MBROLA is removed from this project.
- Persian path should be explicit:
  - preload in `FaTts.Load(...)`,
  - synthesize in `FaTts.synth(...)`,
  - convert float output to PCM16,
  - stream through the existing callback queue immediately.
- NLP library status: NLP library has been removed from this project.
- DualTts low-latency checklist:
  - preload model/voice assets,
  - reuse synthesis/audio buffers,
  - raise synthesis thread priority,
  - cache hot-path metadata,
  - stream chunks immediately to keep TalkBack speech synchronized with focus movement.

## Investigation note
- Direct online source fetch from GitHub may fail in restricted environments.
- If you need exact function names/line numbers, run a local clone and inspect the files above with `rg`.

## Current Task-6 integration status
- `SpeechSynthesis.speak(...)` now runs `EspeakLikeTokenDetector.detectInto(...)` on sentence-like chunks using reusable buffers and logs compact routing profiles (`fa/en/num/emoji/punct/mixed`).
- This keeps hot-path allocations low (buffer reuse) and exposes mixed-script metadata directly in the synthesis hot path without introducing NLP dependencies.
- Persian synthesis path remains ONNX-only: preload `elnaz.onnx` in `FaTts.Load(...)`, synthesize via `FaTts.synth(...)`, convert to PCM16, then stream through the existing callback queue.
- NLP library remains removed from this project.


## Task-8 synthesis chunk sequencing
- Current service flow now splits text into sentence-like chunks and synthesizes each chunk sequentially.
- Chunk boundaries prioritize question marks, Persian full stop/newlines, and context-aware dots; exclamation marks are not treated as hard sentence boundaries to avoid premature splits.
- Dot boundaries skip numeric cases and Persian one-letter abbreviations (for example `2.5`, `د.`) to keep chunks natural for TalkBack rhythm.
- For each chunk, token groups are routed by script: Persian groups use `mFaTts.synth(...)` and English groups use `mEnTts.Speak(...)`.
- After each grouped synthesis step, generated PCM16 wave frames continue through the existing callback queue before moving to the next group/chunk.
- This mirrors eSpeak-style front-end clause/sentence segmentation behavior for lower latency and steadier TalkBack focus synchronization.
