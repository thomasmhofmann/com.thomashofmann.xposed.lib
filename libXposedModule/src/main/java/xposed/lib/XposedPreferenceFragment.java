package xposed.lib;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

public abstract class XposedPreferenceFragment extends PreferenceFragment { // implements OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(Settings.SETTINGS_FILE_NAME);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        //getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
 //       getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        Log.i("SSCF", "Sending broadcast to trigger reload of preferences.");
        Intent intent = new Intent(getPreferencesChangedAction());
        getActivity().sendBroadcast(intent);
    }

//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences paramSharedPreferences, String paramString) {
//        Log.i("SSCF", "SharedPreference have changed. Sending broadcast.");
//        Intent intent = new Intent(getPreferencesChangedAction());
//        getActivity().sendBroadcast(intent);
//    }

    protected abstract String getPreferencesChangedAction();

}