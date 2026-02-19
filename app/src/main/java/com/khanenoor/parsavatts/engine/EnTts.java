package com.khanenoor.parsavatts.engine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.impractical.Language;
import com.khanenoor.parsavatts.impractical.ParsAvaUtteranceProgressListener;
import com.khanenoor.parsavatts.impractical.SpeechSynthesisConfigure;
import com.khanenoor.parsavatts.util.LogUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class EnTts {
    private static final String TAG = EnTts.class.getSimpleName();
    private static final String ENGLISH_UTTERANCE_PREFIX = "ParsAva_English_Engine_";
    //can be placed in R.strings
    public TextToSpeech mEnTts = null;
    public ParsAvaUtteranceProgressListener mUtteranceProgressListener= null;
    public SpeechSynthesisConfigure mConfiureParams = null;
    private boolean mResamplerInitialized = false;

    //There was bug when TalkBack is active and change voice english
    private boolean mTtsInitialized = false;
    private Context mContext = null;
    private final AtomicInteger mUtteranceCounter = new AtomicInteger(0);
    private String mPreferredEngineId = "";
    private String mCurrentEnginePackage = "";
    private int mEngineFailureCount = 0;
    private boolean mPreferAccessibilityRouting = false;
    private AudioAttributes mDtmfAudioAttributes;
    private AudioAttributes mAccessibilityAudioAttributes;
    public EnTts( FaTts.SynthReadyCallback callback , Context context){

       // package_name = pck_name;
        mContext = context;
        //setThisInNative();
        mConfiureParams = new SpeechSynthesisConfigure(context, Language.LANGUAGE_ENGLISH);
        mTtsInitialized=false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mDtmfAudioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setLegacyStreamType(AudioManager.STREAM_DTMF)
                    .build();
            mAccessibilityAudioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build();
        }
    }

    private void hydratePreferredSelections(Context context) {
        Preferences pref = new Preferences(context);
        mPreferredEngineId = pref.getEnglishVoiceName();
        mPreferredEngineId = sanitizeEngineId(mPreferredEngineId);
        String mPreferredVoiceName = pref.getLastVoiceName();
        LogUtils.i(TAG, "Hydrated preferred English engine=" + mPreferredEngineId + " voice=" + mPreferredVoiceName);
    }

    public static String getDefaultEnglishEngine(Context cnx){
        Preferences pref = new Preferences(cnx);
        String englishVoiceName = pref.get(Preferences.ENG_ENGINE_NAME, "");
        if (TextUtils.isEmpty(englishVoiceName) || englishVoiceName.equals(cnx.getPackageName())) {
            LogUtils.w(TAG, "English engine preference empty or self; resolving default selection");
            String resolved = Preferences.selectAndStoreEnglishEngine(cnx, Preferences.getEngines(cnx));
            if (TextUtils.isEmpty(resolved)) {
                resolved = Preferences.findInstalledExternalEnglishEngine(cnx);
            }
            return resolved;
        }
        return englishVoiceName;
    }

    private boolean isEngineInstalled(Context context, String enginePackageName) {
        if (TextUtils.isEmpty(enginePackageName) || context == null) {
            return false;
        }
        try {
            context.getPackageManager().getPackageInfo(enginePackageName, 0);
            return true;
        } catch (Exception e) {
            LogUtils.w(TAG, "Engine package not installed: " + enginePackageName);
            return false;
        }
    }

    private void notifyEngineSwitch(String enginePackageName, String reason) {
        LogUtils.w(TAG, "Switching English engine to " + enginePackageName + " because " + reason);
        //Toast.makeText(mContext, "Switching English TTS to " + enginePackageName, Toast.LENGTH_SHORT).show();
    }

    public void Load(Context context) {
        hydratePreferredSelections(context);
        String  engine_package_name = sanitizeEngineId(!TextUtils.isEmpty(mPreferredEngineId)
                ? mPreferredEngineId
                : getDefaultEnglishEngine(context));
        if (TextUtils.isEmpty(engine_package_name)) {
            engine_package_name = sanitizeEngineId(getDefaultEnglishEngine(context));
        }
        if (TextUtils.isEmpty(engine_package_name)) {
            engine_package_name = sanitizeEngineId(Preferences.findInstalledExternalEnglishEngine(context));
        }
        if (!isEngineInstalled(context, engine_package_name)) {
            LogUtils.w(TAG, "Preferred English engine missing; using default");
            engine_package_name = sanitizeEngineId(getDefaultEnglishEngine(context));
        }
        if (TextUtils.isEmpty(engine_package_name)) {
            engine_package_name = sanitizeEngineId(Preferences.findInstalledExternalEnglishEngine(context));
        }
        if (TextUtils.isEmpty(engine_package_name)) {
            engine_package_name = null;
        }
        notifyEngineSwitch(engine_package_name != null ? engine_package_name : "system-default",
                "initial load");
        mEnTts = new TextToSpeech(context,ttsInitListener,engine_package_name);
        mCurrentEnginePackage = engine_package_name;
        mTtsInitialized=false;
        mEngineFailureCount = 0;
        mUtteranceProgressListener = new ParsAvaUtteranceProgressListener();
        LogUtils.w(TAG,"EnTts Load Finished package:" + engine_package_name);
    }
    public void setOnUtteranceProgressListener(SynthesisCallback callback){
        if(mEnTts==null){
            LogUtils.w(TAG,"EnTts.setOnUtteranceProgressListener mEnTts==null");
        }
        mUtteranceProgressListener.setSynthesisCallback(callback);
        mEnTts.setOnUtteranceProgressListener(mUtteranceProgressListener);
    }
    public void ChangeVoice(Context context){
        mTtsInitialized = false;
        Unload();
        hydratePreferredSelections(context);
        String  engine_package_name = sanitizeEngineId(!TextUtils.isEmpty(mPreferredEngineId)
                ? mPreferredEngineId
                : getDefaultEnglishEngine(context));
        if (TextUtils.isEmpty(engine_package_name)) {
            engine_package_name = sanitizeEngineId(getDefaultEnglishEngine(context));
        }
        if (TextUtils.isEmpty(engine_package_name)) {
            engine_package_name = sanitizeEngineId(Preferences.findInstalledExternalEnglishEngine(context));
        }
        if (!isEngineInstalled(context, engine_package_name)) {
            LogUtils.w(TAG, "Preferred English engine missing during ChangeVoice; falling back to default");
            engine_package_name = sanitizeEngineId(getDefaultEnglishEngine(context));
        }
        if (TextUtils.isEmpty(engine_package_name)) {
            engine_package_name = sanitizeEngineId(Preferences.findInstalledExternalEnglishEngine(context));
        }
        if (TextUtils.isEmpty(engine_package_name)) {
            engine_package_name = null;
        }
        notifyEngineSwitch(engine_package_name != null ? engine_package_name : "system-default",
                "ChangeVoice requested");
        LogUtils.w(TAG,"EnTts.ChangeVoice " + engine_package_name);
        mEnTts = new TextToSpeech(context,ttsInitListener,engine_package_name);
        mCurrentEnginePackage = engine_package_name;
        mEnTts.setOnUtteranceProgressListener(mUtteranceProgressListener);
        mTtsInitialized = false;
        mConfiureParams.mIsRateChange=true;
        mConfiureParams.mIsPitchChange=true;
        mConfiureParams.mIsVolumeChange=true;
        if (mUtteranceProgressListener != null) {
            mUtteranceProgressListener.resetUtteranceTracking("ChangeVoice");
        }
    }
    public void Unload(){
        try {
            LogUtils.w(TAG,"EnTts Unload called");
            mTtsInitialized = false;
            mEnTts.stop();
            mEnTts.shutdown();
        }  catch (Exception ex) {
            LogUtils.e(TAG, " Error in Unload " + ex.getMessage());
        }
    }

    /**
     * Once the TTS is initialized, loads the earcons and plays the Marvin intro
     * clip. ("Here I am, brain the size of a planet ...")
     */
    /*
    private final UtteranceProgressListener mUtterListene = new UtteranceProgressListener(){

        @Override
        public void onStart(String utteranceId) {
            LogUtils.w(TAG, " mUtterListene.onStart " + utteranceId );

        }

        public void onDone(String utteranceId) {
            if (mCallback == null)
                return ;
            LogUtils.w(TAG, " mUtterListene.onDone " + utteranceId );

           //mCallback.onSynthDataComplete();
        }

        @Override
        public void onError(String utteranceId) {

        }
    };
    */

    private final TextToSpeech.OnInitListener ttsInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                mTtsInitialized = true;
                mEngineFailureCount = 0;
                /*
                    String pkgName = MarvinShell.class.getPackage().getName();
                    tts.addSpeech(
                            getString(R.string.marvin_intro_snd_), pkgName, R.raw.marvin_intro);
                    tts.addEarcon(getString(R.string.earcon_tock), pkgName, R.raw.tock_snd);
                    tts.addEarcon(getString(R.string.earcon_tick), pkgName, R.raw.tick_snd);
                    tts.speak(
                            getString(R.string.marvin_intro_snd_), TextToSpeech.QUEUE_FLUSH, null);
                    */
            }
            else {
                mTtsInitialized = false;
            }
        }
    };
    public void Stop()
    {
        if(!mTtsInitialized)
        {
            LogUtils.w(TAG,"EnTts.Stop !mTtsInitialized EnglishTts");
            return;
        }
        mEnTts.stop();
    }

    public void updateAudioRoutingForTalkBack(boolean preferAccessibilityRouting, String reason) {
        mPreferAccessibilityRouting = preferAccessibilityRouting;
        applyAudioAttributesForRouting();
        LogUtils.i(TAG, "English routing "
                + (preferAccessibilityRouting ? "accessibility/STREAM_MUSIC" : "DTMF")
                + " reason=" + reason);
    }

    private void applyAudioAttributesForRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mEnTts != null) {
            final AudioAttributes attributes = mPreferAccessibilityRouting
                    ? mAccessibilityAudioAttributes
                    : mDtmfAudioAttributes;
            if (attributes != null) {
                mEnTts.setAudioAttributes(attributes);
            }
        }
    }

    private int resolveStreamType() {
        return mPreferAccessibilityRouting ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_DTMF;
    }
    @SuppressLint("ObsoleteSdkInt")
    public void Speak(String text){
        Speak(text, false);
    }
    @SuppressLint("ObsoleteSdkInt")
    public void Speak(String text, boolean forceFlush){
        final String utteranceId = ENGLISH_UTTERANCE_PREFIX + mUtteranceCounter.incrementAndGet();
        final int queueMode = forceFlush ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
        if (mUtteranceProgressListener != null) {
            mUtteranceProgressListener.onSpeakCalled(
                    utteranceId,
                    "EnTts.Speak invoked id=" + utteranceId + " queueMode=" + (forceFlush ? "QUEUE_FLUSH" : "QUEUE_ADD"));
        }
        if(!mTtsInitialized || mEnTts==null)
        {
            LogUtils.w(TAG,"EnTts.Speak !mTtsInitialized EnglishTts");
            if (mUtteranceProgressListener != null) {
                mUtteranceProgressListener.forceIdle("Speak called before init");
            }
            return;
        }
        applyAudioAttributesForRouting();
        //LogUtils.w(TAG,"EnTts.Speak Rate:" + Integer.toString(mConfiureParams.mRate) + " Pitch:" + Integer.toString(mConfiureParams.mPitch));
        //Default is 100 for rate , 100 for pitch
        try {
            //LogUtils.w(TAG, "EnTts.Speak called text:"+ text);
            if(mEnTts.isLanguageAvailable(Locale.ENGLISH) == TextToSpeech.LANG_AVAILABLE) {
                int result = mEnTts.setLanguage(Locale.ENGLISH);
                //LogUtils.w(TAG, "EnTts.Speak setLanguage " + result);
                mEngineFailureCount = 0;
            }else
            {
                //In Xiamomi when close recent apps speak was stoped and not work again until kill apps پارس آوا is called
                //In Version 3930 In Xiaomi MIUI , when close recent apps
                //Text to speech request
                //Asked to disconnect from ComponentInfo{com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSService}
                //from En Engine
                //Displayed as level:info
                //So EnTts disconnected and mEngine.mEnTts.mUtteranceProgressListener.mSpeakProgressState remind onFirstAudioPacketReceived not Idle
                //So we need to determine deadlock on it

                LogUtils.w(TAG,"EnTts.isLanguageAvailable return false");
                handleLanguageUnavailable("Language unavailable");
                return;

            }
            if (mConfiureParams.mIsRateChange) {
                float convertedSpeechRate = (float) ((4.0F * mConfiureParams.mRate) / 100.0F);
                LogUtils.w(TAG, "EnTts.Speak isRateChanged SetRate:" + Float.toString(convertedSpeechRate));
                mEnTts.setSpeechRate(convertedSpeechRate);
                mConfiureParams.mIsRateChange = false;
                //LogUtils.w(TAG, "EnTts.Speeak isRateChanged  setSpeechRate mRate:" + mConfiureParams.mRate);
            }
            if (mConfiureParams.mIsPitchChange) {
                float convertedPitch = (float) ((2.0F * mConfiureParams.mPitch) / 100.0F);
                mEnTts.setPitch(convertedPitch);
                mConfiureParams.mIsPitchChange = false;
                LogUtils.w(TAG, "EnTts.Speak isPitchChanged SetPitch:" + Float.toString(convertedPitch));

            }
            if (Build.VERSION.SDK_INT < 21) {
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("utteranceId", utteranceId);
                //in experience when default stream used, talkback monitor stream music, call , ...
                // an notification create and talkback interrupt called so speak call stop .
                hashMap.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(resolveStreamType()));
                hashMap.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "0.0"); // change the 0.0 to any value from 0-1 (1 is default)
                mEnTts.speak(text, queueMode, hashMap);
            } else {
                Bundle params = new Bundle();
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, resolveStreamType());
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.0f); // change the 0.5f to any value from 0f-1f (1f is default)
                mEnTts.speak(text, queueMode, params, utteranceId);
            }
            LogUtils.w(TAG, "EnTts.Speak dispatched id=" + utteranceId + " queueMode="
                    + (queueMode == TextToSpeech.QUEUE_FLUSH ? "QUEUE_FLUSH" : "QUEUE_ADD")
                    + " text=\"" + text + "\"");
        }catch(Exception ex){
            LogUtils.w(TAG,"EnTts Speak Exception:" + ex.getMessage());
            if (mUtteranceProgressListener != null) {
                mUtteranceProgressListener.forceIdle("Speak exception");
            }
        }
    }
    public TextToSpeech getEnTts(){
        return mEnTts;
    }

    public String getCurrentEnginePackage() {
        return mCurrentEnginePackage;
    }

    private void handleLanguageUnavailable(String reason) {
        final boolean hasPreferred = !TextUtils.isEmpty(mPreferredEngineId);
        final boolean preferredInstalled = hasPreferred && isEngineInstalled(mContext, mPreferredEngineId);
        if (hasPreferred && preferredInstalled && mEngineFailureCount == 0) {
            rebuildTextToSpeechEngine(mPreferredEngineId, reason + " retry preferred");
            if (mUtteranceProgressListener != null) {
                mUtteranceProgressListener.forceIdle(reason);
            }
            return;
        }
        rebuildTextToSpeechEngine(getDefaultEnglishEngine(mContext), reason + " fallback default");
        if (mUtteranceProgressListener != null) {
            mUtteranceProgressListener.forceIdle(reason);
        }
    }

    private void rebuildTextToSpeechEngine(String engineOverride, String reason) {
        hydratePreferredSelections(mContext);
        String enginePackageName = sanitizeEngineId(!TextUtils.isEmpty(engineOverride)
                ? engineOverride
                : (!TextUtils.isEmpty(mPreferredEngineId) ? mPreferredEngineId : getDefaultEnglishEngine(mContext)));
        if (TextUtils.isEmpty(enginePackageName)) {
            enginePackageName = sanitizeEngineId(getDefaultEnglishEngine(mContext));
        }
        if (!isEngineInstalled(mContext, enginePackageName)) {
            LogUtils.w(TAG, "Requested English engine missing during rebuild; falling back to system default");
            enginePackageName = sanitizeEngineId(getDefaultEnglishEngine(mContext));
        }
        if (TextUtils.isEmpty(enginePackageName)) {
            enginePackageName = sanitizeEngineId(Preferences.findInstalledExternalEnglishEngine(mContext));
        }
        if (mEnTts != null && mTtsInitialized && TextUtils.equals(mCurrentEnginePackage, enginePackageName)) {
            LogUtils.w(TAG, "Skipping English TTS rebuild; current engine already active: " + enginePackageName);
            return;
        }
        notifyEngineSwitch(enginePackageName != null ? enginePackageName : "system-default", reason);
        LogUtils.w(TAG, "Rebuilding English TTS engine for package: " + enginePackageName);
        try {
            if (mEnTts != null) {
                mEnTts.stop();
                mEnTts.shutdown();
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "Exception while resetting TTS before rebuild: " + e.getMessage());
        }
        mTtsInitialized = false;
        mEngineFailureCount++;
        mEnTts = new TextToSpeech(mContext, ttsInitListener, enginePackageName);
        mCurrentEnginePackage = enginePackageName;
        mEnTts.setOnUtteranceProgressListener(mUtteranceProgressListener);
        if (mUtteranceProgressListener != null) {
            mUtteranceProgressListener.resetUtteranceTracking("rebuildTextToSpeechEngine");
        }
    }
    public boolean isResamplerInitialized() {
        return mResamplerInitialized;
    }
    public void initResampler(int inSampleFreq, int outSampleFreq, int intChannels, int outChannels) {
        loadResample(inSampleFreq, outSampleFreq, intChannels, outChannels);
        mResamplerInitialized = true;
    }
    public void releaseResampler() {
        if (!mResamplerInitialized) {
            return;
        }
        unloadResample();
        mResamplerInitialized = false;
    }
    public native byte[] resample(byte[] dataIn,int inSampleFreq , int outSampleFreq , int intChannels, int outChannels,float volumeRatio);
    public native ByteBuffer resampleNative(ByteBuffer dataIn,int inSampleFreq , int outSampleFreq , int intChannels, int outChannels,float volumeRatio);
    public native void unloadResample();
    public native void loadResample(int inSampleFreq,
                                    int outSampleFreq,
                                    int intChannels,
                                    int outChannels);
    //public native void firstAudioPacketReceived();
    //public static native void reportAudioBufferLevel(int bufferedCount, int capacity);

    @SuppressWarnings("unused")
    private int nativeSpeakEngCallback(String text) {
        //LogUtils.w(TAG, " nativeSpeakEngCallback text:" + text );

        //setBusy(true);
        Speak(text);
        return 0;

    }
    //public native void setBusy(boolean isBusy);
    //public native boolean getBusy();

    private String sanitizeEngineId(String enginePackageName) {
        if (mContext != null && TextUtils.equals(enginePackageName, mContext.getPackageName())) {
            LogUtils.w(TAG, "Ignoring ParsAva package for English TTS engine selection");
            return "";
        }
        return enginePackageName;
    }
}
