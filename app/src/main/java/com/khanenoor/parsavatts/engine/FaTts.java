package com.khanenoor.parsavatts.engine;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;

import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.impractical.Language;
import com.khanenoor.parsavatts.impractical.SpeechSynthesisConfigure;
import com.khanenoor.parsavatts.util.LogUtils;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FaTts implements Serializable {
    public static void onActionTtsQueueCompletedReceived() {
    }

    public void changePunctionanMode(int nValue) {
        //SetOptionsNLP(nlpHand, EnumTTSParam.Opt_PuncMode.ordinal(),nValue);
    }

    public void changeDigitsLanguage(int nValue) {
        LogUtils.e(TAG, "changeDigitsLanguage: " + nValue);
        //SetOptionsNLP(nlpHand, EnumTTSParam.Opt_Digits.ordinal(), nValue);
    }

    public void changeNumberMode(int nValue) {
        //SetOptionsNLP(nlpHand, EnumTTSParam.Opt_DigitsReadMode.ordinal(),nValue);
    }

    public void changeEmojiMode(int nValue) {
        //SetOptionsNLP(nlpHand, EnumTTSParam.Opt_EmojiActive.ordinal(),nValue);
    }



    private static final String TAG = FaTts.class.getSimpleName();
    private static  String package_name = "";
    private int mVoice = 1;
    private Context mContext = null;
    private SynthReadyCallback mCallback = null;
    public SpeechSynthesisConfigure mConfiureParams = null;
    private int mSpeechRateValue = 0;
    private static final ExecutorService INSTALL_CHECK_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final long INSTALL_CHECK_TIMEOUT_SECONDS = 5L;
    private static final String PERSIAN_ONNX_MODEL_ASSET = "elnaz.onnx";
    private transient OrtEnvironment mOnnxEnvironment;
    private transient OrtSession mOnnxSession;
    private String mOnnxModelPath;
    private String mOnnxPrimaryInputName;
    private String mOnnxInputLengthsInputName;
    private String mOnnxScalesInputName;
    private String mOnnxLengthScaleInputName;
    private static final int PCM_16_BIT_BYTES_PER_SAMPLE = 2;
    private static final int SYNTH_CHUNK_SIZE_BYTES = 8192;
    private static final float ONNX_NOISE_SCALE = 0.667f;
    private static final float ONNX_NOISE_W = 0.8f;
    private static final float ONNX_LENGTH_SCALE_MIN = 0.5f;
    private static final float ONNX_LENGTH_SCALE_MAX = 2.0f;
    private volatile float mOnnxLengthScale = 1.0f;
    /////Load Libraries Section
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> INSTALL_CHECK_EXECUTOR.shutdownNow()));
        // Try to load the shared library.
        try {
            System.loadLibrary("Ttslib");
            System.loadLibrary("SampleRate");
            // System.loadLibrary("cryptopp");
            //setNativeLoggingEnabled(LogUtils.isEnabled());
        } catch (UnsatisfiedLinkError e) {
            // The shared library could not be loaded.
            LogUtils.e(TAG, "Could not load shared library: " + e.getMessage());
        } catch (SecurityException e) {
            // The user does not have permission to load the shared library.
            LogUtils.e(TAG, "User does not have permission to load shared library: " + e.getMessage());
        } catch (Exception e){
            LogUtils.e(TAG, "Could not load shared library: " + e.getMessage());
        }
    }
    public FaTts(String pck_name , SynthReadyCallback callback, Context context){

        package_name = pck_name;
        mCallback = callback;
        mConfiureParams = new SpeechSynthesisConfigure(context, Language.LANGUAGE_PERSIAN);
        mSpeechRateValue = mConfiureParams.mRate;
        mContext = context;
    }
    public void Install(AssetManager am ){


        Install(am, package_name);

    }
    public static void Install(AssetManager am , String package_name_static){

        try {

            copyAsset(am, "Settings.xml", "/data/data/" + package_name_static + "/Settings.xml");
            //Problem These Files not relavant to FaTts

            boolean result = copyAsset(am, "parsava_icon.bmp", "/data/data/" + package_name_static + "/parsava_icon.bmp");
            //LogUtils.w(TAG,"FaTts.Install result1:" + result);
            result = copyAsset(am, "ic_action_dark.bmp", "/data/data/" + package_name_static + "/ic_action_dark.bmp");
            result = copyAsset(am, "UserDictionary.dict", "/data/data/" + package_name_static + "/UserDictionary.dict");
            result = copyAsset(am, PERSIAN_ONNX_MODEL_ASSET, "/data/data/" + package_name_static + "/" + PERSIAN_ONNX_MODEL_ASSET);

        } catch (Exception ex){
            LogUtils.w(TAG,"FaTts.Insll " + ex.getMessage());
        }
    }
    public boolean Load(Context cnx,int nSpecialLexiconLoad, int nVoice,
                        int nMode, int nScreenReader) {
        String path =  "/data/data/" + package_name + "/Settings.xml";
        try {
                if (!loadPersianOnnxModel(cnx)) {
                    LogUtils.e(TAG, "Error in Load: failed to initialize Persian ONNX model " + PERSIAN_ONNX_MODEL_ASSET);
                    return false;
                }
                //LogUtils.w(TAG, " RegisterCallbackNLP Done " + nlpHand);
                //Load Preferences and Settings
                Preferences prefs = new Preferences(cnx);
                int setValue = prefs.getEmojiMode();
                changeEmojiMode(setValue);
                setValue = prefs.getNumberMode();
                changeNumberMode(setValue);
                setValue = prefs.getPunctuationMode();
                changePunctionanMode(setValue);
                setValue = prefs.getDigitsLanguage();
                changeDigitsLanguage(setValue);
                return true;
        } catch (Exception ex) {
                LogUtils.e(TAG, " Error in NLP load " + ex.getMessage());
            }
        return false;
    }
    public void Unload(){
        try {
            closePersianOnnxModel();
        }  catch (Exception ex) {
            LogUtils.e(TAG, " Error in UnloadNLP " + ex.getMessage());
        }
    }

    private synchronized boolean loadPersianOnnxModel(Context context) {
        if (context == null) {
            LogUtils.e(TAG, "loadPersianOnnxModel failed: context is null");
            return false;
        }

        if (mOnnxSession != null) {
            return true;
        }

        try {
            final String modelPath = resolvePersianOnnxModelPath(context);
            if (modelPath == null || modelPath.isEmpty()) {
                LogUtils.e(TAG, "loadPersianOnnxModel failed: empty model path");
                return false;
            }

            if (mOnnxEnvironment == null) {
                mOnnxEnvironment = OrtEnvironment.getEnvironment();
            }
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            mOnnxSession = mOnnxEnvironment.createSession(modelPath, options);
            mOnnxModelPath = modelPath;
            mOnnxPrimaryInputName = resolvePrimaryOnnxInputName(mOnnxSession);
            mOnnxInputLengthsInputName = resolveOptionalOnnxInputName(mOnnxSession, "input_lengths");
            mOnnxScalesInputName = resolveOptionalOnnxInputName(mOnnxSession, "scales");
            mOnnxLengthScaleInputName = resolveOptionalOnnxInputName(mOnnxSession, "length_scale");
            if (mOnnxPrimaryInputName == null || mOnnxPrimaryInputName.isEmpty()) {
                LogUtils.e(TAG, "loadPersianOnnxModel failed: model input not found");
                closePersianOnnxModel();
                return false;
            }

            LogUtils.i(TAG, "Persian ONNX model loaded: " + mOnnxModelPath + ", input=" + mOnnxPrimaryInputName);
            return true;
        } catch (IOException | OrtException e) {
            LogUtils.e(TAG, "loadPersianOnnxModel failed: " + e.getMessage());
            closePersianOnnxModel();
        }

        return false;
    }

    private String resolvePersianOnnxModelPath(Context context) throws IOException {
        final String destinationPath = "/data/data/" + package_name + "/" + PERSIAN_ONNX_MODEL_ASSET;
        File destinationFile = new File(destinationPath);
        if (!destinationFile.exists() || destinationFile.length() == 0L) {
            boolean copied = copyAsset(context.getAssets(), PERSIAN_ONNX_MODEL_ASSET, destinationPath);
            if (!copied) {
                throw new IOException("Unable to copy Persian ONNX model to " + destinationPath);
            }
            destinationFile = new File(destinationPath);
        }
        return destinationFile.getAbsolutePath();
    }

    private synchronized void closePersianOnnxModel() {
        if (mOnnxSession != null) {
            try {
                mOnnxSession.close();
            } catch (OrtException e) {
                LogUtils.w(TAG, "closePersianOnnxModel session close failed", e);
            }
            mOnnxSession = null;
        }
        mOnnxModelPath = null;
        mOnnxPrimaryInputName = null;
        mOnnxInputLengthsInputName = null;
        mOnnxScalesInputName = null;
        mOnnxLengthScaleInputName = null;
    }

    private String resolvePrimaryOnnxInputName(OrtSession session) {
        if (session == null || session.getInputNames() == null || session.getInputNames().isEmpty()) {
            return null;
        }
        return session.getInputNames().iterator().next();
    }

    private String resolveOptionalOnnxInputName(OrtSession session, String preferredName) {
        if (session == null || session.getInputNames() == null || session.getInputNames().isEmpty()) {
            return null;
        }

        for (String inputName : session.getInputNames()) {
            if (inputName != null && inputName.equalsIgnoreCase(preferredName)) {
                return inputName;
            }
        }

        for (String inputName : session.getInputNames()) {
            if (inputName != null && inputName.toLowerCase().contains(preferredName.toLowerCase())) {
                return inputName;
            }
        }
        return null;
    }
/*
    public int synth(String persianText) {
        if (mCallback == null) {
            return 0;
        }
        if (persianText == null || persianText.trim().isEmpty()) {
            mCallback.onSynthDataComplete();
            return 1;
        }

        try {
            if (synthWithOnnx(persianText)) {
                return 1;
            }
            return synthWithLegacyNlp(persianText);
        } catch (Exception e) {
            LogUtils.w(TAG, "synth failed, fallback to legacy pipeline", e);
            return synthWithLegacyNlp(persianText);
        }
    }

    private int synthWithLegacyNlp(String persianText) {
        try {
            //ParsTextNLP(getNlpHand(), persianText.getBytes("UTF-32"), true);
            return 1;
        } catch (Exception e) {
            LogUtils.e(TAG, "synthWithLegacyNlp failed: " + e.getMessage());
        }
        return 0;
    }
*/
    synchronized boolean synthWithOnnx(String persianText) {
        if (mOnnxSession == null) {
            if (!loadPersianOnnxModel(mContext)) {
                return false;
            }
        }
        if (mOnnxSession == null) {
            return false;
        }

        try {
            final long[] tokenIds = textToTokenIds(persianText);
            if (tokenIds.length == 0) {
                return false;
            }
            final String inputName = mOnnxPrimaryInputName;
            if (inputName == null || inputName.isEmpty()) {
                return false;
            }
            final java.util.Map<String, OnnxTensor> inputs = new java.util.HashMap<>(3);
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(mOnnxEnvironment, java.nio.LongBuffer.wrap(tokenIds), new long[]{1, tokenIds.length})) {
                inputs.put(inputName, inputTensor);
                if (mOnnxInputLengthsInputName != null && !mOnnxInputLengthsInputName.isEmpty()) {
                    long[] inputLengths = new long[]{tokenIds.length};
                    try (OnnxTensor inputLengthsTensor = OnnxTensor.createTensor(mOnnxEnvironment, java.nio.LongBuffer.wrap(inputLengths), new long[]{1})) {
                        inputs.put(mOnnxInputLengthsInputName, inputLengthsTensor);
                        return runSynthesisSession(inputs);
                    }
                }

                return runSynthesisSession(inputs);
            }
        } catch (Exception ex) {
            LogUtils.w(TAG, "synthWithOnnx failed", ex);
            return false;
        }
    }

    private boolean runSynthesisSession(java.util.Map<String, OnnxTensor> inputs) throws OrtException {
        if (inputs == null || inputs.isEmpty()) {
            return false;
        }

                if (mOnnxScalesInputName != null && !mOnnxScalesInputName.isEmpty()) {
                    float[] scales = new float[]{ONNX_NOISE_SCALE, mOnnxLengthScale, ONNX_NOISE_W};
                    try (OnnxTensor scalesTensor = OnnxTensor.createTensor(mOnnxEnvironment, java.nio.FloatBuffer.wrap(scales), new long[]{3})) {
                        inputs.put(mOnnxScalesInputName, scalesTensor);
                        try (OrtSession.Result result = mOnnxSession.run(inputs)) {
                            final byte[] pcm16Wave = extractPcmWave(result);
                            if (pcm16Wave == null || pcm16Wave.length == 0) {
                                return false;
                            }
                            emitWaveToQueue(pcm16Wave);
                            return true;
                        }
                    }
                }

                if (mOnnxLengthScaleInputName != null && !mOnnxLengthScaleInputName.isEmpty()) {
                    float[] lengthScale = new float[]{mOnnxLengthScale};
                    try (OnnxTensor lengthScaleTensor = OnnxTensor.createTensor(mOnnxEnvironment, java.nio.FloatBuffer.wrap(lengthScale), new long[]{1})) {
                        inputs.put(mOnnxLengthScaleInputName, lengthScaleTensor);
                        try (OrtSession.Result result = mOnnxSession.run(inputs)) {
                            final byte[] pcm16Wave = extractPcmWave(result);
                            if (pcm16Wave == null || pcm16Wave.length == 0) {
                                return false;
                            }
                            emitWaveToQueue(pcm16Wave);
                            return true;
                        }
                    }
                }

                try (OrtSession.Result result = mOnnxSession.run(inputs)) {
                final byte[] pcm16Wave = extractPcmWave(result);
                if (pcm16Wave == null || pcm16Wave.length == 0) {
                    return false;
                }
                emitWaveToQueue(pcm16Wave);
                return true;
                }
    }

    private static final long PHONEME_UNKNOWN_TOKEN_ID = 0L;

    private long mapPhonemeCodePointToTokenId(int codePoint) {
        switch (codePoint) {
            case 0x20: return 3L; //  
            case 0x21: return 4L; // !
            case 0x22: return 150L; // "
            case 0x23: return 149L; // #
            case 0x24: return 2L; // $
            case 0x27: return 5L; // '
            case 0x28: return 6L; // (
            case 0x29: return 7L; // )
            case 0x2C: return 8L; // ,
            case 0x2D: return 9L; // -
            case 0x2E: return 10L; // .
            case 0x30: return 130L; // 0
            case 0x31: return 131L; // 1
            case 0x32: return 132L; // 2
            case 0x33: return 133L; // 3
            case 0x34: return 134L; // 4
            case 0x35: return 135L; // 5
            case 0x36: return 136L; // 6
            case 0x37: return 137L; // 7
            case 0x38: return 138L; // 8
            case 0x39: return 139L; // 9
            case 0x3A: return 11L; // :
            case 0x3B: return 12L; // ;
            case 0x3F: return 13L; // ?
            case 0x58: return 156L; // X
            case 0x5E: return 1L; // ^
            case 0x5F: return 0L; // _
            case 0x61: return 14L; // a
            case 0x62: return 15L; // b
            case 0x63: return 16L; // c
            case 0x64: return 17L; // d
            case 0x65: return 18L; // e
            case 0x66: return 19L; // f
            case 0x67: return 154L; // g
            case 0x68: return 20L; // h
            case 0x69: return 21L; // i
            case 0x6A: return 22L; // j
            case 0x6B: return 23L; // k
            case 0x6C: return 24L; // l
            case 0x6D: return 25L; // m
            case 0x6E: return 26L; // n
            case 0x6F: return 27L; // o
            case 0x70: return 28L; // p
            case 0x71: return 29L; // q
            case 0x72: return 30L; // r
            case 0x73: return 31L; // s
            case 0x74: return 32L; // t
            case 0x75: return 33L; // u
            case 0x76: return 34L; // v
            case 0x77: return 35L; // w
            case 0x78: return 36L; // x
            case 0x79: return 37L; // y
            case 0x7A: return 38L; // z
            case 0xE6: return 39L; // æ
            case 0xE7: return 40L; // ç
            case 0xF0: return 41L; // ð
            case 0xF8: return 42L; // ø
            case 0x127: return 43L; // ħ
            case 0x14B: return 44L; // ŋ
            case 0x153: return 45L; // œ
            case 0x1C0: return 46L; // ǀ
            case 0x1C1: return 47L; // ǁ
            case 0x1C2: return 48L; // ǂ
            case 0x1C3: return 49L; // ǃ
            case 0x250: return 50L; // ɐ
            case 0x251: return 51L; // ɑ
            case 0x252: return 52L; // ɒ
            case 0x253: return 53L; // ɓ
            case 0x254: return 54L; // ɔ
            case 0x255: return 55L; // ɕ
            case 0x256: return 56L; // ɖ
            case 0x257: return 57L; // ɗ
            case 0x258: return 58L; // ɘ
            case 0x259: return 59L; // ə
            case 0x25A: return 60L; // ɚ
            case 0x25B: return 61L; // ɛ
            case 0x25C: return 62L; // ɜ
            case 0x25E: return 63L; // ɞ
            case 0x25F: return 64L; // ɟ
            case 0x260: return 65L; // ɠ
            case 0x261: return 66L; // ɡ
            case 0x262: return 67L; // ɢ
            case 0x263: return 68L; // ɣ
            case 0x264: return 69L; // ɤ
            case 0x265: return 70L; // ɥ
            case 0x266: return 71L; // ɦ
            case 0x267: return 72L; // ɧ
            case 0x268: return 73L; // ɨ
            case 0x26A: return 74L; // ɪ
            case 0x26B: return 75L; // ɫ
            case 0x26C: return 76L; // ɬ
            case 0x26D: return 77L; // ɭ
            case 0x26E: return 78L; // ɮ
            case 0x26F: return 79L; // ɯ
            case 0x270: return 80L; // ɰ
            case 0x271: return 81L; // ɱ
            case 0x272: return 82L; // ɲ
            case 0x273: return 83L; // ɳ
            case 0x274: return 84L; // ɴ
            case 0x275: return 85L; // ɵ
            case 0x276: return 86L; // ɶ
            case 0x278: return 87L; // ɸ
            case 0x279: return 88L; // ɹ
            case 0x27A: return 89L; // ɺ
            case 0x27B: return 90L; // ɻ
            case 0x27D: return 91L; // ɽ
            case 0x27E: return 92L; // ɾ
            case 0x280: return 93L; // ʀ
            case 0x281: return 94L; // ʁ
            case 0x282: return 95L; // ʂ
            case 0x283: return 96L; // ʃ
            case 0x284: return 97L; // ʄ
            case 0x288: return 98L; // ʈ
            case 0x289: return 99L; // ʉ
            case 0x28A: return 100L; // ʊ
            case 0x28B: return 101L; // ʋ
            case 0x28C: return 102L; // ʌ
            case 0x28D: return 103L; // ʍ
            case 0x28E: return 104L; // ʎ
            case 0x28F: return 105L; // ʏ
            case 0x290: return 106L; // ʐ
            case 0x291: return 107L; // ʑ
            case 0x292: return 108L; // ʒ
            case 0x294: return 109L; // ʔ
            case 0x295: return 110L; // ʕ
            case 0x298: return 111L; // ʘ
            case 0x299: return 112L; // ʙ
            case 0x29B: return 113L; // ʛ
            case 0x29C: return 114L; // ʜ
            case 0x29D: return 115L; // ʝ
            case 0x29F: return 116L; // ʟ
            case 0x2A1: return 117L; // ʡ
            case 0x2A2: return 118L; // ʢ
            case 0x2A6: return 155L; // ʦ
            case 0x2B0: return 145L; // ʰ
            case 0x2B2: return 119L; // ʲ
            case 0x2C8: return 120L; // ˈ
            case 0x2CC: return 121L; // ˌ
            case 0x2D0: return 122L; // ː
            case 0x2D1: return 123L; // ˑ
            case 0x2DE: return 124L; // ˞
            case 0x2E4: return 146L; // ˤ
            case 0x303: return 141L; // ̃
            case 0x327: return 140L; // ̧
            case 0x329: return 144L; // ̩
            case 0x32A: return 142L; // ̪
            case 0x32F: return 143L; // ̯
            case 0x33A: return 152L; // ̺
            case 0x33B: return 153L; // ̻
            case 0x3B2: return 125L; // β
            case 0x3B5: return 147L; // ε
            case 0x3B8: return 126L; // θ
            case 0x3C7: return 127L; // χ
            case 0x1D7B: return 128L; // ᵻ
            case 0x2191: return 151L; // ↑
            case 0x2193: return 148L; // ↓
            case 0x2C71: return 129L; // ⱱ
            default:
                return PHONEME_UNKNOWN_TOKEN_ID;
        }
    }

    private long[] textToTokenIds(String text) {
        if (text == null || text.isEmpty()) {
            return new long[]{PHONEME_UNKNOWN_TOKEN_ID};
        }

        final int[] codePoints;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            codePoints = text.codePoints().toArray();
        } else {
            final int count = text.codePointCount(0, text.length());
            codePoints = new int[count];
            int offset = 0;
            for (int i = 0; i < count; i++) {
                final int cp = text.codePointAt(offset);
                codePoints[i] = cp;
                offset += Character.charCount(cp);
            }
        }

        if (codePoints.length == 0) {
            return new long[]{PHONEME_UNKNOWN_TOKEN_ID};
        }

        final long[] tokens = new long[codePoints.length];
        for (int i = 0; i < codePoints.length; i++) {
            tokens[i] = mapPhonemeCodePointToTokenId(codePoints[i]);
        }
        return tokens;
    }

    private byte[] extractPcmWave(OrtSession.Result result) throws OrtException {
        if (result == null || result.size() == 0) {
            return null;
        }

        for (Map.Entry<String, OnnxValue> output : result) {
            final OnnxValue onnxValue = output.getValue();
            if (!(onnxValue instanceof OnnxTensor)) {
                continue;
            }

            final Object tensorValue = ((OnnxTensor) onnxValue).getValue();
            final byte[] pcmWave = toPcm16Wave(tensorValue);
            if (pcmWave != null && pcmWave.length > 0) {
                return pcmWave;
            }
        }

        return null;
    }

    private byte[] toPcm16Wave(Object tensorValue) {
        if (tensorValue instanceof byte[]) {
            return stripWaveHeaderIfPresent((byte[]) tensorValue);
        }

        final float[] floatWave = flattenFloatTensor(tensorValue);
        if (floatWave != null && floatWave.length > 0) {
            return floatToPcm16(floatWave);
        }

        final short[] shortWave = flattenShortTensor(tensorValue);
        if (shortWave != null && shortWave.length > 0) {
            return shortToPcm16(shortWave);
        }

        return null;
    }

    private float[] flattenFloatTensor(Object tensorValue) {
        if (tensorValue instanceof float[]) {
            return (float[]) tensorValue;
        }
        if (tensorValue instanceof float[][]) {
            float[][] arr = (float[][]) tensorValue;
            return arr.length == 0 ? null : arr[0];
        }
        if (tensorValue instanceof float[][][]) {
            float[][][] arr = (float[][][]) tensorValue;
            return (arr.length == 0 || arr[0].length == 0) ? null : arr[0][0];
        }
        return null;
    }

    private short[] flattenShortTensor(Object tensorValue) {
        if (tensorValue instanceof short[]) {
            return (short[]) tensorValue;
        }
        if (tensorValue instanceof short[][]) {
            short[][] arr = (short[][]) tensorValue;
            return arr.length == 0 ? null : arr[0];
        }
        if (tensorValue instanceof short[][][]) {
            short[][][] arr = (short[][][]) tensorValue;
            return (arr.length == 0 || arr[0].length == 0) ? null : arr[0][0];
        }
        return null;
    }

    private byte[] floatToPcm16(float[] waveform) {
        byte[] pcmData = new byte[waveform.length * PCM_16_BIT_BYTES_PER_SAMPLE];
        int idx = 0;
        for (float sample : waveform) {
            if (sample > 1f) sample = 1f;
            if (sample < -1f) sample = -1f;
            short pcm = (short) (sample * Short.MAX_VALUE);
            pcmData[idx++] = (byte) (pcm & 0xff);
            pcmData[idx++] = (byte) ((pcm >> 8) & 0xff);
        }
        return pcmData;
    }

    private byte[] shortToPcm16(short[] waveform) {
        byte[] pcmData = new byte[waveform.length * PCM_16_BIT_BYTES_PER_SAMPLE];
        int idx = 0;
        for (short sample : waveform) {
            pcmData[idx++] = (byte) (sample & 0xff);
            pcmData[idx++] = (byte) ((sample >> 8) & 0xff);
        }
        return pcmData;
    }

    private byte[] stripWaveHeaderIfPresent(byte[] waveBytes) {
        if (waveBytes == null || waveBytes.length < 12) {
            return waveBytes;
        }
        if (waveBytes[0] == 'R' && waveBytes[1] == 'I' && waveBytes[2] == 'F' && waveBytes[3] == 'F'
                && waveBytes[8] == 'W' && waveBytes[9] == 'A' && waveBytes[10] == 'V' && waveBytes[11] == 'E') {
            for (int i = 12; i + 8 <= waveBytes.length; ) {
                int chunkSize = readLittleEndianInt(waveBytes, i + 4);
                if (chunkSize < 0) {
                    break;
                }
                if (waveBytes[i] == 'd' && waveBytes[i + 1] == 'a' && waveBytes[i + 2] == 't' && waveBytes[i + 3] == 'a') {
                    int dataStart = i + 8;
                    int dataEnd = Math.min(waveBytes.length, dataStart + chunkSize);
                    if (dataStart < dataEnd) {
                        byte[] pcm = new byte[dataEnd - dataStart];
                        System.arraycopy(waveBytes, dataStart, pcm, 0, pcm.length);
                        return pcm;
                    }
                    break;
                }
                i += 8 + chunkSize + (chunkSize % 2);
            }
        }
        return waveBytes;
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        if (bytes == null || offset < 0 || offset + 4 > bytes.length) {
            return -1;
        }
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 3] & 0xff) << 24);
    }

    private void emitWaveToQueue(byte[] pcm16Wave) {
        if (mCallback == null || pcm16Wave == null || pcm16Wave.length == 0) {
            if (mCallback != null) {
                mCallback.onSynthDataComplete();
            }
            return;
        }

        int offset = 0;
        while (offset < pcm16Wave.length) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                break;
            }
            final int bytesToWrite = Math.min(SYNTH_CHUNK_SIZE_BYTES, pcm16Wave.length - offset);
            byte[] chunk = new byte[bytesToWrite];
            System.arraycopy(pcm16Wave, offset, chunk, 0, bytesToWrite);
            int callbackResult = mCallback.onSynthDataReady(chunk, chunk.length);
            if (callbackResult != 0) {
                break;
            }
            offset += bytesToWrite;
        }
        mCallback.onSynthDataComplete();
    }

    public int getVoice() {
        return mVoice;
    }
    public boolean isInstalled(){
        //For instance check one file is exist or not
        return isInstalled(package_name,mContext);
    }
    public static boolean isInstalled(String package_name_static,Context c){
        if (c == null) {
            LogUtils.w(TAG, "isInstalled return false, context is null");
            return false;
        }

        final Context appContext = c.getApplicationContext();
        Future<Boolean> firstThreadResult = INSTALL_CHECK_EXECUTOR.submit(() ->
                performInstallCheck(package_name_static, appContext));
        try {
            return firstThreadResult.get(INSTALL_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LogUtils.w(TAG, "isInstalled timed out", e);
            firstThreadResult.cancel(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LogUtils.w(TAG, "isInstalled interrupted", e);
        } catch (ExecutionException e) {
            LogUtils.w(TAG, "isInstalled execution failed", e);
        }
        return false;

    }

    private static boolean performInstallCheck(String packageNameStatic, Context appContext) {
        String filePathString =  "/data/data/" + packageNameStatic + "/Settings.xml";

        File f = new File(filePathString);
        boolean isExist = f.exists() && !f.isDirectory();
        if(!isExist){
            LogUtils.w(TAG, "isInstalled return false, not exist");
            return false;
        }
        try {
            long installed = appContext
                    .getPackageManager()
                    .getPackageInfo(appContext.getPackageName(), 0)
                    .firstInstallTime;
            long created = new File(filePathString).lastModified();
            Date lastModDateDate = new Date(created);
            Date firstInstallPackageDate = new Date(installed);
            LogUtils.w(TAG,"isInstalled Dates installed " + firstInstallPackageDate + " createFileDate:"+lastModDateDate);
            if(created<installed){
                LogUtils.w(TAG, "isInstalled return false created:"+created+" installed: "+installed);
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.w(TAG, "isInstalled PackageManager.NameNotFoundException"+e.getMessage());
        }
        return true;
    }

    //Question Must be public or private ?
    //The callback itself has to be private in appdesigner,
    // there's no reason at all to make it public anyway,
    // but what you do in that private function is entirely up to you.
    // You can call a function from anywhere else within that private callback.
    @SuppressWarnings("unused")
    private int nativeSynthCallback(byte[] audioData , int audioData_size) {
        if (mCallback == null)
            return 0;

        if (audioData == null || audioData_size==0) {
            mCallback.onSynthDataComplete();
            return 0;
        } else {
            return mCallback.onSynthDataReady(audioData,audioData_size);
        }
    }
    private int nativeSynthCallbackV2(ByteBuffer audioData , int audioData_size) {
        if (mCallback == null)
            return 0;

        if (audioData == null || audioData_size==0) {
            mCallback.onSynthDataComplete();
        } else {
            audioData.order(ByteOrder.LITTLE_ENDIAN);
            //LogUtils.w(TAG, "bufferAudio capacity:" +bufDataOut.remaining());

            byte[] audioData_byteArr = new byte[audioData.remaining()];
            audioData.get(audioData_byteArr);
            return mCallback.onSynthDataReady(audioData_byteArr,audioData_size);
        }
        return 0;

    }



    private static final int MBROLA_MIN_RATE = 25;
    private static final int MBROLA_MAX_RATE = 100;

    public void applySpeechRate(int blockRate, int settingRate) {
        final int normalizedBlockRate = clampRate(blockRate);
        final int normalizedSettingRate = clampRate(settingRate);
        final float averageRate = (normalizedBlockRate + normalizedSettingRate) / 2.0f;

        mSpeechRateValue = Math.round(averageRate);
        mOnnxLengthScale = rateToLengthScale(averageRate);

        LogUtils.i(TAG, "applySpeechRate blockRate=" + normalizedBlockRate
                + ", settingRate=" + normalizedSettingRate
                + ", averageRate=" + averageRate
                + ", length_scale=" + mOnnxLengthScale);
    }

    private int clampRate(int rate) {
        return Math.max(0, Math.min(100, rate));
    }

    private float rateToLengthScale(float averageRate) {
        float lengthScale;
        if (averageRate >= 50.0f) {
            lengthScale = 1.0f - ((averageRate - 50.0f) / 100.0f);
        } else {
            lengthScale = 1.0f + ((50.0f - averageRate) / 50.0f);
        }
        return Math.max(ONNX_LENGTH_SCALE_MIN, Math.min(ONNX_LENGTH_SCALE_MAX, lengthScale));
    }


    public int getSpeechRateValue() {
        return mSpeechRateValue;
    }



    private static boolean copyAssetFolder(AssetManager assetManager,
                                          String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            boolean res = true;
            for (String file : files)
                if (file.contains("."))
                    res &= copyAsset(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                else
                    res &= copyAssetFolder(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
            return res;
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to copy asset folder from " + fromAssetPath + " to " + toPath, e);
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager,
                                    String fromAssetPath, String toPath) {
        File destinationFile = new File(toPath);
        File parent = destinationFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LogUtils.w(TAG, "Failed to create directories for " + toPath);
            return false;
        }

        try (InputStream in = assetManager.open(fromAssetPath);
             OutputStream out = new FileOutputStream(destinationFile)) {
            //LogUtils.v(TAG, "toPath " + toPath);
            copyFile(in, out);
            out.flush();
            //LogUtils.w(TAG, fromAssetPath + " copyAsset Copied");
            return true;
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to copy asset from " + fromAssetPath + " to " + toPath, e);
            if (destinationFile.exists() && !destinationFile.delete()) {
                LogUtils.w(TAG, "Unable to delete partially written file " + toPath);
            }
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public void StopTts() {
    }

    public void setVolume(float volume, int mVolume) {
    }

    public interface SynthReadyCallback {
        int onSynthDataReady(byte[] audioData,int audioData_size);

        void onSynthDataComplete();
    }
    /** A signal handler in native code has been triggered. As our last gasp,
     * launch the crash handler (in its own process), because when we return
     * from this function the process will soon exit. */
    /*
    void nativeCrashed()
    {
        if (prefs != null) {
            try {
                System.err.println("saved game was:\n"+prefs.getString("savedGame",""));
            } catch(Exception e) {}
        }
        new RuntimeException("crashed here (native trace should follow after the Java trace)").printStackTrace();
        startActivity(new Intent(this, CrashHandler.class));
    }

     */
}
