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
                if (mOnnxScalesInputName != null && !mOnnxScalesInputName.isEmpty()) {
                    float[] scales = new float[]{ONNX_NOISE_SCALE, mOnnxLengthScale, ONNX_NOISE_W};
                    try (OnnxTensor scalesTensor = OnnxTensor.createTensor(mOnnxEnvironment, java.nio.FloatBuffer.wrap(scales), new long[]{1, 3})) {
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
        } catch (Exception ex) {
            LogUtils.w(TAG, "synthWithOnnx failed", ex);
            return false;
        }
    }

    private long[] textToTokenIds(String text) {
        int[] codePoints = new int[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            codePoints = text.codePoints().toArray();
        }
        if (codePoints.length == 0) {
            return new long[]{0L};
        }
        final long[] tokens = new long[codePoints.length];
        for (int i = 0; i < codePoints.length; i++) {
            tokens[i] = Math.max(0, codePoints[i]);
        }
        return tokens;
    }

    private byte[] extractPcmWave(OrtSession.Result result) throws OrtException {
        if (result == null || result.size() == 0) {
            return null;
        }

        for (Map.Entry<String, OnnxValue> value : result) {
            if (!(value instanceof OnnxTensor)) {
                continue;
            }

            final Object tensorValue = ((OnnxTensor) value).getValue();
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
