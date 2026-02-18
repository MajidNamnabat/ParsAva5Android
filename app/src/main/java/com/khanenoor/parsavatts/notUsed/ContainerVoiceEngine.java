package com.khanenoor.parsavatts.notUsed;

import android.content.Intent;

import java.util.ArrayList;

// Container Class

public class ContainerVoiceEngine {

    private String label;
    private String packageName;
    private ArrayList<String> voices;
    private Intent intent;
    private ArrayList<ContainerVoiceEngine> containerVEArray;
    private int requestCount;

    public ContainerVoiceEngine() {

    }

    public ContainerVoiceEngine(final String label, final String packageName, final ArrayList<String> voices, final Intent intent) {

        this.label = label;
        this.packageName = packageName;
        this.voices = voices;
        this.intent = intent;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(final Intent intent) {
        this.intent = intent;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }
    public ArrayList<String> getVoices() {
        return voices;
    }

    public void setVoices(final ArrayList<String> voices) {
        this.voices = voices;
    }


}
