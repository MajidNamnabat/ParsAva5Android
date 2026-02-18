package com.khanenoor.parsavatts;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * Centralizes access to the app's default {@link SharedPreferences} while
 * respecting the storage context selected by {@link ExtendedApplication}.
 *
 * <p>When the user is locked on Android N+ devices, preferences are served from
 * device-protected storage to keep the TTS engine functional for TalkBack
 * users. Once unlocked, credential-protected storage is used. Call sites should
 * always go through this helper to avoid mismatched preference files or storage
 * modes.</p>
 */
public final class PreferenceStorage {
    private PreferenceStorage() {
    }

    public static Context resolveStorageContext(Context context) {
        Context storageContext = ExtendedApplication.getStorageContext();
        if (storageContext != null) {
            return storageContext;
        }

        if (context == null) {
            return null;
        }

        final Context appContext = context.getApplicationContext();
        return appContext != null ? appContext : context;
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        final Context storageContext = resolveStorageContext(context);
        if (storageContext == null && context == null) {
            throw new IllegalStateException("No context available for SharedPreferences");
        }
        return PreferenceManager.getDefaultSharedPreferences(
                storageContext != null ? storageContext : context);
    }

    public static String getDefaultSharedPreferencesName(Context context) {
        final Context storageContext = resolveStorageContext(context);
        if (storageContext == null && context == null) {
            throw new IllegalStateException("No context available for preference name resolution");
        }

        final Context targetContext = storageContext != null ? storageContext : context;
        return targetContext.getPackageName() + "_preferences";
    }
}
