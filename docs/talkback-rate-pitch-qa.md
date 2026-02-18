# TalkBack regression checks for Persian rate/pitch

These steps help confirm TalkBack stays responsive when Persian synthesis is routed through MBROLA and slider updates in **TtsSettingsActivity**. Fast feedback keeps blind users synchronized with screen changes.

## Manual verification

- Open **ParsAva** settings and launch **Persian voice rate** and **pitch** sliders under **TtsSettingsActivity**.
- With **TalkBack** enabled, set **rate** to a faster value (e.g., 80–90%) to keep MBROLA output aligned with rapid navigation cues.
- Lower **pitch** to a noticeably different level (e.g., 40–50%) so audio changes are easy to hear.
- Switch TalkBack language focus from **English** to **Persian** and back once to mirror real-world toggling. Ensure the Persian slider summaries stay unchanged during the switch.
- Trigger a Persian utterance (e.g., focus a Persian menu item). Confirm the synthesized audio is faster and reflects the lower pitch without reverting when you toggle languages again.

## Expected results

- Slider summaries read by TalkBack match the persisted percentages, and MBROLA playback reflects the new rate and pitch.
- Changing pitch after rate does **not** reset the earlier rate selection; both values remain active when returning to Persian output.
- Repeated English/Persian toggles keep the Persian synthesis aligned with the last slider values so TalkBack retains consistent pacing.
