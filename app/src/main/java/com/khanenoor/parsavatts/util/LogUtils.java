package com.khanenoor.parsavatts.util;

import android.util.Log;

import com.khanenoor.parsavatts.BuildConfig;

/**
 * Wrapper around {@link Log} that allows logging to be toggled at runtime.
 */
public final class LogUtils {
    private static volatile boolean enabled = BuildConfig.DEBUG;

    private LogUtils() {
        // Utility class.
    }

    public static void setEnabled(boolean enableLogging) {
        enabled = enableLogging;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int v(String tag, String msg) {
        return enabled ? Log.v(tag, msg) : 0;
    }

    public static int v(String tag, String msg, Throwable tr) {
        return enabled ? Log.v(tag, msg, tr) : 0;
    }

    public static int d(String tag, String msg) {
        return enabled ? Log.d(tag, msg) : 0;
    }

    public static int d(String tag, String msg, Throwable tr) {
        return enabled ? Log.d(tag, msg, tr) : 0;
    }

    public static int i(String tag, String msg) {
        return enabled ? Log.i(tag, msg) : 0;
    }

    public static int i(String tag, String msg, Throwable tr) {
        return enabled ? Log.i(tag, msg, tr) : 0;
    }

    public static int w(String tag, String msg) {
        return enabled ? Log.w(tag, msg) : 0;
    }

    public static int w(String tag, String msg, Throwable tr) {
        return enabled ? Log.w(tag, msg, tr) : 0;
    }

    public static int e(String tag, String msg) {
        return enabled ? Log.e(tag, msg) : 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        return enabled ? Log.e(tag, msg, tr) : 0;
    }
}
