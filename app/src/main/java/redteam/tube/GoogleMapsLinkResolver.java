package redteam.tube;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Резолвер коротких ссылок Google Maps
 * Поддерживает:
 * - Короткие ссылки: https://maps.app.goo.gl/xxx
 * - Полные ссылки: https://www.google.com/maps/place/.../@lat,lng,zoom/...
 * - Ссылки с параметрами: ...!3d{lat}!4d{lng}...
 */
public class GoogleMapsLinkResolver {

    private static final String TAG = "GoogleMapsResolver";

    public interface Callback {
        void onResult(Double latitude, Double longitude, String finalUrl);
    }

    /**
     * Резолвит ссылку Google Maps и извлекает координаты
     */
    public static void resolve(Context context, final String url, final Callback callback) {
        // Сначала пробуем извлечь координаты напрямую из URL
        double[] coords = extractLatLng(url);
        if (coords != null) {
            Log.d(TAG, "Found coords directly in URL: lat=" + coords[0] + ", lon=" + coords[1]);
            callback.onResult(coords[0], coords[1], url);
            return;
        }

        // Если это короткая ссылка — резолвим через HTTP запрос
        Executors.newSingleThreadExecutor().execute(() -> {
            resolveShortLink(context, url, callback);
        });
    }

    private static void resolveShortLink(Context context, String shortUrl, Callback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            // Следуем редиректам и получаем финальный URL
            String finalUrl = followRedirects(shortUrl);
            Log.d(TAG, "Final URL after redirects: " + finalUrl);

            // Пробуем извлечь координаты из финального URL
            double[] coords = extractLatLng(finalUrl);
            if (coords != null) {
                mainHandler.post(() -> callback.onResult(coords[0], coords[1], finalUrl));
                return;
            }

            // Если координат нет — пробуем извлечь ftid и получить координаты через Place Details
            String ftid = extractFtid(finalUrl);
            if (ftid != null) {
                Log.d(TAG, "Found ftid: " + ftid);
                double[] placeCoords = getCoordinatesFromFtid(ftid);
                if (placeCoords != null) {
                    String url = finalUrl;
                    mainHandler.post(() -> callback.onResult(placeCoords[0], placeCoords[1], url));
                    return;
                }
            }

            // Если ничего не помогло — пробуем через WebView
            mainHandler.post(() -> resolveWithWebView(context, shortUrl, callback));

        } catch (Exception e) {
            Log.e(TAG, "Error resolving short link", e);
            mainHandler.post(() -> callback.onResult(null, null, shortUrl));
        }
    }

    private static String followRedirects(String urlStr) throws Exception {
        String currentUrl = urlStr;
        int maxRedirects = 10;

        for (int i = 0; i < maxRedirects; i++) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");

            int responseCode = conn.getResponseCode();

            if (responseCode >= 300 && responseCode < 400) {
                String location = conn.getHeaderField("Location");
                if (location != null) {
                    if (!location.startsWith("http")) {
                        URL base = new URL(currentUrl);
                        location = base.getProtocol() + "://" + base.getHost() + location;
                    }
                    currentUrl = location;
                    conn.disconnect();
                    continue;
                }
            }

            conn.disconnect();
            break;
        }

        return currentUrl;
    }

    private static String extractFtid(String url) {
        // ftid=0x38ae8b8dc631d0bf:0xcf043a320f05466b
        Pattern pattern = Pattern.compile("ftid=([0-9a-fx:]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static double[] getCoordinatesFromFtid(String ftid) {
        try {
            // Используем Google Maps embed API для получения координат
            String embedUrl = "https://www.google.com/maps/embed?pb=!1m14!1m8!1m3!1d1000!2d0!3d0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s" + ftid + "!2s!5e0";

            URL url = new URL(embedUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            String html = response.toString();

            // Ищем координаты в ответе
            // Формат: [null,null,[lat,lng]]
            Pattern coordPattern = Pattern.compile("\\[null,null,\\[(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)\\]\\]");
            Matcher matcher = coordPattern.matcher(html);
            if (matcher.find()) {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                Log.d(TAG, "Found coords from ftid: lat=" + lat + ", lon=" + lng);
                return new double[]{lat, lng};
            }

            // Альтернативный паттерн
            Pattern altPattern = Pattern.compile("center=(-?\\d+\\.\\d+)%2C(-?\\d+\\.\\d+)");
            Matcher altMatcher = altPattern.matcher(html);
            if (altMatcher.find()) {
                double lat = Double.parseDouble(altMatcher.group(1));
                double lng = Double.parseDouble(altMatcher.group(2));
                return new double[]{lat, lng};
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting coords from ftid", e);
        }
        return null;
    }

    private static void resolveWithWebView(Context context, String shortUrl, Callback callback) {
        WebView webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/91.0.4472.124 Safari/537.36"
        );

        final boolean[] callbackCalled = {false};
        final Handler timeoutHandler = new Handler(Looper.getMainLooper());

        Runnable timeoutRunnable = () -> {
            if (!callbackCalled[0]) {
                callbackCalled[0] = true;
                Log.w(TAG, "WebView timeout");
                webView.destroy();
                callback.onResult(null, null, shortUrl);
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 15000);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "WebView redirect: " + url);

                double[] coords = extractLatLng(url);
                if (coords != null && !callbackCalled[0]) {
                    callbackCalled[0] = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    webView.stopLoading();
                    webView.destroy();
                    callback.onResult(coords[0], coords[1], url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (callbackCalled[0]) return;

                // Извлекаем координаты из JS
                String jsExtract =
                        "(function() {" +
                                "  var html = document.documentElement.outerHTML;" +
                                // Ищем координаты в разных форматах
                                "  var patterns = [" +
                                "    /@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)/," +
                                "    /!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)/," +
                                "    /center=(-?\\d+\\.\\d+)%2C(-?\\d+\\.\\d+)/," +
                                "    /\\\"lat\\\":(-?\\d+\\.\\d+),\\\"lng\\\":(-?\\d+\\.\\d+)/," +
                                "    /\\[(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)\\]/," +
                                "  ];" +
                                "  for (var i = 0; i < patterns.length; i++) {" +
                                "    var match = html.match(patterns[i]);" +
                                "    if (match && match[1] && match[2]) {" +
                                "      var lat = parseFloat(match[1]);" +
                                "      var lng = parseFloat(match[2]);" +
                                "      if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {" +
                                "        return lat + ',' + lng;" +
                                "      }" +
                                "    }" +
                                "  }" +
                                "  return null;" +
                                "})();";

                view.evaluateJavascript(jsExtract, value -> {
                    if (callbackCalled[0]) return;

                    Log.d(TAG, "JS result: " + value);
                    if (value != null && !value.equals("null")) {
                        String clean = value.replace("\"", "");
                        String[] parts = clean.split(",");
                        if (parts.length == 2) {
                            try {
                                double lat = Double.parseDouble(parts[0]);
                                double lng = Double.parseDouble(parts[1]);
                                callbackCalled[0] = true;
                                timeoutHandler.removeCallbacks(timeoutRunnable);
                                webView.destroy();
                                callback.onResult(lat, lng, url);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                });
            }
        });

        Log.d(TAG, "Loading in WebView: " + shortUrl);
        webView.loadUrl(shortUrl);
    }

    /**
     * Извлекает координаты из URL Google Maps
     */
    public static double[] extractLatLng(String urlStr) {
        if (urlStr == null || urlStr.isEmpty()) return null;

        try {
            // Способ 1: Координаты после @ (например: @41.268005,69.244262,17z)
            Pattern atPattern = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            Matcher atMatcher = atPattern.matcher(urlStr);
            if (atMatcher.find()) {
                double lat = Double.parseDouble(atMatcher.group(1));
                double lng = Double.parseDouble(atMatcher.group(2));
                if (isValidCoordinates(lat, lng)) {
                    Log.d(TAG, "Found via '@': lat=" + lat + ", lon=" + lng);
                    return new double[]{lat, lng};
                }
            }

            // Способ 2: Параметры !3d и !4d
            Pattern dataPattern = Pattern.compile("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)");
            Matcher dataMatcher = dataPattern.matcher(urlStr);
            if (dataMatcher.find()) {
                double lat = Double.parseDouble(dataMatcher.group(1));
                double lng = Double.parseDouble(dataMatcher.group(2));
                if (isValidCoordinates(lat, lng)) {
                    Log.d(TAG, "Found via '!3d!4d': lat=" + lat + ", lon=" + lng);
                    return new double[]{lat, lng};
                }
            }

            // Способ 3: Query параметры
            Uri uri = Uri.parse(urlStr);

            String ll = uri.getQueryParameter("ll");
            if (ll != null) {
                String[] parts = ll.split(",");
                if (parts.length == 2) {
                    double lat = Double.parseDouble(parts[0]);
                    double lng = Double.parseDouble(parts[1]);
                    if (isValidCoordinates(lat, lng)) {
                        return new double[]{lat, lng};
                    }
                }
            }

            String center = uri.getQueryParameter("center");
            if (center != null) {
                String[] parts = center.split(",");
                if (parts.length == 2) {
                    double lat = Double.parseDouble(parts[0]);
                    double lng = Double.parseDouble(parts[1]);
                    if (isValidCoordinates(lat, lng)) {
                        return new double[]{lat, lng};
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing: " + e.getMessage());
        }
        return null;
    }

    private static boolean isValidCoordinates(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
}