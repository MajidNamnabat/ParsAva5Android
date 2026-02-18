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
import com.khanenoor.parsavatts.util.LogUtils;


public class EmojiSettingsActivity extends Activity {
    private static final String TAG = EmojiSettingsActivity.class.getSimpleName();
    View.OnClickListener mClickListener = view -> {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        if (view.getId() == R.id.radio_emoji_active) {
            if (checked) {
                savePreferences(1);
                finish();
            }
        } else if (view.getId() == R.id.radio_emoji_inactive) {

            if (checked) {
                savePreferences(0);
                finish();
            }
        }
    };

    private void savePreferences(int active) {
        Runnable r = () -> {
            prefs.setEmojiMode(active);
            int combineCode = Preferences.EMOJI_MODE_CHANGE_ID;
            Intent intent = new Intent(Preferences.CUSTOM_PREFERENCES_CHANGE_BROADCAST);
            intent.putExtra(Preferences.INTENT_CHANGED_PARAM_KEY, Integer.toString(combineCode));
            intent.putExtra(Preferences.TTS_EMOJI_MODE, active);
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
        setContentView(R.layout.layout_emoji);
        prefs = new Preferences(ExtendedApplication.getStorageContext());
        findViewById(R.id.radio_emoji_active).setOnClickListener(mClickListener);
        findViewById(R.id.radio_emoji_inactive).setOnClickListener(mClickListener);

        //Load Value
        int nMode = prefs.getEmojiMode();
        if(nMode==1){
            ((RadioButton) findViewById(R.id.radio_emoji_active)).setChecked(true);
        } else{
            ((RadioButton)findViewById(R.id.radio_emoji_inactive)).setChecked(true);
        }
    }

}
