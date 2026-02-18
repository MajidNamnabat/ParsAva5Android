package com.khanenoor.parsavatts.impractical;

import android.content.Context;

import com.khanenoor.parsavatts.Preferences;

;
public class SpeechSynthesisConfigure {
    public int mPitch;
    public int mRate;
    public int mVolume;
    public boolean mIsPitchChange = true;
    public boolean mIsRateChange = true;
    public boolean mIsVolumeChange = true;

    public SpeechSynthesisConfigure(Context c , Language language){
        Preferences prefs = new Preferences(c);
        if(language== Language.LANGUAGE_PERSIAN){
            mPitch = prefs.getPersianPitch();
            mRate = prefs.getPersianRate();
            mVolume = prefs.getPersianVolume();

        } else if (language == Language.LANGUAGE_ENGLISH){
            mPitch = prefs.getEnglishPitch();
            mRate = prefs.getEnglishRate();
            mVolume = prefs.getEnglishVolume();

        }
    }


}
