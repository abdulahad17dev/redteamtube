package redteam.tube.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "YouTubeSettings";
    private static final String KEY_FULLSCREEN_ENABLED = "fullscreen_enabled";
    private static final String KEY_FULLSCREEN_DISPLAY_LAUNCH = "fullscreen_display_launch"; // Запуск в fullscreen display
    private static final String KEY_HISTORY_ENABLED = "history_enabled";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_PAGE_ZOOM = "page_zoom"; // CSS zoom (50-200%)
    private static final String KEY_TEXT_SIZE = "text_size"; // Text zoom (50-200%)
    private static final String KEY_BOTTOM_BAR_SIZE = "bottom_bar_size"; // Bottom bar size (50-200%)
    private static final String KEY_FLOATING_BACK_ENABLED = "floating_back_enabled"; // Floating back button enabled
    private static final String KEY_FLOATING_BACK_X = "floating_back_x"; // Floating back button X position
    private static final String KEY_FLOATING_BACK_Y = "floating_back_y"; // Floating back button Y position

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

    /**
     * Проверка, включен ли автозапуск в Fullscreen Display (1003)
     */
    public boolean isFullscreenDisplayLaunchEnabled() {
        return preferences.getBoolean(KEY_FULLSCREEN_DISPLAY_LAUNCH, false); // Default: disabled
    }

    /**
     * Включить/выключить автозапуск в Fullscreen Display (1003)
     */
    public void setFullscreenDisplayLaunchEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_FULLSCREEN_DISPLAY_LAUNCH, enabled).apply();
    }

    /**
     * Получить Page Zoom (CSS zoom) в процентах (50-200)
     */
    public int getPageZoom() {
        return preferences.getInt(KEY_PAGE_ZOOM, 100); // Default: 100%
    }

    /**
     * Установить Page Zoom (CSS zoom) в процентах (50-200)
     */
    public void setPageZoom(int zoom) {
        preferences.edit().putInt(KEY_PAGE_ZOOM, zoom).apply();
    }

    /**
     * Получить Text Size в процентах (50-200)
     */
    public int getTextSize() {
        return preferences.getInt(KEY_TEXT_SIZE, 100); // Default: 100%
    }

    /**
     * Установить Text Size в процентах (50-200)
     */
    public void setTextSize(int size) {
        preferences.edit().putInt(KEY_TEXT_SIZE, size).apply();
    }

    /**
     * Получить Bottom Bar Size в процентах (50-200)
     */
    public int getBottomBarSize() {
        return preferences.getInt(KEY_BOTTOM_BAR_SIZE, 100); // Default: 100%
    }

    /**
     * Установить Bottom Bar Size в процентах (50-200)
     */
    public void setBottomBarSize(int size) {
        preferences.edit().putInt(KEY_BOTTOM_BAR_SIZE, size).apply();
    }

    /**
     * Проверка, включена ли floating back кнопка
     */
    public boolean isFloatingBackEnabled() {
        return preferences.getBoolean(KEY_FLOATING_BACK_ENABLED, false); // Default: disabled
    }

    /**
     * Включить/выключить floating back кнопку
     */
    public void setFloatingBackEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_FLOATING_BACK_ENABLED, enabled).apply();
    }

    /**
     * Получить X позицию floating back кнопки
     */
    public int getFloatingBackX() {
        return preferences.getInt(KEY_FLOATING_BACK_X, 100); // Default: 100px from left
    }

    /**
     * Установить X позицию floating back кнопки
     */
    public void setFloatingBackX(int x) {
        preferences.edit().putInt(KEY_FLOATING_BACK_X, x).apply();
    }

    /**
     * Получить Y позицию floating back кнопки
     */
    public int getFloatingBackY() {
        return preferences.getInt(KEY_FLOATING_BACK_Y, 100); // Default: 100px from top
    }

    /**
     * Установить Y позицию floating back кнопки
     */
    public void setFloatingBackY(int y) {
        preferences.edit().putInt(KEY_FLOATING_BACK_Y, y).apply();
    }
}
