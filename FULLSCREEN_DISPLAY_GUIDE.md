# 📺 Fullscreen Display Guide для RedTeamTube

## 🎯 Что было добавлено

Теперь ваше приложение **RedTeamTube** поддерживает запуск в **fullscreen режиме** на мультидисплейной системе Geely/Ecarx, точно так же как приложение IQY (爱奇艺)!

---

## ✅ Реализованные изменения

### 1. **DisplayIdConstants.java** - Константы дисплеев
```java
package redteam.tube.utils;

public class DisplayIdConstants {
    // Передние экраны
    public static final int DISPLAY_ID_FULL = 0x3eb;  // 1003 - FULLSCREEN ⭐
    public static final int DISPLAY_ID_CSD = 0x3e9;   // 1001 - Центральный
    public static final int DISPLAY_ID_PSD = 0x3ea;   // 1002 - Пассажирский

    // Задние экраны
    public static final int DISPLAY_ID_RSD_FULL = 0x23eb;  // 9195 - Задний fullscreen
    // ... другие константы
}
```

### 2. **AndroidManifest.xml** - Поддержка multi-display
```xml
<activity
    android:name=".MainActivity"
    android:resizeableActivity="true"  ⭐ Разрешает изменение размера
    android:configChanges="screenSize|screenLayout|orientation|smallestScreenSize|uiMode">
</activity>
```

### 3. **MainActivity.java** - Определение Display ID
```java
private void detectDisplayId() {
    Display display = getDisplay();
    currentDisplayId = display.getDisplayId();

    if (DisplayIdConstants.isFullscreenDisplay(currentDisplayId)) {
        // Автоматически включаем immersive mode
        enableImmersiveMode();
    }
}
```

### 4. **DisplayLauncher.java** - Утилита для запуска
```java
// Запуск в fullscreen режиме
DisplayLauncher.launchInFullscreen(context, MainActivity.class, null);
```

---

## 🚀 Как использовать

### **Вариант 1: Из другого приложения (Java/Kotlin)**

```java
// Простой запуск в fullscreen
DisplayLauncher.launchInFullscreen(context, MainActivity.class, null);

// Запуск с URL
Bundle extras = new Bundle();
extras.putString("url", "https://m.youtube.com/watch?v=dQw4w9WgXcQ");
DisplayLauncher.launchOnDisplay(context, MainActivity.class,
                                DisplayIdConstants.DISPLAY_ID_FULL, extras);

// Запуск на заднем экране
DisplayLauncher.launchInRearFullscreen(context, MainActivity.class, null);
```

### **Вариант 2: Из ADB**

```bash
# 1. Запуск в fullscreen режиме (Display ID = 1003)
adb shell am start -n redteam.tube/.MainActivity --display 1003

# 2. Запуск на заднем fullscreen (Display ID = 9195)
adb shell am start -n redteam.tube/.MainActivity --display 9195

# 3. Запуск с конкретным видео
adb shell am start -n redteam.tube/.MainActivity \
  --display 1003 \
  --es url "https://m.youtube.com/watch?v=dQw4w9WgXcQ"

# 4. Запуск на центральном экране (обычный режим)
adb shell am start -n redteam.tube/.MainActivity --display 1001

# 5. Проверка текущего Display ID
adb shell dumpsys activity activities | grep -A 10 "MainActivity"
```

### **Вариант 3: Через Intent напрямую**

```java
Intent intent = new Intent();
intent.setClassName("redteam.tube", "redteam.tube.MainActivity");
intent.putExtra("url", "https://m.youtube.com/watch?v=xxx");

// КЛЮЧЕВОЙ МОМЕНТ: ActivityOptions с Display ID
ActivityOptions options = ActivityOptions.makeBasic();
options.setLaunchDisplayId(0x3eb); // 1003 = DISPLAY_ID_FULL

context.startActivity(intent, options.toBundle());
```

---

## 📊 Таблица Display ID

| Display ID (HEX) | Display ID (DEC) | Название | Описание |
|------------------|------------------|----------|----------|
| `0x3e8` | 1000 | WALLPAPER | Wallpaper слой |
| `0x3e9` | 1001 | CSD | Центральный экран водителя |
| `0x3ea` | 1002 | PSD | Пассажирский экран |
| **`0x3eb`** | **1003** | **FULL** | **Полноэкранный режим** ⭐ |
| `0x3ec` | 1004 | FREEFROM | Freeform окно |
| `0x3ed` | 1005 | FULL_TOP | Верхнее fullscreen окно |
| `0x23e9` | 9193 | RSD_LEFT | Задний левый |
| `0x23ea` | 9194 | RSD_RIGHT | Задний правый |
| **`0x23eb`** | **9195** | **RSD_FULL** | **Задний fullscreen** ⭐ |

---

## 🔍 Проверка работы

### **1. Логи приложения**
```bash
adb logcat -s MainActivity:I DisplayLauncher:I
```

Вы увидите:
```
I/MainActivity: ╔═══════════════════════════════════════════════════════════╗
I/MainActivity: ║          DISPLAY ID DETECTION                            ║
I/MainActivity: ╠═══════════════════════════════════════════════════════════╣
I/MainActivity: ║ Display ID: 1003 (0x3eb)
I/MainActivity: ║ Display Name: Fullscreen Mode
I/MainActivity: ║ Is Fullscreen Display: true
I/MainActivity: ║ Is Rear Display: false
I/MainActivity: ╚═══════════════════════════════════════════════════════════╝
I/MainActivity: ▶ Launched in FULLSCREEN display - enabling immersive mode
```

### **2. Toast уведомление**
При запуске в fullscreen режиме приложение покажет Toast:
```
"Launched in Fullscreen Mode"
```

### **3. Dumpsys информация**
```bash
adb shell dumpsys activity activities | grep -A 15 "MainActivity"
```

Ищите поле `mDisplayId`:
```
ActivityRecord{...redteam.tube/.MainActivity...}
  mDisplayId=1003
```

---

## 🎬 Сценарии использования

### **Сценарий 1: Запуск видео на заднем экране**
```bash
# Пассажир на заднем сидении хочет смотреть видео в fullscreen
adb shell am start -n redteam.tube/.MainActivity \
  --display 9195 \
  --es url "https://m.youtube.com/watch?v=xxx"
```

### **Сценарий 2: Автоматический запуск в fullscreen при подключении HDMI**
```java
// В BroadcastReceiver при событии HDMI_PLUGGED
if (hdmiConnected) {
    DisplayLauncher.launchInFullscreen(context, MainActivity.class, null);
}
```

### **Сценарий 3: Переключение между экранами**
```java
// Текущий display ID
int currentDisplay = getCurrentDisplayId();

// Переключиться на fullscreen
if (currentDisplay != DisplayIdConstants.DISPLAY_ID_FULL) {
    DisplayLauncher.launchOnDisplay(this, MainActivity.class,
                                    DisplayIdConstants.DISPLAY_ID_FULL, null);
    finish(); // Закрыть текущую Activity
}
```

---

## ⚙️ Требования

- **Android 8.0+ (API 26+)** - для `ActivityOptions.setLaunchDisplayId()`
- **Android 11+ (API 30+)** - для `Display.getDisplayId()`
- **Geely/Ecarx система** - для поддержки специальных Display ID

---

## 🔧 Дополнительная настройка

### **Автоматический запуск в fullscreen**

Если хотите чтобы приложение **всегда** запускалось в fullscreen, добавьте в `AndroidManifest.xml`:

```xml
<meta-data
    android:name="android.app.default_display"
    android:value="1003" />
```

### **Обработка изменения Display ID во время работы**

```java
@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    // Переопределяем Display ID после изменения конфигурации
    detectDisplayId();
}
```

---

## 📝 Примечания

1. **Обычные Android устройства**: На обычных телефонах/планшетах Display ID всегда будет `0` (default display). Fullscreen функции будут работать через обычный immersive mode.

2. **System permissions**: Для запуска на специальных дисплеях может потребоваться подпись системным ключом или специальные разрешения.

3. **Тестирование**: На эмуляторе Android можно создать виртуальный второй дисплей:
   ```bash
   adb shell settings put global overlay_display_devices 1920x1080/240
   ```

4. **Совместимость**: Код написан с fallback для старых версий Android - приложение будет работать везде, просто без поддержки multi-display.

---

## 🎉 Готово!

Теперь ваше приложение **RedTeamTube** может запускаться в fullscreen режиме точно так же как **IQY (AiQiYi)**!

**Основные преимущества:**
- ✅ Автоматическое определение Display ID
- ✅ Адаптация UI под fullscreen режим
- ✅ Поддержка задних экранов
- ✅ Удобная утилита для запуска на любом дисплее
- ✅ Подробные логи для отладки
- ✅ Обратная совместимость с обычными Android устройствами

---

## 📞 Тестирование

```bash
# 1. Скомпилируйте проект
./gradlew assembleDebug

# 2. Установите на устройство
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Запустите в fullscreen
adb shell am start -n redteam.tube/.MainActivity --display 1003

# 4. Проверьте логи
adb logcat -s MainActivity:I DisplayLauncher:I
```

**Ожидаемый результат:**
Приложение запустится в полноэкранном режиме с автоматическим включением immersive mode и показом Toast уведомления!

🎊 **Happy Coding!**
