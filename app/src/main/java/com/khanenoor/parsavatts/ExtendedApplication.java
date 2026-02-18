package com.khanenoor.parsavatts;

import static java.util.Objects.isNull;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StrictMode;
import android.os.UserManager;
import android.widget.Toast;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.khanenoor.parsavatts.util.LogUtils;
import com.khanenoor.parsavatts.PreferenceStorage;
import com.khanenoor.parsavatts.engine.FaTts;

import java.io.File;
import java.util.Arrays;
/*
Now I get error NullPointerException
when Program go in background , process terminated
So Objects can not stored in Application
Also I get leakage , because process terminated not free the texttospeech service object

 */

/*
However, you should never store mutable instance data inside the Application object because
if you assume that your data will stay there,
your application will inevitably crash at some point with a NullPointerException.
The application object is not guaranteed to stay in memory forever,
it will get killed. Contrary to popular belief,
the app won’t be restarted from scratch.
Android will create a new Application object
and start the activity where the user was before to give the illusion
that the application was never killed in the first place.
 */
/*
Your process was terminated, most likely. See the documentation and the documentation.

    What I need to be able to do is detect when android has flattened the cache

You are not informed when your process is terminated.

    or at least know the minimum amount of time that Android will keep the Application extension

Your process can be terminated milliseconds after it leaves the foreground.

    [Application works] fine as a data store

Only for data that you can easily reload from a persistent data store.

    [Application] works works on multi thread

Only if you add your own thread-synchronization logic. There is nothing magic about properties and functions on Application that makes them thread-safe.

    where there is no place to store mutable data - this is the best alternative

 */
public class ExtendedApplication extends MultiDexApplication {
    // in NLP dll GlobalObjects only one instance always is loaded
    // SpeechSynthesis may be two instance and must be differenct not singleton instance
    // Because mCallback function is different in different instances but NlpHand is same
    private static final String TAG = ExtendedApplication.class.getSimpleName();

    //private long mNlpHand = 0L;
    //private int  mSpeechSyn_ReferenceCount = 0;
    //must be common, solution is in pause activity both, save and close
    public UserDictionaryFile mUserDictionary = null;
    private static boolean preferDeviceProtectedStorage;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    public ExtendedApplication(){
        super();
        LogUtils.w(TAG,"ExtendedApplication construnctor is called! wakeup!");
    }

    private static File resolveCredentialProtectedPrefsFile(Context appContext, String prefsName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return new File(appContext.getDataDir(), "shared_prefs/" + prefsName + ".xml");
        }
        return new File(appContext.getApplicationInfo().dataDir, "shared_prefs/" + prefsName + ".xml");
    }

    private static Context selectStorageContext(Context appContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && deviceProtectedStorageContext != null
                && preferDeviceProtectedStorage) {
            return deviceProtectedStorageContext;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isUserUnlocked(appContext)) {
            LogUtils.w(TAG, "Device-protected storage unavailable while user is locked; falling back to app context");
        }
        return appContext;
    }

    /**
     * Determines whether credential-encrypted storage is available.
     *
     * @param context Any context tied to the current user.
     * @return true when the device user has unlocked their credentials.
     */
    public static boolean isUserUnlocked(Context context) {
        if (context == null) {
            LogUtils.w(TAG, "Context unavailable, assuming user is unlocked");
            return true;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return true;
        }
        final UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (userManager == null) {
            LogUtils.w(TAG, "UserManager unavailable, assuming user is unlocked");
            return true;
        }
        return userManager.isUserUnlocked();
    }
    public void initUserDictionary(){
        if(isNull(mUserDictionary)){
            mUserDictionary = new UserDictionaryFile(this);
        }
    }
    public void setNlpHand(long n){
        //mNlpHand = n;
        final Preferences prefs = new Preferences(ExtendedApplication.storageContext);
        prefs.set(Preferences.NLP_HAND, String.valueOf(n));

    }
    public long getNlpHand(){
        final Preferences prefs = new Preferences(ExtendedApplication.storageContext);
        String nlpHandStr=prefs.get(Preferences.NLP_HAND,"0");
        long nlpHand = 0L;
        try {
            nlpHand = Long.parseLong(nlpHandStr, 10);
        } catch(NumberFormatException nfe){
            LogUtils.w(TAG,"getNlpHand()"+ nfe.getMessage());
        }
        return nlpHand;
    }

    public void reloadUserDictionary() {
        reloadUserDictionary(getNlpHand());
    }

    public void reloadUserDictionary(long nlpHand) {
        if (nlpHand == 0L) {
            LogUtils.w(TAG, "reloadUserDictionary skipped: nlpHand is 0");
            return;
        }
        FaTts.ReloadUserDictionaryNLP(nlpHand);
    }
    public void IncEngineReferenceCount(){
        //mSpeechSyn_ReferenceCount++;
        final Preferences prefs = new Preferences(ExtendedApplication.storageContext);
        String refCountStr=prefs.get(Preferences.NLP_HAND_REFERENCE_COUNT,"0");
        int refCount = 0;
        try {
            refCount = Integer.parseInt(refCountStr, 10);
            refCount++;
        } catch(NumberFormatException nfe){
            LogUtils.w(TAG,"IncEngineReferenceCount()"+ nfe.getMessage());
        }
        prefs.set(Preferences.NLP_HAND_REFERENCE_COUNT, String.valueOf(refCount));
    }
    public void DecEngineReferenceCount(){
        /*
        mSpeechSyn_ReferenceCount--;
        if(mSpeechSyn_ReferenceCount==0){
            mNlpHand=0L;
        }
         */
        final Preferences prefs = new Preferences(ExtendedApplication.storageContext);
        String refCountStr=prefs.get(Preferences.NLP_HAND_REFERENCE_COUNT,"0");
        int refCount = 0;
        try {
            refCount = Integer.parseInt(refCountStr, 10);
            refCount--;
            if(refCount==0){
                prefs.set(Preferences.NLP_HAND, "0");
            }
        } catch(NumberFormatException nfe){
            LogUtils.w(TAG,"DecEngineReferenceCount()"+ nfe.getMessage());
        }
        prefs.set(Preferences.NLP_HAND_REFERENCE_COUNT, String.valueOf(refCount));
    }
    public static String determineArchName() {
        // Note that we cannot use System.getProperty("os.arch") since that may give e.g. "aarch64"
        // while a 64-bit runtime may not be installed (like on the Samsung Galaxy S5 Neo).
        // Instead we search through the supported abi:s on the device, see:
        // http://developer.android.com/ndk/guides/abis.html
        // Note that we search for abi:s in preferred order (the ordering of the
        // Build.SUPPORTED_ABIS list) to avoid e.g. installing arm on an x86 system where arm
        // emulation is available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (String androidArch : Build.SUPPORTED_ABIS) {
                switch (androidArch) {
                    case "arm64-v8a": return "aarch64";
                    case "armeabi-v7a": return "arm";
                    case "x86_64": return "x86_64";
                    case "x86": return "i686";
                }
            }
            throw new RuntimeException("Unable to determine arch from Build.SUPPORTED_ABIS =  " +
                    Arrays.toString(Build.SUPPORTED_ABIS));
        } else {
            return Build.CPU_ABI;
        }
    }
    private static Context storageContext;
    private static Context deviceProtectedStorageContext;
    private static Context credentialProtectedContext;

    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d(TAG, "ExtendedApplication.onCreate() called");
        //Related to Dual Directed
        final Context appContext = getApplicationContext();
        credentialProtectedContext = appContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            deviceProtectedStorageContext = appContext.createDeviceProtectedStorageContext();
            final String defaultPrefsName = PreferenceStorage.getDefaultSharedPreferencesName(appContext);
            final boolean hasCredentialPrefs = resolveCredentialProtectedPrefsFile(
                    appContext, defaultPrefsName).exists();
            final boolean userUnlocked = isUserUnlocked(appContext);
            final boolean migrationSucceeded = deviceProtectedStorageContext != null
                    && deviceProtectedStorageContext.moveSharedPreferencesFrom(appContext, defaultPrefsName);

            if (migrationSucceeded) {
                LogUtils.w(TAG, "Migrated preferences into device-protected storage for direct boot safety");
                preferDeviceProtectedStorage = true;
            } else if (!userUnlocked && deviceProtectedStorageContext != null) {
                LogUtils.w(TAG, "User locked; serving defaults from device-protected storage");
                preferDeviceProtectedStorage = true;
            } else if (!hasCredentialPrefs && deviceProtectedStorageContext != null) {
                LogUtils.w(TAG, "No credential-protected prefs yet; initializing device-protected storage");
                preferDeviceProtectedStorage = true;
            } else if (hasCredentialPrefs) {
                LogUtils.w(TAG, "Preference migration unavailable; keeping credential-protected storage active to preserve data");
                preferDeviceProtectedStorage = false;
            }
        }
        ExtendedApplication.storageContext = selectStorageContext(appContext);
        // Required initialization logic here!
        //Check , when become Idle after that GetUserDictionaryPath cause sigsegv , I want this function is called or not,
        final Preferences prefs = new Preferences(ExtendedApplication.storageContext);
        prefs.set(Preferences.NLP_HAND, "0");
        mUserDictionary = null;

        boolean isDebuggable = false;
        //mNlpHand = 0L;
        try {
            isDebuggable = ( 0 != ( this.getPackageManager().getApplicationInfo("com.khanenoor.parsavatts",0).flags & ApplicationInfo.FLAG_DEBUGGABLE ) );
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.w(TAG,"PackageManager.NameNotFoundException " + e.getMessage());
        } catch (NullPointerException e){
            LogUtils.w(TAG,"PackageManager.NullPointerException " + e.getMessage());
        } catch (Exception e){
            LogUtils.w(TAG,"Exception " + e.getMessage());
        }
        if(isDebuggable) {
            //but you can achieve the exact same thing without resorting to reflection using Strict Mode.
            //https://stackoverflow.com/questions/56911580/w-system-a-resource-failed-to-call-release
            StrictMode.enableDefaults();
                /*if (BuildConfig.DEBUG && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    //This message comes from dalvik.system.CloseGuard. When debugging, you can set it up to create stack traces as you create resources, so that you can track down what objects aren't being closed.
                    //
                    //It's not part of the framework API, so I'm using reflection to turn that on:
                    ReflectiveOperationException need KITKAT more
                    try {
                        Class.forName("dalvik.system.CloseGuard")
                                .getMethod("setEnabled", boolean.class)
                                .invoke(null, true);
                    } catch ( ReflectiveOperationException e) {
                        //throw new RuntimeException(e);
                    }
                }*/
        }

    }
    public static synchronized Context getStorageContext() {
        if (credentialProtectedContext == null) {
            return storageContext;
        }

        storageContext = selectStorageContext(credentialProtectedContext);
        return storageContext;
    }
    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        //4095 sometimes cause error , because ApplicationContext is not visual
        //Toast.makeText(getApplicationContext(),R.string.low_memory_msg,Toast.LENGTH_LONG).show();
        //I guess cause error hang , you must generate message on UI thread , and this is not activity
        /*
        َAlertDialog dlg = new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.app_name).setMessage(R.string.low_memory_msg).setOnCancelListener(mFinishCancelListener).create();
        dlg.show();
        */
        LogUtils.w(TAG,"ExtendedApplication.onLowMemory Detected!");

    }
    /*
    private final DialogInterface.OnCancelListener mFinishCancelListener = dialog -> {

    };
    */
    @Override
    public void onTerminate() {

        super.onTerminate();
        LogUtils.w(TAG,"ExtendedApplication.onTerminate called");
    }

}
