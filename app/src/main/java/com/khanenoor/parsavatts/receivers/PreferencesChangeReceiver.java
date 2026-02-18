package com.khanenoor.parsavatts.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.R;
import com.khanenoor.parsavatts.engine.SpeechSynthesis;
import com.khanenoor.parsavatts.impractical.Language;
import com.khanenoor.parsavatts.util.LogUtils;

public class PreferencesChangeReceiver extends BroadcastReceiver {
    private static final String TAG = PreferencesChangeReceiver.class.getSimpleName();
    private SpeechSynthesis mSpeechSynEngine = null;
    public void setSpeechSynthesis(SpeechSynthesis s){
        mSpeechSynEngine = s;
    }
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Preferences.CUSTOM_PREFERENCES_CHANGE_BROADCAST)) {

            String strCombineCode ;
            String selectedLanguage ;

            int nCombineCode=0;
            Language enmSelLang = Language.LANGUAGE_PERSIAN;
            LogUtils.w(TAG, "onReceive CUSTOM_PREFERENCES_CHANGE_BROADCAST");
            Bundle extras = intent.getExtras();
            if(extras == null) {
                return;
            } else {
                selectedLanguage= extras.getString(Preferences.INTENT_LANGUAGE_PARAM_KEY , "");
                strCombineCode= extras.getString(Preferences.INTENT_CHANGED_PARAM_KEY);
                try {
                    nCombineCode = Integer.parseInt(strCombineCode);
                    if (selectedLanguage.contains(context.getResources().getString(R.string.english_language))) {
                        enmSelLang = Language.LANGUAGE_ENGLISH;
                    }
                } catch(NumberFormatException nfe) {
                    LogUtils.w(TAG,"Could not parse " + nfe.getMessage());
                }
            }
            if(mSpeechSynEngine==null){
                return;
            }
            if((nCombineCode & Preferences.VOICE_CHANGE_ID) == Preferences.VOICE_CHANGE_ID){
                if(enmSelLang == Language.LANGUAGE_PERSIAN) {
                    String strPersianVoiceId = extras.getString(Preferences.PERSIAN_ENGINE_ID);
                    int nPersianVoiceId = 1;
                    if (strPersianVoiceId != null) {
                        try {
                            nPersianVoiceId = Integer.parseInt(strPersianVoiceId);
                        } catch (NumberFormatException nfe) {
                            LogUtils.w(TAG, "Could not parse " + nfe.getMessage());
                        }
                        mSpeechSynEngine.changePersianVoice(nPersianVoiceId);
                    }
                } else {
                    mSpeechSynEngine.ChangeEnglishVoice();
                }
            }
            if((nCombineCode & Preferences.PITCH_CHANGE_ID) == Preferences.PITCH_CHANGE_ID){
                if(enmSelLang == Language.LANGUAGE_PERSIAN) {
                    mSpeechSynEngine.mFaTts.mConfiureParams.mPitch = extras.getInt(Preferences.PERSIAN_ENGINE_PITCH);
                    mSpeechSynEngine.mFaTts.mConfiureParams.mIsPitchChange=true;
                }else {
                    mSpeechSynEngine.mEnTts.mConfiureParams.mPitch = extras.getInt(Preferences.ENG_ENGINE_PITCH);
                    mSpeechSynEngine.mEnTts.mConfiureParams.mIsPitchChange=true;

                }

            }
            if((nCombineCode & Preferences.SPEED_CHANGE_ID) == Preferences.SPEED_CHANGE_ID){
                if(enmSelLang == Language.LANGUAGE_PERSIAN) {
                    mSpeechSynEngine.mFaTts.mConfiureParams.mRate = extras.getInt(Preferences.PERSIAN_ENGINE_RATE);
                    mSpeechSynEngine.mFaTts.mConfiureParams.mIsRateChange=true;
                    LogUtils.w(TAG,"PreferenceChangeReceiver PersianLanguage value:"+mSpeechSynEngine.mFaTts.mConfiureParams.mRate);
                }else {
                    mSpeechSynEngine.mEnTts.mConfiureParams.mRate = extras.getInt(Preferences.ENG_ENGINE_RATE);
                    mSpeechSynEngine.mEnTts.mConfiureParams.mIsRateChange=true;
                    LogUtils.w(TAG,"PreferenceChangeReceiver EnglishLanguage value:"+mSpeechSynEngine.mEnTts.mConfiureParams.mRate);
                }

            }
            if((nCombineCode & Preferences.VOLUME_CHANGE_ID) == Preferences.VOLUME_CHANGE_ID){
                if(enmSelLang == Language.LANGUAGE_PERSIAN) {
                    mSpeechSynEngine.mFaTts.mConfiureParams.mVolume = extras.getInt(Preferences.PERSIAN_ENGINE_VOLUME);
                    mSpeechSynEngine.mFaTts.mConfiureParams.mIsVolumeChange=true;
                }else {
                    mSpeechSynEngine.mEnTts.mConfiureParams.mVolume = extras.getInt(Preferences.ENG_ENGINE_VOLUME);
                    mSpeechSynEngine.mEnTts.mConfiureParams.mIsVolumeChange=true;

                }
            }
            if((nCombineCode & Preferences.PUNCTUATION_MODE_CHANGE_ID) == Preferences.PUNCTUATION_MODE_CHANGE_ID) {
                int nValue  = extras.getInt(Preferences.TTS_PUNCTUATION_MODE);
                LogUtils.w(TAG,"PreferenceChangeReceiver PuncMode value:"+nValue);

                mSpeechSynEngine.mFaTts.changePunctionanMode(nValue);
            }
            if((nCombineCode & Preferences.NUMBER_MODE_CHANGE_ID) == Preferences.NUMBER_MODE_CHANGE_ID) {
                int nValue = extras.getInt(Preferences.TTS_NUMBER_MODE);
                LogUtils.w(TAG,"PreferenceChangeReceiver Number value:"+nValue);

                mSpeechSynEngine.mFaTts.changeNumberMode(nValue);
            }
            if((nCombineCode & Preferences.DIGITS_LANGUAGE_CHANGE_ID) == Preferences.DIGITS_LANGUAGE_CHANGE_ID) {
                int nValue = extras.getInt(Preferences.TTS_DIGITS_LANGUAGE);
                LogUtils.w(TAG, "PreferenceChangeReceiver Digit language value:" + nValue);

                mSpeechSynEngine.mFaTts.changeDigitsLanguage(nValue);
            }
            if((nCombineCode & Preferences.EMOJI_MODE_CHANGE_ID) == Preferences.EMOJI_MODE_CHANGE_ID) {
                int nValue = extras.getInt(Preferences.TTS_EMOJI_MODE);
                LogUtils.w(TAG,"PreferenceChangeReceiver Number value:"+nValue);

                mSpeechSynEngine.mFaTts.changeEmojiMode(nValue);
            }
        }
    }
}

