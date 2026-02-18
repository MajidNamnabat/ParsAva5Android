package com.khanenoor.parsavatts.notUsed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class SpeechWrapper {

    private static TextToSpeech mTts = null;
    private static boolean isSpeaking = false;
    private static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED) && mTts != null) {
                isSpeaking = false;
            }
        }
    };

    private static void Speak(String sMessage, int intQueueType, int delay) {
        if (mTts == null || sMessage == null) return;
        sMessage = sMessage.trim();
        isSpeaking = true;
        if (delay > 0) {
            mTts.playSilence(delay, intQueueType, null);
            intQueueType = TextToSpeech.QUEUE_ADD;
        }
        mTts.speak(sMessage, intQueueType, null);
    }

    public static void Init(Context context) {
        mTts = new TextToSpeech(context, (OnInitListener) context);
        IntentFilter filter = new IntentFilter(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
        context.registerReceiver(receiver, filter);
    }

    public static void Shutdown() {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
    }

    public static boolean isSpeaking() {
        return isSpeaking;
    }

}


