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
import com.khanenoor.parsavatts.impractical.OPT_PUNCT;
import com.khanenoor.parsavatts.util.LogUtils;

public class PunctuationsActivity extends Activity {
    private static final String TAG = PunctuationsActivity.class.getSimpleName();
    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Is the button now checked?
            boolean checked = ((RadioButton) view).isChecked();
            // Check which radio button was clicked
            if (view.getId() == R.id.radio_none) {
                if (checked) {
                    savePreferences(OPT_PUNCT.PUNCT_Never.ordinal());
                    finish();
                }
            } else if (view.getId() == R.id.radio_some) {

                if (checked) {
                    savePreferences(OPT_PUNCT.PUNCT_Default.ordinal());
                    finish();
                }
            } else if (view.getId() == R.id.radio_all) {
                if (checked) {
                    savePreferences(OPT_PUNCT.PUNCT_Always.ordinal());
                    finish();
                }
            }
        }
    };

    private void savePreferences(int ordinal) {
        Runnable r = () -> {
            prefs.setPunctuationMode(ordinal);
            int combineCode = Preferences.PUNCTUATION_MODE_CHANGE_ID;
            Intent intent = new Intent(Preferences.CUSTOM_PREFERENCES_CHANGE_BROADCAST);
            intent.putExtra(Preferences.INTENT_CHANGED_PARAM_KEY, Integer.toString(combineCode));
            intent.putExtra(Preferences.TTS_PUNCTUATION_MODE, ordinal);
            LogUtils.w(TAG, "send broadcast");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        };
        Thread thr = new Thread(r);
        thr.start();
    }

    private Preferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_punctuations);
        prefs = new Preferences(ExtendedApplication.getStorageContext());
        findViewById(R.id.radio_none).setOnClickListener(mClickListener);
        findViewById(R.id.radio_some).setOnClickListener(mClickListener);
        findViewById(R.id.radio_all).setOnClickListener(mClickListener);

        //Load Value
        int nMode = prefs.getPunctuationMode();
        if(nMode==OPT_PUNCT.PUNCT_Always.ordinal()){
            ((RadioButton) findViewById(R.id.radio_all)).setChecked(true);
        } else if(nMode==OPT_PUNCT.PUNCT_Default.ordinal()){
            ((RadioButton)findViewById(R.id.radio_some)).setChecked(true);
        }else if(nMode==OPT_PUNCT.PUNCT_Never.ordinal()) {
            ((RadioButton)findViewById(R.id.radio_none)).setChecked(true);
        }
    }
}
