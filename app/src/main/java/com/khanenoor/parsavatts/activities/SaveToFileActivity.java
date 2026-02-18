package com.khanenoor.parsavatts.activities;

import static java.util.Objects.isNull;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.khanenoor.parsavatts.R;
import com.khanenoor.parsavatts.util.LogUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;


public class SaveToFileActivity extends AppCompatActivity {
    private static final String TAG = SaveToFileActivity.class.getSimpleName();
    private static final int SYNTHESIZE_TIMEOUT_MS = 60000;
    private TextToSpeech mTts = null;
    private boolean mTtsInited = false;
    private boolean mSynthesizeIsFinished = false;
    private final AtomicBoolean mIsDestroyed = new AtomicBoolean(false);
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mSynthesisExecutor = Executors.newSingleThreadExecutor();
    private Future<?> mCurrentSynthesisFuture;
    private final TextToSpeech.OnInitListener mInitListener = status -> {
        if (status == TextToSpeech.SUCCESS) {
            mTtsInited = true;
            findViewById(R.id.loading).setVisibility(View.GONE);
            findViewById(R.id.mainLayout).setVisibility(View.VISIBLE);
        }
        //mHandler.obtainMessage(TTS_INITIALIZED, status, 0).sendToTarget();
    };
    private EditText mEdtText = null;
    private TextView mLogStatus = null;
    private final View.OnClickListener mOnClickListener = v -> {
        if (v.getId() == R.id.btnSave) {
            mLogStatus.setText("");

            if (mEdtText.getText().length() == 0) {
                mLogStatus.setText(R.string.save_file_log1);
                return;
            }
            if (!mTtsInited && !isNull(mTts)) {
                return;
            }
            cancelExistingToast();
            showWaitingToast();
            setUiInProgress(true);
            String textSyn = String.valueOf(mEdtText.getText());
            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {

                }
                //We have when talkback is activate when test button press or english voice changed
                @Override
                public void onDone(String utteranceId) {
                    mSynthesizeIsFinished = true;
                    LogUtils.w(TAG,"UtteranceProgressListener onDone called");
                }

                @Override
                public void onError(String utteranceId) {
                    mSynthesizeIsFinished = true;
                    LogUtils.w(TAG,"UtteranceProgressListener onError called");
                }

                @Override
                public void onStop(String utteranceId, boolean interrupted) {
                    mSynthesizeIsFinished = true;
                    LogUtils.w(TAG,"UtteranceProgressListener onStop called");
                }

            });
            String sessionId = generateSessionId();
            final String outputFileName = resolveOutputFileName();
            startSynthesisTask(textSyn, sessionId, outputFileName);

        }
        //if (v.getId() == R.id.btnConvertFile) {
            // Initialize Builder
            /*StorageChooser chooser = new StorageChooser.Builder()
                    .withActivity(this)
                    .withFragmentManager(getFragmentManager())
                    .withPredefinedPath("/")

                    .showHidden(false)
                    .allowAddFolder(false)
                    .withMemoryBar(true)
                    .allowCustomPath(true)
                    .setType(StorageChooser.FILE_PICKER)
                    .filter(StorageChooser.FileType.DOCS)
                    .setInternalStorageText("/")
                    .disableMultiSelect()
                    .skipOverview(false,"/")

                    .build();

// Show dialog whenever you want by
            chooser.show();

// get path that the user has chosen
            chooser.setOnSelectListener(path -> LogUtils.e("SELECTED_PATH", path));

             */
           /*
            askForFilePermissions();
            DocumentConverter converter = new DocumentConverter();
            File file  = new File("/storage/emulated/0/Qavanin.docx");
            boolean isExist = file.exists();
            Result<String> result = null;

            try {
                result = converter.extractRawText(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String rawText = result.getValue(); // The raw text
            Set<String> warnings = result.getWarnings(); // Any warnings during conver
        }*/
    };
    /*
    public boolean askForFilePermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean hasPermission = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            if (!hasPermission) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return true;
            }
        }

        return false;
    }*/
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!

                } else {
                    // permission denied, boo!
                }

                return;
            }
        }
    }
    int DoSynthesizeToFile(String textSyn, String sessionId, String outputFileName) {
        int nFileIndex = 1;
        String textRemind = "";
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);


        do {
            if (Thread.currentThread().isInterrupted()) {
                LogUtils.w(TAG, "Synthesis interrupted before file creation");
                return 5;
            }
            File file = new File(path, "/ParsAva" + sessionId + "_" + nFileIndex + ".wav");

            if (file.exists() && !file.isDirectory()) {
                boolean result = file.delete();
                //LogUtils.w(TAG,"Lock.DeleteLicenseFile License File is Deleted result:" + result);
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                //throw new RuntimeException(e);
                LogUtils.w(TAG, "readFile IOException" + e.getMessage());
            }
            nFileIndex++;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (textSyn.length() >= TextToSpeech.getMaxSpeechInputLength()) {
                    LogUtils.w(TAG,"textSyn.length() >= TextToSpeech.getMaxSpeechInputLength()");
                    int nLastSpace = textSyn.lastIndexOf(" ",TextToSpeech.getMaxSpeechInputLength()-50);
                    LogUtils.w(TAG,"nLastSpace : " + nLastSpace + " all length: " + textSyn.length());
                    textRemind = textSyn.substring(nLastSpace,textSyn.length());
                    textSyn = textSyn.substring(0, nLastSpace);
                    LogUtils.w(TAG,"textRemind.length:" + textRemind.length());
                }
                else {
                    textRemind="";
                }
            }
            int resultSyn = 0;
            mSynthesizeIsFinished = false;
            if (Build.VERSION.SDK_INT < 21) {
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("utteranceId", "ParsAva_LongUtterance_Wav_Id");
                resultSyn = mTts.synthesizeToFile(textSyn, hashMap, file.getAbsolutePath());
            } else {
                Bundle bundle = new Bundle();
                resultSyn = mTts.synthesizeToFile(textSyn, bundle, file, "ParsAva_LongUtterance_Wav_Id");
            }
            if(resultSyn==-1) {
                LogUtils.w(TAG,"synthesizeToFile return -1");
                return 3;
            }
            LogUtils.w(TAG,"synthesizeToFile return "+ resultSyn);
            if (!waitForSynthesis()) {
                LogUtils.w(TAG, "Synthesize timed out before completion");
                boolean deleted = file.delete();
                LogUtils.w(TAG, "Temporary file cleanup after timeout: " + deleted);
                return 4;
            }
            LogUtils.w(TAG,"Synthesize of file is finished!");
            textSyn = textRemind;
            LogUtils.w(TAG,"textSyn.length():"+textSyn.length());
        } while (textSyn.length() > 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2);
        //////////////////////////////////////Combine All Synthesized Files
        if (Thread.currentThread().isInterrupted()) {
            LogUtils.w(TAG, "Synthesis interrupted before combining files");
            return 5;
        }
        return CombineWavFiles(nFileIndex, sessionId, outputFileName);
    }

    private boolean waitForSynthesis() {
        long start = System.currentTimeMillis();
        while (!mSynthesizeIsFinished && System.currentTimeMillis() - start < SYNTHESIZE_TIMEOUT_MS) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }
        return mSynthesizeIsFinished;
    }

    private String generateSessionId() {
        return String.valueOf(new Random().nextInt(9000) + 1000);
    }

    private String resolveOutputFileName() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String baseName = "ParsAvaOutput";
        String extension = ".wav";
        Random random = new Random();
        for (int attempts = 0; attempts < 10; attempts++) {
            int suffix = random.nextInt(900000) + 100000; // six digit random number
            String fileName = baseName + "_" + suffix + extension;
            File candidate = new File(path, "/" + fileName);
            if (!candidate.exists()) {
                return fileName;
            }
            boolean deleted = candidate.delete();
            LogUtils.w(TAG, "Existing output file " + candidate.getAbsolutePath() + " deleted: " + deleted);
            if (deleted) {
                return fileName;
            }
        }
        return baseName + extension;
    }
    int readSizeFile(String path, int nSkipedBytes){
        int size = 0;
        FileInputStream file = null;
        try {
            file = new FileInputStream(path);
            size = (int) file.getChannel().size();
            file.close();
            return size;
        } catch (FileNotFoundException e) {
            //throw new RuntimeException(e);
            LogUtils.w(TAG, "readFile FileNotFoundException" + e.getMessage());
        }catch (IOException e) {
            //throw new RuntimeException(e);
            LogUtils.w(TAG, "readFile IOException" + e.getMessage());
        }finally {
            try {
                assert file != null;
                file.close();
            } catch (IOException e) {
                //throw new RuntimeException(e);
            }
        }
        return 0;

    }
    byte[] readWholeFile(String path, int nSkipedBytes) {
        //read whole file and return it as byte[]
        int size = 0;
        FileInputStream file = null;
        try {
            file = new FileInputStream(path);
            size = (int) file.getChannel().size();
            byte[] bytes = new byte[size-nSkipedBytes];
            BufferedInputStream buf = new BufferedInputStream(file);
            int nRead = buf.read(bytes, nSkipedBytes, bytes.length-nSkipedBytes);
            buf.close();
            return bytes;
        } catch (FileNotFoundException e) {
            //throw new RuntimeException(e);
            LogUtils.w(TAG, "readFile FileNotFoundException" + e.getMessage());
        }catch (IOException e) {
            //throw new RuntimeException(e);
            LogUtils.w(TAG, "readFile IOException" + e.getMessage());
        }finally {
            try {
                assert file != null;
                file.close();
            } catch (IOException e) {
                //throw new RuntimeException(e);
            }
        }
        return null;
    }

    int CombineWavFiles(int nCountFiles, String sessionId, String outputFileName) {
       try {
           LogUtils.w(TAG, "nCountFiles : " + nCountFiles);

           //Save in Downloads Path of Android
           File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
           //If Only one exist file rename it to correct file name
           if (nCountFiles == 2) {  //In Real 1 File
               int nFileIndex = --nCountFiles;
               File file1 = new File(path, "/ParsAva" + sessionId + "_" + nFileIndex + ".wav");
               LogUtils.w(TAG, "it is one file and absolute path: " + file1.getAbsolutePath());
               File fileCombine = new File(path, "/" + outputFileName);
               LogUtils.w(TAG, "it is one file and absolute path: " + fileCombine.getAbsolutePath());
               boolean result = true;
               if (fileCombine.exists() && !fileCombine.isDirectory()) {
                   result = fileCombine.delete();
                   LogUtils.w(TAG, "fileCombine File is Deleted result:" + result);
               }
               if (!result)
                   return 1;
               if (file1.exists() && !file1.isDirectory()) {
                   result = file1.renameTo(fileCombine);
                   //LogUtils.w(TAG,"Lock.DeleteLicenseFile License File is Deleted result:" + result);
                   LogUtils.w(TAG, "rename file result :" + result);
               }
               if (result)
                   return 0;
               else
                   return 2;
           }
           //else append contents sequence of files to correct named files
           File fileCombine = new File(path, "/" + outputFileName);
           //delete if exist
           boolean result = true;
           if (fileCombine.exists() && !fileCombine.isDirectory()) {
               result = fileCombine.delete();
               LogUtils.w(TAG, "fileCombine:"+ fileCombine.getAbsolutePath()+" result:" + result);
           }
           if (!result) {
               LogUtils.w(TAG,"Can not delete " + fileCombine.getAbsolutePath());
               return 1;
           }
           try {
               fileCombine.createNewFile();
           } catch (IOException e) {
               //throw new RuntimeException(e);
               LogUtils.w(TAG, "CombineWavFiles IOException " + e.getMessage());
           }
           //open it
           FileOutputStream output = null;
           try {
               output = new FileOutputStream(new File(path, "/" + outputFileName), true);
           } catch (FileNotFoundException e) {
               LogUtils.w(TAG, "CombineWavFiles FileNotFoundException " + e.getMessage());
           }
           //append all content files to corrected file
           try {
           if (!isNull(output)) {
               int sizeAll=0;
               //44 Byte in Wav Header File
               for (int n = 1; n < nCountFiles; n++) {
                   sizeAll+=readSizeFile(path + "/ParsAva" + sessionId + "_" + n + ".wav",44);
               }
               byte[] sizeAllByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sizeAll).array();
               for (int n = 1; n < nCountFiles; n++) {
                   byte[] fileContent = readWholeFile(path + "/ParsAva" + sessionId + "_" + n + ".wav", n>1 ? 44:0);
                   //Notice1: One 44 Byte must not read
                   //Notice2: Header of First file size of the data section must increase to the whole file
                   LogUtils.w(TAG, "readWholeFile Bytes Read: " + fileContent.length);
                   assert output != null;
                   if(n==1){
                       fileContent[41]= sizeAllByteArray[0];
                       fileContent[42]= sizeAllByteArray[1];
                       fileContent[43]= sizeAllByteArray[2];
                       fileContent[44]= sizeAllByteArray[3];
                   }
                   output.write(fileContent);
               }
           }
           } catch (IOException e) {
                   //throw new RuntimeException(e);
                   LogUtils.w(TAG, "CombineWavFiles IOException " + e.getMessage());
               } finally {
                   try {
                       assert output != null;
                       output.close();
                   } catch (IOException e) {
                       //throw new RuntimeException(e);
                       LogUtils.w(TAG, "CombineWavFiles IOException " + e.getMessage());
                   }
               }
           if (!isNull(output)) {
               //delete all synthesized files
               for (int n = 1; n < nCountFiles; n++) {
                   File file = new File(path + "/ParsAva" + sessionId + "_" + n + ".wav");
                   if (file.exists() && !file.isDirectory()) {
                       result = file.delete();
                       LogUtils.w(TAG,"file: "+file.getAbsolutePath()+ " delete result : " + result);
                   }
               }
           }
           LogUtils.w(TAG,"CombineWavFiles All files combined!");
       }catch (Exception ex){
           LogUtils.w(TAG,"CombineWavFiles Exception:" + ex.getMessage());
       }
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_to_file);
        Button btnSaveButton = findViewById(R.id.btnSave);
        //Button btnConvertToFileButton = findViewById(R.id.btnConvertFile);
        mEdtText = findViewById(R.id.editTextTextMultiLine);
        mLogStatus = findViewById(R.id.logStatus);
        mTts = new TextToSpeech(this, mInitListener, this.getPackageName());
        btnSaveButton.setOnClickListener(mOnClickListener);
        //btnConvertToFileButton.setOnClickListener(mOnClickListener);

        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        findViewById(R.id.mainLayout).setVisibility(View.GONE);
        mEdtText.addTextChangedListener(textWatcher);
    }

    private Toast mWaitingToast;

    private void cancelExistingToast() {
        if (!isNull(mWaitingToast)) {
            mWaitingToast.cancel();
        }
    }

    private void showWaitingToast() {
        mWaitingToast = Toast.makeText(getApplicationContext(), getString(R.string.save_file_progress_wait), Toast.LENGTH_SHORT);
        mWaitingToast.show();
    }

    private void showResultToast(String messagePrefix, String outputFileName) {
        if (mIsDestroyed.get()) {
            return;
        }
        String text = messagePrefix + " (" + outputFileName + ")";
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    private void startSynthesisTask(String textSyn, String sessionId, String outputFileName) {
        cancelOngoingTask();
        mCurrentSynthesisFuture = mSynthesisExecutor.submit(() -> {
            int value = DoSynthesizeToFile(textSyn, sessionId, outputFileName);
            if (Thread.currentThread().isInterrupted() || mIsDestroyed.get()) {
                return;
            }
            mMainHandler.post(() -> handleSynthesisResult(value, outputFileName));
        });
    }

    private void handleSynthesisResult(int retValue, String outputFileName) {
        if (mIsDestroyed.get()) {
            return;
        }
        findViewById(R.id.loading).setVisibility(View.INVISIBLE);
        switch (retValue){
            case 0:
                mLogStatus.setText(getString(R.string.save_to_file_succeed) + " (" + outputFileName + ")");
                break;
            case 1:
                mLogStatus.setText(R.string.save_to_file_err_not_removable);
                break;
            case 2:
                mLogStatus.setText(R.string.save_to_file_err_not_removable2);
                break;
            default:
                mLogStatus.setText(retValue+" خطا ");

        }
        showResultToast(mLogStatus.getText().toString(), outputFileName);
        mCurrentSynthesisFuture = null;
        setUiInProgress(false);
    }

    private void setUiInProgress(boolean inProgress) {
        findViewById(R.id.loading).setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.btnSave).setEnabled(!inProgress);
        mEdtText.setEnabled(!inProgress);
    }

    private void cancelOngoingTask() {
        if (mCurrentSynthesisFuture != null && !mCurrentSynthesisFuture.isDone()) {
            mCurrentSynthesisFuture.cancel(true);
        }
        mCurrentSynthesisFuture = null;
    }
    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            /*
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = 2*getResources().getDisplayMetrics().heightPixels/3;
            findViewById(R.id.mainLayout).setLayoutParams(new RelativeLayout.LayoutParams(width, height));
            */
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
    @Override
    public void onDestroy() {
        mIsDestroyed.set(true);
        cancelExistingToast();
        cancelOngoingTask();
        mSynthesisExecutor.shutdownNow();
        if (!isNull(mTts)) {
            mTts.stop();
        }
        super.onDestroy();
        if (!isNull(mTts)) {
            mTts.shutdown();
        }
    }

}