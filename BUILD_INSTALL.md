# 🔨 Сборка и установка RedTeamTube

## 🚀 ВАЖНО: Foreground Service

**Новинка!** Теперь приложение использует Foreground Service для приема broadcast команд **даже когда UI закрыт**.

Это означает:
- ✅ Команды работают ВСЕГДА (даже после закрытия приложения)
- ✅ Маленькая иконка в статус баре (это требование Android)
- ✅ Минимальное влияние на батарею

См. [FOREGROUND_SERVICE_INFO.md](FOREGROUND_SERVICE_INFO.md) для деталей.

---

## ✅ Что было добавлено в код:

1. **YouTubeControlReceiver.java** - новый BroadcastReceiver для управления из других приложений
2. **Функции управления временем** - seekToTime, skipForward, skipBackward
3. **Функции парсинга** - searchYouTube, scrapeCurrentPage, scrapeComments
4. **Улучшенное логирование** с emoji для отладки
5. **Автозапуск приложения** из BroadcastReceiver

## 📦 Сборка проекта

### Вариант 1: Через Gradle (командная строка)

```bash
cd C:\Users\User\AndroidStudioProjects\redteamtube
.\gradlew.bat assembleDebug
```

APK будет в: `app\build\outputs\apk\debug\app-debug.apk`

### Вариант 2: Через Android Studio

1. Откройте проект в Android Studio
2. Build → Build Bundle(s) / APK(s) → Build APK(s)
3. Дождитесь завершения сборки

## 📲 Установка на устройство

### Установка через ADB

```bash
# Удалить старую версию (опционально)
adb uninstall redteam.tube

# Установить новую версию
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Установка с сохранением данных

```bash
# -r flag сохраняет данные приложения
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## ✅ Проверка установки

```bash
# Проверить что приложение установлено
adb shell pm list packages | grep redteam

# Должно вывести:
# package:redteam.tube
```

## 🧪 Тестирование новых функций

### 1. Проверить что receiver зарегистрирован

```bash
adb shell dumpsys package redteam.tube | grep -A 5 YouTubeControlReceiver
```

### 2. Закрыть приложение

```bash
adb shell am force-stop redteam.tube
```

### 3. Отправить команду (должна автоматически запустить приложение)

```bash
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "android"
```

### 4. Проверить логи

```bash
adb logcat -s YouTubeControlReceiver:I MainActivity:I
```

**Ожидаемый вывод:**
```
YouTubeControlReceiver: ✅ Received control command: SEARCH
YouTubeControlReceiver: ⚠️ MainActivity not running, launching it...
YouTubeControlReceiver: ✅ MainActivity launched successfully
MainActivity: Searching YouTube: android
```

### 5. Протестировать все команды

```bash
# Play/Pause
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "TOGGLE"

# Next video
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "NEXT"

# Skip forward 10 seconds
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SKIP_FORWARD" --ei value 10

# Seek to 1 minute
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEEK_TO" --ei value 60

# Scrape page
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SCRAPE_PAGE"
```

## 🐛 Troubleshooting

### Ошибка: APK not found

**Причина:** Проект не собран или сборка в другой директории

**Решение:**
```bash
# Найти APK
dir /s app-debug.apk
```

### Ошибка: INSTALL_FAILED_UPDATE_INCOMPATIBLE

**Причина:** Конфликт подписи с установленной версией

**Решение:**
```bash
# Полностью удалить и установить заново
adb uninstall redteam.tube
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Receiver не получает команды

**Причина:** Приложение не пересобрано с новым кодом

**Решение:**
1. Clean build: `.\gradlew.bat clean`
2. Rebuild: `.\gradlew.bat assembleDebug`
3. Reinstall: `adb install -r app\build\outputs\apk\debug\app-debug.apk`

### Логи не показывают новые emoji

**Причина:** Установлена старая версия

**Решение:** Проверить версию кода:
```bash
adb logcat -d | grep "YouTubeControlReceiver"
```

Если не видите emoji (✅, ⚠️, 📤), значит установлена старая версия.

## 📝 Checklist перед тестированием

- [ ] Код обновлен (все файлы сохранены)
- [ ] Проект собран (`.\gradlew.bat assembleDebug`)
- [ ] APK установлен на устройство (`adb install -r`)
- [ ] Приложение запускается без ошибок
- [ ] BroadcastReceiver зарегистрирован в манифесте
- [ ] Логи показывают новые сообщения с emoji

## 🚀 Быстрая установка (одна команда)

```bash
cd C:\Users\User\AndroidStudioProjects\redteamtube && .\gradlew.bat assembleDebug && adb install -r app\build\outputs\apk\debug\app-debug.apk
```

После установки протестируйте:
```bash
adb shell am force-stop redteam.tube && adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "test"
```
