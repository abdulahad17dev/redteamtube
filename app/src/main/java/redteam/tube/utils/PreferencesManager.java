package redteam.tube.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "YouTubeSettings";
    private static final String KEY_FULLSCREEN_ENABLED = "fullscreen_enabled";
    private static final String KEY_HISTORY_ENABLED = "history_enabled";
    private static final String KEY_LANGUAGE = "language";

    private final SharedPreferences preferences;

    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFullscreenEnabled() {
        return preferences.getBoolean(KEY_FULLSCREEN_ENABLED, false); // Default: disabled
    }

    public void setFullscreenEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_FULLSCREEN_ENABLED, enabled).apply();
    }

    public boolean isHistoryEnabled() {
        return preferences.getBoolean(KEY_HISTORY_ENABLED, false); // Default: disabled
    }

    public void setHistoryEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_HISTORY_ENABLED, enabled).apply();
    }

    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, "system"); // Default: system language
    }

    public void setLanguage(String language) {
        preferences.edit().putString(KEY_LANGUAGE, language).apply();
    }
}
