package com.khanenoor.parsavatts.impractical;

import static java.lang.Thread.currentThread;

import android.os.Bundle;
import android.speech.tts.SynthesisCallback;

import com.khanenoor.parsavatts.engine.SpeechSynthesis;
import com.khanenoor.parsavatts.util.LogUtils;

import java.util.Observable;
import java.util.Observer;

public class AudioBufferObserver implements Observer {
    private static final String TAG = AudioBufferObserver.class.getSimpleName();
    private int mMessagePrintCounter = 0 ;
    private SynthesisCallback mImpSynthesisCallback;
    private SpeechSynthesis mSpeechSynEngine = null;

    public void onCreate(Bundle savedInstanceState) {
        //md = getMediator();  // This comes from the custom Application class
    }

    public void setSynthesisCallback(SynthesisCallback param1SynthesisCallback) {
        this.mImpSynthesisCallback = param1SynthesisCallback;
    }

    public void setmSpeechSynEngine(SpeechSynthesis paramSpeechSynEngine) {
        this.mSpeechSynEngine = paramSpeechSynEngine;
        this.mSpeechSynEngine.mAudioBufferCommon.addObserver(this);

    }

    private void getQueueData() {
        if (mImpSynthesisCallback == null)
            return;
        if (mSpeechSynEngine == null)
            return;
        if(mMessagePrintCounter<3) {
            LogUtils.w(TAG, "AudioBufferObserver getQueueData threadId:" + currentThread().getId());
        }
        mMessagePrintCounter++;
        byte[] bufferAudio = null;
        /*
        try {
            bufferAudio = mSpeechSynEngine.mAudioBufferCommon.getBufferAudio();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // can't update textview  get exception CalledFromWrongThreadException
        //byte[] bufferAudio= mEngine.getBufferAudio();
        if (bufferAudio == null)
            return;
        final int maxBytesToCopy = mImpSynthesisCallback.getMaxBufferSize();
        long  nCounter = 0;
        boolean bMessageLog = false;

        int offset = 0;
        while (offset < bufferAudio.length) {
            //LogUtils.w(TAG, "onSynthDataReady Len:" + audioData.length);
            final int bytesToWrite = Math.min(maxBytesToCopy, (bufferAudio.length - offset));
            mImpSynthesisCallback.audioAvailable(bufferAudio, offset, bytesToWrite);
            offset += bytesToWrite;
            nCounter++;
            if (!bMessageLog && nCounter > 50) {
                bMessageLog = true;
                LogUtils.i(TAG, "AudioBufferObserver getQueueData DeadLocked in while");
            }
        }
        */
    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an {@code Observable} object's
     * {@code notifyObservers} method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the {@code notifyObservers}
     *            method.
     */
    @Override
    public void update(Observable o, Object arg) {
        getQueueData();
    }
}
