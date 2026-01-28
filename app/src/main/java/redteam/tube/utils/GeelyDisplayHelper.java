package redteam.tube.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Geely multi-display management
 * Based on analysis of IQY (爱奇艺) app and Geely system services
 *
 * Display IDs for Geely:
 * - CSD (Center Stack Display - Driver):  1001 (0x3e9)
 * - PSD (Passenger Display):              1002 (0x3ea)
 * - FULL (Fullscreen Mode):               1003 (0x3eb)
 * - RSD (Rear Seat Display):              2304x1440 resolution
 */
public class GeelyDisplayHelper {

    private static final String TAG = "GeelyDisplayHelper";

    // Geely Display IDs (from system analysis)
    public static final int DISPLAY_ID_CSD = 0x3e9;   // 1001 - Center Stack Display (Driver)
    public static final int DISPLAY_ID_PSD = 0x3ea;   // 1002 - Passenger Display
    public static final int DISPLAY_ID_FULL = 0x3eb;  // 1003 - Fullscreen Mode

    // Rear Display dimensions (from GeelyMultiDisplayUtil)
    public static final int DISPLAY_RSD_WIDTH = 2304;
    public static final int DISPLAY_RSD_HEIGHT = 1440;

    /**
     * Check if the device is a Geely car infotainment system
     */
    public static boolean isGeelyDevice() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();

        return manufacturer.contains("geely")
            || manufacturer.contains("flyme")
            || model.contains("geely")
            || model.contains("zeekr");
    }

    /**
     * Get display type name based on display ID
     */
    @NonNull
    public static String getDisplayTypeName(int displayId) {
        switch (displayId) {
            case DISPLAY_ID_CSD:
                return "CSD (Driver Display)";
            case DISPLAY_ID_PSD:
                return "PSD (Passenger Display)";
            case DISPLAY_ID_FULL:
                return "FULL (Fullscreen Mode)";
            default:
                if (displayId >= 1000) {
                    return "Geely Display " + displayId;
                }
                return "Standard Display " + displayId;
        }
    }

    /**
     * Check if the display ID is a Geely-specific display
     */
    public static boolean isGeelyDisplay(int displayId) {
        return displayId >= 1000; // Geely displays start from 1000 (0x3e8)
    }

    /**
     * Check if the display is front display (CSD or PSD)
     */
    public static boolean isFrontDisplay(int displayId) {
        return displayId == DISPLAY_ID_CSD || displayId == DISPLAY_ID_PSD;
    }

    /**
     * Check if the display is rear display (based on resolution)
     */
    public static boolean isRearDisplay(@NonNull Display display) {
        Point size = new Point();
        display.getRealSize(size);
        return size.x == DISPLAY_RSD_WIDTH && size.y == DISPLAY_RSD_HEIGHT;
    }

    /**
     * Get all available displays on the device
     */
    @NonNull
    public static List<Display> getAllDisplays(@NonNull Context context) {
        List<Display> displays = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayManager displayManager =
                (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);

            if (displayManager != null) {
                Display[] displayArray = displayManager.getDisplays();
                for (Display display : displayArray) {
                    displays.add(display);
                }
            }
        }

        return displays;
    }

    /**
     * Get display info as string for logging
     */
    @NonNull
    public static String getDisplayInfo(@Nullable Display display) {
        if (display == null) {
            return "Display: null";
        }

        int displayId = display.getDisplayId();
        Point size = new Point();
        display.getRealSize(size);

        return String.format(
            "Display[id=%d (0x%x), type=%s, size=%dx%d]",
            displayId,
            displayId,
            getDisplayTypeName(displayId),
            size.x,
            size.y
        );
    }

    /**
     * Log display change event
     */
    public static void logDisplayChange(int oldDisplayId, int newDisplayId) {
        Log.i(TAG, "╔═══════════════════════════════════════════════════════════╗");
        Log.i(TAG, "║          DISPLAY CHANGED                                 ║");
        Log.i(TAG, "╠═══════════════════════════════════════════════════════════╣");
        Log.i(TAG, "║ From: " + getDisplayTypeName(oldDisplayId) + " (ID: " + oldDisplayId + ")");
        Log.i(TAG, "║   To: " + getDisplayTypeName(newDisplayId) + " (ID: " + newDisplayId + ")");
        Log.i(TAG, "╚═══════════════════════════════════════════════════════════╝");
    }

    /**
     * Get recommended UI scale factor based on display type
     * Front displays (CSD/PSD) are typically wider, rear displays are smaller
     */
    public static float getRecommendedUIScale(@NonNull Display display) {
        Point size = new Point();
        display.getRealSize(size);

        // Rear display (2304x1440) - slightly smaller UI elements
        if (size.x == DISPLAY_RSD_WIDTH && size.y == DISPLAY_RSD_HEIGHT) {
            return 0.9f;
        }

        // Front displays (typically 2560x1600 or larger) - standard UI
        if (size.x >= 2560) {
            return 1.0f;
        }

        // Default for unknown displays
        return 1.0f;
    }

    /**
     * Apply UI adjustments based on display characteristics
     * Call this when display changes or when UI is initialized
     */
    public static void applyDisplayAdjustments(@NonNull Activity activity, @NonNull Display display) {
        int displayId = display.getDisplayId();
        Point size = new Point();
        display.getRealSize(size);

        Log.i(TAG, "Applying display adjustments for " + getDisplayInfo(display));

        // You can implement specific UI adjustments here
        // For example:
        // - Adjust font sizes
        // - Resize UI elements
        // - Change layouts
        // - Adjust margins/padding

        float uiScale = getRecommendedUIScale(display);
        Log.i(TAG, "Recommended UI scale: " + uiScale);
    }

    /**
     * Check if multi-display swipe is likely supported
     * (Based on Geely system detection)
     */
    public static boolean isMultiDisplaySwipeSupported(@NonNull Context context) {
        // Multi-display swipe is supported on Geely devices with multiple displays
        if (!isGeelyDevice()) {
            return false;
        }

        List<Display> displays = getAllDisplays(context);
        int geelyDisplayCount = 0;

        for (Display display : displays) {
            if (isGeelyDisplay(display.getDisplayId())) {
                geelyDisplayCount++;
            }
        }

        // If there are 2+ Geely displays, swipe is likely supported
        return geelyDisplayCount >= 2;
    }
}
