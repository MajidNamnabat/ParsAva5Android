package com.khanenoor.parsavatts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import com.khanenoor.parsavatts.PreferenceStorage;
import com.khanenoor.parsavatts.impractical.TtsEngineInfo;
import com.khanenoor.parsavatts.util.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Preferences {
    private static final String TAG = Preferences.class.getSimpleName();
    public static final String CUSTOM_PREFERENCES_CHANGE_BROADCAST = "com.khanenoor.parsavatts.custombroadcasts.CUSTOM_CHANGE_PREFERENCE_BROADCAST";
    public static final String ENG_ENGINE_NAME = "eng_engine_name";
    public static final String ENG_ENGINE_PITCH = "eng_engine_pitch";
    public static final String ENG_ENGINE_RATE = "eng_engine_rate";
    public static final String ENG_ENGINE_VOLUME = "eng_engine_volume";
    private static final String ELOQUENCE_PACKAGE_NAME = "es.codefactory.eloquencetts";
    private static final String ESPEAK_PACKAGE_NAME = "com.redzoc.ramees.tts.espeak";
    private static final String GOOGLE_PACKAGE_NAME = "com.google.android.tts";

    public static final String PERSIAN_ENGINE_ID = "persian_engine_id";
    public static final String PERSIAN_ENGINE_PITCH = "persian_engine_pitch";
    public static final String PERSIAN_ENGINE_RATE = "persian_engine_rate";
    public static final String PERSIAN_ENGINE_VOLUME = "persian_engine_volume";

    public static final String TTS_PUNCTUATION_MODE = "tts_punctuation_mode";
    public static final String TTS_NUMBER_MODE = "tts_number_mode";
    public static final String TTS_EMOJI_MODE = "tts_emoji_mode";
    public static final String TTS_DIGITS_LANGUAGE = "tts_digits_language";
    
    public static final String APP_UUID_ID = "uuid_id";
    public static final String APP_PRODUCT_KEY = "product_key";
    public static final String FIRST_RUN_COMPLETED = "first_run_completed";
    public static final String NLP_HAND = "nlp_hand";
    public static final String NLP_HAND_REFERENCE_COUNT = "nlp_hand_ref_count";
    public static final String LAST_LANGUAGE = "last_language";
    public static final String LAST_COUNTRY = "last_country";
    public static final String LAST_VARIANT = "last_variant";
    public static final String LAST_VOICE_NAME = "last_voice_name";
    public static final int VOICE_CHANGE_ID = 1;
    public static final int PITCH_CHANGE_ID = 2;
    public static final int SPEED_CHANGE_ID = 4;
    public static final int VOLUME_CHANGE_ID = 8;
    public static final int NUMBER_MODE_CHANGE_ID = 16;
    public static final int PUNCTUATION_MODE_CHANGE_ID = 32;
    public static final int EMOJI_MODE_CHANGE_ID = 64;
    public static final int DIGITS_LANGUAGE_CHANGE_ID = 128;

    public static final String INTENT_LANGUAGE_PARAM_KEY = "language";
    public static final String INTENT_CHANGED_PARAM_KEY = "changed";
    public static final String PREF_DEFAULT_RATE = "default_rate";
    public static final String PREF_DEFAULT_PITCH = "default_pitch";
    public static final int PERSIAN_RATE_MIN = 0;
    public static final int PERSIAN_RATE_MAX = 100;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;
    public Preferences(Context c) {

        Runnable r = () -> {
            this.context = PreferenceStorage.resolveStorageContext(c);
            this.sharedPreferences = PreferenceStorage.getDefaultSharedPreferences(this.context);
            this.editor = this.sharedPreferences.edit();
        };
        Thread thr = new Thread(r);
        thr.start();
        try {
            thr.join();
        } catch (InterruptedException e) {
            LogUtils.w(TAG,"Preferences constructor catch exception: " + e.getMessage());
        }
    }

    public void set(String key, String value) {
        Runnable r = () -> {
            this.editor.putString(key, value);
            this.editor.commit();
        };
        Thread thr = new Thread(r);
        thr.start();
        try {
            thr.join();
        } catch (InterruptedException e) {
            LogUtils.w(TAG,"Preferences set catch exception: " + e.getMessage());
        }

    }

    public String get(String key , String default_value) {
        //int ResId = context.getResources().getIdentifier(key_default, "string", context.getPackageName());
        //String default_value = this.sharedPreferences.getString(key_default,context.getResources().getString(ResId));

        return this.sharedPreferences.getString(key, default_value);

    }

    public void setBoolean(String key, boolean value) {
        Runnable r = () -> {
            this.editor.putBoolean(key, value);
            this.editor.commit();
        };
        Thread thr = new Thread(r);
        thr.start();
        try {
            thr.join();
        } catch (InterruptedException e) {
            LogUtils.w(TAG,"Preferences setBoolean catch exception: " + e.getMessage());
        }

    }
    public int getInt(String key , int resourceId){
        //Experience getInt better not to work
        String default_value_string = context.getResources().getString(resourceId);
        String value = get(key, default_value_string);
        int valueId = 0;

        try {
            valueId = Integer.parseInt(value);
        } catch(NumberFormatException nfe) {
            LogUtils.w(TAG,"Could not parse " + nfe.getMessage());
        }
        return valueId;
    }
    public boolean getBoolean(String key , boolean default_value){
        return this.sharedPreferences.getBoolean(key,default_value);
    }

    public void setLastRequestedVoice(String language, String country, String variant, String voiceName) {
        Runnable r = () -> {
            this.editor.putString(LAST_LANGUAGE, language == null ? "" : language);
            this.editor.putString(LAST_COUNTRY, country == null ? "" : country);
            this.editor.putString(LAST_VARIANT, variant == null ? "" : variant);
            if (!TextUtils.isEmpty(voiceName)) {
                this.editor.putString(LAST_VOICE_NAME, voiceName);
            }
            this.editor.commit();
        };
        Thread thr = new Thread(r);
        thr.start();
        try {
            thr.join();
        } catch (InterruptedException e) {
            LogUtils.w(TAG,"Preferences setLastRequestedVoice catch exception: " + e.getMessage());
        }
    }

    public String getLastVoiceName() {
        return this.sharedPreferences.getString(LAST_VOICE_NAME, "");
    }

    public String getLastLanguage() {
        return this.sharedPreferences.getString(LAST_LANGUAGE, "");
    }

    public String getLastCountry() {
        return this.sharedPreferences.getString(LAST_COUNTRY, "");
    }

    public String getLastVariant() {
        return this.sharedPreferences.getString(LAST_VARIANT, "");
    }

    public void clear(String key) {
        this.editor.remove(key);
        this.editor.commit();
    }

    public void clear() {
        this.editor.clear();
        this.editor.commit();
    }
    //////////////////////Method 2 List of Text to Speech Engines
    public static List<TtsEngineInfo> getEngines(Context context) {
        final PackageManager pm = context.getPackageManager();
        final Intent intent = new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE);
        final List<ResolveInfo> resolveInfos = pm.queryIntentServices(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        final List<TtsEngineInfo> engines = new ArrayList<TtsEngineInfo>(resolveInfos.size());

        for (ResolveInfo resolveInfo : resolveInfos) {
            final TtsEngineInfo engine = getEngineInfo(resolveInfo, pm);
            if (engine != null) {
                engines.add(engine);
            }
        }

        Collections.sort(engines, ENGINE_PRIORITY_COMPARATOR);

        return Collections.unmodifiableList(engines);
    }

    public static String selectAndStoreEnglishEngine(Context context, List<TtsEngineInfo> engines) {
        Preferences preferences = new Preferences(context);
        final String packageName = context.getPackageName().toLowerCase();
        int espeakIndex = -1;
        int eloquenceIndex = -1;
        int googleIndex = -1;
        LogUtils.w(TAG, "Resolving English engine preference. Available engines=" + engines.size());

        for (int index = 0; index < engines.size(); index++) {
            final TtsEngineInfo engine = engines.get(index);
            final String engineNameLower = engine.name.toLowerCase();
            LogUtils.w(TAG, "Evaluating English engine candidate: " + engine);
            if (engineNameLower.contains(packageName)) {
                continue;
            }
            if (engineNameLower.contains(ESPEAK_PACKAGE_NAME)) {
                espeakIndex = index;
            }
            if (engineNameLower.contains(ELOQUENCE_PACKAGE_NAME)) {
                eloquenceIndex = index;
            }
            if (engineNameLower.contains(GOOGLE_PACKAGE_NAME)) {
                googleIndex = index;
            }
        }

        String selectedEngine = "";
        if (eloquenceIndex >= 0) {
            selectedEngine = ELOQUENCE_PACKAGE_NAME;
            LogUtils.w(TAG, "Selecting English engine: Eloquence");
        } else if (espeakIndex >= 0) {
            selectedEngine = ESPEAK_PACKAGE_NAME;
            LogUtils.w(TAG, "Selecting English engine: eSpeak");
        } else if (googleIndex >= 0) {
            selectedEngine = GOOGLE_PACKAGE_NAME;
            LogUtils.w(TAG, "Selecting English engine: Google TTS");
        } else {
            for (TtsEngineInfo engine : engines) {
                final String engineNameLower = engine.name.toLowerCase();
                if (!engineNameLower.contains(packageName)) {
                    selectedEngine = engine.name;
                    LogUtils.w(TAG, "Selecting English engine: first available " + selectedEngine);
                    break;
                }
            }
        }

        if (!TextUtils.isEmpty(selectedEngine)) {
            preferences.set(ENG_ENGINE_NAME, selectedEngine);
            LogUtils.w(TAG, "Stored English engine selection: " + selectedEngine);
        } else {
            LogUtils.w(TAG, "No English engine stored; none available after filtering");
        }

        return selectedEngine;
    }

    public static String findInstalledExternalEnglishEngine(Context context) {
        final PackageManager pm = context.getPackageManager();
        final String[] preferredPackages = new String[] {
                ELOQUENCE_PACKAGE_NAME,
                ESPEAK_PACKAGE_NAME,
                GOOGLE_PACKAGE_NAME
        };
        for (String candidate : preferredPackages) {
            try {
                pm.getPackageInfo(candidate, 0);
                LogUtils.i(TAG, "Found installed English engine package: " + candidate);
                return candidate;
            } catch (Exception ignored) {
                // Continue searching for the next candidate.
            }
        }
        LogUtils.w(TAG, "No preferred external English engine packages installed");
        return "";
    }

    /**
     * Returns the engine info for a given engine name. Note that engines are
     * identified by their package name.
     */
    public static TtsEngineInfo getEngineInfo(Context context, String packageName) {
        if (packageName == null) {
            return null;
        }

        final PackageManager pm = context.getPackageManager();
        final Intent intent = new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE).setPackage(packageName);
        final List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfos.isEmpty()) {
            return null;
        }

        // Note that the current API allows only one engine per
        // package name. Since the "engine name" is the same as
        // the package name.
        return getEngineInfo(resolveInfos.get(0), pm);
    }
    private static TtsEngineInfo getEngineInfo(ResolveInfo resolve, PackageManager pm) {
        final ServiceInfo service = resolve.serviceInfo;
        if (service == null) {
            return null;
        }

        final TtsEngineInfo engine = new TtsEngineInfo();

        // Using just the package name isn't great, since it disallows having
        // multiple engines in the same package, but that's what the existing
        // API does.
        engine.name = service.packageName;

        final CharSequence label = service.loadLabel(pm);
        engine.label = TextUtils.isEmpty(label) ? engine.name : label.toString();
        engine.icon = service.getIconResource();
        engine.priority = resolve.priority;
        engine.system = isSystemEngine(service);

        return engine;
    }
    private static boolean isSystemEngine(ServiceInfo info) {
        final ApplicationInfo appInfo = info.applicationInfo;
        if (appInfo == null) {
            return false;
        }

        return ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    /**
     * Engines that are a part of the system image are always lesser than those
     * that are not. Within system engines / non system engines the engines are
     * sorted in order of their declared priority.
     */
    public static final Comparator<TtsEngineInfo>
            ENGINE_PRIORITY_COMPARATOR = (lhs, rhs) -> {
                if (lhs.system && !rhs.system) {
                    return -1;
                } else if (rhs.system && !lhs.system) {
                    return 1;
                } else {
                    // Either both system engines, or both non system
                    // engines. Note:
                    // this isn't a typo. Higher priority numbers imply
                    // higher
                    // priority, but are "lower" in the sort order.
                    return (rhs.priority - lhs.priority);
                }
            };

    public int getPersianVoiceId(){
        String default_value_string = context.getResources().getString(R.string.persian_voice_default);
        int defaultValueId = 0;


       String strPersianVoiceId = get(PERSIAN_ENGINE_ID,default_value_string);
        try {
            defaultValueId = Integer.parseInt(strPersianVoiceId);
        } catch(NumberFormatException nfe) {
            LogUtils.w(TAG,"Could not parse " + nfe.getMessage());
        }
        return defaultValueId;
    }
    public void setPersianVoiceId(Integer nVoice){
        set(PERSIAN_ENGINE_ID,nVoice.toString());
    }
    public String getEnglishVoiceName(){
        List<TtsEngineInfo> listEngines  = getEngines(context);
        final String pck_name = context.getPackageName();
        List<String> voicesArray = new ArrayList<>();

        for(TtsEngineInfo engine : listEngines) {
            //This tts service not show
            if(engine.name.contains(pck_name))
                continue;
            voicesArray.add(engine.name);
        }
        String default_value = "";
        if(voicesArray.size()>0)
            default_value = voicesArray.get(0);
        String storedEngine = get(ENG_ENGINE_NAME,default_value);
        if (TextUtils.equals(storedEngine, pck_name)) {
            LogUtils.w(TAG, "Ignoring self package as stored English engine; resolving alternative");
            String resolved = Preferences.selectAndStoreEnglishEngine(context, listEngines);
            if (TextUtils.isEmpty(resolved)) {
                resolved = Preferences.findInstalledExternalEnglishEngine(context);
            }
            storedEngine = TextUtils.isEmpty(resolved) ? default_value : resolved;
            if (!TextUtils.isEmpty(storedEngine)) {
                setEnglishVoiceName(storedEngine);
            }
        }
        return storedEngine;
    }
    public void setEnglishVoiceName(String voiceName){
        set(ENG_ENGINE_NAME,voiceName);
    }
    public int getPersianPitch(){
        return getInt(PERSIAN_ENGINE_PITCH,R.string.persian_voice_pitch_default);
    }
    public void setPersianPitch(Integer pitch){
        set(PERSIAN_ENGINE_PITCH , pitch.toString());
    }
    public int getPersianRate(){
        int rate = getInt(PERSIAN_ENGINE_RATE,R.string.persian_voice_rate_default);
        int clampedRate = clampPersianRate(rate);
        if (clampedRate != rate) {
            setPersianRate(clampedRate);
        }
        return clampedRate;
    }
    public void setPersianRate(Integer rate){
        int clampedRate = clampPersianRate(rate);
        set(PERSIAN_ENGINE_RATE,Integer.toString(clampedRate));
    }
    public int getPersianVolume(){
        return getInt(PERSIAN_ENGINE_VOLUME,R.string.persian_voice_volume_default);
    }
    public void setPersianVolume(Integer volume){
        set(PERSIAN_ENGINE_VOLUME,volume.toString());
    }
    private int clampPersianRate(int rate) {
        if (rate < PERSIAN_RATE_MIN) {
            return PERSIAN_RATE_MIN;
        }
        if (rate > PERSIAN_RATE_MAX) {
            return PERSIAN_RATE_MAX;
        }
        return rate;
    }
    ///////////////////////// English Parameters
    public int getEnglishPitch(){
        return getInt(ENG_ENGINE_PITCH,R.string.english_voice_pitch_default);
    }
    public void setEnglishPitch(Integer pitch){
        set(ENG_ENGINE_PITCH , pitch.toString());
    }
    public int getEnglishRate(){
        return getInt(ENG_ENGINE_RATE,R.string.english_voice_rate_default);
    }
    public void setEnglishRate(Integer rate){
        set(ENG_ENGINE_RATE,rate.toString());
    }
    public int getEnglishVolume(){
        return getInt(ENG_ENGINE_VOLUME,R.string.english_voice_volume_default);
    }
    public void setEnglishVolume(Integer volume){
        set(ENG_ENGINE_VOLUME,volume.toString());
    }
    ////////////////////////////////// tts parameters
    public int getNumberMode(){
        return getInt(TTS_NUMBER_MODE,R.string.tts_number_mode_default);
    }
    public void setNumberMode(Integer mode){
        set(TTS_NUMBER_MODE,mode.toString());
    }
    public int getDigitsLanguage(){
        return getInt(TTS_DIGITS_LANGUAGE, R.string.tts_digits_language_default);
    }
    public void setDigitsLanguage(Integer mode){
        set(TTS_DIGITS_LANGUAGE, mode.toString());
    }
    public int getPunctuationMode(){
        return getInt(TTS_PUNCTUATION_MODE,R.string.tts_punctuation_mode_default);
    }
    public void setPunctuationMode(Integer mode){
        set(TTS_PUNCTUATION_MODE,mode.toString());
    }
    public int getEmojiMode(){
        return getInt(TTS_EMOJI_MODE,R.string.tts_emoji_mode_default);
    }
    public void setEmojiMode(Integer mode){
        set(TTS_EMOJI_MODE,mode.toString());
    }

}
