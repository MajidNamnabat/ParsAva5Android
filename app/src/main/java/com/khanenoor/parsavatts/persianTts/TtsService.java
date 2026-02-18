package com.khanenoor.parsavatts.persianTts;

import static android.speech.tts.Voice.LATENCY_LOW;
import static android.speech.tts.Voice.QUALITY_NORMAL;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.Voice;
import android.text.TextUtils;
import android.util.Pair;

import com.khanenoor.parsavatts.engine.FaTts;
import com.khanenoor.parsavatts.engine.SpeechSynthesis;
import com.khanenoor.parsavatts.util.LogUtils;
import com.khanenoor.parsavatts.ttsService.DualTtsService;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TtsService extends TextToSpeechService {
    private static final String TAG = TtsService.class.getSimpleName();
    private static final boolean DEBUG = false;
    //ISO 639-3
    // fa  fas macrolanguage, also known as Farsi
    // https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
    private static final String DEFAULT_LANGUAGE_1 = "fas";
    // ISO 3166-1 alpha-3 codes : IR IRN Numeric:364
    // is eye-free was "uk" but I guess must be 3 characters
    private static final String DEFAULT_COUNTRY_1 = "IRN";
    private static final String DEFAULT_VARIANT_1 = "";
    /////////////////English
    // en eng
    private static final String DEFAULT_LANGUAGE_2 = "eng";
    //United Kingdom of Great Britain and Northern Ireland (the) 	GB 	GBR 	826
    //United States of America (the) 	US 	USA 	840
    //https://www.iban.com/country-codes
    private static final String DEFAULT_COUNTRY_2 = "USA";
    private static final String DEFAULT_VARIANT_2 = "";
    private SpeechSynthesis mEngine;
    private SynthesisCallback mCallback;
    /**
     * Pipes synthesizer output from native eSpeak to an {@linkAudioTrack}.
     */
    private final FaTts.SynthReadyCallback mSynthCallback = new FaTts.SynthReadyCallback() {
        @Override
        public int onSynthDataReady(byte[] audioData, int audioData_size) {
            if ((audioData == null) || (audioData.length == 0)) {
                onSynthDataComplete();
                return 1;
            }

            final int maxBytesToCopy = mCallback.getMaxBufferSize();

            int offset = 0;

            while (offset < audioData.length) {
                //LogUtils.w(TAG, "onSynthDataReady Len:" + audioData.length);
                final int bytesToWrite = Math.min(maxBytesToCopy, (audioData.length - offset));
                mCallback.audioAvailable(audioData, offset, bytesToWrite);
                offset += bytesToWrite;
            }
            return 0;
        }

        @Override
        public void onSynthDataComplete() {

            LogUtils.w(TAG, "onSynthDataComplete ***");
            mCallback.done();
        }
    };
    private String mLanguage = DEFAULT_LANGUAGE_1;
    private String mCountry = DEFAULT_COUNTRY_1;
    private String mVariant = DEFAULT_VARIANT_1;

    public TtsService() {
    }

    /**
     * Retrieves the language code from a synthesis request.
     *
     * @param request The synthesis request.
     * @return A language code in the format "en-uk-n".
     */
    private static String getRequestLanguage(SynthesisRequest request) {
        final StringBuffer result = new StringBuffer(request.getLanguage());

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

    @Override
    public void onCreate() {

        initializeTtsEngine();
        // This calls onIsLanguageAvailable() and must run AFTER initialization!
        super.onCreate();
    }

    private void initializeTtsEngine() {
        if (mEngine != null) {
            mEngine.stop(SpeechSynthesis.StopReason.SERVICE_SHUTDOWN,
                    "TtsService.initializeTtsEngine",
                    "engineRefresh");
            mEngine = null;
        }
        final AssetManager am = this.getAssets();
        final String pck_name = getApplicationContext().getPackageName();
        mEngine = new SpeechSynthesis(this, mSynthCallback, pck_name, am);
        TextToSpeech sd;

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
        boolean hasLanguage = false;
        boolean hasCountry = false;
        if (Objects.equals(lang, DEFAULT_LANGUAGE_1) || Objects.equals(lang, DEFAULT_LANGUAGE_2)) {
            hasLanguage = true;
        }
        if (Objects.equals(country, DEFAULT_COUNTRY_1) || Objects.equals(country, DEFAULT_COUNTRY_2)) {
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
        return new String[]{
                mLanguage, mCountry, mVariant
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
        final int result = onIsLanguageAvailable(lang, country, variant);
        if (result != TextToSpeech.LANG_AVAILABLE && result != TextToSpeech.LANG_COUNTRY_AVAILABLE
                && result != TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
            LogUtils.e(TAG, "Failed to load language {language='" + lang + "', country='" + country
                    + "', variant='" + variant + "'");
            return result;
        }

        synchronized (this) {
            mLanguage = lang;
            mCountry = ((country == null) ? "" : country);
            mVariant = ((variant == null) ? "" : variant);
        }

        return result;

    }
    /*
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    */

    /**
     * Notifies the service that it should stop any in-progress speech synthesis.
     * This method can be called even if no speech synthesis is currently in progress.
     * <p>
     * Can be called on multiple threads, but not on the synthesis thread.
     */
    @Override
    protected void onStop() {
        LogUtils.i(TAG, "Received stop request.");
        if (mEngine != null) {
            mEngine.stop(SpeechSynthesis.StopReason.FLUSH,
                    "TtsService.onStop",
                    "frameworkStop");
        }

    }

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
        final String text = request.getText();
        final String language = getRequestLanguage(request);
        //final int gender = getDefaultGender();
        int rate = request.getSpeechRate();
        int pitch = request.getPitch();
        final Bundle params = request.getParams();

        mLanguage = request.getLanguage();
        mCountry = request.getCountry();
        mVariant = request.getVariant();

        if (DEBUG) {
            LogUtils.i(TAG, "Received synthesis request: {language=\"" + language + "\"}");

            for (String key : params.keySet()) {
                LogUtils.v(TAG,
                        "Synthesis request contained param {" + key + ", " + params.get(key) + "}");
            }
        }
        mCallback = callback;
        mCallback.start(mEngine.getSampleRate(), mEngine.getAudioFormat(),
                mEngine.getChannelCount());
        final Pair<Integer, Integer> resolvedParams = DualTtsService.resolveEffectivePersianRatePitch(
                getApplicationContext(),
                language,
                rate,
                pitch,
                mEngine);
        rate = resolvedParams.first;
        pitch = resolvedParams.second;
        mEngine.setRate(rate);
        mEngine.setPitch(pitch);
        //mEngine.setVoiceByProperties(null, language, gender, 0, 0);
        //synthesize called but not exist this function , why?
        try {
            mEngine.speak(text, language,true);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String onGetDefaultVoiceNameFor(String lang,
                                           String country,
                                           String variant) {
        LogUtils.w(TAG, "onGetDefaultVoiceNameFor lang:" + lang);
        if (lang.contains("eng")) {
            return "ParsAva_English";
        } else if (lang.contains("fas")) {
            return "ParsAva_Persian";
        }
        return null;
    }

    @Override
    public List<Voice> onGetVoices() {
        LogUtils.w(TAG, "onGetVoices ");

        List<Voice> voices = new ArrayList<>();
        voices.add(new Voice("ParsAva_Persian",
                new Locale("fas", "IRN", ""),
                QUALITY_NORMAL, LATENCY_LOW, false, null));
        voices.add(new Voice("ParsAva_English",
                new Locale("eng", "USA", ""),
                QUALITY_NORMAL, LATENCY_LOW, false, null));
        ;
        return voices;
    }

    @Override
    public int onIsValidVoiceName(String voiceName) {
        switch (voiceName) {
            case "ParsAva_Persian":
                return TextToSpeech.SUCCESS;
            case "ParsAva_English":
                return TextToSpeech.SUCCESS;
        }
        return TextToSpeech.ERROR;
    }

    @Override
    public int onLoadVoice(String voiceName) {
        return TextToSpeech.SUCCESS;
    }

}