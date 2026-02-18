# Xiaomi/MIUI Text-to-Speech Issues

This document summarizes the Xiaomi-specific problems visible in the existing codebase so they can be investigated and validated on affected devices.

## Aggressive battery optimizations block TTS
- `SupportActivity` shows Xiaomi/Huawei-only warnings and prompts users to ignore battery optimizations when launching the support screen, highlighting that the app requires whitelisting to stay alive in the background.【F:app/src/main/java/com/khanenoor/parsavatts/SupportActivity.java†L45-L138】
- Inline comments explain that Xiaomi's power-saving layers can freeze background apps after prolonged use, forcing a reinstall. Users must enable **Do not restrict background activity** and **Auto Start** to keep the synthesizer available.【F:app/src/main/java/com/khanenoor/parsavatts/SupportActivity.java†L116-L139】

## MIUI disconnects the Google TTS service after closing recents
- In the English TTS engine, comments note that closing recent apps in MIUI 3930 triggers a disconnect from `com.google.android.tts`, causing language checks to fail and leaving the utterance progress listener stuck outside the idle state. The code tries to rebuild the `TextToSpeech` instance when this happens, suggesting instability specific to Xiaomi devices.【F:app/src/main/java/com/khanenoor/parsavatts/engine/EnTts.java†L167-L193】
- The dual TTS service observes the same MIUI behavior, warning that synthesis threads can deadlock because the English engine's listener remains in a non-idle state after MIUI shuts down the Google TTS service. This highlights a Xiaomi-specific deadlock risk during audio buffer draining.【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L580-L629】
- Users report that the saved English voice effectively “disappears” after clearing recents because MIUI kills the Google TTS process. Reopening the app forces a fresh engine rebuild, so enabling **Do not restrict background activity** and **Auto Start** is essential to keep TalkBack speech responsive.【F:app/src/main/java/com/khanenoor/parsavatts/engine/EnTts.java†L167-L193】【F:app/src/main/java/com/khanenoor/parsavatts/SupportActivity.java†L116-L139】

## Foreground fallback to survive recents clearing
- `DualTtsService` now returns `START_STICKY` and, on Xiaomi hardware, promotes itself to a low-priority foreground service with a minimal notification whenever `onStartCommand` or `onTaskRemoved` runs. This keeps the MBROLA-driven Persian synthesis path available to TalkBack even after the user clears recent apps.【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L270-L308】【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L312-L355】
- The same Xiaomi-only branch restarts the TTS service via `startForegroundService`/`startService` after `onTaskRemoved`, mirroring MIUI’s aggressive process eviction so TalkBack retains rapid speech output without manual relaunch.【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L312-L331】
- During teardown the service guards its shutdown path to avoid double-unregistering shared synthesis resources that TalkBack depends on, reducing the chances of null callbacks while MIUI juggles process state.【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L357-L374】

## Power save status detection gaps
- There is a Xiaomi/Huawei power-save detection helper (`isPowerSaveModeHuaweiXiaomi`) that looks for MIUI's `POWER_SAVE_MODE_OPEN`, but it is never invoked in `SupportActivity`. This means the app does not proactively surface Xiaomi-specific guidance unless users manually open the support screen, potentially leaving TTS killed by MIUI without warning.【F:app/src/main/java/com/khanenoor/parsavatts/SupportActivity.java†L131-L151】
