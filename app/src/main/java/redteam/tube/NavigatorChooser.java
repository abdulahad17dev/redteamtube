package redteam.tube;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Диалог выбора между Яндекс.Картами и Яндекс.Навигатором
 */
public class NavigatorChooser {

    private static final String TAG = "NavigatorChooser";

    private static final String YANDEX_MAPS_PACKAGE = "ru.yandex.yandexmaps";
    private static final String YANDEX_NAVI_PACKAGE = "ru.yandex.yandexnavi";

    public interface OnNavigatorSelectedListener {
        void onMapsSelected();
        void onNaviSelected();
        void onCancelled();
    }

    /**
     * Показывает диалог выбора навигатора и открывает маршрут к указанным координатам
     */
    public static void showAndNavigate(Context context, double latitude, double longitude) {
        show(context, new OnNavigatorSelectedListener() {
            @Override
            public void onMapsSelected() {
                openInYandexMaps(context, latitude, longitude);
            }

            @Override
            public void onNaviSelected() {
                openInYandexNavi(context, latitude, longitude);
            }

            @Override
            public void onCancelled() {
                // Ничего не делаем
            }
        });
    }

    /**
     * Показывает диалог выбора навигатора
     */
    public static void show(Context context, OnNavigatorSelectedListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_navigator_chooser, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Загружаем иконки приложений
        ImageView iconMaps = dialogView.findViewById(R.id.icon_yandex_maps);
        ImageView iconNavi = dialogView.findViewById(R.id.icon_yandex_navi);

        loadAppIcon(context, YANDEX_MAPS_PACKAGE, iconMaps);
        loadAppIcon(context, YANDEX_NAVI_PACKAGE, iconNavi);

        // Яндекс Карты
        dialogView.findViewById(R.id.btn_yandex_maps).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onMapsSelected();
            }
        });

        // Яндекс Навигатор
        dialogView.findViewById(R.id.btn_yandex_navi).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onNaviSelected();
            }
        });

        // Отмена
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onCancelled();
            }
        });

        dialog.show();
    }

    /**
     * Загружает иконку приложения по package name
     */
    private static void loadAppIcon(Context context, String packageName, ImageView imageView) {
        try {
            PackageManager pm = context.getPackageManager();
            Drawable icon = pm.getApplicationIcon(packageName);
            imageView.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "App not installed: " + packageName);
            // Оставляем пустую иконку или можно поставить placeholder
            imageView.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }

    /**
     * Открывает маршрут в Яндекс.Картах
     */
    public static void openInYandexMaps(Context context, double latitude, double longitude) {
        try {
            Intent intent = new Intent("ru.yandex.yandexmaps.action.BUILD_ROUTE_ON_MAP");
            intent.setPackage(YANDEX_MAPS_PACKAGE);
            intent.putExtra("lat_to", latitude);
            intent.putExtra("lon_to", longitude);
            context.startActivity(intent);
            Log.d(TAG, "Opening Yandex Maps: lat=" + latitude + ", lon=" + longitude);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Yandex Maps", e);
            Toast.makeText(context, "Яндекс Карты не установлены", Toast.LENGTH_SHORT).show();
            openPlayStore(context, YANDEX_MAPS_PACKAGE);
        }
    }

    /**
     * Открывает маршрут в Яндекс.Навигаторе
     */
    public static void openInYandexNavi(Context context, double latitude, double longitude) {
        try {
            // Формат для Яндекс.Навигатора: yandexnavi://build_route_on_map?lat_to=...&lon_to=...
            String uri = "yandexnavi://build_route_on_map?lat_to=" + latitude + "&lon_to=" + longitude;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage(YANDEX_NAVI_PACKAGE);
            context.startActivity(intent);
            Log.d(TAG, "Opening Yandex Navi: lat=" + latitude + ", lon=" + longitude);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Yandex Navigator", e);
            Toast.makeText(context, "Яндекс Навигатор не установлен", Toast.LENGTH_SHORT).show();
            openPlayStore(context, YANDEX_NAVI_PACKAGE);
        }
    }

    /**
     * Открывает маршрут в Google Maps
     */
    public static void openInGoogleMaps(Context context, double latitude, double longitude) {
        try {
            // Формат для Google Maps: google.navigation:q=lat,lng
            String uri = "google.navigation:q=" + latitude + "," + longitude;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            context.startActivity(intent);
            Log.d(TAG, "Opening Google Maps: lat=" + latitude + ", lon=" + longitude);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Google Maps", e);
            Toast.makeText(context, "Google Maps не установлен", Toast.LENGTH_SHORT).show();
            openPlayStore(context, "com.google.android.apps.maps");
        }
    }

    /**
     * Открывает Play Store для установки приложения
     */
    private static void openPlayStore(Context context, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + packageName));
            context.startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            context.startActivity(intent);
        }
    }

    /**
     * Проверяет, установлено ли приложение
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
