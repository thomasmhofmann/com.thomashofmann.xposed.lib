package com.thomashofmann.xposed.lib;


import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class Settings {
    static final String TAG = "xposedlib";
    public static String SETTINGS_FILE_NAME = "xposedSettings";
    private XSharedPreferences preferences;
    private File file;

    public Settings(String packageName) {
        file = new File(Environment.getDataDirectory(), "data/" + packageName + "/shared_prefs/" + SETTINGS_FILE_NAME + ".xml");
        preferences = new XSharedPreferences(file);
    }

    public void reload() {
        File prefFile = preferences.getFile();
        for (int i = 0; i < 100 ; i++) {
            if(!prefFile.canRead()) {
                Log.d(TAG, "Cannot read file (" + i + ") :" + prefFile.getAbsolutePath());
                try {
                    Thread.currentThread().sleep(20);
                } catch (InterruptedException e) {
                }
            } else {
                Log.d(TAG, "Can read file now (" + i + ") :" + prefFile.getAbsolutePath());
                preferences.reload();
                Log.d(TAG, "Reloaded preferences: " + preferences.getAll());
                break;
            }
        }
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }
}
