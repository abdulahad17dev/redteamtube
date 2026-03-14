package redteam.tube.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UserAgentManager {

    private static final String TAG = "UserAgentManager";
    private static final String PREFS_NAME = "user_agent_prefs";
    private static final String KEY_CACHED_AGENTS = "cached_agents";
    private static final String KEY_SELECTED_AGENT = "selected_agent";
    private static final String KEY_LAST_FETCH_TIME = "last_fetch_time";

    private static final String DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36";

    // GitHub raw URL — замени на свой репозиторий
    private static final String GITHUB_UA_URL =
        "https://raw.githubusercontent.com/abdulahad17dev/redteamtube-config/main/user_agents.json";

    private final SharedPreferences prefs;

    public UserAgentManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Возвращает текущий UA. Если есть выбранный — его, иначе дефолтный.
     */
    public String getUserAgent() {
        return prefs.getString(KEY_SELECTED_AGENT, DEFAULT_UA);
    }

    /**
     * Загружает UA список с GitHub в фоновом потоке.
     * После загрузки автоматически выбирает случайный UA.
     */
    public void fetchUserAgentsAsync(OnFetchCompleteListener listener) {
        new Thread(() -> {
            try {
                URL url = new URL(GITHUB_UA_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String json = response.toString();
                    JSONArray array = new JSONArray(json);

                    List<String> agents = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        agents.add(array.getString(i));
                    }

                    if (!agents.isEmpty()) {
                        // Кэшируем список
                        prefs.edit()
                            .putString(KEY_CACHED_AGENTS, json)
                            .putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
                            .apply();

                        // Выбираем случайный UA из списка
                        String selected = agents.get(new Random().nextInt(agents.size()));
                        prefs.edit().putString(KEY_SELECTED_AGENT, selected).apply();

                        Log.i(TAG, "Fetched " + agents.size() + " user agents, selected: " + selected);
                        if (listener != null) listener.onSuccess(selected);
                    }
                } else {
                    Log.w(TAG, "Failed to fetch UA list, HTTP " + responseCode);
                    if (listener != null) listener.onError();
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "Failed to fetch UA list: " + e.getMessage());
                // Пробуем загрузить из кэша
                String cached = prefs.getString(KEY_CACHED_AGENTS, null);
                if (cached != null) {
                    try {
                        JSONArray array = new JSONArray(cached);
                        String selected = array.getString(new Random().nextInt(array.length()));
                        prefs.edit().putString(KEY_SELECTED_AGENT, selected).apply();
                        Log.i(TAG, "Using cached UA: " + selected);
                        if (listener != null) listener.onSuccess(selected);
                        return;
                    } catch (Exception ignored) {}
                }
                if (listener != null) listener.onError();
            }
        }).start();
    }

    /**
     * Возвращает список закэшированных UA.
     */
    public List<String> getCachedAgents() {
        List<String> agents = new ArrayList<>();
        String cached = prefs.getString(KEY_CACHED_AGENTS, null);
        if (cached != null) {
            try {
                JSONArray array = new JSONArray(cached);
                for (int i = 0; i < array.length(); i++) {
                    agents.add(array.getString(i));
                }
            } catch (Exception ignored) {}
        }
        return agents;
    }

    /**
     * Устанавливает конкретный UA вручную.
     */
    public void setUserAgent(String ua) {
        prefs.edit().putString(KEY_SELECTED_AGENT, ua).apply();
    }

    /**
     * Устанавливает URL для загрузки UA.
     */
    public static String getGithubUrl() {
        return GITHUB_UA_URL;
    }

    public interface OnFetchCompleteListener {
        void onSuccess(String selectedAgent);
        void onError();
    }
}
