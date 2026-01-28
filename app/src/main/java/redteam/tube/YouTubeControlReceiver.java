package redteam.tube;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver для управления YouTube из других приложений
 *
 * Примеры использования:
 *
 * ADB команды:
 * adb shell am broadcast -a redteam.tube.CONTROL --es action "PLAY"
 * adb shell am broadcast -a redteam.tube.CONTROL --es action "PAUSE"
 * adb shell am broadcast -a redteam.tube.CONTROL --es action "NEXT"
 * adb shell am broadcast -a redteam.tube.CONTROL --es action "SEEK_TO" --ei value 60
 * adb shell am broadcast -a redteam.tube.CONTROL --es action "SKIP_FORWARD" --ei value 10
 * adb shell am broadcast -a redteam.tube.CONTROL --es action "SEARCH" --es value "android tutorial"
 * adb shell am broadcast -a redteam.tube.CONTROL --es action "SCRAPE_PAGE"
 * adb shell am broadcast -a redteam.tube.CONTROL --es action "SCRAPE_COMMENTS"
 *
 * Из другого Android приложения:
 * Intent intent = new Intent("redteam.tube.CONTROL");
 * intent.putExtra("action", "PLAY");
 * context.sendBroadcast(intent);
 *
 * С параметрами:
 * Intent intent = new Intent("redteam.tube.CONTROL");
 * intent.putExtra("action", "SEEK_TO");
 * intent.putExtra("value", 60);
 * context.sendBroadcast(intent);
 */
public class YouTubeControlReceiver extends BroadcastReceiver {

    private static final String TAG = "YouTubeControlReceiver";
    private static final long COMMAND_DEBOUNCE_MS = 500;
    private static long lastCommandTime = 0;

    // Статический listener для связи с MainActivity
    private static ControlListener controlListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Debouncing - игнорируем дублирующиеся команды
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCommandTime < COMMAND_DEBOUNCE_MS) {
            Log.w(TAG, "Command debounced (too soon after previous)");
            return;
        }
        lastCommandTime = currentTime;

        String action = intent.getStringExtra("action");
        if (action == null || action.isEmpty()) {
            Log.w(TAG, "Received broadcast without action");
            return;
        }

        Log.i(TAG, "✅ Received control command: " + action);

        // Если MainActivity не запущена, запускаем её
        if (controlListener == null) {
            Log.i(TAG, "⚠️ MainActivity not running, launching it...");
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            launchIntent.putExtra("control_action", action);

            // Передаем value безопасно - проверяем тип в Bundle
            if (intent.hasExtra("value") && intent.getExtras() != null) {
                android.os.Bundle extras = intent.getExtras();
                Object value = extras.get("value");

                if (value instanceof String) {
                    // Это строка
                    String strValue = (String) value;
                    launchIntent.putExtra("control_value", strValue);
                    Log.i(TAG, "📝 String value: " + strValue);
                } else if (value instanceof Integer) {
                    // Это число
                    int intValue = (Integer) value;
                    launchIntent.putExtra("control_value_int", intValue);
                    Log.i(TAG, "🔢 Int value: " + intValue);
                }
            }

            try {
                context.startActivity(launchIntent);
                Log.i(TAG, "✅ MainActivity launched successfully");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to launch MainActivity", e);
            }
            return;
        }

        // Передаем команду в MainActivity
        Log.i(TAG, "📤 Forwarding command to MainActivity");
        controlListener.onControlCommand(action, intent);
    }

    /**
     * Интерфейс для обработки команд управления
     */
    public interface ControlListener {
        void onControlCommand(String action, Intent intent);
    }

    /**
     * Установить listener для обработки команд
     */
    public static void setControlListener(ControlListener listener) {
        controlListener = listener;
    }

    /**
     * Удалить listener
     */
    public static void removeControlListener() {
        controlListener = null;
    }

    /**
     * Проверить запущена ли MainActivity
     */
    public static boolean isMainActivityRunning() {
        return controlListener != null;
    }

    /**
     * Передать команду напрямую в MainActivity (из Service)
     */
    public static void forwardCommand(String action, Intent intent) {
        if (controlListener != null) {
            controlListener.onControlCommand(action, intent);
        }
    }
}
