@echo off
echo ========================================
echo Rebuilding RedTeamTube...
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] Cleaning build...
call gradlew.bat clean

echo.
echo [2/3] Building APK...
call gradlew.bat assembleDebug

echo.
echo [3/3] Installing on device...
adb install -r app\build\outputs\apk\debug\app-debug.apk

echo.
echo ========================================
echo Done! Testing...
echo ========================================
echo.

echo Stopping app...
adb shell am force-stop redteam.tube

echo.
echo Sending test command...
adb shell am broadcast -n redteam.tube/.YouTubeControlReceiver -a redteam.tube.CONTROL --es action "SEARCH" --es value "test"

echo.
echo Checking logs...
timeout /t 2 >nul
adb logcat -d -s YouTubeControlReceiver:I MainActivity:I | findstr /C:"Received control command" /C:"MainActivity launched" /C:"Searching"

echo.
echo ========================================
echo Rebuild complete!
echo ========================================
pause
