package com.khanenoor.parsavatts.engine;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;

import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.impractical.Language;
import com.khanenoor.parsavatts.impractical.SpeechSynthesisConfigure;
import com.khanenoor.parsavatts.util.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FaTts implements Serializable {
    public void changePunctionanMode(int nValue) {
        SetOptionsNLP(nlpHand, EnumTTSParam.Opt_PuncMode.ordinal(),nValue);
    }

    public void changeDigitsLanguage(int nValue) {
        LogUtils.e(TAG, "changeDigitsLanguage: " + nValue);
        SetOptionsNLP(nlpHand, EnumTTSParam.Opt_Digits.ordinal(), nValue);
    }

    public void changeNumberMode(int nValue) {
        SetOptionsNLP(nlpHand, EnumTTSParam.Opt_DigitsReadMode.ordinal(),nValue);
    }

    public void changeEmojiMode(int nValue) {
        SetOptionsNLP(nlpHand, EnumTTSParam.Opt_EmojiActive.ordinal(),nValue);
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
    /////Load Libraries Section
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> INSTALL_CHECK_EXECUTOR.shutdownNow()));
        // Try to load the shared library.
        try {
            System.loadLibrary("SampleRate");
            // System.loadLibrary("cryptopp");
            setNativeLoggingEnabled(LogUtils.isEnabled());
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

        } catch (Exception ex){
            LogUtils.w(TAG,"FaTts.Insll " + ex.getMessage());
        }
    }
    public boolean Load(Context cnx,int nSpecialLexiconLoad, int nVoice,
                        int nMode, int nScreenReader) {
        String path =  "/data/data/" + package_name + "/Settings.xml";
        try {
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
        }  catch (Exception ex) {
            LogUtils.e(TAG, " Error in UnloadNLP " + ex.getMessage());
        }
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
