package xposed.lib;


import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class Settings {
    public static String SETTINGS_FILE_NAME = "xposedSettings";
    private XSharedPreferences preferences;

    public Settings(String packageName) {
        preferences = new XSharedPreferences(packageName, SETTINGS_FILE_NAME);
        XposedBridge.log("Loaded preferences for " + packageName + ":  " + preferences.getAll());
    }

    public void reload() {
        preferences.reload();
        XposedBridge.log("Reloaded preferences: " + preferences.getAll());
    }

    public XSharedPreferences getPreferences() {
        return preferences;
    }
}
