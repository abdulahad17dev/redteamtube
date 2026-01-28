package redteam.tube;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Foreground Service для прослушивания broadcast команд даже когда UI закрыт
 * Это позволяет управлять приложением из ADB или других приложений в любое время
 */
public class ControlListenerService extends Service {

    private static final String TAG = "ControlListenerService";
    private static final String CHANNEL_ID = "youtube_control_channel";
    private static final int NOTIFICATION_ID = 1001;

    private BroadcastReceiver controlReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "🚀 ControlListenerService started");

        // Создать notification channel
        createNotificationChannel();

        // Запустить как foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Зарегистрировать broadcast receiver
        registerControlReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "📡 Service is running and listening for commands");
        return START_STICKY; // Автоматически перезапускать если убит системой
    }

    /**
     * Регистрация broadcast receiver для приема команд
     */
    private void registerControlReceiver() {
        controlReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getStringExtra("action");
                if (action != null) {
                    Log.i(TAG, "🎯 Received command: " + action);

                    // Проверить запущена ли MainActivity
                    if (YouTubeControlReceiver.isMainActivityRunning()) {
                        // MainActivity работает - передать команду напрямую
                        Log.i(TAG, "✅ MainActivity is running, forwarding command");
                        YouTubeControlReceiver.forwardCommand(action, intent);
                    } else {
                        // MainActivity не работает - запустить её
                        Log.i(TAG, "⚠️ MainActivity not running, launching with command");
                        launchMainActivityWithCommand(action, intent);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("redteam.tube.CONTROL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(controlReceiver, filter);
        }

        Log.i(TAG, "✅ Control receiver registered in service");
    }

    /**
     * Запустить MainActivity с командой
     */
    private void launchMainActivityWithCommand(String action, Intent originalIntent) {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launchIntent.putExtra("control_action", action);

        // Копировать все extras
        if (originalIntent.getExtras() != null) {
            launchIntent.putExtras(originalIntent.getExtras());
        }

        startActivity(launchIntent);
    }

    /**
     * Создать notification channel (для Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "YouTube Control Service",
                    NotificationManager.IMPORTANCE_LOW // Низкий приоритет - без звука
            );
            channel.setDescription("Allows remote control of YouTube playback");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Создать notification для foreground service
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("YouTube Control Active")
                .setContentText("Ready to receive commands")
                .setSmallIcon(android.R.drawable.ic_media_play) // Используем стандартную иконку
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Notification нельзя смахнуть
                .setPriority(Notification.PRIORITY_LOW) // Низкий приоритет
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "🛑 ControlListenerService stopped");

        // Отменить регистрацию receiver
        if (controlReceiver != null) {
            unregisterReceiver(controlReceiver);
            controlReceiver = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Не используем binding
    }
}
