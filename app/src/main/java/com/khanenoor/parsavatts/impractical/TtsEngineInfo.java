package com.khanenoor.parsavatts.impractical;

public class TtsEngineInfo {
    /** Engine package name. */
    public String name;

    /** Localized label for the engine. */
    public String label;

    /** Icon for the engine. */
    public int icon;

    /** Whether this engine is a part of the system image. */
    public boolean system;

    /**
     * The priority the engine declares for the intent filter
     * {@code android.intent.action.TTS_SERVICE}.
     */
    public int priority;

    @Override
    public String toString() {
        return "TtsEngineInfo{name=" + name + "}";
    }
}
