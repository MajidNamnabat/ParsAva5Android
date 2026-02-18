package com.khanenoor.parsavatts.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.widget.Toast;

import com.khanenoor.parsavatts.PreferenceStorage;
import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.R;
import com.khanenoor.parsavatts.SeekBarPreference;
import com.khanenoor.parsavatts.BuildConfig;
import com.khanenoor.parsavatts.util.LogUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Preference screen for tuning the Persian TTS engine.
 *
 * <p>The activity exposes speech rate and pitch controls so TalkBack users can
 * quickly adapt output pacing for accessibility. Summary fields are refreshed
 * in real time to keep screen reader cues consistent with the underlying
 * stored values.</p>
 */
public class TtsSettingsActivity extends PreferenceActivity {
    private static final String TAG = TtsSettingsActivity.class.getSimpleName();
    private static final Preference.OnPreferenceChangeListener mOnPreferenceChanged =
            (preference, newValue) -> {
                if (newValue instanceof String) {
                    String summary = "";
                    if (preference instanceof ListPreference) {
                        final ListPreference listPreference = (ListPreference) preference;
                        final int index = listPreference.findIndexOfValue((String) newValue);
                        final CharSequence[] entries = listPreference.getEntries();

                        if (index >= 0 && index < entries.length) {
                            summary = entries[index].toString();
                        }
                    } else if (preference instanceof SeekBarPreference) {
                        final SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
                        String formatter = seekBarPreference.getFormatter();
                        summary = String.format(formatter, (String) newValue);
                        persistParsAvaSlider(preference.getContext(), preference.getKey(), (String) newValue);
                    } else {
                        summary = (String) newValue;
                    }
                    preference.setSummary(summary);
                }
                return true;
            };
    private static Context storageContext;
    /**
     * Listens for preference changes and updates the summary to reflect the
     * current setting. This shouldn't be necessary, since preferences are
     * supposed to automatically do this when the summary is set to "%s".
     */
    private final Preference.OnPreferenceChangeListener mPreferenceChangeListener =
            (preference, newValue) -> {
                if (preference instanceof ListPreference && newValue instanceof String) {
                    final ListPreference listPreference = (ListPreference) preference;
                    final int index = listPreference.findIndexOfValue((String) newValue);
                    final CharSequence[] entries = listPreference.getEntries();

                    if (index >= 0 && index < entries.length) {
                        preference.setSummary(entries[index].toString().replaceAll("%", "%%"));
                    } else {
                        preference.setSummary("");
                    }
                }

                return true;
            };

    private static int clampSliderValue(int value) {
        if (value < Preferences.PERSIAN_RATE_MIN) {
            return Preferences.PERSIAN_RATE_MIN;
        }
        if (value > Preferences.PERSIAN_RATE_MAX) {
            return Preferences.PERSIAN_RATE_MAX;
        }
        return value;
    }

    private static int parseSliderValue(String rawValue, int fallback) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Builds a {@link SeekBarPreference} for the provided key.
     *
     * <ul>
     *     <li>Initializes min/max to a 0-100 percentage scale to keep changes
     *     predictable for TalkBack users who rely on consistent slider steps.</li>
     *     <li>Restores the persisted value (or provided default) so spoken
     *     summaries align with stored engine parameters immediately.</li>
     *     <li>Attaches a common change listener that formats summaries without
     *     extra work per control.</li>
     * </ul>
     */
    private static Preference createSeekBarPreference(Context context, int parameter, String key, int titleRes) {
        final String title = context.getString(titleRes);

        final SeekBarPreference pref = new SeekBarPreference(context);
        pref.setTitle(title);
        pref.setDialogTitle(title);
        pref.setKey(key);
        pref.setOnPreferenceChangeListener(mOnPreferenceChanged);
        pref.setPersistent(true);
        pref.setFormatter(context.getString(R.string.setting_slider_value_description));


        //pref.setMin(parameter.getMinValue());
        //pref.setMax(parameter.getMaxValue());
        pref.setMin(0);
        pref.setMax(100);
        final int defaultProgress = clampSliderValue(parameter);
        pref.setDefaultValue(defaultProgress);
        LogUtils.w(TAG,"TtsSettingsActivity createSeekBarPreference called");
        final SharedPreferences prefs = PreferenceStorage.getDefaultSharedPreferences(storageContext);
        final String prefString = prefs.getString(key, null);
        final int persistedProgress = prefString == null
                ? clampSliderValue(parameter)
                : clampSliderValue(parseSliderValue(prefString, parameter));
        pref.setProgress(persistedProgress);
        final String summary = String.format(pref.getFormatter(), Integer.toString(pref.getProgress()));
        pref.setSummary(summary);

        return pref;
    }

    private static void persistParsAvaSlider(Context context, String key, String newValue) {
        if (context == null || key == null || newValue == null) {
            return;
        }
        final int value = clampSliderValue(parseSliderValue(newValue, Preferences.PERSIAN_RATE_MIN));

        final Preferences prefs = new Preferences(context);
        int combineCode = 0;
        final Intent intent = new Intent(Preferences.CUSTOM_PREFERENCES_CHANGE_BROADCAST);
        intent.putExtra(Preferences.INTENT_LANGUAGE_PARAM_KEY, context.getString(R.string.persian_language));

        if (Preferences.PERSIAN_ENGINE_RATE.equals(key)) {
            prefs.setPersianRate(value);
            intent.putExtra(Preferences.PERSIAN_ENGINE_RATE, value);
            combineCode |= Preferences.SPEED_CHANGE_ID;
        } else if (Preferences.PERSIAN_ENGINE_PITCH.equals(key)) {
            LogUtils.w(TAG,"setPitch in TtsSettingsSActivity " + Integer.toString(value));
            prefs.setPersianPitch(value);
            intent.putExtra(Preferences.PERSIAN_ENGINE_PITCH, value);
            combineCode |= Preferences.PITCH_CHANGE_ID;
        }

        if (combineCode == 0) {
            return;
        }

        intent.putExtra(Preferences.INTENT_CHANGED_PARAM_KEY, Integer.toString(combineCode));
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(intent);
        /*
        if (BuildConfig.DEBUG) {
            final String debugMessage = "Persisted Persian slider " + key + " => " + value;
            LogUtils.d(TAG, debugMessage);
            Toast.makeText(context.getApplicationContext(), debugMessage, Toast.LENGTH_SHORT).show();
        }
         */
    }

    /**
     * Adds ParsAva-specific preferences to the supplied group.
     *
     * <p>The helper reads persisted rate/pitch values from {@link Preferences}
     * so the TTS engine and on-screen controls remain in sync. Keeping both
     * values aligned avoids abrupt voice changes that could disorient blind
     * users when TalkBack announces setting labels.</p>
     */
    private static void createPreferences(Context context, PreferenceGroup group) {
        Preferences parsAvaPrefs = new Preferences(storageContext);
        int pitchValue = clampSliderValue(parsAvaPrefs.getPersianPitch());
        int rateValue = clampSliderValue(parsAvaPrefs.getPersianRate());
        group.addPreference(createSeekBarPreference(context, rateValue, Preferences.PERSIAN_ENGINE_RATE, R.string.setting_default_rate));
        group.addPreference(createSeekBarPreference(context, pitchValue, Preferences.PERSIAN_ENGINE_PITCH, R.string.setting_default_pitch));
    }

    /**
     * Initializes default speech parameters and inflates the preference UI.
     *
     * <p>Persisted pitch/rate values are pushed back into {@link Preferences}
     * before the UI loads so the engine immediately reflects the sliders. This
     * avoids situations where TalkBack reads a summary that does not match the
     * synthesized output during first launch.</p>
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storageContext = PreferenceStorage.resolveStorageContext(this);
        final SharedPreferences prefs = PreferenceStorage.getDefaultSharedPreferences(storageContext);
        //final SharedPreferences.Editor editor = prefs.edit();

        Preferences parsAvaPrefs = new Preferences(storageContext);
        final int originalPitch = parsAvaPrefs.getPersianPitch();
        final int pitchValue = clampSliderValue(originalPitch);
        if (pitchValue != originalPitch) {
            LogUtils.w(TAG,"setPitch onCreate" + pitchValue);
            parsAvaPrefs.setPersianPitch(pitchValue);
        }
        final int rateValue = clampSliderValue(parsAvaPrefs.getPersianRate());

        final SharedPreferences.Editor prefsEditor = prefs.edit();

        final String pitchDefaultRaw = prefs.getString(
                Preferences.PREF_DEFAULT_PITCH,
                getString(R.string.persian_voice_pitch_default));
        final int safePitchDefault = clampSliderValue(parseSliderValue(pitchDefaultRaw, pitchValue));
        prefsEditor.putString(Preferences.PREF_DEFAULT_PITCH, Integer.toString(safePitchDefault));

        final String rateDefaultRaw = prefs.getString(
                Preferences.PREF_DEFAULT_RATE,
                getString(R.string.persian_voice_rate_default));
        final int persistedRateDefault = clampSliderValue(parseSliderValue(rateDefaultRaw, rateValue));
        final int normalizedRateDefault = clampSliderValue(
                parseSliderValue(getString(R.string.persian_voice_rate_default), rateValue));
        final int safeRateDefault = Math.min(persistedRateDefault, normalizedRateDefault);
        prefsEditor.putString(Preferences.PREF_DEFAULT_RATE, Integer.toString(safeRateDefault));
        prefsEditor.apply();

        if (persistedRateDefault != safeRateDefault && rateValue == persistedRateDefault) {
            parsAvaPrefs.setPersianRate(safeRateDefault);
        }

        getFragmentManager().beginTransaction().replace(
                android.R.id.content,
                new PrefsParsAvaFragment()).commit();
    }

    /**
     * Since the "%s" summary is currently broken, this sets the preference
     * change listener for all {@link ListPreference} views to fill in the
     * summary with the current entry value.
     */
    private void fixListSummaries(PreferenceGroup group) {
        if (group == null) {
            return;
        }

        final int count = group.getPreferenceCount();

        for (int i = 0; i < count; i++) {
            final Preference preference = group.getPreference(i);

            if (preference instanceof PreferenceGroup) {
                // Depth-first traversal ensures nested categories inherit
                // readable summaries, which helps TalkBack users parse the
                // settings tree without missing updates.
                fixListSummaries((PreferenceGroup) preference);
            } else if (preference instanceof ListPreference) {
                preference.setOnPreferenceChangeListener(mPreferenceChangeListener);
            }
        }
    }

    public static class PrefsParsAvaFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            LogUtils.w(TAG,"TtsSettingsActivity PrefsParsAvaFragment class on Create");
            addPreferencesFromResource(R.xml.preferences);
            createPreferences(getActivity(), getPreferenceScreen());
        }
    }
}
