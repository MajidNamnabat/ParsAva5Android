package com.khanenoor.parsavatts.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.khanenoor.parsavatts.ExtendedApplication;
import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.R;
import com.khanenoor.parsavatts.impractical.OPT_DIGITS_LANGUAGE;
import com.khanenoor.parsavatts.util.LogUtils;

public class NumberLanguageActivity extends Activity {
    private static final String TAG = NumberLanguageActivity.class.getSimpleName();
    private Preferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_number_language);
        prefs = new Preferences(ExtendedApplication.getStorageContext());

        findViewById(R.id.radio_digits_farsi).setOnClickListener(mDigitsLanguageClickListener);
        findViewById(R.id.radio_digits_english).setOnClickListener(mDigitsLanguageClickListener);
        findViewById(R.id.radio_digits_default).setOnClickListener(mDigitsLanguageClickListener);

        int digitLanguage = normalizeDigitsLanguage(prefs.getDigitsLanguage());
        if (digitLanguage == OPT_DIGITS_LANGUAGE.DIGITS_English.ordinal()) {
            ((RadioButton) findViewById(R.id.radio_digits_english)).setChecked(true);
        } else if (digitLanguage == OPT_DIGITS_LANGUAGE.DIGITS_Default.ordinal()) {
            ((RadioButton) findViewById(R.id.radio_digits_default)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.radio_digits_farsi)).setChecked(true);
        }
    }

    private final View.OnClickListener mDigitsLanguageClickListener = view -> {
        boolean checked = ((RadioButton) view).isChecked();

        if (view.getId() == R.id.radio_digits_farsi) {
            if (checked) {
                saveDigitsLanguage(OPT_DIGITS_LANGUAGE.DIGITS_Farsi.ordinal());
                finish();
            }
        } else if (view.getId() == R.id.radio_digits_english) {
            if (checked) {
                saveDigitsLanguage(OPT_DIGITS_LANGUAGE.DIGITS_English.ordinal());
                finish();
            }
        } else if (view.getId() == R.id.radio_digits_default) {
            if (checked) {
                saveDigitsLanguage(OPT_DIGITS_LANGUAGE.DIGITS_Default.ordinal());
                finish();
            }
        }
    };

    private void saveDigitsLanguage(int ordinal) {
        int engineOrdinal = convertOrdinalForBroadcast(ordinal);
        Runnable r = () -> {
            prefs.setDigitsLanguage(engineOrdinal);
            int combineCode = Preferences.DIGITS_LANGUAGE_CHANGE_ID;
            Intent intent = new Intent(Preferences.CUSTOM_PREFERENCES_CHANGE_BROADCAST);
            intent.putExtra(Preferences.INTENT_CHANGED_PARAM_KEY, Integer.toString(combineCode));
            intent.putExtra(Preferences.TTS_DIGITS_LANGUAGE, engineOrdinal);
            LogUtils.w(TAG, "send broadcast for digit language");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        };
        Thread thr = new Thread(r);
        thr.start();
    }

    static int normalizeDigitsLanguage(int storedOrdinal) {
        switch (storedOrdinal) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 0:
                return 2;
            default:
                return storedOrdinal;
        }
    }

    private int convertOrdinalForBroadcast(int ordinal) {
        switch (ordinal) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 0;
            default:
                return ordinal;
        }
    }
}
