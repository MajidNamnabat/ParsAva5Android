package com.khanenoor.parsavatts.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;

import com.khanenoor.parsavatts.engine.FaTts;
import com.khanenoor.parsavatts.util.LogUtils;

//private static BroadcastReceiver TtsQueueCompletedReceiver = new BroadcastReceiver() {
public class TtsQueueCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = TtsQueueCompletedReceiver.class.getSimpleName();

    //public FaTts mFaTtsObject = null;
    //public void setFaTts(FaTts faTtsObject){
    //    mFaTtsObject = faTtsObject;
    //}
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED)) {
            LogUtils.w(TAG, "onReceive ACTION_TTS_QUEUE_PROCESSING_COMPLETED");
            //if(mFaTtsObject!=null) {
            //    mFaTtsObject.onActionTtsQueueCompletedReceived();
            //}
            FaTts.onActionTtsQueueCompletedReceived();
            // isSpeaking = false;
        }
    }
};
