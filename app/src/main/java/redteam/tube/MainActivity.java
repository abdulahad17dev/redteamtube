package redteam.tube;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import redteam.tube.data.HistoryDatabase;
import redteam.tube.data.WebViewRepositoryImpl;
import redteam.tube.domain.WebViewRepository;
import redteam.tube.utils.DisplayIdConstants;
import redteam.tube.utils.PreferencesManager;
import redteam.tube.utils.UserAgentManager;
import redteam.tube.utils.YouTubeWebViewClient;

import android.webkit.CookieManager;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private WebView webView;
    private ProgressBar progressBar;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private ViewGroup btnBack, btnForward, btnSettings, btnHome;
    private WebViewRepository repository;
    private ViewGroup bottomNavigationBar;
    private Handler fullscreenCheckHandler;
    private Runnable fullscreenCheckRunnable;
    private PreferencesManager preferencesManager;
    private HistoryDatabase historyDatabase;
    private UserAgentManager userAgentManager;


    // Display ID поддержка
    private int currentDisplayId = 0;
    private boolean isLaunchedInFullscreenDisplay = false;

    // Broadcast receiver для zoom настроек
    private android.content.BroadcastReceiver zoomReceiver;

    // Floating button panel with 3 buttons (internal View - no permission required)
    private android.view.ViewGroup floatingButtonPanel;
    private android.widget.ImageButton floatingBackButton;
    private android.content.BroadcastReceiver floatingBackReceiver;
    private float dX, dY;
    private int lastAction;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Тест: резолвим короткую ссылку и показываем диалог выбора навигатора
//        String shortUrl = "https://yandex.ru/maps/-/CLcyqLKX";

//        YandexLinkResolver.resolve(this, shortUrl, (latitude, longitude, finalUrl) -> {
//            if (latitude != null) {
//                Log.d("MAP", "Lat: " + latitude);
//                Log.d("MAP", "Lon: " + longitude);
//                Log.d("MAP", "Final URL: " + finalUrl);
//
//                // Показываем диалог выбора навигатора
//                NavigatorChooser.showAndNavigate(this, latitude, longitude);
//            } else {
//                Log.e("MAP", "Координаты не найдены!");
//                Toast.makeText(this, "Не удалось получить координаты", Toast.LENGTH_SHORT).show();
//            }
//        });

        String shortUrl = "https://www.google.com/maps/place/ARCADA+game+club/@41.268005,69.244262,17z/data=!3m1!4b1!4m6!3m5!1s0x38ae8b8dc631d0bf:0xcf043a320f05466b!8m2!3d41.268005!4d69.244262!16s%2Fg%2F11pb1_0ddm?entry=ttu&g_ep=EgoyMDI1MTEyMy4xIKXMDSoASAFQAw%3D%3D";

        // Или полная ссылка
//        String fullUrl = "https://www.google.com/maps/place/ARCADA+game+club/@41.268005,69.244262,17z/...";
//
//        GoogleMapsLinkResolver.resolve(this, shortUrl, (latitude, longitude, finalUrl) -> {
//            if (latitude != null) {
//                Log.d("MAP", "Lat: " + latitude);  // 41.268005
//                Log.d("MAP", "Lon: " + longitude); // 69.244262
//
//                // Открыть в навигаторе
////                NavigatorChooser.showAndNavigate(this, latitude, longitude);
//            } else {
//                Log.e("MAP", "Координаты не найдены!");
//            }
//        });



        // Initialize preferences and database
        preferencesManager = new PreferencesManager(this);
        historyDatabase = HistoryDatabase.getInstance(this);

        // Определяем Display ID на котором запущено приложение
        detectDisplayId();

        // КЛЮЧЕВАЯ ЛОГИКА: Автоматический запуск на Display 1003
        if (shouldLaunchOnFullscreenDisplay()) {
            Log.i(TAG, "▶ Auto-launching on Display 1003...");
            launchOnFullscreenDisplay();
        }

        setContentView(R.layout.activity_main);

        // Apply fullscreen setting
        applyFullscreenSetting();

        // Initialize repository
        repository = new WebViewRepositoryImpl();

        // Initialize views
        initViews();

        // Setup WebView
        setupWebView();

        // Setup navigation buttons
        setupNavigationButtons();

        // Start fullscreen monitoring
        startFullscreenMonitoring();

        // Register broadcast receiver для zoom обновлений
        registerZoomReceiver();

        // Initialize floating back button (internal View - no permission required)
        registerFloatingBackReceiver();
        setupFloatingBackButton();

        // Set up control listener for YouTubeControlReceiver
        YouTubeControlReceiver.setControlListener(this::handleControlCommand);

        // Start ControlListenerService для приема команд даже когда UI закрыт
        startControlService();

        // Check if launched with control command
        handleLaunchIntent(getIntent());

        // Load URL from intent or default
        String url = getIntent().getStringExtra("url");
        if (url == null || url.isEmpty()) {
            url = repository.getYouTubeUrl();
        }
        webView.loadUrl(url);
    }

    private void startControlService() {
        try {
            Intent serviceIntent = new Intent(this, ControlListenerService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.i(TAG, "✅ ControlListenerService started");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start ControlListenerService", e);
        }
    }

    /**
     * Handle control commands from launch intent
     */
    private void handleLaunchIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getStringExtra("control_action");
        if (action != null && !action.isEmpty()) {
            // Wait for WebView to be ready
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Create new intent with proper extras
                Intent controlIntent = new Intent();
                controlIntent.putExtra("action", action);

                // Add value extras
                String strValue = intent.getStringExtra("control_value");
                if (strValue != null) {
                    controlIntent.putExtra("value", strValue);
                }

                int intValue = intent.getIntExtra("control_value_int", -1);
                if (intValue != -1) {
                    controlIntent.putExtra("value", intValue);
                }

                handleControlCommand(action, controlIntent);
            }, 2000);
        }
    }

    /**
     * Регистрирует BroadcastReceiver для получения сигналов об обновлении zoom
     */
    private void registerZoomReceiver() {
        zoomReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                if ("redteam.tube.APPLY_ZOOM".equals(intent.getAction())) {
                    Log.i(TAG, "Received zoom update broadcast");
                    applyZoomSettings();
                }
            }
        };

        android.content.IntentFilter filter = new android.content.IntentFilter("redteam.tube.APPLY_ZOOM");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(zoomReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(zoomReceiver, filter);
        }
    }

    /**
     * Регистрирует BroadcastReceiver для toggle floating back button
     */
    private void registerFloatingBackReceiver() {
        floatingBackReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                if ("redteam.tube.TOGGLE_FLOATING_BACK".equals(intent.getAction())) {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    Log.i(TAG, "Received floating back toggle: " + enabled);

                    if (enabled) {
                        showFloatingBackButton();
                    } else {
                        hideFloatingBackButton();
                    }
                }
            }
        };

        android.content.IntentFilter filter = new android.content.IntentFilter("redteam.tube.TOGGLE_FLOATING_BACK");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(floatingBackReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(floatingBackReceiver, filter);
        }
    }


    /**
     * Обработка команд управления
     */
    private void handleControlCommand(String action, Intent intent) {
        try {
            switch (action.toUpperCase()) {
                // Playback controls
                case "PLAY":
                case "START_PLAYBACK":
                    playVideo();
                    showToast("Playing video");
                    break;

                case "PAUSE":
                case "STOP_PLAYBACK":
                    pauseVideo();
                    showToast("Paused video");
                    break;

                case "TOGGLE":
                case "TOGGLE_PLAYBACK":
                    togglePlayPause();
                    showToast("Toggled playback");
                    break;

                // Navigation
                case "NEXT":
                case "NEXT_VIDEO":
                    nextVideo();
                    showToast("Next video");
                    break;

                case "PREVIOUS":
                case "PREV":
                case "BACK":
                    previousVideo();
                    showToast("Previous video");
                    break;

                // Time controls
                case "SEEK_TO":
                    int seekTime = intent.getIntExtra("value", 0);
                    seekToTime(seekTime);
                    showToast("Seeked to " + seekTime + "s");
                    break;

                case "SKIP_FORWARD":
                    int skipForwardAmount = intent.getIntExtra("value", 10);
                    skipForward(skipForwardAmount);
                    showToast("Skipped forward " + skipForwardAmount + "s");
                    break;

                case "SKIP_BACKWARD":
                case "SKIP_BACK":
                    int skipBackAmount = intent.getIntExtra("value", 10);
                    skipBackward(skipBackAmount);
                    showToast("Skipped backward " + skipBackAmount + "s");
                    break;

                // URL control
                case "OPEN_URL":
                case "LOAD_URL":
                    String url = intent.getStringExtra("value");
                    if (url != null && !url.isEmpty()) {
                        webView.loadUrl(url);
                        showToast("Loading URL");
                    }
                    break;

                // Search
                case "SEARCH":
                case "SEARCH_YOUTUBE":
                    String query = intent.getStringExtra("value");
                    if (query != null && !query.isEmpty()) {
                        searchYouTube(query);
                        showToast("Searching: " + query);
                    }
                    break;

                // Scraping
                case "SCRAPE_PAGE":
                case "SCRAPE_CURRENT_PAGE":
                    scrapeCurrentPage(new ScrapingCallback() {
                        @Override
                        public void onSuccess(String result) {
                            Log.i(TAG, "Scraped page: " + result);
                            showToast("Page scraped successfully");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Scraping error: " + error);
                            showToast("Scraping error: " + error);
                        }
                    });
                    break;

                case "SCRAPE_COMMENTS":
                    scrapeComments(new ScrapingCallback() {
                        @Override
                        public void onSuccess(String result) {
                            Log.i(TAG, "Scraped comments: " + result);
                            showToast("Comments scraped successfully");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Comments scraping error: " + error);
                            showToast("Comments error: " + error);
                        }
                    });
                    break;

                case "CLICK_FIRST_VIDEO":
                    clickFirstVideo();
                    showToast("Clicking first video");
                    break;

                default:
                    Log.w(TAG, "Unknown control command: " + action);
                    showToast("Unknown command: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling control command: " + action, e);
            showToast("Error: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * Setup floating back button (internal View - no permission required)
     */
    private void setupFloatingBackButton() {
        // Get floating button from layout
        // Find floating panel and all buttons
        floatingButtonPanel = findViewById(R.id.floatingButtonPanel);
        floatingBackButton = findViewById(R.id.floatingBackButton);
        ImageButton floatingSearchButton = findViewById(R.id.floatingSearchButton);
        ImageButton floatingFullscreenButton = findViewById(R.id.floatingFullscreenButton);

        // Set visibility based on preference
        if (preferencesManager.isFloatingBackEnabled()) {
            floatingButtonPanel.setVisibility(View.VISIBLE);
        } else {
            floatingButtonPanel.setVisibility(View.GONE);
        }

        // Restore saved position for panel
        floatingButtonPanel.post(() -> {
            float savedX = preferencesManager.getFloatingBackX();
            float savedY = preferencesManager.getFloatingBackY();

            if (savedX != 0 || savedY != 0) {
                floatingButtonPanel.setX(savedX);
                floatingButtonPanel.setY(savedY);
                Log.i(TAG, "Restored floating panel position: X=" + savedX + ", Y=" + savedY);
            }
        });

        // 1. Back button - webView.goBack()
        floatingBackButton.setOnClickListener(v -> {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                Log.i(TAG, "Floating back button: navigating back in WebView");
            } else {
                Log.i(TAG, "Floating back button: WebView cannot go back - ignoring");
            }
        });

        // 2. Search button - open YouTube search
        floatingSearchButton.setOnClickListener(v -> {
            webView.evaluateJavascript(
                "(function() {" +
                "  var searchBtn = document.querySelector('.icon-button.topbar-menu-button-avatar-button');" +
                "  if (searchBtn) {" +
                "    searchBtn.click();" +
                "    setTimeout(function() {" +
                "      var searchInput = document.querySelector('.ytSearchboxComponentInput.yt-searchbox-input');" +
                "      if (searchInput) {" +
                "        searchInput.click();" +
                "      }" +
                "    }, 300);" +
                "  }" +
                "})();",
                null
            );
            Log.i(TAG, "Floating search button clicked");
        });

        // 3. Fullscreen button - enter fullscreen mode
        floatingFullscreenButton.setOnClickListener(v -> {
            webView.evaluateJavascript(
                "(function() {" +
                "  var playerBg = document.querySelector('.player-controls-background');" +
                "  if (playerBg) {" +
                "    playerBg.click();" +
                "    setTimeout(function() {" +
                "      var fullscreenBtn = document.querySelector('.icon-button.fullscreen-icon');" +
                "      if (fullscreenBtn) {" +
                "        fullscreenBtn.click();" +
                "      }" +
                "    }, 300);" +
                "  }" +
                "})();",
                null
            );
            Log.i(TAG, "Floating fullscreen button clicked");
        });

        // Add drag functionality for panel
        setupFloatingButtonDrag();

        Log.i(TAG, "Floating back button setup complete (internal View)");
    }

    /**
     * Показать floating back button
     */
    private void showFloatingBackButton() {
        if (floatingButtonPanel != null) {
            floatingButtonPanel.setVisibility(View.VISIBLE);
            Log.i(TAG, "Floating back button shown");
        }
    }

    /**
     * Скрыть floating back button
     */
    private void hideFloatingBackButton() {
        if (floatingButtonPanel != null) {
            floatingButtonPanel.setVisibility(View.GONE);
            Log.i(TAG, "Floating back button hidden");
        }
    }

    /**
     * Setup drag and drop для floating кнопки (internal View version)
     */
    private void setupFloatingButtonDrag() {
        floatingButtonPanel.setOnTouchListener(new View.OnTouchListener() {
            private boolean isDragging = false;
            private float initialX;
            private float initialY;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        initialX = v.getX();
                        initialY = v.getY();
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        lastAction = android.view.MotionEvent.ACTION_DOWN;
                        isDragging = false;
                        return true;

                    case android.view.MotionEvent.ACTION_MOVE:
                        // Calculate new position
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Get screen dimensions
                        android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int screenWidth = displayMetrics.widthPixels;
                        int screenHeight = displayMetrics.heightPixels;

                        // Get panel dimensions
                        int panelWidth = v.getWidth();
                        int panelHeight = v.getHeight();

                        // Apply boundaries - keep panel within screen
                        if (newX < 0) newX = 0;
                        if (newY < 0) newY = 0;
                        if (newX + panelWidth > screenWidth) newX = screenWidth - panelWidth;
                        if (newY + panelHeight > screenHeight) newY = screenHeight - panelHeight;

                        v.setX(newX);
                        v.setY(newY);
                        lastAction = android.view.MotionEvent.ACTION_MOVE;

                        // Check if actually dragging (moved more than threshold)
                        float deltaX = Math.abs(v.getX() - initialX);
                        float deltaY = Math.abs(v.getY() - initialY);
                        if (deltaX > 10 || deltaY > 10) {
                            isDragging = true;
                        }
                        return true;

                    case android.view.MotionEvent.ACTION_UP:
                        // Save position to preferences if dragged
                        if (isDragging) {
                            preferencesManager.setFloatingBackX((int) v.getX());
                            preferencesManager.setFloatingBackY((int) v.getY());
                            Log.i(TAG, "Floating button position saved: X=" + v.getX() + ", Y=" + v.getY());
                        }

                        // If it was a click (not dragging), perform click
                        if (!isDragging && lastAction == android.view.MotionEvent.ACTION_DOWN) {
                            v.performClick();
                            return true;
                        }

                        lastAction = android.view.MotionEvent.ACTION_UP;
                        isDragging = false;
                        return true;
                }
                return false;
            }
        });
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigationBar = findViewById(R.id.bottomNavigationBar);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnSettings = findViewById(R.id.btnSettings);
        btnHome = findViewById(R.id.btnHome);
    }

    private void enableImmersiveMode() {
        // Make activity fullscreen (hide status bar and navigation bar)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        // Keep screen on during playback
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set navigation bar color to black
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setStatusBarColor(Color.BLACK);
    }

    private void disableImmersiveMode() {
        // Show system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        controller.show(WindowInsetsCompat.Type.systemBars());

        // Clear keep screen on flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void applyFullscreenSetting() {
//        if (preferencesManager.isFullscreenEnabled()) {
//            enableImmersiveMode();
//        } else {
//            disableImmersiveMode();
//        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        // Enable WebView debugging для Chrome DevTools (chrome://inspect)
        // Это позволит инспектировать HTML элементы
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        WebSettings webSettings = webView.getSettings();

        // Enable JavaScript (required for YouTube)
        webSettings.setJavaScriptEnabled(true);

        // Enable DOM storage (required for YouTube)
        webSettings.setDomStorageEnabled(true);

        // Enable database storage
        webSettings.setDatabaseEnabled(true);

        // Enable caching for better performance
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Support for media playback
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Enable zoom controls
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);

        // Enable viewport and wide viewport
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // Dynamic UA from GitHub config (cached locally)
        userAgentManager = new UserAgentManager(this);
        webSettings.setUserAgentString(userAgentManager.getUserAgent());
        Log.i(TAG, "Using User-Agent: " + userAgentManager.getUserAgent());

        // Fetch fresh UA list from GitHub in background for next launch
        userAgentManager.fetchUserAgentsAsync(new UserAgentManager.OnFetchCompleteListener() {
            @Override
            public void onSuccess(String selectedAgent) {
                Log.i(TAG, "Fetched new UA from GitHub: " + selectedAgent);
            }
            @Override
            public void onError() {
                Log.w(TAG, "Could not fetch UA from GitHub, using cached/default");
            }
        });

        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Fix 2: Setup cookies before loading YouTube
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        // Fix 3: Accept third-party cookies to keep session alive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
        // Set consent cookies to bypass GDPR/consent screens
        cookieManager.setCookie("https://www.youtube.com", "CONSENT=PENDING+999");
        cookieManager.setCookie("https://m.youtube.com", "CONSENT=PENDING+999");
        cookieManager.setCookie("https://www.youtube.com", "SOCS=CAISEwgDEgk2ODE4MTY1NTQaAmVuIAEaBgiA_LyaBg");
        cookieManager.setCookie("https://m.youtube.com", "SOCS=CAISEwgDEgk2ODE4MTY1NTQaAmVuIAEaBgiA_LyaBg");
        cookieManager.flush();

        // Set WebViewClient
        webView.setWebViewClient(new YouTubeWebViewClient(new YouTubeWebViewClient.OnPageLoadListener() {
            @Override
            public void onPageStarted(String url) {
                updateNavigationButtons();
            }

            @Override
            public void onPageFinished(String url) {
                updateNavigationButtons();

                // Apply zoom settings from preferences
                applyZoomSettings();

                // Save to history only if enabled (exclude default YouTube homepage)
                if (preferencesManager.isHistoryEnabled() &&
                        !url.equals("https://m.youtube.com") &&
                        !url.equals("https://m.youtube.com/") &&
                        !url.equals("https://www.youtube.com") &&
                        !url.equals("https://www.youtube.com/")) {
                    webView.evaluateJavascript(
                            "(function() { return document.title; })();",
                            title -> {
                                String cleanTitle = title != null ? title.replace("\"", "") : "YouTube";
                                historyDatabase.addHistory(url, cleanTitle);
                            }
                    );
                }
            }

            @Override
            public void onPageError() {
                Toast.makeText(MainActivity.this, "Error loading page", Toast.LENGTH_SHORT).show();
            }
        }));

        // Set WebChromeClient for fullscreen video support
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Called when entering fullscreen video mode
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                customView = view;
                customViewCallback = callback;

                // Hide WebView and show fullscreen video view
                webView.setVisibility(View.GONE);
                bottomNavigationBar.setVisibility(View.GONE);

                // Add custom view to DecorView
                FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
                decorView.addView(customView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

                // НЕ перемещаем floating button - просто поднимаем на максимальный elevation
                // Она останется в оригинальном контейнере, но будет поверх всего
                if (floatingBackButton != null && preferencesManager.isFloatingBackEnabled()) {
                    floatingBackButton.setVisibility(View.VISIBLE);
                    floatingBackButton.bringToFront();
                    floatingBackButton.setElevation(1000f); // Максимальный elevation
                    Log.i(TAG, "Floating button brought to front with elevation 1000");
                }

                // Enter fullscreen mode
                enableImmersiveMode();
            }

            @Override
            public void onHideCustomView() {
                // Called when exiting fullscreen video mode
                if (customView == null) {
                    return;
                }

                // Remove custom view
                FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
                decorView.removeView(customView);

                // Floating button не перемещалась - просто убедимся что она видима
                if (floatingBackButton != null && preferencesManager.isFloatingBackEnabled()) {
                    floatingBackButton.setVisibility(View.VISIBLE);
                    floatingBackButton.setElevation(8f); // Вернуть обычный elevation
                    Log.i(TAG, "Floating button visibility restored after fullscreen exit");
                }

                // Show WebView again
                webView.setVisibility(View.VISIBLE);
                bottomNavigationBar.setVisibility(View.VISIBLE);

                customView = null;
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }

                // Restore immersive mode
                enableImmersiveMode();
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (progressBar != null) {
                    if (newProgress < 100) {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void setupNavigationButtons() {
        btnBack.setOnClickListener(v -> handleBackPress());

        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });

        btnSettings.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        btnHome.setOnClickListener(v -> finish());

        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        boolean canGoBack = webView.canGoBack();
        boolean canGoForward = webView.canGoForward();

        btnBack.setEnabled(canGoBack);
        btnBack.setAlpha(canGoBack ? 1.0f : 0.4f);

        btnForward.setEnabled(canGoForward);
        btnForward.setAlpha(canGoForward ? 1.0f : 0.4f);
    }

    private void handleBackPress() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    private void startFullscreenMonitoring() {
        fullscreenCheckHandler = new Handler(Looper.getMainLooper());
        fullscreenCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndUpdateButtonVisibility();
                fullscreenCheckHandler.postDelayed(this, 500); // Check every 500ms
            }
        };
        fullscreenCheckHandler.post(fullscreenCheckRunnable);
    }

    private void stopFullscreenMonitoring() {
        if (fullscreenCheckHandler != null && fullscreenCheckRunnable != null) {
            fullscreenCheckHandler.removeCallbacks(fullscreenCheckRunnable);
        }
    }

    private void checkAndUpdateButtonVisibility() {
        if (webView == null) {
            return;
        }

        webView.evaluateJavascript(
                "(function() { return document.fullscreenElement !== null; })();",
                result -> {
                    boolean isFullscreen = "true".equals(result);
                    updateButtonVisibility(!isFullscreen);
                }
        );
    }

    private void updateButtonVisibility(boolean show) {
        if (bottomNavigationBar != null) {
            bottomNavigationBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        // If in fullscreen video, exit fullscreen first
        if (customView != null) {
            webView.getWebChromeClient().onHideCustomView();
            return;
        }

        // If WebView can go back in history, navigate back
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // Otherwise, close activity
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopFullscreenMonitoring();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-apply fullscreen setting (in case it changed in Settings)
        applyFullscreenSetting();

        startFullscreenMonitoring();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Re-apply fullscreen setting when window regains focus
            applyFullscreenSetting();
        }
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        // Handle media key events from headphones/bluetooth devices
        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    togglePlayPause();
                    return true;

                case android.view.KeyEvent.KEYCODE_MEDIA_PLAY:
                    playVideo();
                    return true;

                case android.view.KeyEvent.KEYCODE_MEDIA_PAUSE:
                case android.view.KeyEvent.KEYCODE_MEDIA_STOP:
                    pauseVideo();
                    return true;

                case android.view.KeyEvent.KEYCODE_MEDIA_NEXT:
                    nextVideo();
                    return true;

                case android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    previousVideo();
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void playVideo() {
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function() {" +
                            "  var video = document.querySelector('video');" +
                            "  if (!video) return 'error: No video element found';" +
                            "  video.play();" +
                            "  return 'Video playing';" +
                            "})();",
                    null
            );
        }
    }

    private void pauseVideo() {
        if (webView != null) {

            webView.evaluateJavascript(
                    "(function() {" +
                            "  var video = document.querySelector('video');" +
                            "  if (!video) return 'error: No video element found';" +
                            "  video.pause();" +
                            "  return 'Video paused';" +
                            "})();",
                    null
            );
        }
    }

    private void togglePlayPause() {
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function() {" +
                            "  var video = document.querySelector('video');" +
                            "  if (!video) return 'error: No video element found';" +
                            "  if (video.paused) {" +
                            "    video.play();" +
                            "    return 'Video resumed';" +
                            "  } else {" +
                            "    video.pause();" +
                            "    return 'Video paused';" +
                            "  }" +
                            "})();",
                    null
            );
        }
    }

    private void nextVideo() {
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function() {" +
                            "  var buttons = document.querySelectorAll('.player-middle-controls-prev-next-button');" +
                            "  if (buttons.length > 1) {" +
                            "    buttons[1].click();" +
                            "    return 'Next video clicked';" +
                            "  }" +
                            "  return 'error: Next button not found';" +
                            "})();",
                    null
            );
        }
    }

    private void previousVideo() {
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function() {" +
                            "  if (window.history.length > 1) {" +
                            "    window.history.back();" +
                            "    return 'Navigated back';" +
                            "  }" +
                            "  return 'error: No history';" +
                            "})();",
                    null
            );
        }
    }

    /**
     * Seek to specific time in video
     * @param seconds Time in seconds to seek to
     */
    private void seekToTime(int seconds) {
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function() {" +
                            "  var video = document.querySelector('video');" +
                            "  if (!video) return 'error: No video element found';" +
                            "  var targetTime = Math.max(0, Math.min(" + seconds + ", video.duration));" +
                            "  video.currentTime = targetTime;" +
                            "  return 'Seeked to ' + targetTime + 's / ' + video.duration + 's';" +
                            "})();",
                    result -> Log.i(TAG, "Seek result: " + result)
            );
        }
    }

    /**
     * Skip forward by N seconds (default 10)
     * @param seconds Number of seconds to skip forward
     */
    private void skipForward(int seconds) {
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function() {" +
                            "  var video = document.querySelector('video');" +
                            "  if (!video) return 'error: No video element found';" +
                            "  var oldTime = video.currentTime;" +
                            "  var skipAmount = " + seconds + ";" +
                            "  video.currentTime = Math.min(video.currentTime + skipAmount, video.duration);" +
                            "  return 'Skipped forward ' + skipAmount + 's (from ' + oldTime + 's to ' + video.currentTime + 's)';" +
                            "})();",
                    result -> Log.i(TAG, "Skip forward result: " + result)
            );
        }
    }

    /**
     * Skip backward by N seconds (default 10)
     * @param seconds Number of seconds to skip backward
     */
    private void skipBackward(int seconds) {
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function() {" +
                            "  var video = document.querySelector('video');" +
                            "  if (!video) return 'error: No video element found';" +
                            "  var oldTime = video.currentTime;" +
                            "  var skipAmount = " + seconds + ";" +
                            "  video.currentTime = Math.max(video.currentTime - skipAmount, 0);" +
                            "  return 'Skipped backward ' + skipAmount + 's (from ' + oldTime + 's to ' + video.currentTime + 's)';" +
                            "})();",
                    result -> Log.i(TAG, "Skip backward result: " + result)
            );
        }
    }

    /**
     * Click on first video in search results or feed
     */
    private void clickFirstVideo() {
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function() {" +
                            "  var firstVideo = document.querySelector('ytm-video-with-context-renderer.item a, ytm-rich-item-renderer a');" +
                            "  if (firstVideo) {" +
                            "    firstVideo.click();" +
                            "    return 'Clicked first video';" +
                            "  }" +
                            "  return 'error: No video found';" +
                            "})();",
                    result -> Log.i(TAG, "Click first video result: " + result)
            );
        }
    }

    /**
     * Search YouTube for videos
     * @param query Search query
     */
    private void searchYouTube(String query) {
        if (webView == null || query == null || query.isEmpty()) {
            return;
        }

        // URL encode the query
        String encodedQuery = Uri.encode(query);
        String searchUrl = "https://www.youtube.com/results?search_query=" + encodedQuery;

        Log.i(TAG, "Searching YouTube: " + query);
        webView.loadUrl(searchUrl);
    }

    /**
     * Scrape current page for video metadata
     * Returns JSON with video information
     */
    private void scrapeCurrentPage(ScrapingCallback callback) {
        if (webView == null) {
            callback.onError("WebView is null");
            return;
        }

        webView.evaluateJavascript(
                "(function() {" +
                        "  var videos = [];" +
                        "  var items = document.querySelectorAll('ytm-video-with-context-renderer.item.adaptive-feed-item, ytm-rich-item-renderer');" +
                        "  items.forEach(function(item) {" +
                        "    var titleEl = item.querySelector('h3.media-item-headline span.yt-core-attributed-string');" +
                        "    var linkEl = item.querySelector('a.media-item-thumbnail-container');" +
                        "    var detailsEls = item.querySelectorAll('ytm-badge-and-byline-renderer span');" +
                        "    var durationEl = item.querySelector('ytm-thumbnail-overlay-time-status-renderer badge-shape div');" +
                        "    " +
                        "    if (titleEl && linkEl) {" +
                        "      var video = {" +
                        "        title: titleEl.textContent.trim()," +
                        "        url: linkEl.href," +
                        "        channel: detailsEls.length > 0 ? detailsEls[0].textContent.trim() : ''," +
                        "        views: detailsEls.length > 1 ? detailsEls[1].textContent.trim() : ''," +
                        "        recency: detailsEls.length > 2 ? detailsEls[2].textContent.trim() : ''," +
                        "        duration: durationEl ? durationEl.textContent.trim() : ''" +
                        "      };" +
                        "      videos.push(video);" +
                        "    }" +
                        "  });" +
                        "  return JSON.stringify({videos: videos, count: videos.length});" +
                        "})();",
                result -> {
                    if (result != null && !result.equals("null")) {
                        callback.onSuccess(result.replace("\\", ""));
                    } else {
                        callback.onError("No videos found");
                    }
                }
        );
    }

    /**
     * Scrape comments from current video
     */
    private void scrapeComments(ScrapingCallback callback) {
        if (webView == null) {
            callback.onError("WebView is null");
            return;
        }

        // First, click comments button to open comments panel
        webView.evaluateJavascript(
                "(function() {" +
                        "  var commentsBtn = document.querySelector('button[aria-label*=\"Comment\"], button[aria-label*=\"комментар\"]');" +
                        "  if (commentsBtn) {" +
                        "    commentsBtn.click();" +
                        "    return 'clicked';" +
                        "  }" +
                        "  return 'not_found';" +
                        "})();",
                clickResult -> {
                    if ("\"clicked\"".equals(clickResult)) {
                        // Wait 2 seconds for comments to load, then scrape
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            scrapeCommentsContent(callback);
                        }, 2000);
                    } else {
                        callback.onError("Comments button not found");
                    }
                }
        );
    }

    /**
     * Scrape comments content (after panel is opened)
     */
    private void scrapeCommentsContent(ScrapingCallback callback) {
        webView.evaluateJavascript(
                "(function() {" +
                        "  var comments = [];" +
                        "  var commentThreads = document.querySelectorAll('ytm-comment-thread-renderer');" +
                        "  commentThreads.forEach(function(thread) {" +
                        "    var textEl = thread.querySelector('ytm-comment-renderer p.YtmCommentRendererText span.yt-core-attributed-string');" +
                        "    var likesEl = thread.querySelector('ytm-comment-renderer span.YtmCommentRendererCount span.yt-core-attributed-string');" +
                        "    " +
                        "    if (textEl) {" +
                        "      var comment = {" +
                        "        text: textEl.textContent.trim()," +
                        "        likes: likesEl ? likesEl.textContent.trim() : '0'" +
                        "      };" +
                        "      comments.push(comment);" +
                        "    }" +
                        "  });" +
                        "  return JSON.stringify({comments: comments, count: comments.length});" +
                        "})();",
                result -> {
                    if (result != null && !result.equals("null")) {
                        callback.onSuccess(result.replace("\\", ""));
                    } else {
                        callback.onError("No comments found");
                    }
                }
        );
    }

    /**
     * Callback interface for scraping operations
     */
    interface ScrapingCallback {
        void onSuccess(String result);
        void onError(String error);
    }


    /**
     * Определение Display ID на котором запущена Activity
     */
    private void detectDisplayId() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            android.view.Display display = getDisplay();
            if (display != null) {
                currentDisplayId = display.getDisplayId();
                isLaunchedInFullscreenDisplay = DisplayIdConstants.isFullscreenDisplay(currentDisplayId);

                Log.i(TAG, "╔═══════════════════════════════════════════════════════════╗");
                Log.i(TAG, "║          DISPLAY ID DETECTION                            ║");
                Log.i(TAG, "╠═══════════════════════════════════════════════════════════╣");
                Log.i(TAG, "║ Display ID: " + currentDisplayId + " (0x" +
                        Integer.toHexString(currentDisplayId) + ")");
                Log.i(TAG, "║ Display Name: " + DisplayIdConstants.getDisplayName(currentDisplayId));
                Log.i(TAG, "║ Is Fullscreen Display: " + isLaunchedInFullscreenDisplay);
                Log.i(TAG, "║ Is Rear Display: " + DisplayIdConstants.isRearDisplay(currentDisplayId));
                Log.i(TAG, "╚═══════════════════════════════════════════════════════════╝");

                // Если запущено в fullscreen режиме - принудительно включаем immersive mode
                if (isLaunchedInFullscreenDisplay) {
                    Log.i(TAG, "▶ Launched in FULLSCREEN display - enabling immersive mode");
                    enableImmersiveMode();

                    // Показываем Toast с информацией
                    Toast.makeText(this,
                            "Launched in " + DisplayIdConstants.getDisplayName(currentDisplayId),
                            Toast.LENGTH_LONG).show();
                }
            }
        } else {
            // Для старых версий Android
            Log.w(TAG, "Display ID detection requires Android 11+ (API 30+)");
        }
    }

    /**
     * Получить текущий Display ID
     */
    public int getCurrentDisplayId() {
        return currentDisplayId;
    }

    /**
     * Проверка, запущено ли приложение в fullscreen режиме
     */
    public boolean isLaunchedInFullscreenMode() {
        return isLaunchedInFullscreenDisplay;
    }

    /**
     * Проверка, нужно ли открыть fullscreen режим через Wms API
     *
     * @return true если нужно открыть fullscreen, false если нет
     */
    /**
     * Проверка нужно ли запускать приложение на Display 1003
     */
    private boolean shouldLaunchOnFullscreenDisplay() {
        // Проверяем включена ли настройка автозапуска в fullscreen
        boolean autoLaunchEnabled = preferencesManager.isFullscreenDisplayLaunchEnabled();

        // Проверяем что мы НЕ на fullscreen дисплее (Display ID != 1003)
        boolean notInFullscreen = currentDisplayId != DisplayIdConstants.DISPLAY_ID_FULL;

        Log.i(TAG, "shouldLaunchOnFullscreenDisplay() check:");
        Log.i(TAG, "  - Auto-launch enabled: " + autoLaunchEnabled);
        Log.i(TAG, "  - Current Display ID: " + currentDisplayId);
        Log.i(TAG, "  - Not in fullscreen: " + notInFullscreen);

        return autoLaunchEnabled && notInFullscreen;
    }

    /**
     * Запуск приложения на Display 1003
     */
    private void launchOnFullscreenDisplay() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("redteam.tube", "redteam.tube.MainActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(1003);

            this.startActivity(intent, options.toBundle());

            Log.i(TAG, "✅ Launch intent sent for Display 1003");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to launch on Display 1003", e);
            Toast.makeText(this, "Failed to launch on fullscreen display", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Применяет zoom настройки из preferences
     */
    private void applyZoomSettings() {
        // Page Zoom (CSS zoom на body)
        int pageZoom = preferencesManager.getPageZoom(); // 50-200%
        float pageZoomFloat = pageZoom / 100.0f; // Convert to 0.5-2.0

        // JavaScript с handler который постоянно проверяет fullscreen режим
        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("(function() {");
        scriptBuilder.append("  var originalZoom = ").append(pageZoomFloat).append(";");
        scriptBuilder.append("  var isFullscreenActive = false;");
        scriptBuilder.append("  function checkFullscreenAndApplyZoom() {");
        scriptBuilder.append("    var player = document.querySelector('#movie_player');");
        scriptBuilder.append("    if (player) {");
        scriptBuilder.append("      var isNowFullscreen = player.classList.contains('ytp-fullscreen');");
        scriptBuilder.append("      if (isNowFullscreen !== isFullscreenActive) {");
        scriptBuilder.append("        isFullscreenActive = isNowFullscreen;");
        scriptBuilder.append("        if (isFullscreenActive) {");
        scriptBuilder.append("          document.body.style.zoom = '1.0';");
        scriptBuilder.append("        } else {");
        scriptBuilder.append("          document.body.style.zoom = originalZoom;");
        scriptBuilder.append("        }");
        scriptBuilder.append("      }");
        scriptBuilder.append("    }");
        scriptBuilder.append("  }");
        scriptBuilder.append("  document.body.style.zoom = originalZoom;");
        scriptBuilder.append("  if (window.fullscreenZoomChecker) {");
        scriptBuilder.append("    clearInterval(window.fullscreenZoomChecker);");
        scriptBuilder.append("  }");
        scriptBuilder.append("  window.fullscreenZoomChecker = setInterval(checkFullscreenAndApplyZoom, 500);");
        scriptBuilder.append("  checkFullscreenAndApplyZoom();");
        scriptBuilder.append("})();");

        webView.evaluateJavascript(scriptBuilder.toString(), null);

//        int videoZoomPercent = (int)(100.0f / pageZoomFloat);
//        String scriptVideoZoom =
//            "(function() {" +
//            "  var video = document.querySelector('video');" +
//            "  if (video) {" +
//            "    video.style.zoom = '" + videoZoomPercent + "%';" +
//            "  }" +
//            "})();";
//        webView.evaluateJavascript(scriptVideoZoom, null);

        // Text Size (setTextZoom)
        int textSize = preferencesManager.getTextSize(); // 50-200%
        webView.getSettings().setTextZoom(textSize);

        // Apply separate zoom to bottom navigation bar icons only
        int bottomBarSize = preferencesManager.getBottomBarSize(); // 50-200%
        float bottomBarFloat = bottomBarSize / 100.0f; // Convert to 0.5-2.0
        if (bottomNavigationBar != null) {
            // Найти все ImageView иконки внутри bottom bar
            for (int i = 0; i < ((ViewGroup) bottomNavigationBar).getChildCount(); i++) {
                View child = ((ViewGroup) bottomNavigationBar).getChildAt(i);
                if (child instanceof ViewGroup) {
                    ViewGroup buttonLayout = (ViewGroup) child;
                    for (int j = 0; j < buttonLayout.getChildCount(); j++) {
                        View icon = buttonLayout.getChildAt(j);
                        if (icon instanceof android.widget.ImageView) {
                            icon.setScaleX(bottomBarFloat);
                            icon.setScaleY(bottomBarFloat);
                        }
                    }
                }
            }
        }

        Log.i(TAG, "Applied zoom - Page: " + pageZoom + "%, Text: " + textSize + "%, BottomBar: " + bottomBarSize + "%");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFullscreenMonitoring();

        // Remove control listener
        YouTubeControlReceiver.removeControlListener();

        // Unregister zoom receiver
        if (zoomReceiver != null) {
            unregisterReceiver(zoomReceiver);
            zoomReceiver = null;
        }

        // Unregister floating back receiver
        if (floatingBackReceiver != null) {
            unregisterReceiver(floatingBackReceiver);
            floatingBackReceiver = null;
        }

        // Remove floating back button
        hideFloatingBackButton();

        // WebView cleanup
        if (webView != null) {
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl("about:blank");
            webView.onPause();
            webView.removeAllViews();
            webView.destroyDrawingCache();
            webView.destroy();
            webView = null;
        }
    }
}