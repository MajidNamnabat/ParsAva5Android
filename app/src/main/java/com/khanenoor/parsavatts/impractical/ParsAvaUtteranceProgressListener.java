package com.khanenoor.parsavatts.impractical;

import static android.media.AudioFormat.ENCODING_PCM_16BIT;

import android.speech.tts.SynthesisCallback;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.Nullable;

import com.khanenoor.parsavatts.engine.SpeechSynthesis;
import com.khanenoor.parsavatts.util.LogUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

;
public class ParsAvaUtteranceProgressListener extends UtteranceProgressListener {
    private static final String TAG = ParsAvaUtteranceProgressListener.class.getSimpleName();
    private volatile enmSpeakProgressStates mSpeakProgressState = enmSpeakProgressStates.onIdle;
    //public boolean mIsBusy = false;
    //private boolean mIsFirstAudioPacketReceived = false;

    private int mMessagePrintCounter = 0;
    private SynthesisCallback mImpSynthesisCallback;
    public int mSampleRateInHz;
    public int mAudioFormat;
    public int mChannelCount;
    private SpeechSynthesis mSpeechSynEngine = null;
    private String mActiveUtteranceId = null;
    private final ExecutorService mAudioProcessingExecutor = Executors.newSingleThreadExecutor();
    private final Object mStateLock = new Object();
    @Nullable
    private SpeakProgressStateListener mStateListener;

    public interface SpeakProgressStateListener {
        void onSpeakProgressStateChanged(enmSpeakProgressStates previous,
                                         enmSpeakProgressStates current,
                                         String reason);
    }
    public ParsAvaUtteranceProgressListener() {
        mMessagePrintCounter = 0;
        mImpSynthesisCallback = null;
        //mIsBusy = false;
        mSampleRateInHz = 0;
        mAudioFormat = ENCODING_PCM_16BIT;
        mChannelCount = 1;
        mSpeechSynEngine = null;
        mActiveUtteranceId = null;
        mSpeakProgressState = enmSpeakProgressStates.onIdle;
        //mIsFirstAudioPacketReceived = false;
        //mExecutorService = Executors.newSingleThreadExecutor();
        //mExecutorService = Executors.newFixedThreadPool(1);
    }

    public void setSpeakProgressStateListener(@Nullable SpeakProgressStateListener listener) {
        synchronized (mStateLock) {
            mStateListener = listener;
        }
    }

    public enmSpeakProgressStates getSpeakProgressState() {
        return mSpeakProgressState;
    }

    public void forceIdle(String reason) {
        enmSpeakProgressStates previous = transitionToState(enmSpeakProgressStates.onIdle, reason);
        if (previous == enmSpeakProgressStates.onIdle) {
            LogUtils.w(TAG, "forceIdle requested while already idle. reason=" + reason);
        }
    }

    public enmSpeakProgressStates transitionToState(enmSpeakProgressStates newState, String reason) {
        SpeakProgressStateListener listener;
        enmSpeakProgressStates previous;
        synchronized (mStateLock) {
            previous = mSpeakProgressState;
            if (previous == newState) {
                return previous;
            }
            mSpeakProgressState = newState;
            listener = mStateListener;
        }
        LogUtils.v(TAG, "Speak state transition " + previous + " -> " + newState + " reason=" + reason);
        if (listener != null) {
            listener.onSpeakProgressStateChanged(previous, newState, reason);
        }
        return previous;
    }

    public void setSynthesisCallback(SynthesisCallback param1SynthesisCallback) {
        this.mImpSynthesisCallback = param1SynthesisCallback;
    }

    public void setmSpeechSynEngine(SpeechSynthesis paramSpeechSynEngine) {
        this.mSpeechSynEngine = paramSpeechSynEngine;

    }

    public void onSpeakCalled(String utteranceId, String reason) {
        updateActiveUtteranceId(utteranceId, "onSpeakCalled");
        transitionToState(enmSpeakProgressStates.onSpeakCalled, reason);
    }

    /**
     * Called when an utterance has successfully completed processing.
     * All audio will have been played back by this point for audible output, and all
     * output will have been written to disk for file synthesis requests.
     * <p>
     * This request is guaranteed to be called after {@link #onStart(String)}.
     *
     * @param utteranceId The utterance ID of the utterance.
     */
    @Override
    public void onDone(String utteranceId) {
        //LogUtils.w(TAG, "ParsAvaUtteranceProgressListener.onDone threadId: " + Thread.currentThread().getId() + " utteranceId: " + utteranceId);
        if (!isCurrentUtterance(utteranceId, "onDone")) {
            return;
        }
        finishSynthesis("onDone");
        // mImpSynthesisCallback.done();
        //mSpeechSynEngine.mEnTts.unloadResample();
    }

    /**
     * Called when an error has occurred during processing. This can be called
     * at any point in the synthesis process. Note that there might be calls
     * to {@link #onStart(String)} for specified utteranceId but there will never
     * be a call to both {@link #onDone(String)} and {@link #onError(String)} for
     * the same utterance.
     *
     * @param utteranceId The utterance ID of the utterance.
     * @deprecated Use {@link #onError(String, int)} instead
     */
    @Override
    public void onError(String utteranceId) {
        LogUtils.w(TAG, "ParsAvaUtteranceProgressListener.onError " + utteranceId);
        if (!isCurrentUtterance(utteranceId, "onError")) {
            return;
        }
        finishSynthesis("onError");
    }

    @Override
    public void onError(String utteranceId, int param1Int) {
        LogUtils.w(TAG, "ParsAvaUtteranceProgressListener.onError " + utteranceId);
        if (!isCurrentUtterance(utteranceId, "onErrorWithCode")) {
            return;
        }
        finishSynthesis("onError");
    }

    /**
     * Called when an utterance "starts" as perceived by the caller. This will
     * be soon before audio is played back in the case of a {@link TextToSpeech#speak}
     * or before the first bytes of a file are written to the file system in the case
     * of {@link TextToSpeech#synthesizeToFile}.
     *
     * @param utteranceId The utterance ID of the utterance.
     */

    @Override
    public void onStart(String utteranceId) {
        //LogUtils.w(TAG, "ParsAvaUtteranceProgressListener.onStart " + utteranceId);

    }

    @Override
    public void onStop(String utteranceId, boolean param1Boolean) {
        if (!isCurrentUtterance(utteranceId, "onStop")) {
            return;
        }
        final enmSpeakProgressStates currentState = getSpeakProgressState();
        final boolean hasAudioPlaybackStarted = currentState == enmSpeakProgressStates.onFirstAudioPacketReceived
                || currentState == enmSpeakProgressStates.onOtherAudioPacketReceiving;

        if (!param1Boolean && !hasAudioPlaybackStarted) {
            LogUtils.i(TAG,
                    "Ignoring NEW_REQUEST onStop before first audio: utteranceId=" + utteranceId
                            + " started=" + param1Boolean
                            + " state=" + currentState);
            return;
        }

        if (!hasAudioPlaybackStarted) {
            LogUtils.i(TAG,
                    "Ignoring onStop before first audio packet: utteranceId=" + utteranceId
                            + " started=" + param1Boolean
                            + " state=" + currentState);
            return;
        }

        LogUtils.w(TAG,
                "Processing onStop: utteranceId=" + utteranceId
                        + " started=" + param1Boolean
                        + " state=" + currentState);
        finishSynthesis("onStop started=" + param1Boolean + ", state=" + currentState);
    }
    private void finishSynthesis(String reason){
        enmSpeakProgressStates previous = transitionToState(enmSpeakProgressStates.onIdle, reason);
        if (previous == enmSpeakProgressStates.onIdle) {
            LogUtils.w(TAG, "finishSynthesis invoked while already idle. reason=" + reason);
            mMessagePrintCounter = 0;
            return;
        }

        if (mSpeechSynEngine != null) {
            mSpeechSynEngine.mEnTts.releaseResampler();
            //mSpeechSynEngine.mEnTts.setBusy(false);
        }
        mMessagePrintCounter = 0;
        resetUtteranceTracking("finishSynthesis");

    }
    @Override
    public void onBeginSynthesis(String utteranceId, int sampleRateInHz, int audioFormat, int channelCount) {
        //LogUtils.w(TAG, " mUtterListene.onBeginSynthesis " + utteranceId + " " + sampleRateInHz + " " + audioFormat + " " + channelCount + " ThreadId:" + Thread.currentThread().getId());
        mMessagePrintCounter = 0;
        //mIsBusy = true;
        //mIsFirstAudioPacketReceived = false;
        updateActiveUtteranceId(utteranceId, "onBeginSynthesis");
        transitionToState(enmSpeakProgressStates.onBeginSynthesisCalled, "onBeginSynthesis");
        mSampleRateInHz = sampleRateInHz;
        mAudioFormat = audioFormat;
        mChannelCount = channelCount;
        if (mSpeechSynEngine == null)
            return;
        mSpeechSynEngine.mEnTts.initResampler(mSampleRateInHz, mSpeechSynEngine.getSampleRate(), mChannelCount, mSpeechSynEngine.getChannelCount());
        //mSpeechSynEngine.mEnTts.setBusy(true);
        //LogUtils.w(TAG, " mUtterListene.onBeginSynthesis finished!");

    }

    @Override
    public void onAudioAvailable(String utteranceId, byte[] audio) {
        //LogUtils.w(TAG, " mUtterListene.onAudioAvailable " + utteranceId + " " + audio.length );
        if (mSpeechSynEngine == null) {
            LogUtils.w(TAG, " mUtterListene.onAudioAvailable mSpeechSynEngine==null ");
            return;

        }
        if (mImpSynthesisCallback == null) {
            LogUtils.w(TAG, " mUtterListene.onAudioAvailable mImpSynthesisCallback==null ");

            return;
        }
        final byte[] audioCopy = Arrays.copyOf(audio, audio.length);
        mAudioProcessingExecutor.execute(() -> handleAudioPacket(audioCopy));
        //mExecutorService.execute(r);
        /*Thread threadProducer = new Thread(r);
        threadProducer.start();

        threadProducer.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        try {
            // Wait a while for existing tasks to terminate
            if(!mExecutorService.awaitTermination(60, TimeUnit.SECONDS)){
                mExecutorService.shutdownNow();// Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!mExecutorService.awaitTermination(60, TimeUnit.SECONDS))
                    LogUtils.w(TAG, " mUtterListener.onAudioAvailable mExecutorService did not terminate");
            }
            //threadProducer.join(0);
        } catch (InterruptedException e) {
            LogUtils.w(TAG, "mUtterListene.onAudioAvailable InterruptedException");
            e.printStackTrace();
        }
        */
        //LogUtils.w(TAG, "onAudioAvailable threadId:" + currentThread().getId() );
        // must be synthesize thread but it is not , I guess it is synthesize thread of english engine
        //byte[] resampledAudio = audio.clone();
        /*final int maxBytesToCopy = mImpSynthesisCallback.getMaxBufferSize();

        int offset = 0;

        while (offset < resampledAudio.length) {
            //LogUtils.w(TAG, "onSynthDataReady Len:" + audioData.length);
            final int bytesToWrite = Math.min(maxBytesToCopy, (resampledAudio.length - offset));
            mImpSynthesisCallback.audioAvailable(resampledAudio, offset, bytesToWrite);
            offset += bytesToWrite;
        }*/
        /*
        try {
            mSpeechSynEngine.mAudioBufferCommon.putBufferAudio(resampledAudio.clone());
        } catch (InterruptedException e) {
            LogUtils.w(TAG, " mUtterListener.onAudioAvailable InterruptedException " );

            //throw new RuntimeException(e);
        }
        */

        //No. Local references are garbage collected when the native function returns to Java (when Java calls native) or when the calling thread is detached from the JVM (in native calls Java).

        //DeleteLocalRef()
    }

    private void handleAudioPacket(byte[] audioData) {
        if (audioData == null || audioData.length == 0 || mSpeechSynEngine == null) {
            return;
        }
        try {
            if (getSpeakProgressState() == enmSpeakProgressStates.onBeginSynthesisCalled) {
                transitionToState(enmSpeakProgressStates.onFirstAudioPacketReceived, "first audio packet");
                //mSpeechSynEngine.mEnTts.firstAudioPacketReceived();
            }
            byte[] processedAudio = maybeResample(audioData);
            if (processedAudio == null || processedAudio.length == 0) {
                return;
            }
            //LogUtils.w(TAG, " mUtterListene.onAudioAvailable processed:" + processedAudio.length + " threadId:" + Thread.currentThread().getId());
            mMessagePrintCounter++;
            mSpeechSynEngine.mAudioBufferCommon.putBufferAudio(processedAudio);
        } catch (InterruptedException e) {
            LogUtils.w(TAG, " mUtterListener.onAudioAvailable InterruptedException ");
            Thread.currentThread().interrupt();
        }
    }

    private byte[] maybeResample(byte[] audioData) {
        if (!needsResample()) {
            return audioData;
        }
        ByteBuffer bufDataIn = ByteBuffer.allocateDirect(audioData.length);
        bufDataIn.order(ByteOrder.LITTLE_ENDIAN);
        bufDataIn.put(audioData);
        ByteBuffer bufDataOut = mSpeechSynEngine.ResampleNative(bufDataIn,
                mSampleRateInHz,
                mSpeechSynEngine.getSampleRate(),
                mChannelCount,
                mSpeechSynEngine.getChannelCount(),
                resolveVolumeRatio());
        if (bufDataOut == null) {
            LogUtils.w(TAG, "onAudioAvailable resample returned null");
            return null;
        }
        bufDataOut.order(ByteOrder.LITTLE_ENDIAN);
        byte[] bufferAudio = new byte[bufDataOut.remaining()];
        bufDataOut.get(bufferAudio);
        return bufferAudio;
    }

    private boolean needsResample() {
        if (mSpeechSynEngine == null) {
            return false;
        }
        return mSampleRateInHz != mSpeechSynEngine.getSampleRate()
                || mChannelCount != mSpeechSynEngine.getChannelCount();
    }

    private float resolveVolumeRatio() {
        if (mSpeechSynEngine == null
                || mSpeechSynEngine.mEnTts == null
                || mSpeechSynEngine.mEnTts.mConfiureParams == null) {
            return 1.0F;
        }
        float ratio = (mSpeechSynEngine.mEnTts.mConfiureParams.mVolume) / 50.0F;
        if (ratio < 0.1F) {
            ratio = 0.1F;
        }
        return ratio;
    }

    private void updateActiveUtteranceId(String utteranceId, String callbackName) {
        synchronized (mStateLock) {
            mActiveUtteranceId = utteranceId;
        }
        LogUtils.i(TAG, callbackName + " tracking utteranceId=" + utteranceId);
    }

    private boolean isCurrentUtterance(String utteranceId, String callbackName) {
        String activeId;
        synchronized (mStateLock) {
            activeId = mActiveUtteranceId;
        }
        final boolean matches = activeId != null && activeId.equals(utteranceId);
        if (!matches) {
            LogUtils.w(TAG, callbackName + " ignored for utteranceId=" + utteranceId + " activeId=" + activeId
                    + " state=" + mSpeakProgressState);
        } else {
            LogUtils.i(TAG, callbackName + " accepted for utteranceId=" + utteranceId
                    + " state=" + mSpeakProgressState);
        }
        return matches;
    }

    public void resetUtteranceTracking(String reason) {
        synchronized (mStateLock) {
            mActiveUtteranceId = null;
        }
        LogUtils.i(TAG, "Active utteranceId cleared due to " + reason);
    }
}
