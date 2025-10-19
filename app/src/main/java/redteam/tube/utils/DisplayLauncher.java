package redteam.tube.utils;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * Утилита для запуска Activity на определенных дисплеях
 * Используется для поддержки мультидисплейной системы Geely/Ecarx
 */
public class DisplayLauncher {

    private static final String TAG = "DisplayLauncher";

    /**
     * Запустить Activity в fullscreen режиме (Display ID = 0x3eb / 1003)
     *
     * @param context Context для запуска
     * @param activityClass Класс Activity для запуска
     * @param extras Дополнительные данные (может быть null)
     */
    public static void launchInFullscreen(Context context, Class<?> activityClass, Bundle extras) {
        launchOnDisplay(context, activityClass, DisplayIdConstants.DISPLAY_ID_FULL, extras);
    }

    /**
     * Запустить Activity в fullscreen режиме заднего экрана (Display ID = 0x23eb / 9195)
     *
     * @param context Context для запуска
     * @param activityClass Класс Activity для запуска
     * @param extras Дополнительные данные (может быть null)
     */
    public static void launchInRearFullscreen(Context context, Class<?> activityClass, Bundle extras) {
        launchOnDisplay(context, activityClass, DisplayIdConstants.DISPLAY_ID_RSD_FULL, extras);
    }

    /**
     * Запустить Activity на конкретном дисплее
     *
     * @param context Context для запуска
     * @param activityClass Класс Activity для запуска
     * @param displayId ID дисплея (см. DisplayIdConstants)
     * @param extras Дополнительные данные (может быть null)
     */
    public static void launchOnDisplay(Context context, Class<?> activityClass, int displayId, Bundle extras) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "Multi-display launch requires Android 8.0+ (API 26+)");
            // Обычный запуск для старых версий
            Intent intent = new Intent(context, activityClass);
            if (extras != null) {
                intent.putExtras(extras);
            }
            context.startActivity(intent);
            return;
        }

        try {
            // Создаем Intent
            Intent intent = new Intent(context, activityClass);

            // Добавляем extras если есть
            if (extras != null) {
                intent.putExtras(extras);
            }

            // Добавляем флаг NEW_TASK если контекст не Activity
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            // КЛЮЧЕВОЙ МОМЕНТ: Создаем ActivityOptions с указанием Display ID
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displayId);

            Log.i(TAG, "╔═══════════════════════════════════════════════════════════╗");
            Log.i(TAG, "║          LAUNCHING ACTIVITY ON DISPLAY                   ║");
            Log.i(TAG, "╠═══════════════════════════════════════════════════════════╣");
            Log.i(TAG, "║ Activity: " + activityClass.getSimpleName());
            Log.i(TAG, "║ Target Display ID: " + displayId + " (0x" +
                  Integer.toHexString(displayId) + ")");
            Log.i(TAG, "║ Display Name: " + DisplayIdConstants.getDisplayName(displayId));
            Log.i(TAG, "╚═══════════════════════════════════════════════════════════╝");

            // Запускаем Activity с указанием дисплея
            context.startActivity(intent, options.toBundle());

        } catch (Exception e) {
            Log.e(TAG, "Failed to launch activity on display " + displayId, e);
            // Fallback: обычный запуск без указания дисплея
            Intent intent = new Intent(context, activityClass);
            if (extras != null) {
                intent.putExtras(extras);
            }
            context.startActivity(intent);
        }
    }

    /**
     * Запустить Activity с URL в fullscreen режиме
     *
     * @param context Context для запуска
     * @param activityClass Класс Activity для запуска
     * @param url URL для загрузки в WebView
     * @param displayId ID дисплея
     */
    public static void launchWithUrl(Context context, Class<?> activityClass, String url, int displayId) {
        Bundle extras = new Bundle();
        extras.putString("url", url);
        launchOnDisplay(context, activityClass, displayId, extras);
    }

    /**
     * Пример использования: Запуск MainActivity в fullscreen
     *
     * Использование из кода:
     * <pre>
     * // Простой запуск в fullscreen
     * DisplayLauncher.launchInFullscreen(context, MainActivity.class, null);
     *
     * // Запуск с URL
     * DisplayLauncher.launchWithUrl(context, MainActivity.class,
     *                              "https://m.youtube.com/watch?v=xxx",
     *                              DisplayIdConstants.DISPLAY_ID_FULL);
     *
     * // Запуск на заднем экране
     * DisplayLauncher.launchInRearFullscreen(context, MainActivity.class, null);
     * </pre>
     *
     * Использование из ADB:
     * <pre>
     * # Запуск в fullscreen режиме
     * adb shell am start -n redteam.tube/.MainActivity --display 1003
     *
     * # Запуск на заднем fullscreen
     * adb shell am start -n redteam.tube/.MainActivity --display 9195
     *
     * # Запуск с URL
     * adb shell am start -n redteam.tube/.MainActivity \
     *   --display 1003 \
     *   --es url "https://m.youtube.com/watch?v=xxx"
     * </pre>
     */
    public static class Usage {
        // Класс только для документации, не используется в коде
    }
}
