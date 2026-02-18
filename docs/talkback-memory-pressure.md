# TalkBack long-run stability under memory pressure

This note summarizes why TalkBack playback can lose context after days of rapid speech and how to keep DualTts responsive for blind users who rely on MBROLA for Persian output.

## Why context drops occur
- DualTts pushes MBROLA and English audio into an unbounded `LinkedBlockingQueue` (`mBufferAudio`), so a busy device can accumulate buffers faster than the binder thread drains them. The backlog grows until memory pressure forces the pipeline idle or skips queued utterances. 【F:app/src/main/java/com/khanenoor/parsavatts/impractical/AudioBufferObservable.java†L8-L47】【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L923-L1085】
- The drain loop ties its timeout to utterance length instead of available memory, so it can keep waiting for late buffers even after RAM is already bloated. 【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L923-L1085】

## Why it shows up after days of TalkBack use
- Long sessions keep MBROLA loaded and continually enqueue Persian buffers while TalkBack stays in rapid-speech mode. GC pauses and binder slowdowns under memory pressure make `pollBufferAudio` lag behind `audioAvailable`, so the queue expands unchecked until the system drops audio. 【F:app/src/main/java/com/khanenoor/parsavatts/engine/SpeechSynthesis.java†L196-L215】【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L923-L1085】

## Mitigations to protect TalkBack users
- Replace `mBufferAudio` with a bounded queue or drop-oldest policy so MBROLA and English buffers cannot exhaust RAM during long sessions. 【F:app/src/main/java/com/khanenoor/parsavatts/impractical/AudioBufferObservable.java†L8-L47】
- Add a memory or queue-size watchdog around `pollBufferAudio` that forces `stopSynthesisThreadAndVoices` before OOM can truncate context. 【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L923-L1085】
- Reuse audio buffers and raise synthesis/drain thread priority when TalkBack is active so rapid speech stays in sync for blind readers. 【F:app/src/main/java/com/khanenoor/parsavatts/ttsService/DualTtsService.java†L923-L1085】
