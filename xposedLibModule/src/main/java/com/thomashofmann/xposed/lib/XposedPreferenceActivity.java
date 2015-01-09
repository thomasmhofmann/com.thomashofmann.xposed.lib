package com.thomashofmann.xposed.lib;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

public abstract class XposedPreferenceActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(Settings.SETTINGS_FILE_NAME);
        //noinspection deprecation
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        //getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.i("xmso", "Registering OnSharedPreferenceChangeListener.");
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("xmso", "Unregistering OnSharedPreferenceChangeListener.");
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
//        Log.i("xmso", "Sending broadcast to trigger reload of preferences.");
//        Intent intent = new Intent(getPreferencesChangedAction());
//        sendBroadcast(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences paramSharedPreferences, String paramString) {
        Log.i("xmso", "SharedPreference have changed. Sending broadcast.");
        Intent intent = new Intent(getPreferencesChangedAction());
        sendBroadcast(intent);
    }

    protected abstract String getPreferencesChangedAction();

}