package com.khanenoor.parsavatts.engine;

import static java.util.Objects.isNull;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.khanenoor.parsavatts.ExtendedApplication;
import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.R;
import com.khanenoor.parsavatts.impractical.TtsEngineInfo;
import com.khanenoor.parsavatts.util.LogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckVoiceData extends Activity {
    private static final String TAG = CheckVoiceData.class.getSimpleName();
    private static final int REQUEST_DOWNLOAD = 1;
    private final ExecutorService installerExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ProgressBar progressIndicator;
    private TextView loadingMessage;

    public static File getDataPath(Context context) {
        final String pck_name = context.getPackageName();
        return new File("/data/data/" + pck_name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_voice_data);
        progressIndicator = findViewById(R.id.progress_indicator);
        loadingMessage = findViewById(R.id.loading_message);
        checkForVoices(false);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_DOWNLOAD:
                checkForVoices(true);
        }
    }

    private void checkEnglishVoices() {
        Preferences prefs = new Preferences(ExtendedApplication.getStorageContext());
        String englishVoiceName = prefs.get(Preferences.ENG_ENGINE_NAME, "");
        LogUtils.w(TAG, "Checking English engine preference: " + englishVoiceName);

        if (englishVoiceName.equals("") || englishVoiceName.equals("com.khanenoor.parsavatts")) {
            List<TtsEngineInfo> engines = Preferences.getEngines(ExtendedApplication.getStorageContext());
            String selectedEngine = Preferences.selectAndStoreEnglishEngine(ExtendedApplication.getStorageContext(), engines);
            if (TextUtils.isEmpty(selectedEngine)) {
                LogUtils.w(TAG, "No English engine available to select");
            }
            //3950 Haj aqa say emoji default off
            prefs.setEmojiMode(0);
            //3950 check Settings default become active or not
            prefs.set(Preferences.PREF_DEFAULT_PITCH,Integer.toString(prefs.getPersianPitch()));
            prefs.set(Preferences.PREF_DEFAULT_RATE,Integer.toString(prefs.getPersianRate()));

        }
    }

    private void checkForVoices(boolean attemptedInstall) {
        LogUtils.w(TAG,"CheckVoiceData.checkForVoices");
        showLoading(true);
        installerExecutor.execute(() -> {
            final String pck_name = getApplicationContext().getPackageName();
            Exception installError = null;
            try {
                if (!FaTts.isInstalled(pck_name, CheckVoiceData.this)) {
                    AssetManager am = CheckVoiceData.this.getAssets();
                    FaTts.Install(am, pck_name);
                    checkEnglishVoices();
                }
            } catch (Exception e) {
                installError = e;
                LogUtils.e(TAG, "CheckVoiceData.installation failed", e);
            }
            Exception finalInstallError = installError;
            mainHandler.post(() -> handleVoiceCheckResult(finalInstallError));
        });


    }

    /**
     * Launches the voice data installer.
     */
    private void downloadVoiceData() {
        final Intent checkIntent = new Intent(this, DownloadVoiceData.class);

        startActivityForResult(checkIntent, REQUEST_DOWNLOAD);
    }

    private void handleVoiceCheckResult(Exception installError) {
        showLoading(false);
        ArrayList<String> availableLanguages = new ArrayList<String>();
        ArrayList<String> unavailableLanguages = new ArrayList<String>();
        if (installError != null) {
            Toast.makeText(this, R.string.voice_data_failed_message, Toast.LENGTH_LONG).show();
            returnResults(Engine.CHECK_VOICE_DATA_FAIL, availableLanguages, unavailableLanguages);
            return;
        }

        availableLanguages.add(Locale.ENGLISH.toString());
        availableLanguages.add("fas");
        try {
            final ArrayList<String> checkFor = getIntent().getStringArrayListExtra(
                    TextToSpeech.Engine.EXTRA_CHECK_VOICE_DATA_FOR);

            if (!isNull(checkFor) && !(checkFor != null && checkFor.isEmpty())) {
                final Set<String> checkForSet = new HashSet<String>(checkFor);
                availableLanguages = filter(availableLanguages, checkForSet);
                unavailableLanguages = filter(unavailableLanguages, checkForSet);
            }
            LogUtils.w(TAG,"CheckVoiceData.checkForVoices before checkEnglishVoices");

            checkEnglishVoices();
            returnResults(Engine.CHECK_VOICE_DATA_PASS, availableLanguages,
                    unavailableLanguages);
        }catch(Exception ex){
            LogUtils.w(TAG,"CheckVoiceData.Create " + ex.getMessage());
            Toast.makeText(this, R.string.error_message, Toast.LENGTH_LONG).show();
            returnResults(Engine.CHECK_VOICE_DATA_FAIL, availableLanguages, unavailableLanguages);
        }


    }

    private void returnResults(int result, ArrayList<String> availableLanguages,
                               ArrayList<String> unavailableLanguages) {
        final Intent returnData = new Intent();
        returnData.putStringArrayListExtra(Engine.EXTRA_AVAILABLE_VOICES, availableLanguages);
        returnData.putStringArrayListExtra(Engine.EXTRA_UNAVAILABLE_VOICES, unavailableLanguages);

        // Don't bother returning Engine.EXTRA_VOICE_DATA_FILES,
        // Engine.EXTRA_VOICE_DATA_FILES_INFO, or
        // Engine.EXTRA_VOICE_DATA_ROOT_DIRECTORY
        // because they're don't seem necessary.

        setResult(result, returnData);
        finish();
    }

    /**
     * Filters a given array list, maintaining only elements that are in the
     * constraint. Returns a new list containing only the filtered elements.
     */
    private ArrayList<String> filter(ArrayList<String> in, Set<String> constraint) {
        final ArrayList<String> out = new ArrayList<String>(constraint.size());

        for (String s : in) {
            if (constraint.contains(s)) {
                out.add(s);
            }
        }

        return out;
    }

    private void showLoading(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        if (progressIndicator != null) {
            progressIndicator.setVisibility(visibility);
        }
        if (loadingMessage != null) {
            loadingMessage.setVisibility(visibility);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        installerExecutor.shutdownNow();
    }

}
