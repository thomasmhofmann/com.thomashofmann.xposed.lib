package xposed.lib;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;

abstract class XposedPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(Settings.SETTINGS_FILE_NAME);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences paramSharedPreferences, String paramString) {
        Intent intent = new Intent(getPreferencesChangedAction());
        getActivity().sendBroadcast(intent);
    }

    protected abstract String getPreferencesChangedAction();

}