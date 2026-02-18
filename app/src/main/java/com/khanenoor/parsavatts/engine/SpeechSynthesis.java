package com.khanenoor.parsavatts.engine;

import static android.media.AudioFormat.ENCODING_PCM_16BIT;

import android.content.Context;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.speech.tts.TextToSpeech;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.khanenoor.parsavatts.Lock;
import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.impractical.AudioBufferObservable;
import com.khanenoor.parsavatts.receivers.PreferencesChangeReceiver;
import com.khanenoor.parsavatts.util.LogUtils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class SpeechSynthesis  {
    public enum StopReason {
        NEW_REQUEST,
        FLUSH,
        SERVICE_SHUTDOWN,
        PREFERENCE_CHANGE,
        ERROR_RECOVERY,
        TALKBACK_REQUEST,
        TIMEOUT,
        UNKNOWN
    }
    final int VOICE0_SINA_SAMPLE_RATE = 16000;
    final int VOICE1_MINA_SAMPLE_RATE = 22050;
    private static final String TAG = SpeechSynthesis.class.getSimpleName();
    private static final long STOP_SUMMARY_INTERVAL_MS = 60_000L;
    private static final Map<StopReason, Integer> STOP_REASON_COUNTS = new EnumMap<>(StopReason.class);
    private static long sLastStopSummaryTimestamp = 0L;

    public static final int GENDER_MALE = 0;    //Sina
    public static final int GENDER_FEMALE = 1;  //Mina
    private boolean mInitialized = false;
    public FaTts mFaTts = null;
    public EnTts mEnTts = null;
    public AudioBufferObservable mAudioBufferCommon = new AudioBufferObservable();
    
    private  PreferencesChangeReceiver mPrefChangeReceiver = null;
    private int mPersianVoiceId = 1;
    private Context mContext=null;

    private int resolveStoredPersianVoiceId(Context context) {
        try {
            final Preferences prefs = new Preferences(context);
            final int storedVoiceId = prefs.getPersianVoiceId();
            if (storedVoiceId == GENDER_MALE || storedVoiceId == GENDER_FEMALE) {
                return storedVoiceId;
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "Falling back to default Persian voice", e);
        }

        return GENDER_FEMALE;
    }
    /**
     * Constructor; pass a language code such as "en" for English.
     */
    public SpeechSynthesis(Context context, FaTts.SynthReadyCallback callback , String packageName, AssetManager am) {

        if (mInitialized) {
            return;
        }
        //not define TalkBackService
        //TalkBackService mService;
        mContext=context;

        mFaTts = new FaTts(packageName,callback,context);
        mEnTts = new EnTts(callback,context);
        // function espeak_Initialize called in nativeCreate so initialize must be done here
        /*if(!mFaTts.isInstalled()){
            mFaTts.Install(am);
        }*/
        mPersianVoiceId = resolveStoredPersianVoiceId(context);
        mInitialized = mFaTts.Load(context,3,mPersianVoiceId,4,1);
        if(!mInitialized){
            LogUtils.e(TAG, "Failed to initialize speech synthesis library");
            return;
        }
        mEnTts.Load(context);
        mEnTts.mUtteranceProgressListener.setmSpeechSynEngine(this);
        //IntentFilter filter = new IntentFilter(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
        //Bundle args = new Bundle();
        //args.putSerializable("FaTts",(Serializable)mFaTts);
        //filter.putExtra("DATA",args);
        //context.registerReceiver(receiver, filter);
        ////////////////////////// PreferencesChangeReceiver
        mPrefChangeReceiver = new PreferencesChangeReceiver();
        mPrefChangeReceiver.setSpeechSynthesis(this);
        IntentFilter filter = new IntentFilter(Preferences.CUSTOM_PREFERENCES_CHANGE_BROADCAST);
        //Bundle args = new Bundle();
        LogUtils.w(TAG, "register CUSTOM_PREFERENCES_CHANGE_BROADCAST");

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mPrefChangeReceiver,filter);
        //OR
        /*
        LocalBroadcastManager.getInstance(this)
        .registerReceiver(mPrefChangeReceiver,
                new IntentFilter(com.khanenoor.parsavatts.custombroadcasts.CUSTOM_PREFERENCES_CHANGE_BROADCAST));
        */
        String phoneCode = Lock.getHardwareCode(context);
        String appLicenseUniqueId = Lock.Generate_Or_Get_App_UUID(packageName,context,false);
        String hardwareAppId = Lock.getCombineHardAppId(packageName,context,phoneCode,appLicenseUniqueId);
        Lock.SetHardwareAppID(hardwareAppId,packageName);
    }
    public int getSampleRate() {
        if(mFaTts.getVoice()==0) {
            return VOICE0_SINA_SAMPLE_RATE;
        }
        else {
            return VOICE1_MINA_SAMPLE_RATE;
        }
    }

    public int getChannelCount() {
        //always is One channel
        //   CHANNEL_COUNT_MONO = 1,
        //return 1;
        return 1;
    }

    public int getAudioFormat() {
        //Always is 16 Bit
        //ENCODING_PCM_16BIT = 0x02, in com_googlecode_eyesfree_espeak_eSpeakService

            return ENCODING_PCM_16BIT;
    }
    public void setLanguage(String language, int variant) {
        //nativeSetLanguage(language, variant);
    }
    public void setRate(int rate) {
        if (mFaTts == null || mFaTts.mConfiureParams == null) {
            return;
        }
        mFaTts.mConfiureParams.mRate = rate;
        if (mFaTts.getSpeechRateValue() != rate) {
            mFaTts.mConfiureParams.mIsRateChange = true;
        }
        if (mFaTts.mConfiureParams.mIsRateChange) {
            LogUtils.w(TAG, "SpeechSynthesis.setRate rate:" + mFaTts.mConfiureParams.mRate);
            mFaTts.applySpeechRate(rate, mFaTts.mConfiureParams.mRate);
            mFaTts.mConfiureParams.mIsRateChange = false;
        }

    }

    public void setPitch(int pitch) {
        if (mFaTts == null || mFaTts.mConfiureParams == null) {
            return;
        }
        mFaTts.mConfiureParams.mPitch = pitch;
        if (mFaTts.getPitchValue() != pitch) {
            mFaTts.mConfiureParams.mIsPitchChange = true;
        }
        if (mFaTts.mConfiureParams.mIsPitchChange) {
            mFaTts.applyPitch(pitch, mFaTts.mConfiureParams.mPitch);
            mFaTts.mConfiureParams.mIsPitchChange = false;
        }

    }
    public void setVolume(float volume){
        if(mFaTts.mConfiureParams.mIsVolumeChange){
            mFaTts.setVolume(volume,mFaTts.mConfiureParams.mVolume);
            mFaTts.mConfiureParams.mIsVolumeChange=false;
        }

    }

    /*
    It is not possible for two invocations of synchronized methods on the
    same object to interleave. When one thread is executing a
    synchronized method for an object, all other threads that invoke
    synchronized methods for the same object block (suspend execution)
    until the first thread is done with the object.
    When a synchronized method exits,
    it automatically establishes a happens-before
    relationship with any subsequent invocation of a synchronized method
    for the same object.
    This guarantees that changes to the state of the object are visible to all threads.
    when TalkBack is run change of Mina and Sina sometimes cause SigFault
    */

    public synchronized void changePersianVoice(int voiceId){
        if(voiceId==mPersianVoiceId)
            return;
        try {
            LogUtils.w(TAG,"SpeechSynthesis.changePersianVoice threadId: "+ Thread.currentThread().getId() + " voiceId:" + voiceId +" hashCode:" + this.hashCode());
            mPersianVoiceId = voiceId;
            mFaTts.mConfiureParams.mIsRateChange = true;
            mFaTts.mConfiureParams.mIsPitchChange = true;
            mFaTts.mConfiureParams.mIsVolumeChange = true;
        }catch(Exception e){
            LogUtils.w(TAG,"SpeechSynthesis.changePersianVoice catch" + e.getMessage());
        }
    }
    public void ChangeEnglishVoice() {
        mEnTts.ChangeVoice(mContext);
    }
    /**
     * Stops and clears the AudioTrack.
     */
    public final void stop() {
        stop(StopReason.UNKNOWN, "legacy", null);
    }

    public final void stop(@NonNull StopReason reason, @NonNull String caller) {
        stop(reason, caller, null);
    }

    public final void stop(@NonNull StopReason reason,
                           @NonNull String caller,
                           @Nullable String identifier) {
        final long now = System.currentTimeMillis();
        final long threadId = Thread.currentThread().getId();
        final int bufferCount = mAudioBufferCommon != null ? mAudioBufferCommon.getBufferedCount() : -1;
        final String faVoice = mFaTts != null ? String.valueOf(mFaTts.getVoice()) : "unknown";
        final String enEngine = (mEnTts != null && mEnTts.getEnTts() != null)
                ? mEnTts.getEnTts().getDefaultEngine()
                : "uninitialized";
        final String formattedIdentifier = identifier == null ? "" : ", id=" + identifier;

        // Keep the log concise but structured to help TalkBack debugging without flooding output.
        LogUtils.w(TAG,
                "SpeechSynthesis.stop reason=" + reason
                        + ", caller=" + caller
                        + formattedIdentifier
                        + ", ts=" + now
                        + ", thread=" + threadId
                        + ", faVoiceId=" + faVoice
                        + ", enEngine=" + enEngine
                        + ", queueSize=" + bufferCount);

        recordStopReason(reason, now);

        mFaTts.StopTts();
        //nativeStop();
        mEnTts.Stop();
        LogUtils.w(TAG,"SpeechSynthesis.stop mEnTts Stop is requested");
    }

    private void recordStopReason(@NonNull StopReason reason, long now) {
        synchronized (STOP_REASON_COUNTS) {
            int updatedCount = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                updatedCount = STOP_REASON_COUNTS.getOrDefault(reason, 0) + 1;
            }
            STOP_REASON_COUNTS.put(reason, updatedCount);

            // Aggregate counts on a coarse interval so TalkBack users can correlate stop bursts
            // without drowning logs during rapid focus changes.
            if (now - sLastStopSummaryTimestamp >= STOP_SUMMARY_INTERVAL_MS) {
                sLastStopSummaryTimestamp = now;
                LogUtils.i(TAG, "Stop reason summary (throttled): " + STOP_REASON_COUNTS);
            }
        }
    }
    /**
     * Synthesize speech and speak it directly using AudioTrack.
     * final Object object = new Object();
     * synchronized(object) {
     * can also used
     * }
     */
    public synchronized int speak(String text , String language,boolean isText) throws UnsupportedEncodingException {
        //LogUtils.w(TAG, "SpeechSynthesis1.speak language:" + language + " text:" + text);
        //LogUtils.w(TAG,"SpeechSynthesis.speak threadId: "+ Thread.currentThread().getId() + " hashCode:" + this.hashCode());
        if (Thread.interrupted()) {
            LogUtils.i(TAG, "SpeechSynthesis.speak interrupted before processing");
            Thread.currentThread().interrupt();
            return TextToSpeech.ERROR;
        }
        if(text.length()>0){
            mFaTts.ParsTextNLP(mFaTts.getNlpHand(),text.getBytes("UTF-32"),isText);
            if (Thread.interrupted()) {
                LogUtils.i(TAG, "SpeechSynthesis.speak interrupted after ParsTextNLP");
                Thread.currentThread().interrupt();
                return TextToSpeech.ERROR;
            }
        }
        return 1;
    }

    /**
     * Synthesize speech to a file. The current implementation writes a valid WAV
     * file to the given path, assuming it is writable. Something like
     * "/sdcard/???.wav" is recommended.
     */
    public final void synthesizeToFile(String text, String filename){

    }
    public void Shutdown() {
        if(mInitialized) {
            LogUtils.w(TAG, "SpeechSynthesis.Shutdown");
            mFaTts.Unload();
            mEnTts.Unload();

            LocalBroadcastManager.getInstance(mContext)
                    .unregisterReceiver(mPrefChangeReceiver);
            mInitialized = false;
        }

    }
    /*public interface SynthReadyCallback {
        void onSynthDataReady(byte[] audioData);

        void onSynthDataComplete();
    }*/
    /**
     * Attempts a partial match against a query locale.
     *
     * @param query The locale to match.
     * @return A text-to-speech availability code. One of:
     *         <ul>
     *         <li>{@link TextToSpeech#LANG_NOT_SUPPORTED}
     *         <li>{@link TextToSpeech#LANG_AVAILABLE}
     *         <li>{@link TextToSpeech#LANG_COUNTRY_AVAILABLE}
     *         <li>{@link TextToSpeech#LANG_COUNTRY_VAR_AVAILABLE}
     *         </ul>
     */
    public int match(Locale query,Locale locale) {

        if (!locale.getISO3Language().equals(query.getISO3Language())) {
            return TextToSpeech.LANG_NOT_SUPPORTED;
        } else if (!locale.getISO3Country().equals(query.getISO3Country())) {
            return TextToSpeech.LANG_AVAILABLE;
        } else if (!locale.getVariant().equals(query.getVariant())) {
            return TextToSpeech.LANG_COUNTRY_AVAILABLE;
        } else {
            return TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
        }
    }
    public byte[] Resample(byte[] dataIn , int inSampleFreq , int outSampleFreq , int intChannels, int outChannels,float volumeRatio) {
        if (mEnTts == null || !mEnTts.isResamplerInitialized()) {
            return dataIn;
        }
        return mEnTts.resample(dataIn,inSampleFreq,outSampleFreq,intChannels,outChannels, volumeRatio);
    }
    public ByteBuffer ResampleNative(ByteBuffer dataIn , int inSampleFreq , int outSampleFreq , int intChannels, int outChannels, float volumeRatio) {
        if (mEnTts == null || !mEnTts.isResamplerInitialized()) {
            return dataIn;
        }
        return mEnTts.resampleNative(dataIn,inSampleFreq,outSampleFreq,intChannels,outChannels, volumeRatio);
    }

    //https://stackoverflow.com/questions/50153168/how-to-enable-text-to-speech-talkback-from-another-app-in-android
    /*
    public static void enableTalkBack()
    {
        try {
            AccessibilityManager am = (AccessibilityManager)(Extension.mainContext.getSystemService(Context.ACCESSIBILITY_SERVICE));
            List<AccessibilityServiceInfo> services = am.getInstalledAccessibilityServiceList();

            if (services.isEmpty()) {
                return;
            }

            AccessibilityServiceInfo service = services.get(0);

            boolean enableTouchExploration = (service.flags
                    & AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE) != 0;
            // Try to find a service supporting explore by touch.
            if (!enableTouchExploration) {
                final int serviceCount = services.size();
                for (int i = 1; i < serviceCount; i++) {
                    AccessibilityServiceInfo candidate = services.get(i);
                    if ((candidate.flags & AccessibilityServiceInfo
                            .FLAG_REQUEST_TOUCH_EXPLORATION_MODE) != 0) {
                        enableTouchExploration = true;
                        service = candidate;
                        break;
                    }
                }
            }

            ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            String enabledServiceString = componentName.flattenToString();
            ContentResolver resolver = Extension.mainContext.getContentResolver();

            Settings.Secure.putString(resolver, "enabled_accessibility_services", enabledServiceString);
            Settings.Secure.putString(resolver,
                    "touch_exploration_granted_accessibility_services",
                    enabledServiceString);
            if (enableTouchExploration) {
                Settings.Secure.putInt(resolver, "touch_exploration_enabled", 1);
            }
            Settings.Secure.putInt(resolver, "accessibility_script_injection", 1);
            Settings.Secure.putInt(resolver, "accessibility_enabled", 1);
        }
        catch(Exception e) {
            LogUtils.e("Device", "Failed to enable accessibility: " + e);
        }
    }

     */

}
