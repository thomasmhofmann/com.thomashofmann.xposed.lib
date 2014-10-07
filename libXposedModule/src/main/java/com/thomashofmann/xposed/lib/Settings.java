package com.thomashofmann.xposed.lib;


import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class Settings {
    private static final String TAG = "xposedlib";
    public static String SETTINGS_FILE_NAME = "xposedSettings";
    private XSharedPreferences preferences;
    private File file;

    public Settings(String packageName) {
        file = new File(Environment.getDataDirectory(), "data/" + packageName + "/shared_prefs/" + SETTINGS_FILE_NAME + ".xml");
        preferences = new XSharedPreferences(file);
        if(preferences != null) {
            XposedBridge.log("Loaded preferences for " + packageName + ":  " + preferences.getAll());
        }
    }

    public void reload() {
        preferences.reload();
        XposedBridge.log("Reloaded preferences: " + preferences.getAll());
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }
}
