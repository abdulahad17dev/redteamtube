package com.example.myapp; // Замените на ваш package

import android.content.Context;
import android.content.Intent;

/**
 * Пример контроллера для управления RedTeamTube из другого приложения
 *
 * Использование:
 *
 * YouTubeControllerExample controller = new YouTubeControllerExample(context);
 * controller.play();
 * controller.searchVideo("android tutorial");
 * controller.seekTo(60);
 */
public class YouTubeControllerExample {

    private Context context;
    private static final String ACTION = "redteam.tube.CONTROL";
    private static final String PACKAGE = "redteam.tube";
    private static final String RECEIVER = "redteam.tube.YouTubeControlReceiver";

    public YouTubeControllerExample(Context context) {
        this.context = context;
    }

    // ==================== УПРАВЛЕНИЕ ВОСПРОИЗВЕДЕНИЕМ ====================

    /**
     * Запустить воспроизведение видео
     */
    public void play() {
        sendCommand("PLAY");
    }

    /**
     * Приостановить воспроизведение
     */
    public void pause() {
        sendCommand("PAUSE");
    }

    /**
     * Переключить play/pause
     */
    public void toggle() {
        sendCommand("TOGGLE");
    }

    // ==================== НАВИГАЦИЯ ====================

    /**
     * Следующее видео
     */
    public void nextVideo() {
        sendCommand("NEXT");
    }

    /**
     * Предыдущее видео (назад в истории)
     */
    public void previousVideo() {
        sendCommand("PREVIOUS");
    }

    // ==================== УПРАВЛЕНИЕ ВРЕМЕНЕМ ====================

    /**
     * Перемотать на конкретное время
     * @param seconds Время в секундах
     */
    public void seekTo(int seconds) {
        sendCommandWithValue("SEEK_TO", seconds);
    }

    /**
     * Перемотать вперед на N секунд
     * @param seconds Количество секунд (по умолчанию 10)
     */
    public void skipForward(int seconds) {
        sendCommandWithValue("SKIP_FORWARD", seconds);
    }

    /**
     * Перемотать вперед на 10 секунд
     */
    public void skipForward() {
        skipForward(10);
    }

    /**
     * Перемотать назад на N секунд
     * @param seconds Количество секунд (по умолчанию 10)
     */
    public void skipBackward(int seconds) {
        sendCommandWithValue("SKIP_BACKWARD", seconds);
    }

    /**
     * Перемотать назад на 10 секунд
     */
    public void skipBackward() {
        skipBackward(10);
    }

    // ==================== URL И ПОИСК ====================

    /**
     * Открыть конкретный URL
     * @param url URL для открытия
     */
    public void openUrl(String url) {
        sendCommandWithValue("OPEN_URL", url);
    }

    /**
     * Открыть конкретное видео по ID
     * @param videoId YouTube video ID (например: dQw4w9WgXcQ)
     */
    public void playVideoById(String videoId) {
        openUrl("https://www.youtube.com/watch?v=" + videoId);
    }

    /**
     * Поиск видео на YouTube
     * @param query Поисковый запрос
     */
    public void searchVideo(String query) {
        sendCommandWithValue("SEARCH", query);
    }

    // ==================== ПАРСИНГ ДАННЫХ ====================

    /**
     * Извлечь информацию о видео на текущей странице
     * Результат будет в logcat с тегом "MainActivity"
     */
    public void scrapePage() {
        sendCommand("SCRAPE_PAGE");
    }

    /**
     * Извлечь комментарии к текущему видео
     * Результат будет в logcat с тегом "MainActivity"
     */
    public void scrapeComments() {
        sendCommand("SCRAPE_COMMENTS");
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Отправить простую команду без параметров
     */
    private void sendCommand(String action) {
        Intent intent = new Intent(ACTION);
        intent.setComponent(new android.content.ComponentName(PACKAGE, RECEIVER));
        intent.putExtra("action", action);
        context.sendBroadcast(intent);
    }

    /**
     * Отправить команду с целочисленным значением
     */
    private void sendCommandWithValue(String action, int value) {
        Intent intent = new Intent(ACTION);
        intent.setComponent(new android.content.ComponentName(PACKAGE, RECEIVER));
        intent.putExtra("action", action);
        intent.putExtra("value", value);
        context.sendBroadcast(intent);
    }

    /**
     * Отправить команду со строковым значением
     */
    private void sendCommandWithValue(String action, String value) {
        Intent intent = new Intent(ACTION);
        intent.setComponent(new android.content.ComponentName(PACKAGE, RECEIVER));
        intent.putExtra("action", action);
        intent.putExtra("value", value);
        context.sendBroadcast(intent);
    }

    // ==================== УДОБНЫЕ КОМБИНАЦИИ ====================

    /**
     * Поиск и воспроизведение первого результата
     * @param query Поисковый запрос
     */
    public void searchAndPlay(String query) {
        searchVideo(query);
        // Ждем 3 секунды пока загрузятся результаты
        new android.os.Handler().postDelayed(() -> {
            // Здесь можно добавить логику для клика на первое видео
            // Но так как у нас нет прямого доступа к WebView,
            // пользователю нужно будет кликнуть вручную
        }, 3000);
    }

    /**
     * Перемотка вперед на 1 минуту
     */
    public void skipOneMinute() {
        skipForward(60);
    }

    /**
     * Перемотка назад на 1 минуту
     */
    public void rewindOneMinute() {
        skipBackward(60);
    }

    /**
     * Перемотка на начало видео
     */
    public void restartVideo() {
        seekTo(0);
    }
}

// ==================== ПРИМЕР ИСПОЛЬЗОВАНИЯ В ACTIVITY ====================

/*

public class MainActivity extends AppCompatActivity {

    private YouTubeControllerExample youtubeController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация контроллера
        youtubeController = new YouTubeControllerExample(this);

        // Пример: кнопки управления
        findViewById(R.id.btnPlay).setOnClickListener(v -> youtubeController.play());
        findViewById(R.id.btnPause).setOnClickListener(v -> youtubeController.pause());
        findViewById(R.id.btnNext).setOnClickListener(v -> youtubeController.nextVideo());
        findViewById(R.id.btnPrev).setOnClickListener(v -> youtubeController.previousVideo());

        // Пример: поиск
        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            String query = editTextSearch.getText().toString();
            youtubeController.searchVideo(query);
        });

        // Пример: перемотка
        findViewById(R.id.btnSkipForward).setOnClickListener(v -> youtubeController.skipForward(10));
        findViewById(R.id.btnSkipBack).setOnClickListener(v -> youtubeController.skipBackward(10));

        // Пример: открыть конкретное видео
        findViewById(R.id.btnOpenVideo).setOnClickListener(v -> {
            youtubeController.playVideoById("dQw4w9WgXcQ");
        });
    }

    // Обработка клавиш медиа (наушники, Bluetooth пульт)
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    youtubeController.toggle();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    youtubeController.nextVideo();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    youtubeController.previousVideo();
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}

*/

// ==================== ПРИМЕР ИСПОЛЬЗОВАНИЯ В SERVICE ====================

/*

public class YouTubeControlService extends Service {

    private YouTubeControllerExample controller;

    @Override
    public void onCreate() {
        super.onCreate();
        controller = new YouTubeControllerExample(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra("action");

        if (action != null) {
            handleAction(action, intent);
        }

        return START_STICKY;
    }

    private void handleAction(String action, Intent intent) {
        switch (action) {
            case "play":
                controller.play();
                break;
            case "pause":
                controller.pause();
                break;
            case "search":
                String query = intent.getStringExtra("query");
                controller.searchVideo(query);
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

*/

// ==================== ПРИМЕР ИНТЕГРАЦИИ С TASKER / MACRODROID ====================

/*

// В Tasker/MacroDroid можно создать профили для автоматизации:

// Профиль 1: При подключении Bluetooth наушников - пауза
if (bluetoothConnected) {
    controller.pause();
}

// Профиль 2: При входе в авто - возобновить воспроизведение
if (carModeActivated) {
    controller.play();
}

// Профиль 3: Голосовая команда "YouTube search cats"
if (voiceCommand.contains("youtube search")) {
    String query = voiceCommand.replace("youtube search", "").trim();
    controller.searchVideo(query);
}

// Профиль 4: Жест - свайп вправо = следующее видео
if (gestureSwipeRight) {
    controller.nextVideo();
}

*/
