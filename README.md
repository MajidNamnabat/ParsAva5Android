# ParsAva Android Text-to-Speech Engine

ParsAva is a bilingual text-to-speech (TTS) engine tailored for Android accessibility users—especially blind users relying on TalkBack. It provides high-speed, low-latency playback for rapidly switching content, supports long running sessions without memory slowdowns, and can fluently read mixed Persian and English text using two dedicated voices.

### Synthesis technology
- **MBROLA role in Persian synthesis** – MBROLA remains the trusted fallback path for Persian output and preserves natural articulation at very rapid speech speeds required by Google TalkBack users.
- **ONNX Persian path (`elnaz.onnx`)** – `FaTts` now preloads the ONNX model at `Load(...)`, synthesizes Persian text through `synth(...)`, converts model output to PCM16, and streams chunks to the existing queue callback contract.
- **TalkBack-first controls** – Persian rate and pitch sliders stay available so blind users can keep speech fast enough to stay aligned with focus changes.

## Project layout
- **app/src/main/java/com/khanenoor/parsavatts** – Core application package containing activities, application-level state, providers, and utilities.
- **app/src/main/java/com/khanenoor/parsavatts/engine** – Native-backed TTS engine bridge, voice data checks, and synthesis helpers.
- **app/src/main/java/com/khanenoor/parsavatts/ttsService** – TextToSpeechService implementation and voice metadata.
- **app/src/main/java/com/khanenoor/parsavatts/persianTts** – Legacy Persian-only service wrapper.
- **app/src/main/java/com/khanenoor/parsavatts/impractical** – Experimental or legacy support classes and data models.
- **app/src/main/java/com/khanenoor/parsavatts/notUsed** – Archived experiments kept for reference.
- **app/src/main/java/com/khanenoor/parsavatts/providers** – Content provider for exporting settings.
- **app/src/main/res** – Layouts, strings, and drawables (not listed exhaustively here).

## Class reference
### Application and utilities
- **ExtendedApplication** – MultiDex-aware `Application` that initializes user dictionary storage, tracks NLP handle references via `Preferences`, and exposes architecture detection helpers for loading the correct native libraries. Optimizes long-running sessions by resetting handles when counts drop to zero.
- **Preferences** – Wrapper around shared preferences with typed getters/setters used across the app for engine handles, feature flags, and user settings.
- **SeekBarPreference** – Custom preference widget that exposes a slider UI for numeric engine options.
- **Lock** – Simple synchronization helper to coordinate concurrent operations.
- **UserDictionaryFile** – Manages on-device user dictionary persistence, loading, and updates used by the synthesis engine.
- **SettingsProvider** – Exposes selected configuration values via a content provider for other apps or services.

### Activities
- **ParsAvaActivity** – Main configuration activity orchestrating permission prompts, licensing, voice downloads, and engine initialization for TalkBack integration.
- **TtsSettingsActivity** – Hosts overall TTS preferences (rate, pitch, language selection) surfaced to Android’s system TTS settings.
- **VoiceSettingsActivity** – Lets users pick between Persian and English voices and adjust related options.
- **NumbersActivity** – Controls how digits are spoken (e.g., grouped vs. per-digit) for fast numeric navigation.
- **PunctuationsActivity** – Configures punctuation reading behavior for screen-reader friendly output.
- **EmojiSettingsActivity** – Toggles emoji handling modes within synthesized speech.
- **UserDictWordListActivity** – Displays and edits entries in the user dictionary to fine-tune pronunciations.
- **UserDictNEWordActivity** – UI for adding or editing named-entity dictionary entries.
- **SaveToFileActivity** – Exports synthesized speech to audio files for offline reuse.
- **SupportActivity** – Shows support/contact information and guidance for users.
- **LicenseActivity** – Presents license details and activation information.
- **CrashHandler** – Activity invoked to display crash diagnostics and capture user feedback when errors occur.

### Engine bridge and synthesis helpers
- **ParsAva** – Minimal activity placeholder enabling packaging of the native TTS shared libraries inside the APK.
- **SpeechSynthesis** – Primary bridge to native synthesis, orchestrating audio buffers, callbacks, and threading to maintain low latency under heavy TalkBack use.
- **FaTts** – Persian synthesis facade that loads native libraries, applies punctuation/number/emoji options, and streams audio through callbacks.
- **EnTts** – English synthesis facade paralleling `FaTts` for the English voice path.
- **GetSampleText** – Supplies sample utterances for the Android TTS “listen to an example” flow.
- **CheckVoiceData** – Validates presence of required voice data packages when the engine is installed or configured.
- **DownloadVoiceData** – Handles downloading and unpacking of voice assets from remote sources.

### TTS service layer
- **DualTtsService** – Core `TextToSpeechService` implementation that selects Persian or English voices per utterance, streams synthesized audio, manages buffers, and coordinates stop/flush events for rapid content switching.
- **ParsAvaVoice** – Metadata holder describing available voices, locales, and quality/latency flags exposed to Android’s TTS framework.
- **TtsQueueCompletedReceiver** – Broadcast receiver that reacts when queued synthesis completes, allowing cleanup and state resets.
- **PreferencesChangeReceiver** – Broadcast receiver that responds to preference updates to refresh engine configuration promptly.

### Persian TTS legacy wrapper
- **persianTts.TtsService** – Older single-language service kept for compatibility; routes requests through the Persian synthesis pipeline.

### Experimental/legacy helpers (`impractical` and `notUsed` packages)
- **impractical** – Contains experimental networking models (`Customer`, `TtsEngineInfo`, `Language`), audio buffer observables, progress listeners, and configuration toggles used during earlier prototypes.
- **notUsed** – Houses deprecated fragments, service experiments, and Retrofit sequencing examples retained as references.
- **First2Fragment / Second2Fragment** – Placeholder UI fragments preserved from earlier multi-pane experiments.

## Performance and memory considerations
The engine is designed for blind users who navigate content rapidly with TalkBack, so latency and stability are critical. Key practices embodied in the codebase include:
- **Native library management** – Dynamic architecture detection and explicit load order to minimize startup overhead while ensuring the correct binaries are used.
- **MBROLA-tuned Persian synthesis** – MBROLA voice data is optimized for quick buffer generation so Persian output keeps pace with rapid TalkBack navigation.
- **Bounded audio queue for TalkBack** – Audio buffers now flow through a capacity-capped queue that drops the oldest packets when rapid TalkBack speech floods the pipeline, keeping both MBROLA and English workers responsive without spiking memory.
- **Reference counting** – Engine handle tracking in `ExtendedApplication` to release NLP resources when no longer needed, preventing leaks during long sessions.
- **Threaded synthesis** – Background executors in `FaTts`/`SpeechSynthesis` coupled with buffer queues in `DualTtsService` to keep audio streaming smooth under rapid queue changes.
- **Configurable parsing** – Switchable punctuation, emoji, and digit reading modes enable concise output tailored to high-speed navigation without extra processing.
- **Offline-ready assets** – Download and validation flows for voice data reduce repeated network fetches and help keep memory usage predictable.

### DualTts speedup checklist
- **Preload both Persian paths** – Warm up MBROLA fallback assets and `elnaz.onnx` (`FaTts.Load`) before the first TalkBack utterance to remove cold-start lag.
- **Reuse buffers aggressively** – Recycle PCM chunk buffers in `SpeechSynthesis`/`DualTtsService` to reduce allocations and GC pauses that can desync spoken feedback from the screen.
- **Prioritize synthesis threads** – Keep producer/drain threads at TalkBack-friendly priority (`URGENT_AUDIO` when appropriate) so rapid focus movement is spoken in sync.
- **Keep queue drain low-latency** – Emit PCM chunks immediately from ONNX/MBROLA output and avoid waiting for full utterances; this keeps TalkBack feedback responsive during fast flick navigation.
- **Cache hot metadata** – Cache ONNX input names and frequently used parsing options (punctuation/digits/emoji) to avoid repeated lookups in the hot path.

### Manual QA checklist: English rate and pitch propagation
- **Goal** – Keep the English path responsive without regressing the MBROLA-powered Persian pipeline that TalkBack users rely on for rapid speech. The checks below focus on `PreferencesChangeReceiver` applying English slider updates to `mEnTts.mConfiureParams`.
- **Setup**
  - Install the APK, open ParsAva in system TTS settings, and switch the language selector to **English** so the English sliders are active (they should not be greyed out).
  - Enable TalkBack or a screen reader so slider movements mirror real-world usage where latency is most noticeable for blind users.
- **Slider propagation**
  - Move the English **Rate** slider to a noticeable new value, then the **Pitch** slider. This should emit `Preferences.CUSTOM_PREFERENCES_CHANGE_BROADCAST` with `ENG_ENGINE_RATE`/`ENG_ENGINE_PITCH` extras that land in `PreferencesChangeReceiver` and update `mEnTts.mConfiureParams.mRate`/`mPitch`.
  - Confirm via `adb logcat` that the receiver logs the new English rate value (`PreferenceChangeReceiver EnglishLanguage value:`) and that no warnings about parsing errors appear.
- **Audible confirmation**
  - Use Android TTS settings’ **Listen to an example** while English is selected. The sample should immediately reflect the new speed and pitch, indicating the engine is honoring the updated parameters.
  - Flip back to Persian and trigger a sample to ensure the MBROLA-driven Persian playback still sounds rapid and unchanged, preserving TalkBack pacing.
- **Acceptance notes**
  - English rate and pitch sliders stay enabled when English is active.
  - Adjusting either slider triggers receiver updates that write into `mEnTts.mConfiureParams`.
  - Listening to the English sample after adjustments audibly confirms both rate and pitch shifts.
  - Returning to Persian leaves MBROLA responsiveness intact so fast TalkBack navigation is preserved.

## Usage
1. Install the APK and select ParsAva as the default TTS engine in Android settings.
2. Open **ParsAvaActivity** to download voices, accept licenses, and tune speed/pitch preferences.
3. Configure punctuation, digit, emoji, and voice options through the dedicated activities to match TalkBack usage.
4. Use **SaveToFileActivity** to export frequently used prompts to audio files for offline playback.

