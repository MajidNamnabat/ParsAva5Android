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
import com.khanenoor.parsavatts.impractical.OPT_DIGITS_READ;
import com.khanenoor.parsavatts.util.LogUtils;

public class NumbersActivity extends Activity {
    private static final String TAG = NumbersActivity.class.getSimpleName();
    private Preferences prefs = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_numbers);
        prefs = new Preferences(ExtendedApplication.getStorageContext());
        findViewById(R.id.radio_one_digit).setOnClickListener(mClickListener);
        findViewById(R.id.radio_pairs).setOnClickListener(mClickListener);
        findViewById(R.id.radio_triplets).setOnClickListener(mClickListener);
        findViewById(R.id.radio_continuous).setOnClickListener(mClickListener);

        //Load Values
        int nMode = prefs.getNumberMode();

        if(nMode== OPT_DIGITS_READ.DIGITS_Digits.ordinal()){
            ((RadioButton) findViewById(R.id.radio_one_digit)).setChecked(true);
        }else if(nMode== OPT_DIGITS_READ.DIGITS_Pairs.ordinal()){
            ((RadioButton) findViewById(R.id.radio_pairs)).setChecked(true);
        } else if(nMode==OPT_DIGITS_READ.DIGITS_Triplets.ordinal()){
            ((RadioButton)findViewById(R.id.radio_triplets)).setChecked(true);
        }else if(nMode==OPT_DIGITS_READ.DIGITS_Continuous.ordinal()) {
            ((RadioButton)findViewById(R.id.radio_continuous)).setChecked(true);
        }

    }
    View.OnClickListener mClickListener = view -> {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        if (view.getId() == R.id.radio_one_digit){
            if (checked) {
                savePreferences(OPT_DIGITS_READ.DIGITS_Digits.ordinal());
                finish();
            }
        }else if (view.getId() == R.id.radio_pairs) {
            if (checked) {
                savePreferences(OPT_DIGITS_READ.DIGITS_Pairs.ordinal());
                finish();
            }
        } else if (view.getId() == R.id.radio_triplets) {

            if (checked) {
                savePreferences(OPT_DIGITS_READ.DIGITS_Triplets.ordinal());
                finish();
            }
        } else if (view.getId() == R.id.radio_continuous) {
            if (checked) {
                savePreferences(OPT_DIGITS_READ.DIGITS_Continuous.ordinal());
                finish();
            }
        }
    };

    private void savePreferences(int ordinal) {
        Runnable r = () -> {
            prefs.setNumberMode(ordinal);
            int combineCode = Preferences.NUMBER_MODE_CHANGE_ID;
            Intent intent = new Intent(Preferences.CUSTOM_PREFERENCES_CHANGE_BROADCAST);
            intent.putExtra(Preferences.INTENT_CHANGED_PARAM_KEY, Integer.toString(combineCode));
            intent.putExtra(Preferences.TTS_NUMBER_MODE, ordinal);
            LogUtils.w(TAG, "send broadcast");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        };
        Thread thr = new Thread(r);
        thr.start();
    }

}
