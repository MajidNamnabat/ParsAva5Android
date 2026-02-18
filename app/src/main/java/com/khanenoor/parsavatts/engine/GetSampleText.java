/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.khanenoor.parsavatts.engine;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import com.khanenoor.parsavatts.R;
import com.khanenoor.parsavatts.util.LogUtils;

import java.util.Locale;

/*
 * Returns the sample text string for the language requested
 */
public class GetSampleText extends Activity {
    private static final String TAG = GetSampleText.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Locale locale = getLocaleFromIntent(getIntent());
        //final Resources res = getResourcesForLocale(this, locale);
        LogUtils.w(TAG,"GetSampleText.onCreate");
        String text = null;

        try {
            text = getString(R.string.sample_text, locale.getDisplayName(locale));
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        final String language = (locale == null) ? "eng" : locale.getISO3Language();
        if (text != null) {
            switch (language) {
                case "fa":
                    text = getString(R.string.fa);
                    break;
                case "fas":
                    text = getString(R.string.fas);
                    break;
                case "en":
                case "eng":
                    text = getString(R.string.eng);
                    break;
                default:
                    LogUtils.w(TAG, "GetSampleText Activity: Missing sample text for " + language);
                    text = getString(R.string.eng);
                    break;
            }
        }
        int result = TextToSpeech.LANG_AVAILABLE;
        Intent returnData = new Intent();
        returnData.putExtra("sampleText", text);

        setResult(result, returnData);
        finish();
    }
    private static Locale getLocaleFromIntent(Intent intent) {
        if (intent != null) {
            final String language = intent.getStringExtra("language");

            if (language != null) {
                return new Locale(language);
            }
        }

        return Locale.getDefault();
    }

}
