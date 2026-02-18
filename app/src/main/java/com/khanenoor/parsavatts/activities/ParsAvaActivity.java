package com.khanenoor.parsavatts.activities;


import static java.util.Objects.isNull;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.khanenoor.parsavatts.ExtendedApplication;
import com.khanenoor.parsavatts.Lock;
import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.R;
import com.khanenoor.parsavatts.SupportActivity;
import com.khanenoor.parsavatts.engine.CheckVoiceData;
import com.khanenoor.parsavatts.engine.DownloadVoiceData;
import com.khanenoor.parsavatts.impractical.IParsAvaWebService;
import com.khanenoor.parsavatts.util.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ParsAvaActivity extends Activity {
    private static final String TAG = ParsAvaActivity.class.getSimpleName();
    private static final String ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";

    /**
     * Handler code for TTS initialization hand-off.
     */

    private static final int TTS_INITIALIZED = 1;

    private static final int REQUEST_CHECK = 1;
    private static final int REQUEST_DOWNLOAD = 2;
    private static final int REQUEST_DEFAULT = 3;
    private static final int DIALOG_SET_DEFAULT = 1;
    private static final int DIALOG_DOWNLOAD_FAILED = 2;
    private static final int DIALOG_ERROR = 3;
    private static final int DIALOG_UPDATE_VERSION_NEED = 4;
    private static final int DIALOG_PERMISSION_CALL_NEED = 5;
    private static final long LICENSE_CHECK_TIMEOUT_MS = 500L;
    private final DialogInterface.OnClickListener mDialogClickListener = (dialog, which) -> {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            startActivityForResult(new Intent(ACTION_TTS_SETTINGS), REQUEST_DEFAULT);
        }
    };
    private final DialogInterface.OnClickListener mReportClickListener = (dialog, which) -> {
        // TODO: Send a crash report.
        finish();
    };
    private final DialogInterface.OnClickListener mFinishClickListener = (dialog, which) -> finish();
    private final DialogInterface.OnCancelListener mFinishCancelListener = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            finish();
        }
    };
    private final DialogInterface.OnClickListener mNothingDoneListener = (dialog, which) -> {

    };
    private TextToSpeech mTts = null;
    private boolean mDownloadedVoiceData;
    private boolean mInitializationCompleted;
    private ArrayList<String> mVoices;
    private final ExecutorService mLicenseCheckExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService mLicenseTimeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Button mSampleReadButton = null;
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @SuppressLint("ObsoleteSdkInt")
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.engine_command) {
                /*startActivityForResult(
                        new Intent(ParsAvaActivity.this, TtsSettingsActivity.class),
                        REQUEST_DEFAULT);

                 */
                Intent myIntent = new Intent(ParsAvaActivity.this, VoiceSettingsActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                if(isIntentAvailable(myIntent,ParsAvaActivity.this)) {
                    ParsAvaActivity.this.startActivity(myIntent);
                }
            }
           /* else if(v.getId()==R.id.ttsSettings){
                startActivityForResult(new Intent(ACTION_TTS_SETTINGS), REQUEST_DEFAULT);

            }*/
            else if (v.getId() == R.id.tts_sample) {
                LogUtils.w(TAG, "ParsAvaActivity.mOnClickListener tts_sample clicked");
                mSampleReadButton.setEnabled(false);
                //SampleRatesSupport();
                Runnable r = () -> {
                    if (isNull(mTts)) {
                        new Thread() {
                            public void run() {
                                ParsAvaActivity.this.runOnUiThread(() -> {
                                    mSampleReadButton.setEnabled(true);
                                });
                            }
                        }.start();
                        LogUtils.w(TAG, "app.mTts==null why?");
                    } else {
                        Preferences preferences = new Preferences(ExtendedApplication.getStorageContext());
                        //int persianRate = preferences.getPersianRate();
                        //float normalizedRate = (1.0F * persianRate) / 100.0F;
                        //mTts.setSpeechRate(normalizedRate);
                        mTts.setLanguage(new Locale("fas", "IRN", ""));
                        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {

                            }

                            @Override
                            public void onDone(String utteranceId) {
                                new Thread() {
                                    public void run() {
                                        ParsAvaActivity.this.runOnUiThread(() -> {
                                            mSampleReadButton.setEnabled(true);
                                        });
                                    }
                                }.start();
                            }

                            @Override
                            public void onError(String utteranceId) {
                                new Thread() {
                                    public void run() {
                                        ParsAvaActivity.this.runOnUiThread(() -> {
                                            mSampleReadButton.setEnabled(true);
                                        });
                                    }
                                }.start();
                            }

                            @Override
                            public void onStop(String utteranceId, boolean interrupted) {
                                new Thread() {
                                    public void run() {
                                        ParsAvaActivity.this.runOnUiThread(() -> {
                                            mSampleReadButton.setEnabled(true);
                                        });
                                    }
                                }.start();
                            }
                        });
                        //ParsAvaUtteranceProgressListener utteranceProgressListener;
                        //utteranceProgressListener = new ParsAvaUtteranceProgressListener();
                        //mTts.setOnUtteranceProgressListener(utteranceProgressListener);
                        String strTextMain = " این یک متن نمونه به فارسی و نیز English می باشد. سعدی شیرازی در شهر شیراز و در سال 606 تا 690 هجری زیست. This is an English sample text. ";
                        String strTextMain1 = " این یک متن نمونه به فارسی می باشد. سعدی شیرازی در شهر شیراز و در سال 606 تا 690 هجری زیست.  ";
                        String strTextMain2 = "This is an English sample text. This is an English sample text. This is an English sample text. This is an English sample text. This is an English sample text.  ";
                        //String strTextMain = "اکی   ";
                        if (Build.VERSION.SDK_INT < 21) {
                            HashMap<String, String> hashMap = new HashMap<>();

                            //mTts.setLanguage(new Locale("fas","IRN",""));
                            //app.mTts.setLanguage(new Locale("eng", "USA", ""));

                            hashMap.put("utteranceId", "ParsAva_Test_Fa");
                            //String strTextOld = "این یک متن نمونه English Document هست که برای تست Test in English خوانده می شود! اکنون متن را طولانی تر نموده و به سطح یک Paragraph می رسانیم. لازم به ذکر است که سعدی شیرازی در شهر Shiraz from Iran متولد گردید. ";
                            mTts.speak(strTextMain, TextToSpeech.QUEUE_FLUSH, hashMap);
                            //hashMap.clear();
                            //hashMap.put("utteranceId", "ParsAva_Test_En");
                            //hashMap.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "0.0"); // change the 0.0 to any value from 0-1 (1 is default)

                            //mTts.speak(" This is English Text ", TextToSpeech.QUEUE_ADD, hashMap);

                        } else {
                            //HashMap<String, String> hashMap = new HashMap<>();

                            //mTts.setLanguage(new Locale("fas","IRN",""));
                            //hashMap.put("utteranceId", "ParsAva_Test_En");

                            //mTts.setLanguage(new Locale("eng","USA",""));
                            //0.5 must double slow , 2.0 must double speed , 1.0 is normal value
                            //app.mTts.setSpeechRate(2.0F);
                            //1.0 is normal , lower than 1.0 must lower , greater must increase
                            //app.mTts.setPitch(0.9F);
                            Bundle params = new Bundle();
                            //params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.5f); // change the 0.5f to any value from 0f-1f (1f is default)
                            //mTts.speak("من مجید نم نبات هستم! ", TextToSpeech.QUEUE_FLUSH, null,"ParsAva_Test_Fa");
                            //mTts.speak("این یک متن نمونه English Document هست که برای تست Test in English خوانده می شود! اکنون متن را طولانی تر نموده و به سطح یک Paragraph می رسانیم. لازم به ذکر است که سعدی شیرازی در شهر Shiraz from Iran متولد گردید. ", TextToSpeech.QUEUE_FLUSH,  params,"ParsAva_Test_Fa");
                            String strText1 = "'Swipe up' or 'Swipe down' to adjust, Labels available; use Tap with 3 fingers to view.";
                            String strText2 = " \uD83D\uDD39 این یک متن نمونه English Document هست که برای تست Test in English خوانده می شود! اکنون متن را طولانی تر نموده و به سطح یک Paragraph می رسانیم. لازم به ذکر است که سعدی شیرازی در شهر Shiraz from Iran متولد گردید. ";
                            String strText3 = "\uD83D\uDE01";//😁 ParsTextNLP char1: 1f601
                            String strText5 = "\uD83C\uDDEE\uD83C\uDDF7پرچم ایران ";
                            String strText6 = "1285405722";
                            String strText7 = "❤️❣️";
                            String strText8 = "س";
                            String strText9 = "Screen off.";
                            String strText10 = "Amir Hatami";
                            String strText11 = "مانده 1,306,259";
                            String strText12 = new String(new byte[]{0x0a});
                            String strText13 = "تنظیمات صدا, Out of list";
                            String strText14 = "🔹آن شب ما خواب بودیم، دوساعتی می‌شد که کمپ را تعطیل کرده بودیم و در چادر بودیم که یکهو متوجه شدیم افرادی وارد کمپ شدند و با سر و صدا و شلیک تیرهوایی همه ما را از داخل چادرها بیرون کشیدند. در ادامه هم تا جایی که می‌توانستند ما را کتک زدند و فریاد می‌زدند که شب سوم محرم آمده‌اید تفریح؟ مدام می‌پرسیدند که مشروب خوردید؟ اعتراف کنید که مشروب خورده‌اید. ما در جواب می‌گفتیم نه و باز هم ما را می‌زدند. تمام وسایل ما را از داخل چادرها بیرون ریختند. با باتوم و شوکر می‌زدند. یکی از آنها با قنداق اسلحه به سینه یکی از دوستانمان زد و با باتوم و شوکر به کمر من. آنقدر ضربه سنگین بود که اعصاب کمرم آسیب دیده و یک هفته است نمی‌توانم روی پاهایم بایستم. بقیه را هم با باتوم می‌زدند.\n";
                            String strText15 = "آن شب ما خواب بودیم، دوساعتی می‌شد که کمپ را تعطیل کرده بودیم و در چادر بودیم که یکهو متوجه شدیم افرادی وارد کمپ شدند و با سر و صدا و شلیک تیرهوایی همه ما را از داخل چادرها بیرون کشیدند. در ادامه هم تا جایی که می‌توانستند ما را کتک زدند و فریاد می‌زدند که شب سوم محرم آمده‌اید تفریح؟ مدام می‌پرسیدند که مشروب خوردید؟ اعتراف کنید که مشروب خورده‌اید. ما در جواب می‌گفتیم نه و باز هم ما را می‌زدند. تمام وسایل ما را از داخل چادرها بیرون ریختند. با باتوم و شوکر می‌زدند. یکی از آنها با قنداق اسلحه به سینه یکی از دوستانمان زد و با باتوم و شوکر به کمر من. آنقدر ضربه سنگین بود که اعصاب کمرم آسیب دیده و یک هفته است نمی‌توانم روی پاهایم بایستم. بقیه را هم با باتوم می‌زدند.\n";
                            String strText16 = "🔹آن شب ما خواب بودیم، دوساعتی می‌شد که کمپ را تعطیل کرده بودیم و در چادر بودیم که یکهو متوجه شدیم افرادی وارد کمپ شدند و با سر و صدا و شلیک تیرهوایی همه ما را از داخل چادرها بیرون کشیدند. در ادامه هم تا جایی که می‌توانستند ما را کتک زدند و فریاد می‌زدند که شب سوم محرم آمده‌اید تفریح؟ مدام می‌پرسیدند که مشروب خوردید؟ اعتراف کنید که مشروب خورده‌اید. ما در جواب می‌گفتیم نه و باز هم ما را می‌زدند. تمام وسایل ما را از داخل چادرها بیرون ریختند. با باتوم و شوکر می‌زدند. یکی از آنها با قنداق اسلحه به سینه یکی از دوستانمان زد و با باتوم و شوکر به کمر من. آنقدر ضربه سنگین بود که اعصاب کمرم آسیب دیده و یک هفته است نمی‌توانم روی پاهایم بایستم. بقیه را هم با باتوم می‌زدند.\n";
                            String strText17 = "❤️♥️";
                            String strText18 = "\uD83D\uDD39رحیم سرهنگی مدیرعامل سازمان منطقه آزاد کیش، در حکمی مهدی خانعلی زاده را به عنوان مدیر روابط عمومی سازمان منطقه آزاد کیش منصوب کرد.";
                            String strText19 = "در میان مدت البته طبق تحلیل 30 . 8 . 00 همچنان اعداد بالاتری برای این شاخص محتمل است.";
                            String strText20 = "تماس ورودی, \u200F\u202A+98 21 9100 2911\u202C\u200F, ۱۸:۱۶, ۸ از ۱۰۹";
                            String strText21 = ", \u200F\u202A+98 21 9100 2911\u202C\u200F";
                            String strText22 = ", \u200F\u202A+98 21 9100 2911\u202C\u200F";
                            String strText23 = "";
                            String strText24 = "21 9100 29";
                            String strText25 = "\u200F\u202A8 2";
                            String strText26 = "انتقال وجه با سپرده\n" + "برداشت از:700797804237\n" + "مبلغ:10,000,000ريال\n" + "مبلغ:+10,000,000  ريال\n" + "موجودي:4,267,603 ريال" + "3/4 نسبت" + "19,876,789";
                            String strText27 = "Book 98734 book";

                            //((EditText)findViewById(R.id.sample_text)).setText(strText25);
                            //String strSampleText = ((EditText)findViewById(R.id.sample_text)).getText().toString();
                            mTts.speak(strTextMain, TextToSpeech.QUEUE_FLUSH, params, "ParsAva_Test_Fa");

                            //while(utteranceProgressListener.mIsBusy);
                            //mTts.speak(" This is English Text ", TextToSpeech.QUEUE_ADD, params , "ParsAva_Test_En");

                        }
                    }
                };
                Thread threadProducer = new Thread(r);
                threadProducer.start();
                LogUtils.w(TAG, "ParsAvaActivity.mOnClickListener tts_sample thread run");

            } else if (v.getId() == R.id.puncuation_command) {
                //PuncuationsDialog dialog=new PuncuationsDialog(ParsAvaActivity.this);
                //dialog.show();
                Intent myIntent = new Intent(ParsAvaActivity.this, PunctuationsActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                ParsAvaActivity.this.startActivity(myIntent);

            } else if (v.getId() == R.id.number_command) {
                Intent myIntent = new Intent(ParsAvaActivity.this, NumbersActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                ParsAvaActivity.this.startActivity(myIntent);

            } else if (v.getId() == R.id.number_language_command) {
                Intent myIntent = new Intent(ParsAvaActivity.this, NumberLanguageActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                ParsAvaActivity.this.startActivity(myIntent);

            } else if (v.getId() == R.id.emoji_command) {
                Intent myIntent = new Intent(ParsAvaActivity.this, EmojiSettingsActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                ParsAvaActivity.this.startActivity(myIntent);

            } else if (v.getId() == R.id.register_command) {
                Intent myIntent = new Intent(ParsAvaActivity.this, LicenseActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                ParsAvaActivity.this.startActivity(myIntent);


            } else if (v.getId() == R.id.save_to_file) {
                LogUtils.w(TAG, "save_to_file is clicked");
                Intent myIntent = new Intent(ParsAvaActivity.this, SaveToFileActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                ParsAvaActivity.this.startActivity(myIntent);
            } 
            else if (v.getId() == R.id.support_command) {
                Intent myIntent = new Intent(ParsAvaActivity.this, SupportActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                ParsAvaActivity.this.startActivity(myIntent);
            } else if (v.getId() == R.id.about_command) {
                Intent myIntent = new Intent(ParsAvaActivity.this, AboutActivity.class);
                ParsAvaActivity.this.startActivity(myIntent);

            }
        }
    };


    private final DialogInterface.OnClickListener mShowSettingsListener = (dialog, which) -> {
        loadingIsDone();
    };
    /*private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TTS_INITIALIZED:
                    break;
            }
        }
    };*/
    private String mLinkUpdate = "";
    private final TextToSpeech.OnInitListener mInitListener = status -> {
        if (status == TextToSpeech.SUCCESS) {
            onInitialized(status);

        }
        //mHandler.obtainMessage(TTS_INITIALIZED, status, 0).sendToTarget();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pars_ava);
        setState(State.LOADING);
        manageSettingVisibility();
        checkVoiceData();
        mSampleReadButton = findViewById(R.id.tts_sample);
        mSampleReadButton.setEnabled(false);
        if(Lock.IS_CHECKLOCK) {
            findViewById(R.id.register_command).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.w(TAG, "ParsAvaActivity.onDestroy called");

        mLicenseCheckExecutor.shutdownNow();
        mLicenseTimeoutExecutor.shutdownNow();
        if (!isNull(mTts)) {
            LogUtils.w(TAG, "ParsAvaActivity.onDestroy  mTts is shutdown");
            mTts.shutdown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.w(TAG, "ParsAvaActivity.onResume called");
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtils.w(TAG, "ParsAvaActivity.onStart called");
    }

    @Override
    public void onStop() {
        LogUtils.w(TAG, "ParsAvaActivity.onStop called");
        super.onStop();
    }

    /**
     * Sets the UI state.
     *
     * @param state The current state.
     */
    private void setState(State state) {
        findViewById(R.id.loading).setVisibility((state == State.LOADING) ? View.VISIBLE : View.GONE);
        findViewById(R.id.success).setVisibility((state == State.SUCCESS) ? View.VISIBLE : View.GONE);
        findViewById(R.id.failure).setVisibility((state == State.FAILURE) ? View.VISIBLE : View.GONE);
    }

    /**
     * Launcher the voice data verifier.
     */
    private void checkVoiceData() {
        final Intent checkIntent = new Intent(this, CheckVoiceData.class);

        startActivityForResult(checkIntent, REQUEST_CHECK);
    }

    /**
     * Launches the voice data installer.
     */
    private void downloadVoiceData() {
        final Intent checkIntent = new Intent(this, DownloadVoiceData.class);

        startActivityForResult(checkIntent, REQUEST_DOWNLOAD);
    }

    /**
     * Initializes the TTS engine.
     */
    private void initializeEngine() {
        //Create new Instance to Increment Reference Count
        LogUtils.w(TAG, "ParsAvaActivity.initializeEngine mTts  new");

        mTts = new TextToSpeech(this, mInitListener, this.getPackageName());
        //onInitialized(TextToSpeech.SUCCESS);
        LogUtils.w(TAG, "ParsAvaActivity.initializeEngine new TextToSpeech");
    }

    /**
     * Hides preferences according to SDK level.
     */
    private void manageSettingVisibility() {
        /*
        if (Build.VERSION.SDK_INT < 14) {

            // Hide the eSpeak setting button on pre-ICS.
            findViewById(R.id.engineSettings).setVisibility(View.GONE);
        }*/
    }

    /**
     * Handles the result of voice data verification. If verification fails
     * following a successful installation, displays an error dialog. Otherwise,
     * either launches the installer or attempts to initialize the TTS engine.
     *
     * @param resultCode The result of voice data verification.
     * @param data       The intent containing available voices.
     */
    private void onDataChecked(int resultCode, Intent data) {
        if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            if (mDownloadedVoiceData) {
                setState(State.FAILURE);
                showDialog(DIALOG_ERROR);
            } else {
                downloadVoiceData();
            }
            return;
        }

        mVoices = data.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
        initializeEngine();
    }

    /**
     * Handles the result of voice data installation. Either shows a failure
     * dialog or launches the voice data verifier.
     *
     * @param resultCode
     */
    private void onDataDownloaded(int resultCode) {
        if (resultCode != RESULT_OK) {
            setState(State.FAILURE);
            showDialog(DIALOG_DOWNLOAD_FAILED);
            return;
        }

        mDownloadedVoiceData = true;

        checkVoiceData();
    }

    /**
     * Handles the result of TTS engine initialization. Either displays an error
     * dialog or populates the activity's UI.
     *
     * @param status The TTS engine initialization status.
     */
    // This function called when TTS instance is inited
    private void onInitialized(int status) {
        if (status == TextToSpeech.SUCCESS) {
            findViewById(R.id.tts_sample).setEnabled(true);
            final String packageName = getPackageName();
            Preferences preferences = new Preferences(ExtendedApplication.getStorageContext());
            boolean firstRunCompleted = preferences.getBoolean(Preferences.FIRST_RUN_COMPLETED, false);
            if (!firstRunCompleted) {
                preferences.setBoolean(Preferences.FIRST_RUN_COMPLETED, true);
                handleInitializationCompletion(status);
                return;
            }
            handleInitializationCompletion(status);
            //checkLicenseAsync(packageName, hasLicense -> handleLicenseCheckResult(status, packageName, hasLicense));
            return;
        }
        handleInitializationCompletion(status);
    }

    private void handleLicenseCheckResult(int status, String packageName, boolean hasLicense) {
        /*Lock.IsLicenseFileExist(getPackageName()) this condition insert why?
            because if license is not valid and so not speaking can input to system with oher tts
        * */
        if (mTts != null && packageName != null && !packageName.equals(mTts.getDefaultEngine()) && (hasLicense || !Lock.IS_CHECKLOCK)) {
            showDialog(DIALOG_SET_DEFAULT, null);
            return;
        } else if (Lock.IsInternetConnection(this)) {
            //Check Version and File Sign only ?
            //Or check License all ?
            //and update date of License ,
            //lic must be delete and renew ,
            //may be speak cancelled when file existance ,
            // we can't call registerV2, must call getLicenseKey ,
            //but getLicenseKey or common in android and computer
            // BuildOutputFormat in server not called , there was two ways:
            //one: change web server function platform based , return full output,
            //call BuildOutputFormat and then replace lic file ,
            //in this saturation replace is can cause check license (only First time)
            //not work , (pay attention: TTS is initialized , speak may be not called yet
            // or talkback is on , talkback is on then speaked so license is checked
            //  second solution , in check function , in delete randomly after 6 month,
            // save static global variable data (I think this modify and check always done in one function and call so lock not need)
            // but How can check license , version and filesigns ,
            // watch and pay attention: filesigns not benefitial work , it is in java , if modify source code , they remove these codes so
            // remove these sections , But check version is really need , to news updates also version updates consider
            // also getProductKey return licensekey , are you store license key ? only check license key with the file lic and update date
            // where check license key ? have a global static contained license key.

            // Investigation Finished . call checkVersion, getLicensekey , save ProductKey in prefs.
            // build a functions set date and (dates must checked be after lic date)
            // and set license key
            // in delete lic randomly , if this global variables set update lic
            // problem : if talksback on , check lic only once , delete only once in last ,
            // so a new function if checkLicense insert if variables set , update variables
            // This is not for hack , it is hackable , it is to help legable customers more , announce updates
            // , not removing license randomly
            //delete randomly can be decrease its random , from 7 to 5
            //mFirstCheck variable must unchecked it is ttslib , This is most in order that mFirstCheck
            // move to lock.cpp
            ////////////////////////////////////////Read Version of Manifest and Encrypt it
            Preferences prefs = new Preferences(ExtendedApplication.getStorageContext());
            IParsAvaWebService parsWebService = Lock.getRetrofitInstance();

            String encryptVersionCode = "";
            try {
                int versionCode = getPackageManager().getPackageInfo(packageName, 0).versionCode;
                //Version Code is in Build.Gradle
                encryptVersionCode = Lock.getEncodeData(packageName, String.valueOf(versionCode), Lock.STYLE_PUBLIC_LICENSE_SERVER_ONLY);
                //LogUtils.w(TAG,"ParsAvaActivity versionCode :" + versionCode + " encryptVersionCode : " + encryptVersionCode);
            } catch (PackageManager.NameNotFoundException e) {
                //encryptVersionCode ="";
                //throw new RuntimeException(e);
                LogUtils.w(TAG, "ParsAvaActivity.onInitialized catch exception PackageManager.NameNotFoundException" + e.getMessage());
            }

            String productKey = prefs.get(Preferences.APP_PRODUCT_KEY, "");
            String encryptHardwareAppId = "";
            if (!TextUtils.isEmpty(productKey)) {
                String phoneCode = Lock.getHardwareCode(this);
                String appLicenseUniqueId = Lock.Generate_Or_Get_App_UUID(packageName, this, false);
                if (!TextUtils.isEmpty(appLicenseUniqueId)) {
                    String hardwareAppId = Lock.getCombineHardAppId(packageName, getApplicationContext(), phoneCode, appLicenseUniqueId);
                    //LogUtils.w(TAG, "LicenseActivity.mOnClickListener hardwareAppId:" + hardwareAppId);
                    encryptHardwareAppId = Lock.getEncodeData(packageName, hardwareAppId, Lock.STYLE_PUBLIC_LICENSE_SERVER_AND_ENCRYPT);
                    //LogUtils.w(TAG, "LicenseActivity.mOnClickListener encrypted hardwareAppId : " + encryptHardwareAppId);
                }
            }
            String requestBodyText = Lock.MakeRequest_GetLicenseKeyV2(productKey, encryptHardwareAppId, encryptVersionCode);
            RequestBody requestBody = RequestBody.create(requestBodyText, MediaType.parse("text/xml"));
            Call<ResponseBody> FuncGetLicenseKeyV2 = parsWebService.GetLicenseKeyV2(requestBody);
            FuncGetLicenseKeyV2.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        //int statusCode = response.code();
                        //LogUtils.w(TAG, "status code: " + statusCode);
                        String strInput = null;
                        try {
                            if (response.body() == null) {
                                return;
                            }
                            strInput = response.body().string();
                        } catch (IOException e) {
                            //throw new RuntimeException(e);
                            LogUtils.w(TAG, "LicenseActivity.GetLicenseKey exception " + e.getMessage());
                        }
                        String result = Lock.Analyse_ResponseXml(strInput, "GetLicenseKeyV2Result");
                        //LogUtils.w(TAG, "onResponse response: " + result);
                        String[] arrOfSections = result.split("#", 0);
                        if (arrOfSections.length > 0 && arrOfSections[0].equals("1031")) {
                            Lock.DeleteLicenseFile(packageName);
                            if (arrOfSections.length > 1 && arrOfSections[1].length() > 0) {
                                //Update Link is here
                                mLinkUpdate = arrOfSections[1];
                            }
                            showDialog(DIALOG_UPDATE_VERSION_NEED);
                        }
                        //result 0 : Succeed
                        //result other 0 : Not Found
                        else if (Lock.IS_CHECKLOCK && (arrOfSections.length > 0 && arrOfSections[0].equals("0"))) {
                            if (arrOfSections.length > 2 && arrOfSections[2].length() > 0) {
                                //Lock.DeleteLicenseFile(packageName);
                                //Lock.CreateLicenseFile(packageName, arrOfSections[2]);
                            }
                            //1031 error in invalid version and url is send with it
                        } else if (Lock.IS_CHECKLOCK && arrOfSections.length > 0) {
                            //License is not valid and not expired
                            LogUtils.w(TAG, "ParsAvaActivity License is not Valid");
                            //v58 Lock.DeleteLicenseFile(packageName);
                        }
                    }

                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    LogUtils.w(TAG, "Call WebService ParsAvaActivity Receive License failed");
                }
            });


            LogUtils.w(TAG, "ParsAvaActivity.mInitListener SUCECSS");
        } else if (Lock.IS_CHECKLOCK && !hasLicense) {
            Intent myIntent = new Intent(ParsAvaActivity.this, LicenseActivity.class);
            if(isIntentAvailable(myIntent,ParsAvaActivity.this)) {
                ParsAvaActivity.this.startActivity(myIntent);
            }
        } else if (Lock.IS_CHECKLOCK && (hasLicense && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)) {
            //I guess haj jafari denied this
            //I guess this is very easily and can cause unpredicted behaviour
            try {
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

                /*
                 * Android Q has restricted to access for both IMEI and serial no.
                 *  It is available only for platform and apps with special carrier permission.
                 *  Also the permission READ_PRIVILEGED_PHONE_STATE is not available
                 * for non platform apps.
                 */
                if (telephonyManager != null) /* Android 10 Level 29*/ {
                    // if permissions are not provided we are requesting for permissions.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, Lock.REQUEST_CODE);
                }
            } catch (Exception ex) {
                LogUtils.w(TAG, "getPhoneCode generate exception ");
            }


        }
        handleInitializationCompletion(status);
    }

    private void handleInitializationCompletion(int status) {
        if (mInitializationCompleted) {
            return;
        }
        mInitializationCompleted = true;
        if (status == TextToSpeech.ERROR || mVoices == null) {
            setState(State.FAILURE);
            showDialog(DIALOG_ERROR);
            return;
        }
        /*
        final Locale ttsLocale = mTts.getLanguage();

        final String localeText = getString(
                R.string.current_tts_locale, ttsLocale.getDisplayName());

        final TextView currentLocale = (TextView) findViewById(R.id.currentLocale);
        currentLocale.setText(localeText);

        final String voicesText = getString(R.string.available_voices, mVoices.size());

        final TextView availableVoices = (TextView) findViewById(R.id.availableVoices);
        availableVoices.setText(voicesText);
        */
        //findViewById(R.id.ttsSettings).setOnClickListener(mOnClickListener);
        loadingIsDone();
    }

    private void checkLicenseAsync(String packageName, LicenseCheckCallback callback) {
        AtomicBoolean delivered = new AtomicBoolean(false);
        Future<?> future = mLicenseCheckExecutor.submit(() -> {
            boolean hasLicense = Lock.IsLicenseFileExist(packageName);
            deliverLicenseResult(callback, delivered, hasLicense);
        });
        mLicenseTimeoutExecutor.schedule(() -> {
            if (delivered.compareAndSet(false, true)) {
                future.cancel(true);
                LogUtils.w(TAG, "License check timed out after " + LICENSE_CHECK_TIMEOUT_MS + "ms");
                mMainHandler.post(() -> callback.onResult(false));
            }
        }, LICENSE_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void deliverLicenseResult(LicenseCheckCallback callback, AtomicBoolean delivered, boolean result) {
        if (delivered.compareAndSet(false, true)) {
            mMainHandler.post(() -> callback.onResult(result));
        }
    }

    private interface LicenseCheckCallback {
        void onResult(boolean hasLicense);
    }

    private void loadingIsDone() {
        findViewById(R.id.engine_command).setOnClickListener(mOnClickListener);
        findViewById(R.id.puncuation_command).setOnClickListener(mOnClickListener);
        findViewById(R.id.number_command).setOnClickListener(mOnClickListener);
        findViewById(R.id.number_language_command).setOnClickListener(mOnClickListener);
        findViewById(R.id.emoji_command).setOnClickListener(mOnClickListener);
        findViewById(R.id.add_word).setOnClickListener(mOnClickListener);
        findViewById(R.id.word_list).setOnClickListener(mOnClickListener);
        findViewById(R.id.user_dictionary_manage).setOnClickListener(mOnClickListener);
        findViewById(R.id.save_to_file).setOnClickListener(mOnClickListener);
        findViewById(R.id.tts_sample).setOnClickListener(mOnClickListener);
        findViewById(R.id.register_command).setOnClickListener(mOnClickListener);
        findViewById(R.id.support_command).setOnClickListener(mOnClickListener);
        findViewById(R.id.about_command).setOnClickListener(mOnClickListener);

        setState(State.SUCCESS);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK:
                onDataChecked(resultCode, data);
                break;
            case REQUEST_DOWNLOAD:
                onDataDownloaded(resultCode);
                break;
            case REQUEST_DEFAULT:
                LogUtils.w(TAG, "ParsAvaActivity.onActivityResult REQUEST_DEFAULT initializeEngine");

                initializeEngine();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    /*
    private void disableBackButton(){
        setDefaultKeyMode();
        ((AppCompatActivity) this).setDisplayShowCustomEnabled(false);
        (getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        //((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
    }*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_SET_DEFAULT:

                return new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.app_name).setMessage(R.string.set_default_message).setNegativeButton(android.R.string.no, mShowSettingsListener).setPositiveButton(android.R.string.ok, mDialogClickListener).create();
            case DIALOG_DOWNLOAD_FAILED:
                return new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.app_name).setMessage(R.string.voice_data_failed_message).setNegativeButton(android.R.string.ok, mFinishClickListener).setOnCancelListener(mFinishCancelListener).create();
            case DIALOG_ERROR:
                return new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.app_name).setMessage(R.string.error_message).setNegativeButton(android.R.string.no, mFinishClickListener).setNegativeButton(android.R.string.ok, mReportClickListener).setOnCancelListener(mFinishCancelListener).create();
            case DIALOG_UPDATE_VERSION_NEED:
                LogUtils.w(TAG, "ParsAvaActivity.onCreateDialog DIALOG_UPDATE_VERSION_NEED called");
                String msgOutDate = getResources().getString(R.string.version_apk_outdated);
                String msgUpdate = getResources().getString(R.string.version_apk_update);
                //final SpannableString msgLink = new SpannableString(mLinkUpdate);
                //mLinkUpdate = "https://wir3.maralhost.com:8443/smb/file-manager/download/domainId/1216/?currentDir=%2Fhttpdocs%2FSetups%2F&file=parsava_3900.APK";
                Spanned termsMsg = Html.fromHtml(msgOutDate + "\n" + msgUpdate + "\n" + "<a href='" + mLinkUpdate + "' download>لینک دانلود</a>", Html.FROM_HTML_MODE_COMPACT);
                //Linkify.addLinks(msgLink,Linkify.ALL);
                //String allMessages = msgOutDate + "\n" + msgUpdate + "\n" + msgLink + "\n";
                TextView tv = new TextView(this);
                tv.setLinksClickable(true);
                tv.setPadding(10, 10, 10, 10);
                tv.setGravity(Gravity.CENTER_VERTICAL);
                tv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                //tv.setWidth(View.M);
                tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
                //tv.setW
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                tv.setText(termsMsg);

                return new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.app_name)
                        //.setMessage(termsMsg)
                        .setView(tv).setNegativeButton(android.R.string.ok, mFinishClickListener).setOnCancelListener(mFinishCancelListener).create();
            case DIALOG_PERMISSION_CALL_NEED:
                return new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.app_name).setMessage(R.string.permission_call_need).setNegativeButton(android.R.string.ok, mNothingDoneListener).setOnCancelListener(mFinishCancelListener).create();

        }

        return super.onCreateDialog(id);
    }

    /*

    private ArrayList<ContainerVoiceEngine> containerVEArray;
    private int requestCount;
    private void getEngines() {

        requestCount = 0;

        final Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

        final PackageManager pm = getPackageManager();

        final List<ResolveInfo> list = pm.queryIntentActivities(ttsIntent, PackageManager.GET_META_DATA);

        containerVEArray = new ArrayList<ContainerVoiceEngine>(list.size());

        for (int i = 0; i < list.size(); i++) {

            final ContainerVoiceEngine cve = new ContainerVoiceEngine();

            cve.setLabel(list.get(i).loadLabel(pm).toString());
            cve.setPackageName(list.get(i).activityInfo.applicationInfo.packageName);

            final Intent getIntent = new Intent();
            getIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

            getIntent.setPackage(cve.getPackageName());
            getIntent.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
            getIntent.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES);

            cve.setIntent(getIntent);

            containerVEArray.add(cve);
        }

        LogUtils.d("TAG", "containerVEArray: " + containerVEArray.size());

        for (int i = 0; i < containerVEArray.size(); i++) {
            startActivityForResult(containerVEArray.get(i).getIntent(), i);
        }

    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        LogUtils.i("TAG", "onActivityResult: requestCount: " + " - requestCode: " + requestCode);

        requestCount++;

        try {

            if (data != null) {

                final Bundle bundle = data.getExtras();

                if (bundle != null) {

                    LogUtils.d("TAG", containerVEArray.get(requestCode).getLabel() + " - Bundle Data");

                    final Set<String> keys = bundle.keySet();
                    final Iterator<String> it = keys.iterator();

                    while (it.hasNext()) {
                        final String key = it.next();
                        LogUtils.d("TAG", "Key: " + key + " = " + bundle.get(key));
                    }

                }

                if (data.hasExtra("availableVoices")) {
                    containerVEArray.get(requestCode).setVoices(data.getStringArrayListExtra("availableVoices"));
                } else {
                    containerVEArray.get(requestCode).setVoices(new ArrayList<String>());
                }
            }

            if (requestCount == containerVEArray.size()) {

                for (int i = 0; i < containerVEArray.size(); i++) {

                    LogUtils.v("TAG", "cve: " + containerVEArray.get(i).getLabel() + " - "
                            + containerVEArray.get(i).getVoices().size() + " - " + containerVEArray.get(i).getVoices().toString());
                }
            }

        } catch (final IndexOutOfBoundsException e) {
            LogUtils.e("TAG", "IndexOutOfBoundsException");
            e.printStackTrace();
        } catch (final NullPointerException e) {
            LogUtils.e("TAG", "NullPointerException");
            e.printStackTrace();
        } catch (final Exception e) {
            LogUtils.e("TAG", "Exception");
            e.printStackTrace();
        }
    }
    */
    /*
    public void SampleRatesSupport() {
        int idx = 0;
        int[] list = {8000, 11025, 16000, 22050, 44100, 96000, 1000000};
        StringBuilder toastMessage = new StringBuilder();
        for (int j : list) {
            int bufferSize = AudioRecord.getMinBufferSize(j, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize != AudioRecord.ERROR) {

                //frequencyslist.add(2, idx, Menu.NONE, String.valueOf(list[i]));
                toastMessage.append(j).append(",");
                //idx++;
            }
            Toast.makeText(this, toastMessage.toString(), Toast.LENGTH_SHORT).show();

        }
    }*/
/////////////////////////////List text to speech Engines
    // Usage within an Activity - Debugging only!

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Lock.REQUEST_CODE) {
            // in the below line, we are checking if permission is granted.
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // if permissions are granted we are displaying below toast message.
                //Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                // in the below line, we are displaying toast message
                // if permissions are not granted.
                //Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
                LogUtils.w(TAG, "Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED");
                Lock.DeleteLicenseFile(getPackageName());
            }
        }
    }


    private enum State {
        LOADING, FAILURE, SUCCESS
    }
    //In espeak , show TTs settings page I guess
    private static boolean isIntentAvailable(Intent intent, Context context) {
        return intent != null && context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }
}
