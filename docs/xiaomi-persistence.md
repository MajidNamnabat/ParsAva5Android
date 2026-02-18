# Xiaomi/MIUI persistence notes

- DualTtsService now runs with `START_STICKY` and promotes itself to a foreground service on Xiaomi devices using the `parsava-tts-persistence` channel to reduce kill/restart cycles for TalkBack users.
- The service persists the preferred English engine and voice via `PreferenceStorage` before teardown and reloads them on startup so user choices survive MIUI task sweeps.
- Engine switches are logged and surfaced via toasts to make fallback paths visible during on-device testing; this helps confirm that MBROLA-backed Persian output stays aligned with fast TalkBack speech.
