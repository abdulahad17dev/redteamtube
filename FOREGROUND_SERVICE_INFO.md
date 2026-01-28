# 🚀 Foreground Service для постоянного управления

## 🎯 Решение проблемы

**Проблема:** Android 8.0+ блокирует broadcast messages для закрытых приложений.

**Решение:** Легкий Foreground Service который работает в фоне и принимает команды **в любое время**.

---

## ✨ Как это работает

### 1. ControlListenerService
- Запускается автоматически при открытии приложения
- Работает как **foreground service** (показывает маленькую иконку в статус баре)
- Остается активным даже когда вы закрываете UI приложения
- Принимает broadcast команды **24/7**
- Автоматически перезапускается если убит системой (START_STICKY)

### 2. Минимальное влияние на батарею
- ✅ Низкий приоритет notification (без звука и вибрации)
- ✅ Не использует GPS, сеть или другие ресурсоемкие API
- ✅ Только слушает broadcast messages
- ✅ Можно остановить через Settings → Apps → RedTeamTube → Force Stop

### 3. Notification
Вы увидите постоянное уведомление:
```
🎵 YouTube Control Active
   Ready to receive commands
```

Это требование Android для foreground services. Notification:
- ✅ Не издает звуков
- ✅ Не вибрирует
- ✅ Имеет низкий приоритет
- ✅ Можно свернуть (minimized)
- ❌ Нельзя смахнуть (это защита Android от убийства сервиса)

---

## 🎮 Использование

### Теперь команды работают ВСЕГДА:

```bash
# Закрыть приложение полностью
adb shell am force-stop redteam.tube

# Подождать пару секунд...
sleep 3

# Отправить команду - приложение откроется и выполнит команду!
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "kotlin"
```

### Работает в трех сценариях:

#### 1️⃣ Приложение открыто (UI видимый)
```
Broadcast → ControlListenerService → MainActivity (напрямую)
```

#### 2️⃣ Приложение в фоне (UI закрыт)
```
Broadcast → ControlListenerService → Запускает MainActivity → Выполняет команду
```

#### 3️⃣ Приложение полностью закрыто
```
Broadcast → ControlListenerService (перезапускается) → Запускает MainActivity → Выполняет команду
```

---

## 📋 Что изменилось

### Новые файлы:
1. **ControlListenerService.java** - foreground service для приема команд
2. **FOREGROUND_SERVICE_INFO.md** - эта документация

### Обновленные файлы:
1. **AndroidManifest.xml** - добавлен Service и permissions
2. **MainActivity.java** - автозапуск Service при старте
3. **YouTubeControlReceiver.java** - добавлены методы для Service

### Новые permissions:
- `FOREGROUND_SERVICE` - для запуска foreground service
- `POST_NOTIFICATIONS` - для показа notification (Android 13+)

---

## 🔧 Управление Service

### Запустить вручную (если нужно):
```bash
adb shell am startservice redteam.tube/.ControlListenerService
```

### Проверить что Service запущен:
```bash
adb shell dumpsys activity services redteam.tube
```

Должно показать:
```
ServiceRecord{...ControlListenerService}
  app=ProcessRecord{...redteam.tube}
```

### Остановить Service:
```bash
# Через force-stop
adb shell am force-stop redteam.tube

# Или через stopservice
adb shell am stopservice redteam.tube/.ControlListenerService
```

### Проверить логи Service:
```bash
adb logcat -s ControlListenerService:I
```

---

## 🧪 Тестирование

### Полный тест работы:

```bash
# 1. Собрать и установить новую версию
cd C:\Users\User\AndroidStudioProjects\redteamtube
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 2. Запустить приложение (Service запустится автоматически)
adb shell am start -n redteam.tube/.MainActivity

# 3. Проверить что Service запущен
adb logcat -d -s MainActivity:I | findstr "ControlListenerService started"

# 4. Закрыть приложение
adb shell am force-stop redteam.tube

# 5. Подождать 3 секунды
sleep 3

# 6. Отправить команду
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "android"

# 7. Проверить логи
adb logcat -d -s ControlListenerService:I | findstr "Received command"
```

**Ожидаемый результат:**
```
ControlListenerService: 🎯 Received command: SEARCH
ControlListenerService: ⚠️ MainActivity not running, launching with command
MainActivity: Searching YouTube: android
```

---

## 💡 Альтернативы (если не нравится notification)

### Вариант 1: Скрыть notification (требует root)
```bash
# На устройстве с root доступом
adb shell settings put global heads_up_notifications_enabled 0
```

### Вариант 2: Minimize notification
Settings → Apps → RedTeamTube → Notifications → "YouTube Control Service" → Minimize

### Вариант 3: Использовать без Service (manual start)
Просто не закрывайте приложение полностью, держите в фоне.

---

## ❓ FAQ

### Q: Почему нужен foreground service?
**A:** Android 8.0+ не позволяет приложениям получать broadcasts когда они полностью закрыты. Foreground service - единственный надежный способ.

### Q: Можно ли убрать notification?
**A:** Нет, это требование Android для всех foreground services начиная с Android 8.0. Это защита от злоупотреблений.

### Q: Сильно ли влияет на батарею?
**A:** Минимально. Service только слушает broadcasts (не использует CPU), нет сетевой активности, GPS или других ресурсов.

### Q: Можно ли остановить Service?
**A:** Да, через Settings → Apps → RedTeamTube → Force Stop. Но тогда broadcasts не будут работать пока не запустите приложение снова.

### Q: Будет ли Service работать после перезагрузки?
**A:** Нет. После перезагрузки нужно один раз открыть приложение, и Service запустится автоматически.

### Q: Можно ли сделать автозапуск при загрузке системы?
**A:** Да, можно добавить BOOT_COMPLETED receiver, но это требует дополнительного permission и может раздражать пользователя.

---

## 🎯 Итоговая архитектура

```
┌─────────────────────────────────────────┐
│         Другое приложение/ADB           │
└──────────────────┬──────────────────────┘
                   │ Broadcast
                   ▼
┌─────────────────────────────────────────┐
│      YouTubeControlReceiver (static)    │
│      (registered in Manifest)           │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│      ControlListenerService             │
│      (Foreground Service - ALWAYS ON)   │
└──────────────────┬──────────────────────┘
                   │
                   ├──► MainActivity открыта?
                   │    ├─ Да  → Передать команду напрямую
                   │    └─ Нет → Запустить MainActivity
                   │
                   ▼
┌─────────────────────────────────────────┐
│           MainActivity                  │
│      (WebView + Command Handler)        │
└─────────────────────────────────────────┘
```

---

## ✅ Преимущества

- ✅ Работает **всегда** - даже когда UI закрыт
- ✅ Автоматический перезапуск если убит системой
- ✅ Минимальное влияние на батарею
- ✅ Не требует root доступа
- ✅ Соответствует Android best practices
- ✅ Легко остановить если не нужно

---

## 🚀 После установки

1. Запустите приложение один раз
2. Service запустится автоматически
3. Можете закрыть UI
4. Broadcast команды будут работать **всегда**!

Notification покажет что Service активен. Это нормально и правильно! 🎉
