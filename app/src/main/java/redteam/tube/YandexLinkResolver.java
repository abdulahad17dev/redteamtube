package redteam.tube;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YandexLinkResolver {

    private static final String TAG = "MAP";

    public interface Callback {
        void onResult(Double latitude, Double longitude, String finalUrl);
    }

    /**
     * Резолвит короткую ссылку Яндекс.Карт через WebView (для JS-редиректов)
     * Должен вызываться из Main thread!
     */
    public static void resolve(Context context, final String shortUrl, final Callback callback) {
        new Handler(Looper.getMainLooper()).post(() -> {
            resolveWithWebView(context, shortUrl, callback);
        });
    }

    private static void resolveWithWebView(Context context, String shortUrl, Callback callback) {
        WebView webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString(
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
        );

        final boolean[] callbackCalled = {false};
        final Handler timeoutHandler = new Handler(Looper.getMainLooper());

        // Таймаут на случай если координаты не найдены
        Runnable timeoutRunnable = () -> {
            if (!callbackCalled[0]) {
                callbackCalled[0] = true;
                Log.w(TAG, "WebView timeout - no coordinates found");
                webView.destroy();
                callback.onResult(null, null, shortUrl);
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 15000); // 15 сек таймаут

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "WebView redirect to: " + url);

                // Проверяем, есть ли координаты в URL
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
                Log.d(TAG, "WebView page finished: " + url);

                if (callbackCalled[0]) return;

                // Сначала проверяем URL
                double[] coords = extractLatLng(url);
                if (coords != null) {
                    callbackCalled[0] = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    webView.destroy();
                    callback.onResult(coords[0], coords[1], url);
                    return;
                }

                // Если в URL нет координат, пробуем извлечь из HTML (для страниц организаций)
                // Яндекс хранит координаты в JSON внутри страницы
                String jsExtractCoords =
                    "(function() {" +
                    "  var html = document.documentElement.outerHTML;" +
                    // Ищем координаты в разных форматах JSON
                    "  var patterns = [" +
                    "    /\"coordinates\"\\s*:\\s*\\[\\s*(-?\\d+\\.\\d+)\\s*,\\s*(-?\\d+\\.\\d+)\\s*\\]/," +
                    "    /\"point\"\\s*:\\s*\\{[^}]*\"lon\"\\s*:\\s*(-?\\d+\\.\\d+)[^}]*\"lat\"\\s*:\\s*(-?\\d+\\.\\d+)/," +
                    "    /\"point\"\\s*:\\s*\\{[^}]*\"lat\"\\s*:\\s*(-?\\d+\\.\\d+)[^}]*\"lon\"\\s*:\\s*(-?\\d+\\.\\d+)/," +
                    "    /\"lon\"\\s*:\\s*(-?\\d+\\.\\d+)[^}]*\"lat\"\\s*:\\s*(-?\\d+\\.\\d+)/," +
                    "    /\"lat\"\\s*:\\s*(-?\\d+\\.\\d+)[^}]*\"lon\"\\s*:\\s*(-?\\d+\\.\\d+)/," +
                    "    /ll=(-?\\d+\\.\\d+)%2C(-?\\d+\\.\\d+)/," +
                    "    /ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)/" +
                    "  ];" +
                    "  for (var i = 0; i < patterns.length; i++) {" +
                    "    var match = html.match(patterns[i]);" +
                    "    if (match) {" +
                    "      return match[1] + ',' + match[2];" +
                    "    }" +
                    "  }" +
                    "  return null;" +
                    "})();";

                view.evaluateJavascript(jsExtractCoords, value -> {
                    if (callbackCalled[0]) return;

                    Log.d(TAG, "JS extraction result: " + value);

                    if (value != null && !value.equals("null") && !value.isEmpty()) {
                        String cleanValue = value.replace("\"", "");
                        String[] parts = cleanValue.split(",");
                        if (parts.length == 2) {
                            try {
                                double coord1 = Double.parseDouble(parts[0]);
                                double coord2 = Double.parseDouble(parts[1]);

                                double lat, lon;
                                // Определяем что есть что (lon обычно больше для России/СНГ)
                                // coordinates обычно [lon, lat], а point {lat, lon}
                                if (Math.abs(coord1) <= 90 && Math.abs(coord2) > 90) {
                                    lat = coord1;
                                    lon = coord2;
                                } else if (Math.abs(coord2) <= 90 && Math.abs(coord1) > 90) {
                                    lat = coord2;
                                    lon = coord1;
                                } else {
                                    // Для Яндекса coordinates = [lon, lat]
                                    lon = coord1;
                                    lat = coord2;
                                }

                                Log.d(TAG, "Found coords via JS: lat=" + lat + ", lon=" + lon);
                                callbackCalled[0] = true;
                                timeoutHandler.removeCallbacks(timeoutRunnable);
                                webView.destroy();
                                callback.onResult(lat, lon, url);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Failed to parse coords: " + e.getMessage());
                            }
                        }
                    }
                });
            }
        });

        Log.d(TAG, "Loading URL in WebView: " + shortUrl);
        webView.loadUrl(shortUrl);
    }

    public static double[] extractLatLng(String urlStr) {
        try {
            Uri uri = Uri.parse(urlStr);

            // Способ 1: параметр ll (longitude,latitude)
            String ll = uri.getQueryParameter("ll");
            if (ll != null) {
                String[] parts = ll.split(",");
                if (parts.length == 2) {
                    double lon = Double.parseDouble(parts[0]);
                    double lat = Double.parseDouble(parts[1]);
                    Log.d(TAG, "Found coords via 'll' param: lat=" + lat + ", lon=" + lon);
                    return new double[]{lat, lon};
                }
            }

            // Способ 2: параметр whatshere[point] (longitude,latitude)
            String whatshere = uri.getQueryParameter("whatshere[point]");
            if (whatshere != null) {
                String[] parts = whatshere.split(",");
                if (parts.length == 2) {
                    double lon = Double.parseDouble(parts[0]);
                    double lat = Double.parseDouble(parts[1]);
                    Log.d(TAG, "Found coords via 'whatshere[point]' param: lat=" + lat + ", lon=" + lon);
                    return new double[]{lat, lon};
                }
            }

            // Способ 3: параметр pt (point marker)
            String pt = uri.getQueryParameter("pt");
            if (pt != null) {
                String[] parts = pt.split(",");
                if (parts.length >= 2) {
                    double lon = Double.parseDouble(parts[0]);
                    double lat = Double.parseDouble(parts[1]);
                    Log.d(TAG, "Found coords via 'pt' param: lat=" + lat + ", lon=" + lon);
                    return new double[]{lat, lon};
                }
            }

            Log.d(TAG, "No coordinates in URL params: " + urlStr);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing URL: " + e.getMessage());
        }
        return null;
    }
}
