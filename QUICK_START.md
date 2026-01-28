# 🚀 Быстрый старт - Управление RedTeamTube

## ⚡ Важно: Используйте explicit broadcast!

Начиная с Android 8.0, **implicit broadcasts** не работают для закрытых приложений.
Используйте **explicit broadcast** с флагом `-n` (component name).

---

## 📡 Правильный формат команды

```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "КОМАНДА"
```

### Структура:
- `-n redteam.tube/.YouTubeControlReceiver` - explicit receiver (обязательно!)
- `-a redteam.tube.CONTROL` - action
- `--es action "КОМАНДА"` - команда (string extra)
- `--es value "значение"` - строковое значение (опционально)
- `--ei value число` - числовое значение (опционально)

---

## 🎮 Самые используемые команды

### Воспроизведение
```bash
# Запустить
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "PLAY"

# Пауза
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "PAUSE"

# Переключить play/pause
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "TOGGLE"
```

### Навигация
```bash
# Следующее видео
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "NEXT"

# Предыдущее
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "PREVIOUS"
```

### Перемотка
```bash
# Перемотать на 1 минуту
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEEK_TO" --ei value 60

# Пропустить вперед 10 секунд
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SKIP_FORWARD" --ei value 10

# Назад 10 секунд
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SKIP_BACKWARD" --ei value 10
```

### Поиск
```bash
# Поиск видео
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "android tutorial"

# Открыть конкретное видео
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "OPEN_URL" --es value "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

### Парсинг
```bash
# Получить список видео на странице
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SCRAPE_PAGE"

# Получить комментарии
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SCRAPE_COMMENTS"
```

---

## 📊 Просмотр логов

```bash
# Все логи приложения
adb logcat -s MainActivity:I YouTubeControlReceiver:I

# Только команды управления
adb logcat | grep "control command"

# Результаты парсинга
adb logcat | grep "Scraped"
```

---

## ✅ Проверка работы

### 1. Проверить что приложение установлено
```bash
adb shell pm list packages | grep redteam
# Должно вывести: package:redteam.tube
```

### 2. Запустить приложение
```bash
adb shell am start -n redteam.tube/.MainActivity
```

### 3. Отправить тестовую команду
```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "TOGGLE"
```

### 4. Проверить логи
```bash
adb logcat -s YouTubeControlReceiver:I | grep "Received control command"
```

Должно вывести:
```
YouTubeControlReceiver: ✅ Received control command: TOGGLE
```

---

## 🔧 Troubleshooting

### ❌ Broadcast completed: result=0 но ничего не происходит

**Причина:** Используется implicit broadcast вместо explicit.

**Решение:** Добавьте `-n redteam.tube/.YouTubeControlReceiver` в команду:
```bash
# ❌ Неправильно (implicit)
adb shell am broadcast -a redteam.tube.CONTROL --es action "PLAY"

# ✅ Правильно (explicit)
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "PLAY"
```

### ❌ Component not found

**Причина:** Приложение не установлено или неправильное имя receiver.

**Решение:**
1. Проверьте установку: `adb shell pm list packages | grep redteam`
2. Пересоберите и установите приложение
3. Проверьте что receiver зарегистрирован в AndroidManifest.xml

### ❌ Команда не выполняется сразу

**Причина:** Приложение закрыто и запускается впервые.

**Решение:** Это нормально! При первой команде приложение запустится, повторите команду через 2-3 секунды.

---

## 🎯 Сценарии использования

### Создать плейлист и автоматически переключать видео
```bash
# Открыть первое видео
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "музыка"

# Каждые 3 минуты - следующее видео
while true; do
  sleep 180
  adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "NEXT"
done
```

### Управление с клавиатуры (Windows PowerShell)
```powershell
# Создать shortcuts
function yt-play { adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "PLAY" }
function yt-pause { adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "PAUSE" }
function yt-next { adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "NEXT" }
function yt-search { param($query) adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "$query" }

# Использование:
yt-play
yt-next
yt-search "android"
```

### Автоматический поиск и парсинг
```bash
# Поиск
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "kotlin tutorial"

# Подождать загрузки (2 секунды)
sleep 2

# Получить список видео
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SCRAPE_PAGE"

# Посмотреть результат в логах
adb logcat -d | grep "Scraped page"
```

---

## 📚 Полная документация

Для полного списка команд и примеров см. [API_USAGE.md](API_USAGE.md)

Для примера Java класса см. [YouTubeControllerExample.java](YouTubeControllerExample.java)
