# RedTeamTube API Documentation

## Управление YouTube из других приложений

RedTeamTube поддерживает управление через BroadcastReceiver, что позволяет любому Android приложению или ADB управлять воспроизведением видео.

---

## 📡 Broadcast Action

**Action:** `redteam.tube.CONTROL`

**Параметры:**
- `action` (String) - команда для выполнения
- `value` (String или Int) - опциональное значение для команды

---

## 🎮 Доступные команды

### Управление воспроизведением

#### PLAY / START_PLAYBACK
Запустить воспроизведение видео
```bash
# Explicit broadcast (работает даже если приложение закрыто)
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "PLAY"

# Или короткая версия (работает только если приложение открыто)
adb shell am broadcast -a redteam.tube.CONTROL -p redteam.tube --es action "PLAY"
```

#### PAUSE / STOP_PLAYBACK
Приостановить воспроизведение
```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "PAUSE"
```

#### TOGGLE / TOGGLE_PLAYBACK
Переключить воспроизведение (play/pause)
```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "TOGGLE"
```

---

### Навигация по видео

#### NEXT / NEXT_VIDEO
Следующее видео
```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "NEXT"
```

#### PREVIOUS / PREV / BACK
Предыдущее видео (назад в истории)
```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "PREVIOUS"
```

---

### Управление временем

#### SEEK_TO
Перемотать на конкретное время (в секундах)
```bash
# Перемотать на 1 минуту
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEEK_TO" --ei value 60

# Перемотать на 5 минут
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEEK_TO" --ei value 300
```

#### SKIP_FORWARD
Перемотать вперед на N секунд (по умолчанию 10)
```bash
# Перемотать вперед на 10 секунд
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SKIP_FORWARD"

# Перемотать вперед на 30 секунд
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SKIP_FORWARD" --ei value 30
```

#### SKIP_BACKWARD / SKIP_BACK
Перемотать назад на N секунд (по умолчанию 10)
```bash
# Перемотать назад на 10 секунд
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SKIP_BACKWARD"

# Перемотать назад на 30 секунд
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SKIP_BACKWARD" --ei value 30
```

---

### Управление URL и поиском

#### OPEN_URL / LOAD_URL
Открыть конкретный URL
```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "OPEN_URL" --es value "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

#### SEARCH / SEARCH_YOUTUBE
Поиск видео на YouTube
```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "android tutorial"
```

---

### Парсинг данных

#### SCRAPE_PAGE / SCRAPE_CURRENT_PAGE
Извлечь информацию о видео на текущей странице (главная, поиск, рекомендации)

Возвращает JSON с информацией о видео:
- Название
- Канал
- Просмотры
- Длительность
- URL

```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SCRAPE_PAGE"
```

**Пример вывода в logcat:**
```json
{
  "videos": [
    {
      "title": "Android Tutorial for Beginners",
      "url": "https://m.youtube.com/watch?v=abc123",
      "channel": "Tech Channel",
      "views": "1.2M views",
      "recency": "2 days ago",
      "duration": "10:30"
    }
  ],
  "count": 1
}
```

#### SCRAPE_COMMENTS
Извлечь комментарии к текущему видео

Возвращает JSON с комментариями:
- Текст комментария
- Количество лайков

```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SCRAPE_COMMENTS"
```

**Пример вывода в logcat:**
```json
{
  "comments": [
    {
      "text": "Great video!",
      "likes": "125"
    }
  ],
  "count": 1
}
```

---

## 📱 Использование из другого Android приложения

### Пример 1: Простая команда (Play)

```java
// Explicit broadcast - работает даже если приложение закрыто
Intent intent = new Intent("redteam.tube.CONTROL");
intent.setComponent(new ComponentName("redteam.tube", "redteam.tube.YouTubeControlReceiver"));
intent.putExtra("action", "PLAY");
context.sendBroadcast(intent);

// Или короче через setClassName:
Intent intent = new Intent("redteam.tube.CONTROL");
intent.setClassName("redteam.tube", "redteam.tube.YouTubeControlReceiver");
intent.putExtra("action", "PLAY");
context.sendBroadcast(intent);
```

### Пример 2: Команда с параметром (Seek)

```java
Intent intent = new Intent("redteam.tube.CONTROL");
intent.setComponent(new ComponentName("redteam.tube", "redteam.tube.YouTubeControlReceiver"));
intent.putExtra("action", "SEEK_TO");
intent.putExtra("value", 60); // Перемотать на 60 секунд
context.sendBroadcast(intent);
```

### Пример 3: Поиск видео

```java
Intent intent = new Intent("redteam.tube.CONTROL");
intent.setComponent(new ComponentName("redteam.tube", "redteam.tube.YouTubeControlReceiver"));
intent.putExtra("action", "SEARCH");
intent.putExtra("value", "android development");
context.sendBroadcast(intent);
```

### Пример 4: Открыть конкретное видео

```java
Intent intent = new Intent("redteam.tube.CONTROL");
intent.setComponent(new ComponentName("redteam.tube", "redteam.tube.YouTubeControlReceiver"));
intent.putExtra("action", "OPEN_URL");
intent.putExtra("value", "https://www.youtube.com/watch?v=VIDEO_ID");
context.sendBroadcast(intent);
```

### Пример 5: Комплексное управление

```java
public class YouTubeController {
    private Context context;
    private static final String ACTION = "redteam.tube.CONTROL";
    private static final String PACKAGE = "redteam.tube";
    private static final String RECEIVER = "redteam.tube.YouTubeControlReceiver";

    public YouTubeController(Context context) {
        this.context = context;
    }

    public void play() {
        sendCommand("PLAY");
    }

    public void pause() {
        sendCommand("PAUSE");
    }

    public void next() {
        sendCommand("NEXT");
    }

    public void seekTo(int seconds) {
        Intent intent = new Intent(ACTION);
        intent.setComponent(new ComponentName(PACKAGE, RECEIVER));
        intent.putExtra("action", "SEEK_TO");
        intent.putExtra("value", seconds);
        context.sendBroadcast(intent);
    }

    public void search(String query) {
        Intent intent = new Intent(ACTION);
        intent.setComponent(new ComponentName(PACKAGE, RECEIVER));
        intent.putExtra("action", "SEARCH");
        intent.putExtra("value", query);
        context.sendBroadcast(intent);
    }

    private void sendCommand(String action) {
        Intent intent = new Intent(ACTION);
        intent.setComponent(new ComponentName(PACKAGE, RECEIVER));
        intent.putExtra("action", action);
        context.sendBroadcast(intent);
    }
}
```

---

## 🎯 Примеры использования

### Управление с помощью кнопок

```java
// В вашей Activity
YouTubeController controller = new YouTubeController(this);

btnPlay.setOnClickListener(v -> controller.play());
btnPause.setOnClickListener(v -> controller.pause());
btnNext.setOnClickListener(v -> controller.next());
btnSeek.setOnClickListener(v -> controller.seekTo(120)); // 2 минуты
```

### Управление с помощью клавиатуры/пульта

```java
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    YouTubeController controller = new YouTubeController(this);

    switch (keyCode) {
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            controller.sendCommand("TOGGLE");
            return true;
        case KeyEvent.KEYCODE_MEDIA_NEXT:
            controller.next();
            return true;
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            controller.sendCommand("PREVIOUS");
            return true;
    }
    return super.onKeyDown(keyCode, event);
}
```

### Голосовое управление

```java
// Обработка голосовых команд
public void handleVoiceCommand(String command) {
    YouTubeController controller = new YouTubeController(this);

    if (command.contains("play")) {
        controller.play();
    } else if (command.contains("pause")) {
        controller.pause();
    } else if (command.contains("next")) {
        controller.next();
    } else if (command.contains("search")) {
        String query = command.replace("search", "").trim();
        controller.search(query);
    }
}
```

---

## 🚗 Интеграция с автомобильными системами

### CarPlay / Android Auto альтернатива

Вы можете создать приложение-контроллер для рулевого управления:

```java
public class SteeringWheelController extends Service {
    private YouTubeController controller;

    @Override
    public void onCreate() {
        super.onCreate();
        controller = new YouTubeController(this);

        // Подключение к рулевым кнопкам через CAN bus или Bluetooth
        registerSteeringWheelButtons();
    }

    private void onSteeringWheelButton(int buttonId) {
        switch (buttonId) {
            case BUTTON_PLAY:
                controller.play();
                break;
            case BUTTON_NEXT:
                controller.next();
                break;
            case BUTTON_VOLUME_UP:
                // Управление громкостью системы
                break;
        }
    }
}
```

---

## 📊 Мониторинг и логирование

Все команды логируются с тегом `MainActivity`:

```bash
# Просмотр логов в реальном времени
adb logcat -s MainActivity:I

# Фильтр только команд управления
adb logcat | grep "Control command"

# Фильтр результатов scraping
adb logcat | grep "Scraped"
```

---

## ⚠️ Важные заметки

1. **Debouncing**: Команды игнорируются если отправлены чаще чем раз в 500ms
2. **WebView ready**: При запуске приложения нужно подождать 2 секунды пока WebView инициализируется
3. **Автозапуск**: Если приложение не запущено, BroadcastReceiver автоматически запустит MainActivity
4. **Scraping**: Данные возвращаются в logcat, для получения в коде используйте callback interface

---

## 🔒 Безопасность

BroadcastReceiver экспортирован (`android:exported="true"`), что позволяет любому приложению отправлять команды.

Для продакшена рекомендуется:
1. Добавить проверку подписи вызывающего приложения
2. Использовать custom permission
3. Ограничить список разрешенных команд

---

## 🛠️ Troubleshooting

### Команда не выполняется
- Проверьте что приложение запущено
- Убедитесь что WebView загружен (подождите 2-3 секунды)
- Проверьте logcat на наличие ошибок

### Scraping возвращает пустой результат
- Убедитесь что страница полностью загружена
- Проверьте что вы на странице YouTube
- YouTube может изменить CSS селекторы - проверьте актуальность

### Приложение не запускается автоматически
- Проверьте что BroadcastReceiver зарегистрирован в манифесте
- Убедитесь что `exported="true"`
- Проверьте разрешения приложения

---

## 📝 Changelog

### Version 1.0
- ✅ Управление воспроизведением (Play/Pause/Toggle)
- ✅ Навигация (Next/Previous)
- ✅ Управление временем (Seek/Skip Forward/Skip Backward)
- ✅ Поиск видео
- ✅ Парсинг страниц
- ✅ Парсинг комментариев
- ✅ ADB управление
- ✅ Управление из других приложений
- ✅ Автозапуск при получении команды

---

## 🤝 Использование в проектах

Этот API идеален для:
- 🚗 Автомобильных информационно-развлекательных систем
- 🎮 Игровых контроллеров
- 🏠 Умных домов (Home Assistant, etc.)
- 🤖 Автоматизации (Tasker, MacroDroid)
- 🎤 Голосовых ассистентов
- ⌚ Умных часов
- 📱 Приложений-компаньонов

---

## 📧 Поддержка

Для вопросов и предложений создайте issue в репозитории проекта.
