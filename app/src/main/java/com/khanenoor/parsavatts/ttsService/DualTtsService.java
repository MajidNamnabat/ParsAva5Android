package com.khanenoor.parsavatts.ttsService;

import static android.speech.tts.Voice.QUALITY_NORMAL;
import static java.lang.Thread.currentThread;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.Voice;
import android.text.TextUtils;
import android.util.Pair;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.khanenoor.parsavatts.ExtendedApplication;
import com.khanenoor.parsavatts.PreferenceStorage;
import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.R;
import com.khanenoor.parsavatts.engine.FaTts;
import com.khanenoor.parsavatts.engine.SpeechSynthesis;
import com.khanenoor.parsavatts.impractical.ParsAvaUtteranceProgressListener;
import com.khanenoor.parsavatts.impractical.enmSpeakProgressStates;
import com.khanenoor.parsavatts.util.LogUtils;

import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A {@link TextToSpeechService} implementation that multiplexes between two internal engines
 * (Persian and English) while streaming synthesized audio to Android's TTS callbacks.
 *
 * <p>This service keeps minimal state and documents its control flow so future maintainers can
 * reason about the interaction between synthesis threads, buffer queues and Android lifecycle
 * callbacks. The high-level responsibilities include:
 *
 * <ul>
 *     <li>Managing {@link SpeechSynthesis} as the shared bridge to the native synthesis engines.</li>
 *     <li>Switching active voices based on the request locale while keeping {@link SynthesisCallback}
 *     buffers fed from {@link FaTts.SynthReadyCallback}.</li>
 *     <li>Synchronizing stop/flush events so stale worker threads cannot leak audio or block buffer
 *     clearing.</li>
 *     <li>Providing lightweight diagnostics through structured logging.</li>
 * </ul>
 */
public class DualTtsService extends TextToSpeechService {
    private static final String TAG = DualTtsService.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final String MANUFACTURER_XIAOMI = "xiaomi";
    private static final String FOREGROUND_CHANNEL_ID = "parsava-tts-persistence";
    private static final int FOREGROUND_NOTIFICATION_ID = 1001;
    private static Context mStorageContext;
    //ISO 639-3
    // fa  fas macrolanguage, also known as Farsi
    // https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
    private static final String DEFAULT_LANGUAGE_1 = "eng";
    // ISO 3166-1 alpha-3 codes : IR IRN Numeric:364
    // is eye-free was "uk" but I guess must be 3 characters
    private static final String DEFAULT_COUNTRY_1 = "USA";
    private static final String DEFAULT_VARIANT_1 = "";
    /////////////////English
    // en eng
    private static final String DEFAULT_LANGUAGE_2 = "fas";
    //United Kingdom of Great Britain and Northern Ireland (the) 	GB 	GBR 	826
    //United States of America (the) 	US 	USA 	840
    //https://www.iban.com/country-codes
    private static final String DEFAULT_COUNTRY_2 = "IRN";
    private static final String DEFAULT_VARIANT_2 = "";
    private SpeechSynthesis mEngine;
    private final Map<String, ParsAvaVoice> mAvailableVoices = new HashMap<String, ParsAvaVoice>();
    protected ParsAvaVoice mMatchingVoice = null;
    /**
     * Pipes synthesizer output from native eSpeak to an {link AudioTrack}.
     */
    private final FaTts.SynthReadyCallback mSynthCallback = new FaTts.SynthReadyCallback() {
        @Override
        public int onSynthDataReady(byte[] audioData, int audioData_size) {
            final Thread synthesisThread = mCurrentSynthesisThread;
            if (synthesisThread != null && synthesisThread != Thread.currentThread()) {
                LogUtils.i(TAG, "Ignoring audio from stale synthesis thread");
                return 1;
            }
            if (mIsStopCalled) {
                LogUtils.i(TAG, "onSynthDataReady ignored because stop was requested");
                return 1;
            }
            if ((audioData == null) || (audioData.length == 0)) {
                onSynthDataComplete();
                return 1;
            }
            /*
            final int maxBytesToCopy = mCallback.getMaxBufferSize();

            int offset = 0;

            while (offset < audioData.length) {
                //LogUtils.w(TAG, "onSynthDataReady Len:" + audioData.length);
                final int bytesToWrite = Math.min(maxBytesToCopy, (audioData.length - offset));
                mCallback.audioAvailable(audioData, offset, bytesToWrite);
                offset += bytesToWrite;
            }

             */
            try {
                mEngine.mAudioBufferCommon.putBufferAudio(audioData.clone());
            } catch (InterruptedException e) {
                //throw new RuntimeException(e);
                LogUtils.w(TAG, " onSynthDataReady InterruptedException ");

            }
            return 0;
        }

        @Override
        public void onSynthDataComplete() {
            final Thread synthesisThread = mCurrentSynthesisThread;
            if (synthesisThread == null || synthesisThread == Thread.currentThread()) {
                //LogUtils.w(TAG, "DualTtsService onSynthDataComplete ***");
                FaTts.onActionTtsQueueCompletedReceived();
                // mCallback.done();
            } else {
                LogUtils.i(TAG, "Skipping completion callback from stale synthesis thread");
            }
        }
    };
    private SynthesisCallback mCallback;
    private String mLanguage = DEFAULT_LANGUAGE_1;
    private String mCountry = DEFAULT_COUNTRY_1;
    private String mVariant = DEFAULT_VARIANT_1;
    private final Object mSynthesisLock = new Object();
    private volatile boolean mIsStopCalled=false;
    private static final long STOP_JOIN_TIMEOUT_MS = 2000L;
    private static final long MAX_ENGLISH_DRAIN_TIMEOUT_MS = 3000L;
    private volatile Thread mCurrentSynthesisThread;
    private final Object mSpeakStateLock = new Object();
    private volatile enmSpeakProgressStates mObservedSpeakProgressState = enmSpeakProgressStates.onIdle;
    private final Object mLifecycleLock = new Object();
    private volatile boolean mIsDestroying = false;
    private volatile boolean mXiaomiForegroundActive = false;
    private Preferences mPreferences;
    private String mPreferredEnglishEngineId = "";
    private String mPreferredEnglishVoiceName = "";
    private AudioFocusRequest mAccessibilityFocusRequest;
    private final ParsAvaUtteranceProgressListener.SpeakProgressStateListener mSpeakStateRelay =
            (previous, current, reason) -> {
                mObservedSpeakProgressState = current;
                if (DEBUG) {
                    LogUtils.v(TAG, "Observed speak state " + previous + " -> " + current + " reason=" + reason);
                }
                if (current == enmSpeakProgressStates.onIdle) {
                    synchronized (mSpeakStateLock) {
                        mSpeakStateLock.notifyAll();
                    }
                }
            };

    /**
     * Default constructor. Initialization is deferred to {@link #onCreate()} so that Android can
     * finish binding the service before heavy engine setup runs.
     */
    public DualTtsService() {
    }

    /**
     * Records the current buffered audio count to make timing-related issues visible in logs.
     *
     * @param context A short label describing the lifecycle event being logged.
     */
    private void logAudioBufferState(@NonNull String context) {
        if (mEngine != null && mEngine.mAudioBufferCommon != null) {
            LogUtils.i(TAG, context + " audioBufferCount=" + mEngine.mAudioBufferCommon.getBufferedCount());
        }
    }

    private static String describeAudioMode(int mode) {
        switch (mode) {
            case AudioManager.MODE_CURRENT:
                return "MODE_CURRENT";
            case AudioManager.MODE_NORMAL:
                return "MODE_NORMAL";
            case AudioManager.MODE_RINGTONE:
                return "MODE_RINGTONE";
            case AudioManager.MODE_IN_CALL:
                return "MODE_IN_CALL";
            case AudioManager.MODE_IN_COMMUNICATION:
                return "MODE_IN_COMMUNICATION";
            default:
                return "MODE_UNKNOWN";
        }
    }

    /**
     * Records the current audio focus environment to diagnose TalkBack fallbacks that may
     * originate from missing or noisy AudioManager state.
     */
    private void logAudioEnvironment(@NonNull String context) {
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            LogUtils.w(TAG, context + " audioManager unavailable");
            return;
        }
        final int mode = audioManager.getMode();
        final boolean musicActive = audioManager.isMusicActive();
        final int dtmfVolume = audioManager.getStreamVolume(AudioManager.STREAM_DTMF);
        final int dtmfMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_DTMF);
        LogUtils.i(TAG, context + " audioEnv mode=" + mode + " (" + describeAudioMode(mode) + ")"
                + " musicActive=" + musicActive
                + " dtmfVol=" + dtmfVolume + "/" + dtmfMax);
    }

    private Preferences getPreferences() {
        if (mPreferences == null) {
            final Context targetContext = mStorageContext != null ? mStorageContext : getApplicationContext();
            mPreferences = new Preferences(targetContext);
        }
        return mPreferences;
    }

    static void persistPreferredEnglishEngine(@NonNull Context context,
                                              @Nullable String engineId,
                                              @Nullable String voiceName) {
        final Context storageContext = PreferenceStorage.resolveStorageContext(context);
        final Preferences preferences = new Preferences(storageContext);
        if (!TextUtils.isEmpty(engineId)) {
            preferences.setEnglishVoiceName(engineId);
        }
        if (!TextUtils.isEmpty(voiceName)) {
            preferences.setLastRequestedVoice("eng", "USA", "", voiceName);
        }
    }

    static Pair<String, String> loadPreferredEnglishEngine(@NonNull Context context) {
        final Context storageContext = PreferenceStorage.resolveStorageContext(context);
        final Preferences preferences = new Preferences(storageContext);
        final String engineId = preferences.getEnglishVoiceName();
        final String voiceName = preferences.getLastVoiceName();
        return new Pair<>(engineId, voiceName);
    }

    private void restorePreferredEnglishSelection(@NonNull String caller) {
        final Pair<String, String> preferred = loadPreferredEnglishEngine(mStorageContext != null
                ? mStorageContext
                : getApplicationContext());
        mPreferredEnglishEngineId = preferred.first;
        if (TextUtils.equals(mPreferredEnglishEngineId, getPackageName())) {
            LogUtils.w(TAG, caller + " ignoring self package as preferred English engine");
            mPreferredEnglishEngineId = "";
        }
        mPreferredEnglishVoiceName = preferred.second;
        LogUtils.i(TAG, caller + " restorePreferredEnglishSelection engine=" + mPreferredEnglishEngineId
                + " voice=" + mPreferredEnglishVoiceName);

        if (mEngine == null || mEngine.mEnTts == null || mEngine.mEnTts.getEnTts() == null) {
            return;
        }
        final String currentEngineId = mEngine.mEnTts.getEnTts().getDefaultEngine();
        if (!TextUtils.isEmpty(mPreferredEnglishEngineId)
                && !TextUtils.equals(currentEngineId, mPreferredEnglishEngineId)) {
            LogUtils.w(TAG, "Re-applying preferred English engine after " + caller + ": " + mPreferredEnglishEngineId);
            mEngine.ChangeEnglishVoice();
        }
    }

    private void persistCurrentEnglishSelection(@NonNull String caller) {
        String engineId = null;
        String voiceName = mPreferredEnglishVoiceName;
        final String selfPackage = getPackageName();
        if (mEngine != null && mEngine.mEnTts != null && mEngine.mEnTts.getEnTts() != null) {
            engineId = mEngine.mEnTts.getCurrentEnginePackage();
            if (TextUtils.isEmpty(engineId)) {
                engineId = mEngine.mEnTts.getEnTts().getDefaultEngine();
            }
        }
        if (TextUtils.isEmpty(engineId)) {
            engineId = mPreferredEnglishEngineId;
        }
        if (TextUtils.equals(engineId, selfPackage)) {
            LogUtils.w(TAG, caller + " not persisting self package as English engine");
            engineId = "";
        }
        if (TextUtils.isEmpty(voiceName) && mMatchingVoice != null) {
            voiceName = mMatchingVoice.name;
        }
        if (!TextUtils.isEmpty(engineId) || !TextUtils.isEmpty(voiceName)) {
            LogUtils.i(TAG, caller + " persisting English engine=" + engineId + " voice=" + voiceName);
            persistPreferredEnglishEngine(mStorageContext != null ? mStorageContext : getApplicationContext(),
                    engineId,
                    voiceName);
        }
    }

    /**
     * Retrieves the language code from a synthesis request.
     *
     * @param request The synthesis request.
     * @return A language code in the format "en-uk-n".
     */
    private static String getRequestLanguage(SynthesisRequest request) {
        final StringBuilder result = new StringBuilder(request.getLanguage());
        LogUtils.w(TAG,"DualTtsService getRequestLanguage called " + request.getLanguage());
        final String country = request.getCountry();
        final String variant = request.getVariant();

        if (!TextUtils.isEmpty(country)) {
            result.append('-');
            result.append(country);
        }

        if (!TextUtils.isEmpty(variant)) {
            result.append('-');
            result.append(variant);
        }

        return result.toString();
    }

    public static Pair<Integer, Integer> resolveEffectivePersianRatePitch(Context context,
                                                                          String language,
                                                                          int requestedRate,
                                                                          int requestedPitch,
                                                                          @Nullable SpeechSynthesis engine) {
        final boolean isPersian = !TextUtils.isEmpty(language) && language.startsWith("fa");
        if (!isPersian) {
            LogUtils.w(TAG,"setPitch resolveEffectivePersianRatePitch not Persian" + Integer.toString(requestedPitch)+ " "+ Integer.toString(requestedRate));
            //Default is 100
            return new Pair<>(requestedRate, (requestedPitch - 50));
        }
        final Preferences prefs = new Preferences(context);
        final int storedRate = prefs.getPersianRate();
        final int storedPitch = prefs.getPersianPitch();
        // Persisted rates come from a 0-100 seekbar. Treat TalkBack's request rate as a
        // multiplier so the user's stored baseline still governs MBROLA (ir1/ir2) speed and we
        // avoid double-applying the screen reader's own scaling.
        LogUtils.w(TAG,"setPitch resolveEffectivePersianRatePitch is Persian " + Integer.toString(requestedPitch)+ " "+ Integer.toString(requestedRate));

        final int clampedStoredRate = Math.max(1, Math.min(100, storedRate));
        final float screenReaderMultiplier = requestedRate > 0 ? (requestedRate / 100.0f) : 1.0f;
        final int scaledRate = Math.round(clampedStoredRate * screenReaderMultiplier);
        final int resolvedRate = Math.max(25, Math.min(400, scaledRate));

        final int clampedStoredPitch = Math.max(1, Math.min(100, storedPitch));
        final float screenReaderPitchMultiplier = requestedPitch > 0 ? (requestedPitch / 100.0f) : 1.0f;
        final int scaledPitch = Math.round(clampedStoredPitch * screenReaderPitchMultiplier);

        int resolvedPitch = Math.max(25, Math.min(100, scaledPitch));
        //default is 100
        //resolvedPitch =  resolvedPitch == 100 ? 50 : storedPitch; //MN inesrt
        LogUtils.w(TAG,"setPitch " + resolvedPitch);
        if (engine != null && engine.mFaTts != null && engine.mFaTts.mConfiureParams != null) {
            engine.mFaTts.mConfiureParams.mRate = resolvedRate;
            engine.mFaTts.mConfiureParams.mPitch = resolvedPitch;
            engine.mFaTts.mConfiureParams.mIsRateChange = true;
            engine.mFaTts.mConfiureParams.mIsPitchChange = true;
        }
        return new Pair<>(resolvedRate, resolvedPitch);
    }

    @Override
    public void onCreate() {
        mStorageContext = ExtendedApplication.getStorageContext();
        if (mStorageContext == null) {
            mStorageContext = getApplicationContext();
        }
        mPreferences = new Preferences(mStorageContext);
        restorePreferredEnglishSelection("onCreate-preInit");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !ExtendedApplication.isUserUnlocked(mStorageContext)) {
            final Context deviceContext = getApplicationContext().createDeviceProtectedStorageContext();
            if (deviceContext != null) {
                mStorageContext = deviceContext;
            }
            LogUtils.i(TAG, "Using device-protected storage while user is locked");
        }

        try {
            initializeTtsEngine();
        } catch (IllegalStateException e) {
            LogUtils.e(TAG, "Unable to initialize TTS engine while user storage is locked", e);
            stopSelf();
            return;
        }
        restorePreferredEnglishSelection("onCreate-postInit");
        // This calls onIsLanguageAvailable() and must run AFTER initialization!
        super.onCreate();
    }

    /*
     * onCreate() is called when the Service object is instantiated 
     * (ie: when the service is created). You should do things in this method 
     * that you need to do only once (ie: initialize some variables, etc.). 
     * onCreate() will only ever be called once per instantiated object.

        You only need to implement onCreate() if you actually want/need to initialize 
        something only once.

        onStartCommand() is called every time a client starts the service using 
        startService(Intent intent). This means that onStartCommand() 
        can get called multiple times. You should do the things in this method 
        that are needed each time a client requests something from your service. 
        This depends a lot on what your service does and 
        how it communicates with the clients (and vice-versa).
     */
    @Override
    public int onStartCommand(Intent intent,
                              int flags,
                              int startId) {
        LogUtils.w(TAG, "onStartCommand called " + (intent != null ? intent.getAction() : ""));
        restorePreferredEnglishSelection("onStartCommand");
        persistCurrentEnglishSelection("onStartCommand");
        if (shouldEnableXiaomiPersistence()) {
            ensureForegroundForXiaomi();
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        LogUtils.w(TAG, "onTaskRemoved called ");
        persistCurrentEnglishSelection("onTaskRemoved");
        if (shouldEnableXiaomiPersistence()) {
            ensureForegroundForXiaomi();
            final Intent restartIntent = new Intent(getApplicationContext(), DualTtsService.class);
            restartIntent.setPackage(getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        }
        super.onTaskRemoved(rootIntent);

    }
    public List<ParsAvaVoice> getAvailableVoices() {
        final List<ParsAvaVoice> voices = new LinkedList<ParsAvaVoice>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            voices.add(new ParsAvaVoice("ParsAva_English",
                    "ParsAva_English",0,40,new Locale("eng", "USA", "")
                    ));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            voices.add(new ParsAvaVoice("ParsAva_Persian",
                    "ParsAva_Persian",1,40,
                    new Locale("fas", "IRN", "")
                    ));
        }
        return voices;
    }

    private void ensureVoicesLoaded() {
        if (!mAvailableVoices.isEmpty()) {
            return;
        }
        for (ParsAvaVoice voice : getAvailableVoices()) {
            mAvailableVoices.put(voice.name, voice);
        }
    }

    private boolean shouldEnableXiaomiPersistence() {
        return Build.MANUFACTURER != null && Build.MANUFACTURER.equalsIgnoreCase(MANUFACTURER_XIAOMI);
    }

    private void ensureForegroundForXiaomi() {
        if (mXiaomiForegroundActive) {
            return;
        }
        LogUtils.w(TAG, "Starting Xiaomi foreground guard");
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            LogUtils.w(TAG, "Notification manager unavailable; skipping Xiaomi foreground guard");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "ParsAva TTS",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps ParsAva speaking for TalkBack on Xiaomi");
            notificationManager.createNotificationChannel(channel);
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setSmallIcon(R.mipmap.parsava)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.xiaomi_foreground_notification_body))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setAutoCancel(false);
        final Notification notification = builder.build();
        startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        mXiaomiForegroundActive = true;
    }

    private boolean isTalkBackEnabled() {
        final AccessibilityManager accessibilityManager =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        return accessibilityManager != null
                && accessibilityManager.isEnabled()
                && accessibilityManager.isTouchExplorationEnabled();
    }

    private void maybeBoostThreadPriority(boolean shouldBoost, @NonNull String threadLabel) {
        if (!shouldBoost) {
            return;
        }
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            LogUtils.i(TAG, threadLabel + " priority boosted for TalkBack");
        } catch (SecurityException | IllegalArgumentException e) {
            LogUtils.w(TAG, threadLabel + " priority boost skipped: " + e.getMessage());
        }
    }

    private void requestTransientAccessibilityFocus(@Nullable AudioManager audioManager) {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mAccessibilityFocusRequest == null) {
                final AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                mAccessibilityFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(attributes)
                        .setWillPauseWhenDucked(false)
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener(focusChange -> { })
                        .build();
            }
            audioManager.requestAudioFocus(mAccessibilityFocusRequest);
            return;
        }
        audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
    }

    private void abandonTransientAccessibilityFocus(@Nullable AudioManager audioManager) {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mAccessibilityFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(mAccessibilityFocusRequest);
            }
            return;
        }
        audioManager.abandonAudioFocus(null);
    }

    private void configureEnglishAudioRouting(@NonNull String language) {
        if (mEngine == null || mEngine.mEnTts == null) {
            return;
        }
        if (!language.startsWith("en")) {
            mEngine.mEnTts.updateAudioRoutingForTalkBack(false, "non-English request");
            abandonTransientAccessibilityFocus((AudioManager) getSystemService(Context.AUDIO_SERVICE));
            return;
        }
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            LogUtils.w(TAG, "configureEnglishAudioRouting audioManager unavailable");
            return;
        }
        final int mode = audioManager.getMode();
        final int dtmfVolume = audioManager.getStreamVolume(AudioManager.STREAM_DTMF);
        final int dtmfMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_DTMF);
        final boolean preferAccessibilityRouting = isTalkBackEnabled()
                && mode == AudioManager.MODE_IN_COMMUNICATION;

        if (preferAccessibilityRouting) {
            LogUtils.i(TAG,
                    "Preferring accessibility routing for English playback: mode=" + mode
                            + " (" + describeAudioMode(mode) + ") dtmfVol=" + dtmfVolume + "/" + dtmfMax
                            + " speakerphone=" + audioManager.isSpeakerphoneOn());
            mEngine.mEnTts.updateAudioRoutingForTalkBack(true,
                    "mode=" + describeAudioMode(mode) + " dtmf=" + dtmfVolume + "/" + dtmfMax);
            requestTransientAccessibilityFocus(audioManager);
        } else {
            mEngine.mEnTts.updateAudioRoutingForTalkBack(false,
                    "mode=" + describeAudioMode(mode) + " dtmf=" + dtmfVolume + "/" + dtmfMax);
            abandonTransientAccessibilityFocus(audioManager);
        }
    }

    /**
     * Recreates the {@link SpeechSynthesis} bridge and reattaches voice metadata.
     *
     * <p>This method is called during {@link #onCreate()} and after explicit stop requests to
     * guarantee that the engine state is always fresh. The shared {@link ExtendedApplication}
     * keeps the NLP handle to avoid repeated native initializations.
     */
    private void initializeTtsEngine() {
        if (mEngine != null) {
            mEngine.stop(SpeechSynthesis.StopReason.SERVICE_SHUTDOWN,
                    "initializeTtsEngine",
                    "recreateEngine");
            mEngine = null;
        }
        //AssetManager am = this.getAssets();
            final String pck_name = getApplicationContext().getPackageName();
            mEngine = new SpeechSynthesis(mStorageContext, mSynthCallback, pck_name, null);
        attachSpeakProgressObserver();
        mAvailableVoices.clear();
        for (ParsAvaVoice voice : getAvailableVoices()) {
                mAvailableVoices.put(voice.name, voice);
        }
        ExtendedApplication app = (ExtendedApplication)this.getApplication();
        //These below code not used later , whenever an instance need it is new it
        //The below code was to sarfejoyi, only one instance in program level exist , but it was mistake
        app.IncEngineReferenceCount();

        if(app.getNlpHand()==0L) {
            app.setNlpHand(mEngine.mFaTts.getNlpHand());
        }
        LogUtils.w(TAG, "DualTtsService.initializeTtsEngine called ");

        // TextToSpeech sd;

    }

    /**
     * Hooks the English TTS listener so we can mirror its progress state into
     * {@link #mObservedSpeakProgressState}. This allows stop/flush routines to wait on the same
     * transitions the engine observes.
     */
    private void attachSpeakProgressObserver() {
        if (mEngine == null || mEngine.mEnTts == null || mEngine.mEnTts.mUtteranceProgressListener == null) {
            return;
        }
        mEngine.mEnTts.mUtteranceProgressListener.setSpeakProgressStateListener(mSpeakStateRelay);
        mObservedSpeakProgressState = mEngine.mEnTts.mUtteranceProgressListener.getSpeakProgressState();
    }
    /**
     * Locates the most appropriate {@link ParsAvaVoice} for the requested locale.
     *
     * @param language ISO3 language code requested by Android.
     * @param country  ISO3 country code requested by Android.
     * @param variant  Optional variant information.
     * @return Pair of the matching voice (or {@code null}) and the TTS language availability code
     *         expected by framework callers.
     */
    private Pair<ParsAvaVoice, Integer> findVoice(String language, String country, String variant) {
        final Locale query = new Locale(language, country, variant);

        ParsAvaVoice languageVoice = null;
        ParsAvaVoice countryVoice = null;
        synchronized (mAvailableVoices) {
            for (ParsAvaVoice voice : mAvailableVoices.values()) {
                LogUtils.w(TAG,"DualTtsVoice:findVoice " + voice.name);
                switch (voice.match(query)) {
                    case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                        return new Pair<>(voice, TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE);
                    case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                        countryVoice = voice;
                    case TextToSpeech.LANG_AVAILABLE:
                        languageVoice = voice;
                        break;
                }
            }
            if (languageVoice == null) {
                return new Pair<>(null, TextToSpeech.LANG_NOT_SUPPORTED);
            } else if (countryVoice == null) {
                return new Pair<>(languageVoice, TextToSpeech.LANG_AVAILABLE);
            } else {
                return new Pair<>(countryVoice, TextToSpeech.LANG_COUNTRY_AVAILABLE);
            }
        }
    }
    /**
     * Checks whether the engine supports a given language.
     * <p>
     * Can be called on multiple threads.
     * <p>
     * Its return values HAVE to be consistent with onLoadLanguage.
     *
     * @param lang    ISO-3 language code.
     * @param country ISO-3 country code. May be empty or null.
     * @param variant Language variant. May be empty or null.
     * @return Code indicating the support status for the locale.
     * One of {@link TextToSpeech#LANG_AVAILABLE},
     * {@link TextToSpeech#LANG_COUNTRY_AVAILABLE},
     * {@link TextToSpeech#LANG_COUNTRY_VAR_AVAILABLE},
     * {@link TextToSpeech#LANG_MISSING_DATA}
     * {@link TextToSpeech#LANG_NOT_SUPPORTED}.
     */
    @Override
    protected int onIsLanguageAvailable(String lang, String country, String variant) {
        LogUtils.w(TAG,"onIsLanguageAvailable lang:"+lang);
        final String safeLang = lang == null ? "" : lang;
        final String safeCountry = country == null ? "" : country;
        boolean hasLanguage = false;
        boolean hasCountry = false;
        if (safeLang.contains(DEFAULT_LANGUAGE_1) || safeLang.contains(DEFAULT_LANGUAGE_2)) {
            hasLanguage = true;
        }
        if (safeCountry.contains(DEFAULT_COUNTRY_1) || safeCountry.contains(DEFAULT_COUNTRY_2)) {
            hasCountry = true;
        }
        if (!hasLanguage) {
            return TextToSpeech.LANG_NOT_SUPPORTED;
        } else if (!hasCountry) {
            return TextToSpeech.LANG_AVAILABLE;
        } else {
            return TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
            //return TextToSpeech.LANG_COUNTRY_AVAILABLE;
        }

    }
    @Override
    protected Set<String> onGetFeaturesForLanguage(String lang, String country, String variant) {
        return new HashSet<String>();
    }
    /**
     * Returns the language, country and variant currently being used by the TTS engine.
     * <p>
     * This method will be called only on Android 4.2 and before (API <= 17). In later versions
     * this method is not called by the Android TTS framework.
     * <p>
     * Can be called on multiple threads.
     *
     * @return A 3-element array, containing language (ISO 3-letter code),
     * country (ISO 3-letter code) and variant used by the engine.
     * The country and variant may be {@code ""}. If country is empty, then variant must
     * be empty too.
     * @see Locale#getISO3Language()
     * @see Locale#getISO3Country()
     * @see Locale#getVariant()
     */
    @Override
    protected String[] onGetLanguage() {
        if(mMatchingVoice==null) {
            return new String[]{
                    mLanguage, mCountry, mVariant
            };
        }
            return new String[] {
                    mMatchingVoice.locale.getISO3Language(),
                    mMatchingVoice.locale.getISO3Country(),
                    mMatchingVoice.locale.getVariant()
            };
    }

    /**
     * Notifies the engine that it should load a speech synthesis language. There is no guarantee
     * that this method is always called before the language is used for synthesis. It is merely
     * a hint to the engine that it will probably get some synthesis requests for this language
     * at some point in the future.
     * <p>
     * Can be called on multiple threads.
     * In <= Android 4.2 (<= API 17) can be called on main and service binder threads.
     * In > Android 4.2 (> API 17) can be called on main and synthesis threads.
     *
     * @param lang    ISO-3 language code.
     * @param country ISO-3 country code. May be empty or null.
     * @param variant Language variant. May be empty or null.
     * @return Code indicating the support status for the locale.
     * One of {@link TextToSpeech#LANG_AVAILABLE},
     * {@link TextToSpeech#LANG_COUNTRY_AVAILABLE},
     * {@link TextToSpeech#LANG_COUNTRY_VAR_AVAILABLE},
     * {@link TextToSpeech#LANG_MISSING_DATA}
     * {@link TextToSpeech#LANG_NOT_SUPPORTED}.
     */
    @Override
    protected int onLoadLanguage(String lang, String country, String variant) {
        final Pair<ParsAvaVoice, Integer> match = findVoice(lang, country, variant);
        if (match.first != null) {
            mMatchingVoice = match.first;
        }
        new Preferences(ExtendedApplication.getStorageContext())
                .setLastRequestedVoice(lang, country, variant, match.first != null ? match.first.name : null);
        return match.second;

    }
    /*
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    */

    /**
     * Immediately stops ongoing synthesis and clears any queued audio.
     *
     * <p>This method is conservative: it interrupts the worker thread, instructs listeners to
     * enter the idle state, clears buffers, and waits briefly for the worker to finish. It is used
     * by {@link #onStop()} and internally when overlapping requests arrive.
     */
    private void haltCurrentSynthesis(@NonNull SpeechSynthesis.StopReason stopReason,
                                      @NonNull String reason) {
        synchronized (mSynthesisLock) {
            mIsStopCalled = true;
            mObservedSpeakProgressState = enmSpeakProgressStates.onIdle;
            LogUtils.i(TAG, "Halting synthesis because: " + reason + "; callerThreadId=" + Thread.currentThread().getId()
                    + " synthesisThreadId=" + (mCurrentSynthesisThread == null ? "null" : mCurrentSynthesisThread.getId()));
            if (mEngine != null && mEngine.mEnTts != null && mEngine.mEnTts.mUtteranceProgressListener != null) {
                mEngine.mEnTts.mUtteranceProgressListener.forceIdle(reason);
            }
            final Thread synthesisThread = mCurrentSynthesisThread;
            if (synthesisThread != null && synthesisThread.isAlive()) {
                synthesisThread.interrupt();
            }
            if (mEngine != null) {
                mEngine.stop(stopReason, "haltCurrentSynthesis", reason);
            }
            if (synthesisThread != null && synthesisThread != Thread.currentThread()) {
                try {
                    synthesisThread.join(STOP_JOIN_TIMEOUT_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LogUtils.w(TAG, "Interrupted while waiting for synthesis worker to stop");
                }
            }
            if (mEngine != null) {
                final int bufferedCount = mEngine.mAudioBufferCommon.getBufferedCount();
                mEngine.mAudioBufferCommon.clearBuffers();
                LogUtils.i(TAG, "Cleared audio buffers after halt; clearedCount=" + bufferedCount);
            }
            if (synthesisThread == mCurrentSynthesisThread) {
                mCurrentSynthesisThread = null;
            }
        }
    }

    /**
     * Waits for a previous synthesis thread to finish before starting a new request.
     *
     * @param synthesisThread The thread that previously handled synthesis, or {@code null}.
     * @return {@code true} if the previous thread is already stopped or successfully joined.
     */
    private boolean waitForPreviousSynthesisToStop(@Nullable Thread synthesisThread) {
        if (synthesisThread == null) {
            return true;
        }
        if (synthesisThread == Thread.currentThread()) {
            return !synthesisThread.isAlive();
        }
        if (synthesisThread.isAlive()) {
            try {
                synthesisThread.join(STOP_JOIN_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LogUtils.w(TAG, "Interrupted while waiting for synthesis worker to stop");
            }
        }
        return !synthesisThread.isAlive();
    }

    /**
     * Stops the synthesis thread and resets both engine and buffer state.
     *
     * <p>This is a lighter-weight alternative to {@link #haltCurrentSynthesis(SpeechSynthesis.StopReason, String)} used when
     * the service already knows it is stopping because of an explicit flag.
     */
    private void stopSynthesisThreadAndVoices(@Nullable Thread synthesisThread) {
        if (mEngine != null) {
            mEngine.stop(SpeechSynthesis.StopReason.FLUSH,
                    "stopSynthesisThreadAndVoices",
                    synthesisThread == null ? "noThread" : "thread-" + synthesisThread.getId());
            mEngine.mAudioBufferCommon.clearBuffers();
        }
        if (synthesisThread != null && synthesisThread.isAlive()) {
            synthesisThread.interrupt();
            try {
                synthesisThread.join(STOP_JOIN_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LogUtils.w(TAG, "Interrupted while waiting for synthesis worker to stop");
            }
        }
        if (synthesisThread == mCurrentSynthesisThread) {
            mCurrentSynthesisThread = null;
        }
    }
    /**
     * Notifies the service that it should stop any in-progress speech synthesis.
     * This method can be called even if no speech synthesis is currently in progress.
     * <p>
     * Can be called on multiple threads, but not on the synthesis thread.
     */
    @Override
    protected void onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LogUtils.e(TAG, "DualTts onStop Received stop request. ThreadId:" + LocalTime.now() + " Thread Id: " + currentThread().getId());
        }
        logAudioBufferState("onStop before halt");
        haltCurrentSynthesis(SpeechSynthesis.StopReason.TALKBACK_REQUEST, "onStop invoked");
    }
    //In Espeak source
    @SuppressWarnings("deprecation")
    private String getRequestString(SynthesisRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return request.getCharSequenceText().toString();
        } else {
            return request.getText();
        }
    }
    public static final String KEY_PARAM_ISTEXT =
            "KEY_PARAM_ISTEXT";
    /**
     * Tells the service to synthesize speech from the given text. This method should block until
     * the synthesis is finished. Called on the synthesis thread.
     *
     * @param request  The synthesis request.
     * @param callback The callback that the engine must use to make data available for playback or
     *                 for writing to a file.
     */
    @Override
    protected void onSynthesizeText(SynthesisRequest request, SynthesisCallback callback) {

        // Flow overview:
        // 1. Normalize request text and language, then stop any previous worker thread.
        // 2. Launch a worker that streams engine output into a shared buffer queue.
        // 3. Drain buffers into the framework callback while monitoring for stop/deadlock signals.
        // 4. Mark completion and clean up state so the next request starts from a blank slate.

        //final String text = request.getText().length()!=0 ?  " جمله " + request.getText() : request.getText();
        final String text = getRequestString(request);
        //text.startsWith()
        final String language = getRequestLanguage(request);
        LogUtils.w(TAG,"DualTtsService.onSynthesizeText :" + language + " " + text + " "  + text.length());
        logAudioEnvironment("onSynthesizeText");
        configureEnglishAudioRouting(language);
        final Thread previousSynthesisThread;
        synchronized (mSynthesisLock) {
            previousSynthesisThread = mCurrentSynthesisThread;
        }
        haltCurrentSynthesis(SpeechSynthesis.StopReason.NEW_REQUEST, "Starting new synthesis request");
        final boolean previousStopped = waitForPreviousSynthesisToStop(previousSynthesisThread);
        if (!previousStopped && previousSynthesisThread != null && previousSynthesisThread.isAlive()) {
            previousSynthesisThread.interrupt();
        }
        //final int gender = getDefaultGender();
        // 100 default
        int rate = request.getSpeechRate();
        //100 default
        int pitch = request.getPitch();
        final Bundle params = request.getParams();

        boolean isText = params.getBoolean(KEY_PARAM_ISTEXT,true);
        //1.0 default
        float volume = params.getFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,1.0F);
        final Pair<Integer, Integer> resolvedParams = resolveEffectivePersianRatePitch(
                mStorageContext,
                language,
                rate,
                pitch,
                mEngine);
        rate = resolvedParams.first;
        pitch = resolvedParams.second;
        //LogUtils.w(TAG,"DualTtsService.onSynthesizeText rate:" + rate + " pitch:" + pitch + " volume:" + volume + " text: " + text  + " len: " + text.length());
        if(text.length()==0){
            LogUtils.w(TAG,"DualTtsService.onSynthesizeText text.length()==0 !!" );
            return;
        }
        //text = " جمله " + text;
        mEngine.setRate(rate);
        mEngine.setPitch(pitch);
        mEngine.setVolume(volume);
        mLanguage = request.getLanguage();
        mCountry = request.getCountry();
        mVariant = request.getVariant();
        synchronized (mSynthesisLock) {
            if (previousStopped) {
                mIsStopCalled = false;
                mObservedSpeakProgressState = enmSpeakProgressStates.onIdle;
            }
        }
        if (DEBUG) {
            LogUtils.i(TAG, "Received synthesis request: {language=\"" + language + "\"}");

            for (String key : params.keySet()) {
                LogUtils.v(TAG,
                        "Synthesis request contained param {" + key + ", " + params.get(key) + "}");
            }
        }
        //if (language.contains("fa")) {
        mCallback = callback;

        logAudioBufferState("onSynthesizeText before clearBuffers");
        mEngine.mAudioBufferCommon.clearBuffers();
        logAudioBufferState("onSynthesizeText after clearBuffers");
        //Suggest by chatgpt to become comment: System.gc();
        mCallback.start(mEngine.getSampleRate(), mEngine.getAudioFormat(),
                mEngine.getChannelCount());
        mEngine.mEnTts.setOnUtteranceProgressListener(mCallback);
        attachSpeakProgressObserver();
        //AudioBufferObserver observer = new AudioBufferObserver();
        //observer.setmSpeechSynEngine(mEngine);
        //observer.setSynthesisCallback(mCallback);
        //LogUtils.w(TAG, "DualTtsService onSynthesizeText called threadId:" + Thread.currentThread().getId() + " text:" + text);


        //}
        //mEngine.setVoiceByProperties(null, language, gender, 0, 0);
        //mEngine.setRate(rate);
        //mEngine.setPitch(pitch);
        //synthesize called but not exist this function , why?
        //final boolean[] isThreadFinished = {false};
        final boolean prioritizeTalkBack = isTalkBackEnabled();
        Runnable r = () -> {
            try {
                //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
                //LogUtils.w(TAG, "DualTtsService onSynthesizeText runThread thraedId:" + Thread.currentThread().getId());
                maybeBoostThreadPriority(prioritizeTalkBack, "Synthesis producer");

                //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
                mEngine.speak(text, language, isText);
            } catch (UnsupportedEncodingException e) {
                LogUtils.w(TAG, "DualTtsService onSynthesizeText UnsupportedEncodingException");
                e.printStackTrace();
                //throw new RuntimeException(e);
            } catch (Exception e) {
                LogUtils.w(TAG, "DualTtsService onSynthesizeText Exception");
                e.printStackTrace();
            }
        };
        Thread threadSpeak = new Thread(r, "Tts Thread");
        synchronized (mSynthesisLock) {
            mCurrentSynthesisThread = threadSpeak;
        }
        threadSpeak.start();

        // I guess the SIGSEGV error cause from a function similar to this function action
        //threadSpeak.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        //thread.setPriority((Thread.MAX_PRIORITY + Thread.NORM_PRIORITY) / 2);
        int nCounter = 0;
        boolean bMessageLog = false;
        boolean isDeadLocked = false;
        final long MAXIMUM_CHARACTER_DURATION = 500;
        long maximumMiliSecond = (long) MAXIMUM_CHARACTER_DURATION * text.length();
        /*CountDownTimer countDownTimer = new CountDownTimer(maximumMiliSecond, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                LogUtils.w(TAG,"CountDownTimer onTick called");

            }

            @Override
            public void onFinish() {
                //isDeadLocked = true;
                LogUtils.w(TAG,"CountDownTimer onFinish called");
                mEngine.mEnTts.mUtteranceProgressListener.forceIdle("CountDownTimer onFinish");
            }
        }.start();

         */
        long startInNano = System.nanoTime();
        final long bufferPollTimeoutMs = 50L;
        // Allow a bounded amount of time for late audio buffers before forcefully stopping the loop.
        // Clamp the timeout so short requests still get a short grace period while longer utterances
        // fail fast instead of waiting tens of seconds after the English engine stalls. English letters
        // that require resampling sometimes take longer than two seconds to deliver buffers, so allow
        // a slightly longer grace period before treating the pipeline as stuck.
        long drainTimeoutMs = 0;
        if (text.length()>1) {
            drainTimeoutMs = Math.min(MAX_ENGLISH_DRAIN_TIMEOUT_MS, Math.max(1000L, maximumMiliSecond));
        }
        else
        {
            drainTimeoutMs = MAX_ENGLISH_DRAIN_TIMEOUT_MS + 200000;
        }
        long idleDeadline = -1L;
        long nonIdleStallStartMs = -1L;
        boolean stopHandled = false;
        do {
            if (Thread.currentThread().isInterrupted()) {
                LogUtils.i(TAG, "Synthesis thread interrupted before draining loop iteration");
                break;
            }
            try {
                // Block briefly for new audio instead of spinning the binder thread.
                final byte[] bufferAudio = mEngine.mAudioBufferCommon.pollBufferAudio(bufferPollTimeoutMs);
                if (bufferAudio == null) {
                    if (mIsStopCalled) {
                        stopSynthesisThreadAndVoices(threadSpeak);
                        stopHandled = true;
                        break;
                    }
                    if (!threadSpeak.isAlive()
                            && mObservedSpeakProgressState == enmSpeakProgressStates.onIdle
                            && !mEngine.mAudioBufferCommon.hasBufferedAudio()) {
                        // Exit once the producer is idle and no buffers arrive within the drain timeout.
                        // This prevents the binder thread from spinning forever when upstream stalls.
                        if (idleDeadline < 0) {
                            idleDeadline = SystemClock.elapsedRealtime() + drainTimeoutMs;
                        } else if (SystemClock.elapsedRealtime() >= idleDeadline) {
                            LogUtils.i(TAG, "Drain timeout reached with no audio. Breaking synthesis loop.");
                            break;
                        }
                        nonIdleStallStartMs = -1L;
                    } else if (!threadSpeak.isAlive()
                            && !mEngine.mAudioBufferCommon.hasBufferedAudio()
                            && mObservedSpeakProgressState != enmSpeakProgressStates.onIdle) {
                        if (nonIdleStallStartMs < 0) {
                            nonIdleStallStartMs = SystemClock.elapsedRealtime();
                        } else if (SystemClock.elapsedRealtime() - nonIdleStallStartMs >= drainTimeoutMs) {
                            LogUtils.w(TAG, "Force idling English TTS after MIUI-style disconnect stall");
                            mEngine.mEnTts.mUtteranceProgressListener.forceIdle("Stalled without buffers");
                            mObservedSpeakProgressState = enmSpeakProgressStates.onIdle;
                            break;
                        }
                    } else {
                        /*
                        LogUtils.i(TAG, "Audio poll returned null while producer alive; bufferCount="
                                + mEngine.mAudioBufferCommon.getBufferedCount() + " stopCalled=" + mIsStopCalled
                                + " speakState=" + mObservedSpeakProgressState);
                         */
                    }
                    continue;
                }
                idleDeadline = -1L;
                nonIdleStallStartMs = -1L;
                final int maxBytesToCopy = mCallback.getMaxBufferSize();

                int offset = 0;
                nCounter = 0;
                bMessageLog = false;
                //restartIfDisabled(2501): releaseBuffer() track disabled due to previous underrun, restarting
                // معنی اش این است که بافر در حال نوشته شدن هست، بعد به نقطه آخر رسیده و بافر جدید هنوز دریافت نشده وقتی پردازش بیشتر طول بکشد و با تاخیر بافر صوتی نوشته شود
                while (offset < bufferAudio.length && !mIsStopCalled && !Thread.currentThread().isInterrupted()) {
                    //LogUtils.w(TAG, "onSynthDataReady Len:" + audioData.length);
                    final int bytesToWrite = Math.min(maxBytesToCopy, (bufferAudio.length - offset));
                    mCallback.audioAvailable(bufferAudio, offset, bytesToWrite);
                    offset += bytesToWrite;
                    nCounter++;
                    if (!bMessageLog && nCounter > 100) {
                        bMessageLog = true;
                        LogUtils.i(TAG, "onSynthesizeText DeadLocked in while");
                        isDeadLocked = true;
                    }
                }
            } catch (InterruptedException e) {
                //throw new RuntimeException(e);
                LogUtils.i(TAG, "onSynthesizeText InterruptedException");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LogUtils.i(TAG, "onSynthesizeText Exception");
                e.printStackTrace();

            }
            if (Thread.currentThread().isInterrupted()) {
                LogUtils.i(TAG, "Synthesis thread interrupted while draining buffers");
                break;
            }
            if(mIsStopCalled && threadSpeak.isAlive()){
                LogUtils.w(TAG,"DualTtsService.onSynthesizeText mIsStopCalled && threadSpeak.isAlive()");
            }
            //mEngine.mEnTts.mUtteranceProgressListener.mIsBusy in only english text ,
            // the thread finished , and after that buffer is become fill
            //LogUtils.w(TAG,"mUtteranceProgressListener.mSpeakProgressState: " + mEngine.mEnTts.mUtteranceProgressListener.mSpeakProgressState);
            //In Version 3930 In Xiaomi MIUI , when close recent apps
            //Text to speech request
            //Asked to disconnect from ComponentInfo{com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSService}
            //from En Engine
            //Displayed as level:info
            //So EnTts disconnected and mEngine.mEnTts.mUtteranceProgressListener.mSpeakProgressState remind onFirstAudioPacketReceived not Idle
            //So we need to determine deadlock on it
            //LogUtils.w(TAG,"In while" + mEngine.mEnTts.mUtteranceProgressListener.mSpeakProgressState);
                long endInNano = System.nanoTime();
            long duration = (endInNano-startInNano)/1000000;
            if(duration>=maximumMiliSecond)
            {
                isDeadLocked=true;
                mEngine.mEnTts.mUtteranceProgressListener.forceIdle("maximum duration exceeded");
                LogUtils.w(TAG,"duration>=maximumMiliSecond isDeadLocked=true");
            }
            } while ((threadSpeak.isAlive()
                    || mEngine.mAudioBufferCommon.hasBufferedAudio()
                    || mObservedSpeakProgressState != enmSpeakProgressStates.onIdle)
                    && !isDeadLocked
                    && !Thread.currentThread().isInterrupted()
                    && !mIsStopCalled);

        if (mIsStopCalled && !stopHandled) {
            stopSynthesisThreadAndVoices(threadSpeak);
            stopHandled = true;
        }
        LogUtils.w(TAG,"DualTtsService.onSynthesizeText SyncThreadLoop is finished");
        //countDownTimer.cancel();
        try {
            threadSpeak.join(Math.max(1000L, drainTimeoutMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LogUtils.w(TAG, "Interrupted while waiting for synthesis thread to finish");
        }
        if (mCurrentSynthesisThread == threadSpeak) {
            mCurrentSynthesisThread = null;
        }
        mCallback.done();
        abandonTransientAccessibilityFocus((AudioManager) getSystemService(Context.AUDIO_SERVICE));

        //In new model (before that was obsever ) but I underestand observer call with thread of run() thread that I create myself
        //After that I guess not run
        //try {
            //I hope join not block my sync thread , I remember not blocked
            //threadSpeak.join(0);

            /*
             * Basically, everything that affects the GUI in any way must happen
             *  on a single thread. This is because experience shows 
             * that a multi-threaded GUI is impossible to get right.

                In Swing, this special GUI thread is called the Event Dispatch 
                Thread, or EDT. It is started as soon as 
                a Swing top-level component is displayed, 
                and it's bascially a worker thread that has a FIFO queue 
                of event objects that it executes one after another.

                And now for the finish: invokeAndWait()
                 places the Runnable you pass to it into the 
                 EDT event queue and waits until the EDT has executed it. 
                 This should be used when a non-GUI thread 
                 needs to do something that affects the GUI, 
                 but also needs to wait until it is actually done before
                  it can continue. If you just want to do something that affects
                   the GUI but do not care when it is finished, 
                   you should instead use invokeLater().

                   https://stackoverflow.com/questions/5499921/invokeandwait-method-in-swingutilities
                   my Idea: not suitable because wait until worker thread finish
             */
            /*
             try {
                     SwingUtilities.invokeAndWait(doHelloWorld);
                 }
                 catch (Exception e) {
                     e.printStackTrace();
                 }
             */
            /*if (!mEngine.mAudioBufferCommon.mBufferAudio.isEmpty())
            {
                LogUtils.w(TAG, "onSynthesizeText not mBufferAudio.isEmpty");
                boolean isMessagePrint = false;
                int nLoopCount = 0;
                do {
                    Thread.sleep(1);
                    nLoopCount++;
                    if (!isMessagePrint && nLoopCount > 100) {
                        isMessagePrint = true;
                        LogUtils.w(TAG, "onSynthesizeText DeadLock");
                    }
                } while (!mEngine.mAudioBufferCommon.mBufferAudio.isEmpty());
            }*/
        /*} finally {
            mCallback.done();
        }*/

    }

    @Override
    public String onGetDefaultVoiceNameFor(String lang,
                                           String country,
                                           String variant) {
        LogUtils.w(TAG, "onGetDefaultVoiceNameFor lang:" + lang);
        final Pair<ParsAvaVoice, Integer> match = findVoice(lang, country, variant);
        return (match.first == null) ? null : match.first.name;
        /*
        if (lang.contains("eng")) {
            return "ParsAva_English";
        } else if (lang.contains("fas")) {
            return "ParsAva_Persian";
        }
        */

        //return null;
    }

    @Override
    public List<Voice> onGetVoices() {
        //LogUtils.w(TAG, "onGetVoices ");

        List<Voice> voices = new ArrayList<Voice>();
        ensureVoicesLoaded();
        for (ParsAvaVoice voice : mAvailableVoices.values()) {
            int quality = QUALITY_NORMAL;
            int latency = Voice.LATENCY_VERY_LOW;
            Locale locale = new Locale(voice.locale.getISO3Language(), voice.locale.getISO3Country(), voice.locale.getVariant());
            Set<String> features = onGetFeaturesForLanguage(locale.getLanguage(), locale.getCountry(), locale.getVariant());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                voices.add(new Voice(voice.name, voice.locale, quality, latency, false, features));
            }
        }
        return voices;
    }

    @Override
    public int onIsValidVoiceName(String voiceName) {
        LogUtils.w(TAG,"DualTtsService.onIsValidVoiceName voiceName : " + voiceName);
        switch (voiceName) {
            case "ParsAva_Persian":
            case "ParsAva_English":
                return TextToSpeech.SUCCESS;
        }
        return TextToSpeech.ERROR;
    }

    @Override
    public int onLoadVoice(String voiceName) {
        LogUtils.w(TAG,"DualService.onLoadVoice is called voiceName:" + voiceName);
        ensureVoicesLoaded();
        if (TextUtils.isEmpty(voiceName)) {
            voiceName = new Preferences(ExtendedApplication.getStorageContext()).getLastVoiceName();
        }
        ParsAvaVoice voice = mAvailableVoices.get(voiceName);
        if (voice == null) {
            voice = mAvailableVoices.get("ParsAva_Persian");
        }
        if (voice == null && !mAvailableVoices.isEmpty()) {
            voice = mAvailableVoices.values().iterator().next();
        }
        if (voice == null) {
            LogUtils.w(TAG,"onLoadVoice no voices available; reporting success to avoid fallback");
            return TextToSpeech.SUCCESS;
        }
        mMatchingVoice = voice;
        new Preferences(ExtendedApplication.getStorageContext())
                .setLastRequestedVoice(voice.locale.getISO3Language(), voice.locale.getISO3Country(), voice.locale.getVariant(), voice.name);
        return TextToSpeech.SUCCESS;
    }
    /*
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtils.w(TAG, "DualTtsService onBind called");

        return super.onBind(intent);
    }

     */
    @Override
    public void onDestroy(){
        synchronized (mLifecycleLock) {
            if (mIsDestroying) {
                LogUtils.w(TAG, "DualTtsService.onDestroy called while already tearing down");
                return;
            }
            mIsDestroying = true;
        }
        if (mXiaomiForegroundActive) {
            stopForeground(true);
            mXiaomiForegroundActive = false;
        }
        persistCurrentEnglishSelection("onDestroy");
        if(mEngine!=null){
            LogUtils.w(TAG,"DualTtsService.onDestroy called");
            mEngine.Shutdown();
            ExtendedApplication app = (ExtendedApplication)this.getApplication();
            app.DecEngineReferenceCount();
            mEngine = null;
        }
        super.onDestroy();
    }
    private int selectVoice(SynthesisRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final String name = request.getVoiceName();
            if (name != null && !name.isEmpty()) {
                return onLoadVoice(name);
            }
        }

        final int result = onLoadLanguage(request.getLanguage(), request.getCountry(), request.getVariant());
        switch (result) {
            case TextToSpeech.LANG_MISSING_DATA:
            case TextToSpeech.LANG_NOT_SUPPORTED:
                return TextToSpeech.ERROR;
        }
        return TextToSpeech.SUCCESS;
    }

}
