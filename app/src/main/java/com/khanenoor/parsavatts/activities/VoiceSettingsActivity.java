package com.khanenoor.parsavatts.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.khanenoor.parsavatts.BuildConfig;
import com.khanenoor.parsavatts.ExtendedApplication;
import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.R;
import com.khanenoor.parsavatts.impractical.TtsEngineInfo;
import com.khanenoor.parsavatts.util.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoiceSettingsActivity extends Activity {
    final int REQUEST_CODE_ACTION_TTS_SETTINGS = 568;

    private static final String ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";

    private static final String TAG = ParsAvaActivity.class.getSimpleName();
    private static final boolean DEBUG_LOGGING = BuildConfig.DEBUG;
    private static final int RATE_STEP = 1;
    private static final int DEFAULT_SEEKBAR_STEP = 5;
    private Preferences prefs = null;
    private SeekBar mSpeedSeekBar = null;
    private SeekBar mVolumeSeekBar = null;
    private SeekBar mPitchSeekBar = null;
    private Spinner mSpinnerVoices = null;
    private Spinner mSpinnerLanguages = null;
    private Button mTestButton = null;

    private final AdapterView.OnItemSelectedListener mmSpinnerLanguagesItemListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // TODO Auto-generated method stub
            SpinnerAdapter adapter = mSpinnerLanguages.getAdapter();
            String value = (String) adapter.getItem(position);
            //Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
            switch (position) {
                case 0:
                    FillmSpinnerVoices(0);
                    LoadPreferences(0);
                    AdjustButtonProgressText(1|2|4);
                    mTestButton.setEnabled(false);
                    setSpeedAndPitchControlsEnabled(true);
                    break;
                case 1:
                    FillmSpinnerVoices(1);
                    LoadPreferences(1);
                    AdjustButtonProgressText(1|2|4);
                    setSpeedAndPitchControlsEnabled(true);
                    initializeEnglishSampleIfNeeded();
                    mTestButton.setEnabled(true);
                    break;
            }
            ////////////////// Edit Shared Preferences
            ////////////////////////////////////////////

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };
    private final AdapterView.OnItemSelectedListener mmSpinnerVoicesItemListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // TODO Auto-generated method stub
            SpinnerAdapter adapter = mSpinnerVoices.getAdapter();
            String value = (String) adapter.getItem(position);
            //Toast.makeText(getApplicationContext(), value, Toast.LENGTH_SHORT).show();
            // in UI thread Save Change
            SavePrefences(Preferences.VOICE_CHANGE_ID);
            initializeEnglishSampleIfNeeded();

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };
    // Get the progress value of the SeekBar
    // using setOnSeekBarChangeListener() method
    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        // When the progress value has changed
        @Override
        public void onProgressChanged(
                SeekBar seekBar,
                int progress,
                boolean fromUser) {
            // SeekBar.
            // increment 1 in progress and
            // increase the textsize
            // with the value of progress
            //message.setTextSize(progress + 1);
            //to check when
            LogUtils.w(TAG, "mSeekBarChangeListener.onProgressChanged");
            if (seekBar.getId() == R.id.speed_slider && fromUser) {
                SavePrefences(Preferences.SPEED_CHANGE_ID);
                AdjustButtonProgressText(1);
            } else if (seekBar.getId() == R.id.volume_slider && fromUser) {
                SavePrefences(Preferences.VOLUME_CHANGE_ID);
                AdjustButtonProgressText(2);
            } else if (seekBar.getId() == R.id.pitch_slider && fromUser) {
                SavePrefences(Preferences.PITCH_CHANGE_ID);
                AdjustButtonProgressText(4);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

            // This method will automatically
            // called when the user touches the SeekBar
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            // This method will automatically
            // called when the user
            // stops touching the SeekBar
        }
    };
    private TextToSpeech mEnglishSampleTts = null;
    private final HandlerThread mSampleHandlerThread = new HandlerThread("EnglishSampleThread");
    private Handler mSampleHandler;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean mSampleBusy = new AtomicBoolean(false);
    private volatile boolean mIsEnglishSampleInitialized = false;
    private volatile boolean mPlayAfterInit = false;
    private volatile int mPendingSamplePitch;
    private volatile int mPendingSampleRate;
    private volatile int mPendingSampleVolume;
    private String mInitializedEnglishVoice;
    public final TextToSpeech.OnInitListener mInitListener = status -> {
        if (status == TextToSpeech.SUCCESS) {
            mIsEnglishSampleInitialized = true;
            if (mEnglishSampleTts != null) {
                mEnglishSampleTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        mMainHandler.post(() -> mTestButton.setEnabled(false));
                    }
                    //We have when talkback is activate when test button press or english voice changed
                    @Override
                    public void onDone(String utteranceId) {
                        mMainHandler.post(() -> {
                            mTestButton.setEnabled(true);
                            mSampleBusy.set(false);
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        mMainHandler.post(() -> {
                            mTestButton.setEnabled(true);
                            mSampleBusy.set(false);
                        });
                    }

                    @Override
                    public void onStop(String utteranceId, boolean interrupted) {
                        mMainHandler.post(() -> {
                            mTestButton.setEnabled(true);
                            mSampleBusy.set(false);
                        });
                    }

                });
            }

            if (mPlayAfterInit) {
                mPlayAfterInit = false;
                if (mSampleHandler != null) {
                    final int pitchProgress = mPendingSamplePitch;
                    final int rateProgress = mPendingSampleRate;
                    final int volumeProgress = mPendingSampleVolume;
                    mSampleHandler.post(() -> playEnglishSample(pitchProgress, rateProgress, volumeProgress));
                }
            }

        } else {
            mPlayAfterInit = false;
            mMainHandler.post(() -> {
                Toast.makeText(getApplicationContext(), R.string.sample_not_ready, Toast.LENGTH_SHORT).show();
                mTestButton.setEnabled(true);
                mSampleBusy.set(false);
            });

        }
    };

    private void playEnglishSample(int pitchProgress, int rateProgress, int volumeProgress) {
        if (mEnglishSampleTts == null || !mIsEnglishSampleInitialized) {
            mMainHandler.post(() -> {
                Toast.makeText(getApplicationContext(), R.string.sample_not_ready, Toast.LENGTH_SHORT).show();
                mTestButton.setEnabled(true);
                mSampleBusy.set(false);
            });
            return;
        }

        mEnglishSampleTts.setLanguage(new Locale("eng", "USA", ""));
        String strEnglishSampleText = this.getResources().getString(R.string.english_simple_sample_text);
        float fPitch = pitchProgress/50.0F;
        //1 is default value
        mEnglishSampleTts.setPitch(fPitch);
        //float fRate = rateProgress/50.0F;
        final int nRateAdjust = 5*(rateProgress-50)+50;
        float fRate = (float) ((4.0F * nRateAdjust) / 100.0F);
        //1.0 is normal , 0.5 and 2 doubles
        mEnglishSampleTts.setSpeechRate(fRate);

        int speakResult;
        if (Build.VERSION.SDK_INT < 21) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("utteranceId", "ParsAva_Test_En");
            Float fVolume = volumeProgress/100.0F;
            hashMap.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, fVolume.toString()); // change the 0.0 to any value from 0-1 (1 is default)

            speakResult = mEnglishSampleTts.speak(strEnglishSampleText, TextToSpeech.QUEUE_FLUSH, hashMap);
        } else {
            Bundle params = new Bundle();
            Float fVolume = volumeProgress/100.0F;

            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, fVolume); // change the 0.5f to any value from 0f-1f (1f is default)
            speakResult = mEnglishSampleTts.speak(strEnglishSampleText, TextToSpeech.QUEUE_FLUSH, params, "ParsAva_Test_En");
        }

        if (speakResult == TextToSpeech.ERROR) {
            mMainHandler.post(() -> {
                Toast.makeText(getApplicationContext(), R.string.sample_not_ready, Toast.LENGTH_SHORT).show();
                mTestButton.setEnabled(true);
                mSampleBusy.set(false);
            });
        }
    }

    private void ensureSampleHandler() {
        if (!mSampleHandlerThread.isAlive()) {
            mSampleHandlerThread.start();
            mSampleHandler = new Handler(mSampleHandlerThread.getLooper());
        } else if (mSampleHandler == null) {
            mSampleHandler = new Handler(mSampleHandlerThread.getLooper());
        }
    }

    private void initializeEnglishSampleTts(String selectedVoice) {
        if (mEnglishSampleTts != null) {
            mEnglishSampleTts.shutdown();
        }
        mIsEnglishSampleInitialized = false;
        mEnglishSampleTts = new TextToSpeech(getApplicationContext(), mInitListener, selectedVoice);
        mInitializedEnglishVoice = selectedVoice;
    }
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int nCurrentProgress = 0;
            int nNextLevelProgress = 0;
            if (v.getId() == R.id.vnspeak_speed_decrease) {
                nCurrentProgress = mSpeedSeekBar.getProgress();
                nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, false, RATE_STEP);
                mSpeedSeekBar.setProgress(nNextLevelProgress);
                AdjustButtonProgressText(1);
                SavePrefences(Preferences.SPEED_CHANGE_ID);
            } else if (v.getId() == R.id.vnspeak_speed_increase) {
                nCurrentProgress = mSpeedSeekBar.getProgress();
                nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, true, RATE_STEP);
                mSpeedSeekBar.setProgress(nNextLevelProgress);
                SavePrefences(Preferences.SPEED_CHANGE_ID);
                AdjustButtonProgressText(1);
            } else if (v.getId() == R.id.vnspeak_volume_decrease) {
                nCurrentProgress = mVolumeSeekBar.getProgress();
                nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, false);
                mVolumeSeekBar.setProgress(nNextLevelProgress);
                SavePrefences(Preferences.VOLUME_CHANGE_ID);
                AdjustButtonProgressText(2);
            } else if (v.getId() == R.id.vnspeak_volume_increase) {
                nCurrentProgress = mVolumeSeekBar.getProgress();
                nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, true);
                mVolumeSeekBar.setProgress(nNextLevelProgress);
                SavePrefences(Preferences.VOLUME_CHANGE_ID);
                AdjustButtonProgressText(2);
            } else if (v.getId() == R.id.vnspeak_pitch_decrease) {
                nCurrentProgress = mPitchSeekBar.getProgress();
                nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, false);
                mPitchSeekBar.setProgress(nNextLevelProgress);
                SavePrefences(Preferences.PITCH_CHANGE_ID);
                AdjustButtonProgressText(4);
            } else if (v.getId() == R.id.vnspeak_pitch_increase) {
                nCurrentProgress = mPitchSeekBar.getProgress();
                nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, true);
                mPitchSeekBar.setProgress(nNextLevelProgress);
                SavePrefences(Preferences.PITCH_CHANGE_ID);
                AdjustButtonProgressText(4);
            } else if (v.getId() == R.id.vnspeak_reset) {
                int nDefaultPitch = 50, nDefaultSpeed = 50, nDefaultVolume = 100;
                String strDefaultPitch, strDefaultSpeed, strDefaultVolume;
                final String selectedLanguage = mSpinnerLanguages.getSelectedItem().toString();
                if (selectedLanguage.contains(getApplicationContext().getResources().getString(R.string.persian_language))) {
                    strDefaultPitch = getApplicationContext().getResources().getString(R.string.persian_voice_pitch_default);
                    strDefaultSpeed = getApplicationContext().getResources().getString(R.string.persian_voice_rate_default);
                    strDefaultVolume = getApplicationContext().getResources().getString(R.string.persian_voice_volume_default);
                } else {
                    strDefaultPitch = getApplicationContext().getResources().getString(R.string.english_voice_pitch_default);
                    strDefaultSpeed = getApplicationContext().getResources().getString(R.string.english_voice_rate_default);
                    strDefaultVolume = getApplicationContext().getResources().getString(R.string.english_voice_volume_default);
                }
                try {
                    nDefaultPitch = Integer.parseInt(strDefaultPitch);
                    nDefaultSpeed = Integer.parseInt(strDefaultSpeed);
                    nDefaultVolume = Integer.parseInt(strDefaultVolume);
                } catch (NumberFormatException nfe) {
                    LogUtils.w(TAG, "Could not parse " + nfe.getMessage());
                }
                mPitchSeekBar.setProgress(nDefaultPitch);
                mVolumeSeekBar.setProgress(nDefaultVolume);
                mSpeedSeekBar.setProgress(nDefaultSpeed);
                SavePrefences(Preferences.PITCH_CHANGE_ID | Preferences.VOLUME_CHANGE_ID | Preferences.SPEED_CHANGE_ID);
                AdjustButtonProgressText(1|2|4);

            } else if (v.getId() == R.id.testbutton) {
                if (!mSampleBusy.compareAndSet(false, true)) {
                    return;
                }

                mTestButton.setEnabled(false);
                ensureSampleHandler();
                final String selectedLanguage = mSpinnerLanguages.getSelectedItem().toString();
                final String selectedVoice = mSpinnerVoices.getSelectedItem().toString();
                final int pitchProgress = mPitchSeekBar.getProgress();
                final int rateProgress = mSpeedSeekBar.getProgress();
                final int volumeProgress = mVolumeSeekBar.getProgress();
                SavePrefences(Preferences.PITCH_CHANGE_ID | Preferences.SPEED_CHANGE_ID | Preferences.VOLUME_CHANGE_ID);
                // save on Disk in a Thread
                if (selectedLanguage.contains(getApplicationContext().getResources().getString(R.string.persian_language))) {
                    Toast.makeText(getApplicationContext(), R.string.sample_not_ready, Toast.LENGTH_SHORT).show();
                    mSampleBusy.set(false);
                    mTestButton.setEnabled(true);
                    return;
                }

                mSampleHandler.post(() -> {
                    mPendingSamplePitch = pitchProgress;
                    mPendingSampleRate = rateProgress;
                    mPendingSampleVolume = volumeProgress;
                    if (mEnglishSampleTts == null || !mIsEnglishSampleInitialized) {
                        mPlayAfterInit = true;
                        initializeEnglishSampleTts(selectedVoice);
                        mMainHandler.post(() -> Toast.makeText(getApplicationContext(), R.string.sample_not_ready, Toast.LENGTH_SHORT).show());
                        return;
                    }
                    playEnglishSample(pitchProgress, rateProgress, volumeProgress);
                });
            } else if (v.getId() == R.id.btnShowTtsSettings) {
                LogUtils.w(TAG, "R.id.btnShowTtsSettings ");
                /*
                Intent voiceSettingsIntent = new Intent(VoiceSettingsActivity.this, VoiceSettingsActivity.class);
                voiceSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(voiceSettingsIntent);

                 */
                launchGeneralTtsSettings();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mEnglishSampleTts!=null){
            mEnglishSampleTts.shutdown();
            mEnglishSampleTts=null;
        }
        mInitializedEnglishVoice = null;
        if (mSampleHandlerThread.isAlive()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mSampleHandlerThread.quitSafely();
            }
        }
        mSampleBusy.set(false);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_voices);
        ensureSampleHandler();
        prefs = new Preferences(ExtendedApplication.getStorageContext());
        ///////////////////// Fill Language Spinner
        mSpinnerLanguages = findViewById(R.id.vnspeak_languages);

        List<String> languageArray = new ArrayList<>();
        // add languages
        languageArray.add(getApplicationContext().getResources().getString(R.string.persian_language));
        languageArray.add(getApplicationContext().getResources().getString(R.string.english_language));

        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, languageArray);

        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinnerLanguages.setAdapter(languageAdapter);
        mSpinnerLanguages.setOnItemSelectedListener(mmSpinnerLanguagesItemListener);
        ///////////////////////////////////////////////Spinner Voices
        mSpinnerVoices = findViewById(R.id.vnspeak_voices);
        mSpinnerVoices.setOnItemSelectedListener(mmSpinnerVoicesItemListener);
        //////////////////////////////////////////////////
        /*
         */
        // Set Selection Item 0
        mSpinnerLanguages.setSelection(0);

        ////////// Set OnClick Listener
        findViewById(R.id.vnspeak_speed_decrease).setOnClickListener(mOnClickListener);
        findViewById(R.id.vnspeak_speed_increase).setOnClickListener(mOnClickListener);
        findViewById(R.id.vnspeak_volume_decrease).setOnClickListener(mOnClickListener);
        findViewById(R.id.vnspeak_volume_increase).setOnClickListener(mOnClickListener);
        findViewById(R.id.vnspeak_pitch_decrease).setOnClickListener(mOnClickListener);
        findViewById(R.id.vnspeak_pitch_increase).setOnClickListener(mOnClickListener);

        findViewById(R.id.vnspeak_reset).setOnClickListener(mOnClickListener);
        mTestButton = ((Button)findViewById(R.id.testbutton));
        mTestButton.setEnabled(false);
        mTestButton.setOnClickListener(mOnClickListener);
        Button ttsSettingsButton = findViewById(R.id.btnShowTtsSettings);
        if (ttsSettingsButton != null) {
            ttsSettingsButton.setOnClickListener(mOnClickListener);
        }
        ///////// Set Spinner Listener
        mSpeedSeekBar = (SeekBar) findViewById(R.id.speed_slider);
        mVolumeSeekBar = (SeekBar) findViewById(R.id.volume_slider);
        mPitchSeekBar = (SeekBar) findViewById(R.id.pitch_slider);
        mSpeedSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mVolumeSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mPitchSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        // Load Prefereneces
        LoadPreferences(0);
        ///////////////// Previous 3770 : its title is + - , now blind people prefer number
        AdjustButtonProgressText(1|2|4);
    }

    private void setSpeedAndPitchControlsEnabled(boolean enabled) {
        if (mSpeedSeekBar != null) {
            mSpeedSeekBar.setEnabled(enabled);
        }
        if (mPitchSeekBar != null) {
            mPitchSeekBar.setEnabled(enabled);
        }
        View speedDecrease = findViewById(R.id.vnspeak_speed_decrease);
        if (speedDecrease != null) {
            speedDecrease.setEnabled(enabled);
        }
        View speedIncrease = findViewById(R.id.vnspeak_speed_increase);
        if (speedIncrease != null) {
            speedIncrease.setEnabled(enabled);
        }
        View pitchDecrease = findViewById(R.id.vnspeak_pitch_decrease);
        if (pitchDecrease != null) {
            pitchDecrease.setEnabled(enabled);
        }
        View pitchIncrease = findViewById(R.id.vnspeak_pitch_increase);
        if (pitchIncrease != null) {
            pitchIncrease.setEnabled(enabled);
        }
    }

    private void initializeEnglishSampleIfNeeded() {
        if (mSpinnerLanguages == null || mSpinnerVoices == null) {
            return;
        }
        Object selectedLanguageObj = mSpinnerLanguages.getSelectedItem();
        if (selectedLanguageObj == null) {
            return;
        }
        String selectedLanguage = selectedLanguageObj.toString();
        if (!selectedLanguage.contains(getApplicationContext().getResources().getString(R.string.english_language))) {
            return;
        }
        Object selectedVoiceObj = mSpinnerVoices.getSelectedItem();
        if (selectedVoiceObj == null) {
            return;
        }
        String selectedVoice = selectedVoiceObj.toString();
        if (selectedVoice.equals(mInitializedEnglishVoice) && mEnglishSampleTts != null && mIsEnglishSampleInitialized) {
            return;
        }
        ensureSampleHandler();
        initializeEnglishSampleTts(selectedVoice);
    }
    //1 Speed , 2 Volume , 3 Pitch
    private void AdjustButtonProgressText(int nProgressType){
        int nCurrentProgress,nNextLevelProgress;
        String title;
        if((nProgressType & 1)==1) {
            nCurrentProgress = mSpeedSeekBar.getProgress();
            nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, false, RATE_STEP);
            //title = nCurrentProgress + "-";
            //((Button) findViewById(R.id.vnspeak_speed_decrease)).setText(title);

            title = this.getResources().getString(R.string.vnspeak_speed_decrease) + " " +
                    this.getResources().getString(R.string.vnspeak_concatenator_be) + " " +
                    nNextLevelProgress;
            ((Button) findViewById(R.id.vnspeak_speed_decrease)).setContentDescription(title);
            nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, true, RATE_STEP);
            //title = nCurrentProgress + "+";
            //((Button) findViewById(R.id.vnspeak_speed_increase)).setText(title);
            title = this.getResources().getString(R.string.vnspeak_speed_increase) + " " +
                    this.getResources().getString(R.string.vnspeak_concatenator_be) + " " +
                    nNextLevelProgress;
            ((Button) findViewById(R.id.vnspeak_speed_increase)).setContentDescription(title);

        }
        /////////////
        if((nProgressType & 2)==2) {
            nCurrentProgress = mVolumeSeekBar.getProgress();
            nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, false);
            //title = nCurrentProgress + "-";
            //((Button) findViewById(R.id.vnspeak_volume_decrease)).setText(title);
            title = this.getResources().getString(R.string.vnspeak_volume_decrease) + " " +
                    this.getResources().getString(R.string.vnspeak_concatenator_be) + " " +
                    nNextLevelProgress;
            ((Button) findViewById(R.id.vnspeak_volume_decrease)).setContentDescription(title);

            nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, true);
            //title = nCurrentProgress + "+";
            //((Button) findViewById(R.id.vnspeak_volume_increase)).setText(title);
            title = this.getResources().getString(R.string.vnspeak_volume_increase) + " " +
                    this.getResources().getString(R.string.vnspeak_concatenator_be) + " " +
                    nNextLevelProgress;
            ((Button) findViewById(R.id.vnspeak_volume_increase)).setContentDescription(title);

        }
        ////////////
        if((nProgressType & 4)==4) {
            nCurrentProgress = mPitchSeekBar.getProgress();
            nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, false);
            //title = nCurrentProgress + "-";
            //((Button) findViewById(R.id.vnspeak_pitch_decrease)).setText(title);

            title = this.getResources().getString(R.string.vnspeak_pitch_decrease) + " " +
                    this.getResources().getString(R.string.vnspeak_concatenator_be) + " " +
                    nNextLevelProgress;
            ((Button) findViewById(R.id.vnspeak_pitch_decrease)).setContentDescription(title);

            nNextLevelProgress = getNextLevelSeekBar(nCurrentProgress, 0, 100, true);
            //title = nCurrentProgress + "+";
            //((Button) findViewById(R.id.vnspeak_pitch_increase)).setText(title);
            title = this.getResources().getString(R.string.vnspeak_pitch_increase) + " " +
                    this.getResources().getString(R.string.vnspeak_concatenator_be) + " " +
                    nNextLevelProgress;
            ((Button) findViewById(R.id.vnspeak_pitch_increase)).setContentDescription(title);
        }
    }
    private void FillmSpinnerVoices(int position) {

        List<String> voicesArray = new ArrayList<>();
        switch (position) {
            case 0:
                ///////////////////// Fill Voices Spinner from Persian

                voicesArray.add(this.getResources().getString(R.string.sina_voice));
                voicesArray.add(getApplicationContext().getResources().getString(R.string.mina_voice));


                //////////////////////////////////////////////////

                break;
            case 1:

                List<TtsEngineInfo> engines = Preferences.getEngines(this);
                //TextView textView = new TextView(ParsAvaActivity.this);
                //textView.setText(R.string.listview_engines_header);

                //listViewEngine.addHeaderView(textView);
                final String pck_name = getApplicationContext().getPackageName();

                for (TtsEngineInfo engine : engines) {
                    //This tts service not show
                    if (engine.name.contains(pck_name))
                        continue;
                    voicesArray.add(engine.name);
                }

                break;
        }
        ArrayAdapter<String> voiceAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, voicesArray);

        voiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinnerVoices.setAdapter(voiceAdapter);

    }

    @Override
    protected void onPause() {

        super.onPause();
        LogUtils.w(TAG, "onPause is called");
        //SavePrefences();
    }

    //1 , 2 , 4 , 8 , 16
    private void SavePrefences(Integer combineCode) {
        // in UI thread
        final String selectedLanguage = mSpinnerLanguages.getSelectedItem().toString();
        final String selectedVoice = mSpinnerVoices.getSelectedItem().toString();
        // save on Disk in a Thread
        Runnable r = () -> {
            String strValue = "";
            Integer nValue = 0;
            Intent intent = new Intent(Preferences.CUSTOM_PREFERENCES_CHANGE_BROADCAST);
            intent.putExtra(Preferences.INTENT_LANGUAGE_PARAM_KEY, selectedLanguage);
            intent.putExtra(Preferences.INTENT_CHANGED_PARAM_KEY, combineCode.toString());
            if (selectedLanguage.contains(this.getResources().getString(R.string.persian_language))) {
                if ((combineCode & Preferences.VOICE_CHANGE_ID) == Preferences.VOICE_CHANGE_ID) {
                    if (selectedVoice.contains(this.getResources().getString(R.string.sina_voice))) {
                        nValue = 0;
                        prefs.setPersianVoiceId(0);
                        intent.putExtra(Preferences.PERSIAN_ENGINE_ID, "0");
                    } else {
                        nValue = 1;
                        prefs.setPersianVoiceId(1);
                        intent.putExtra(Preferences.PERSIAN_ENGINE_ID, "1");
                    }
                }
                if ((combineCode & Preferences.PITCH_CHANGE_ID) == Preferences.PITCH_CHANGE_ID) {
                    nValue = mPitchSeekBar.getProgress();
                    prefs.setPersianPitch(nValue);
                    intent.putExtra(Preferences.PERSIAN_ENGINE_PITCH, nValue);
                    debugNotifyPersianUpdate("pitch", nValue);

                }
                if ((combineCode & Preferences.SPEED_CHANGE_ID) == Preferences.SPEED_CHANGE_ID) {
                    nValue = mSpeedSeekBar.getProgress();
                    prefs.setPersianRate(mSpeedSeekBar.getProgress());
                    intent.putExtra(Preferences.PERSIAN_ENGINE_RATE, nValue);
                    LogUtils.w(TAG,"SavePrefences Persian SpeedRate:" + nValue);
                    debugNotifyPersianUpdate("rate", nValue);
                }
                if ((combineCode & Preferences.VOLUME_CHANGE_ID) == Preferences.VOLUME_CHANGE_ID) {
                    nValue = mVolumeSeekBar.getProgress();
                    prefs.setPersianVolume(nValue);
                    intent.putExtra(Preferences.PERSIAN_ENGINE_VOLUME, nValue);

                }
            } else {
                if ((combineCode & Preferences.VOICE_CHANGE_ID) == Preferences.VOICE_CHANGE_ID) {
                    prefs.setEnglishVoiceName(selectedVoice);
                    intent.putExtra(Preferences.ENG_ENGINE_NAME, selectedVoice);

                }
                if ((combineCode & Preferences.PITCH_CHANGE_ID) == Preferences.PITCH_CHANGE_ID) {
                    nValue = mPitchSeekBar.getProgress();
                    prefs.setEnglishPitch(nValue);
                    intent.putExtra(Preferences.ENG_ENGINE_PITCH, nValue);
                    debugNotifyEnglishUpdate("pitch", nValue);

                }
                if ((combineCode & Preferences.SPEED_CHANGE_ID) == Preferences.SPEED_CHANGE_ID) {
                    nValue = mSpeedSeekBar.getProgress();
                    nValue = 5*(nValue-50)+50;
                    prefs.setEnglishRate(nValue);
                    intent.putExtra(Preferences.ENG_ENGINE_RATE, nValue);
                    LogUtils.w(TAG,"SavePrefences English SpeedRate:" + nValue);
                    debugNotifyEnglishUpdate("rate", nValue);

                }
                if ((combineCode & Preferences.VOLUME_CHANGE_ID) == Preferences.VOLUME_CHANGE_ID) {
                    nValue = mVolumeSeekBar.getProgress();
                    prefs.setEnglishVolume(nValue);
                    intent.putExtra(Preferences.ENG_ENGINE_VOLUME, nValue);

                }
            }
            LogUtils.w(TAG, "send broadcast");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        };
        Thread thr = new Thread(r);
        thr.start();
        //r.run();
    }

    private void debugNotifyPersianUpdate(String field, int value) {
        if (!DEBUG_LOGGING) {
            return;
        }

        final String message = "Persian " + field + " => " + value;
        LogUtils.d(TAG, message);
        //mMainHandler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void debugNotifyEnglishUpdate(String field, int value) {
        if (!DEBUG_LOGGING) {
            return;
        }
        final String message = "English " + field + " => " + value;
        LogUtils.d(TAG, message);
        //mMainHandler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void LoadPreferences(int nLanguagePos ) {
        int nPitch, nRate, nVolume;

        ///////////////// Load Preferences
        //Persian Language
        if (nLanguagePos == 0) {
            int nPersianVoiceId = prefs.getPersianVoiceId();
            nPitch = prefs.getPersianPitch();
            nRate = prefs.getPersianRate();
            nVolume = prefs.getPersianVolume();

            mSpinnerVoices.setSelection(nPersianVoiceId);

        } else {
            nPitch = prefs.getEnglishPitch();
            nRate = prefs.getEnglishRate();
            nVolume = prefs.getEnglishVolume();
            String englishEngine = prefs.getEnglishVoiceName();
            SpinnerAdapter adapter = mSpinnerVoices.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (mSpinnerVoices.getItemAtPosition(i).equals(englishEngine)) {
                    mSpinnerVoices.setSelection(i);
                    break;
                }
            }
            //adapter.
            //int spinnerPosition = mSpinnerVoices.getAdapter().getPosition(englishEngine);
            //mSpinnerVoices.setSelection(spinnerPosition);
        }
        mSpeedSeekBar.setProgress(nRate);
        mVolumeSeekBar.setProgress(nVolume);
        mPitchSeekBar.setProgress(nPitch);

    }

    private int getNextLevelSeekBar(int nCurrentValue, int nMin, int nMax, boolean bIncrease, int step) {
        int obtained_progress;
        int adjustedStep = Math.max(1, step);
        if (bIncrease && nCurrentValue + adjustedStep >= nMax) {
            obtained_progress = nMax;
        } else if (!bIncrease && nCurrentValue - adjustedStep <= nMin) {
            obtained_progress = nMin;
        } else if (bIncrease) {
            obtained_progress = nCurrentValue + adjustedStep;
        } else {
            obtained_progress = nCurrentValue - adjustedStep;
        }
        return obtained_progress;
    }

    private int getNextLevelSeekBar(int nCurrentValue, int nMin, int nMax, boolean bIncrease) {
        return getNextLevelSeekBar(nCurrentValue, nMin, nMax, bIncrease, DEFAULT_SEEKBAR_STEP);
    }
    private void launchGeneralTtsSettings()
    {
        Intent intent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            // The Text-to-Speech settings is a Fragment on 3.x:
            intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, "com.android.settings.TextToSpeechSettings");
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, intent.getExtras());
        }
        else
        {
            // The Text-to-Speech settings is an Activity on 2.x and 4.x:
            intent = new Intent(ACTION_TTS_SETTINGS);
        }
        startActivityForResult(intent, REQUEST_CODE_ACTION_TTS_SETTINGS);
    }

}
