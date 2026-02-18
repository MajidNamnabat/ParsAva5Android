package com.khanenoor.parsavatts.engine;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class DownloadVoiceData extends AppCompatActivity {
    private static final String TAG = DownloadVoiceData.class.getSimpleName();
    private final static String MARKET_URI = "market://search?q=pname:com.svox.langpack.installer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri marketUri = Uri.parse(MARKET_URI);
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
        startActivityForResult(marketIntent, 0);
        finish();
    }
}