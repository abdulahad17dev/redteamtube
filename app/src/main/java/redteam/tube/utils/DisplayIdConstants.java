package redteam.tube.utils;

/**
 * Константы Display ID для мультидисплейной системы Geely/Ecarx
 * Используются для запуска Activity на конкретных экранах
 */
public class DisplayIdConstants {

    // Передние экраны (Front displays)
    public static final int DISPLAY_ID_CSD = 0x3e9;          // 1001 - Центральный экран водителя
    public static final int DISPLAY_ID_PSD = 0x3ea;          // 1002 - Пассажирский экран
    public static final int DISPLAY_ID_FULL = 0x3eb;         // 1003 - ПОЛНОЭКРАННЫЙ режим ⭐
    public static final int DISPLAY_ID_FREEFROM = 0x3ec;     // 1004 - Freeform окно
    public static final int DISPLAY_ID_FULL_TOP_WINDOW = 0x3ed; // 1005 - Верхнее полноэкранное окно
    public static final int DISPLAY_ID_WALLPAPER = 0x3e8;    // 1000 - Wallpaper layer

    // Задние экраны (Rear displays)
    public static final int DISPLAY_ID_RSD_FULL = 0x23eb;    // 9195 - Задний полный экран
    public static final int DISPLAY_ID_RSD_LEFT = 0x23e9;    // 9193 - Задний левый
    public static final int DISPLAY_ID_RSD_RIGHT = 0x23ea;   // 9194 - Задний правый

    /**
     * Проверка, является ли Display ID полноэкранным режимом
     */
    public static boolean isFullscreenDisplay(int displayId) {
        return displayId == DISPLAY_ID_FULL || displayId == DISPLAY_ID_RSD_FULL;
    }

    /**
     * Проверка, является ли Display ID задним экраном
     */
    public static boolean isRearDisplay(int displayId) {
        return displayId >= DISPLAY_ID_RSD_LEFT && displayId <= DISPLAY_ID_RSD_FULL;
    }

    /**
     * Получить название дисплея по ID
     */
    public static String getDisplayName(int displayId) {
        switch (displayId) {
            case DISPLAY_ID_CSD:
                return "Central Screen (Driver)";
            case DISPLAY_ID_PSD:
                return "Passenger Screen";
            case DISPLAY_ID_FULL:
                return "Fullscreen Mode";
            case DISPLAY_ID_FREEFROM:
                return "Freeform Window";
            case DISPLAY_ID_FULL_TOP_WINDOW:
                return "Top Fullscreen Window";
            case DISPLAY_ID_WALLPAPER:
                return "Wallpaper Layer";
            case DISPLAY_ID_RSD_FULL:
                return "Rear Fullscreen";
            case DISPLAY_ID_RSD_LEFT:
                return "Rear Left";
            case DISPLAY_ID_RSD_RIGHT:
                return "Rear Right";
            default:
                return "Unknown Display (" + displayId + ")";
        }
    }
}
