package com.khanenoor.parsavatts.impractical;


import java.util.Observable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AudioBufferObservable extends Observable {
    private static final String TAG = AudioBufferObservable.class.getSimpleName();
    public final LinkedBlockingQueue<byte[]> mBufferAudio = new LinkedBlockingQueue<>();

    /**
     * Add data to a queue(s) for consumption
     */
    public void putBufferAudio(byte[] data) throws InterruptedException {
        if(data!=null && data.length>0) {
            //synchronized (lock)
            {
                mBufferAudio.put(data);
                //setChanged();
                //notifyObservers();
            }
        }
        //notifyObservers();
    }

    /**
     * Return data from the queue for the Feature calculations
     */
    public byte[] pollBufferAudio(long timeoutMs) throws InterruptedException {
        if (timeoutMs <= 0) {
            return mBufferAudio.take();
        }
        return mBufferAudio.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public boolean hasBufferedAudio() {
        return !mBufferAudio.isEmpty();
    }

    public void clearBuffers() {
        mBufferAudio.clear();
    }

    public int getBufferedCount() {
        return mBufferAudio.size();
    }
}