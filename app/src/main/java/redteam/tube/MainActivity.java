package redteam.tube;

import android.annotation.SuppressLint;
import android.graphics.Color;
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
import redteam.tube.utils.YouTubeWebViewClient;

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
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1001;

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


    // Display ID поддержка
    private int currentDisplayId = 0;
    private boolean isLaunchedInFullscreenDisplay = false;

    // Broadcast receiver для zoom настроек
    private android.content.BroadcastReceiver zoomReceiver;

    // Floating back button overlay
    private android.view.WindowManager windowManager;
    private View floatingBackButton;
    private android.view.WindowManager.LayoutParams floatingParams;
    private android.content.BroadcastReceiver floatingBackReceiver;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // Initialize WindowManager and floating back button
        windowManager = (android.view.WindowManager) getSystemService(WINDOW_SERVICE);
        registerFloatingBackReceiver();

        // Show floating back button if enabled
        if (preferencesManager.isFloatingBackEnabled()) {
            showFloatingBackButton();
        }

        // Load URL from intent or default
        String url = getIntent().getStringExtra("url");
        if (url == null || url.isEmpty()) {
            url = repository.getYouTubeUrl();
        }
        webView.loadUrl(url);
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
        registerReceiver(zoomReceiver, filter);
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
        registerReceiver(floatingBackReceiver, filter);
    }

    /**
     * Показать floating back button overlay
     */
    private void showFloatingBackButton() {
        if (floatingBackButton != null) {
            // Already shown
            return;
        }

        try {
            // Inflate floating button layout
            floatingBackButton = getLayoutInflater().inflate(R.layout.floating_back_button, null);

            // Setup WindowManager.LayoutParams
            // Для Geely/Ecarx используем TYPE_PHONE который не требует специального разрешения
            int windowType;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Android 8.0+ - пробуем TYPE_APPLICATION_OVERLAY если есть разрешение
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                        && Settings.canDrawOverlays(this)) {
                    windowType = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    // Fallback на TYPE_PHONE (deprecated но работает без разрешений)
                    windowType = android.view.WindowManager.LayoutParams.TYPE_PHONE;
                }
            } else {
                // Android 7.x и ниже - TYPE_PHONE работает без проблем
                windowType = android.view.WindowManager.LayoutParams.TYPE_PHONE;
            }

            floatingParams = new android.view.WindowManager.LayoutParams(
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                    windowType,
                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    android.graphics.PixelFormat.TRANSLUCENT
            );

            // Set initial position from preferences
            floatingParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            floatingParams.x = preferencesManager.getFloatingBackX();
            floatingParams.y = preferencesManager.getFloatingBackY();

            // Set click listener - только webView.goBack(), НЕ закрывать приложение
            android.widget.ImageButton btnFloating = floatingBackButton.findViewById(R.id.floatingBackButton);
            btnFloating.setOnClickListener(v -> {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                    Log.i(TAG, "Floating back button: navigating back in WebView");
                } else {
                    Log.i(TAG, "Floating back button: WebView cannot go back");
                    Toast.makeText(this, "Cannot go back", Toast.LENGTH_SHORT).show();
                }
            });

            // Add drag functionality AFTER click listener
            setupFloatingButtonDrag(btnFloating);

            // Add to window
            windowManager.addView(floatingBackButton, floatingParams);

            Log.i(TAG, "Floating back button shown at X=" + floatingParams.x + ", Y=" + floatingParams.y + " with type=" + windowType);

        } catch (android.view.WindowManager.BadTokenException e) {
            Log.e(TAG, "BadTokenException - trying to request overlay permission", e);

            // Если не получилось, пробуем запросить разрешение (только если система поддерживает)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                requestOverlayPermission();
            } else {
                Toast.makeText(this, "Failed to show floating button", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show floating back button", e);
            Toast.makeText(this, "Failed to show floating button: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Запросить разрешение на отображение overlay
     */
    private void requestOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);

                Toast.makeText(this,
                        "Please grant overlay permission to use floating back button",
                        Toast.LENGTH_LONG).show();
            } catch (android.content.ActivityNotFoundException e) {
                Log.w(TAG, "MANAGE_OVERLAY_PERMISSION activity not found (custom ROM?)", e);
                Toast.makeText(this,
                        "Overlay permission screen not available on this device",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Log.i(TAG, "Overlay permission granted");
                    Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show();

                    // Показываем floating кнопку если она должна быть включена
                    if (preferencesManager.isFloatingBackEnabled()) {
                        showFloatingBackButton();
                    }
                } else {
                    Log.w(TAG, "Overlay permission denied");
                    Toast.makeText(this, "Overlay permission denied. Floating button disabled.", Toast.LENGTH_LONG).show();

                    // Выключаем настройку если разрешение не дали
                    preferencesManager.setFloatingBackEnabled(false);
                }
            }
        }
    }

    /**
     * Скрыть floating back button overlay
     */
    private void hideFloatingBackButton() {
        if (floatingBackButton != null) {
            try {
                windowManager.removeView(floatingBackButton);
                floatingBackButton = null;
                Log.i(TAG, "Floating back button hidden");
            } catch (Exception e) {
                Log.e(TAG, "Failed to hide floating back button", e);
            }
        }
    }

    /**
     * Setup drag and drop для floating кнопки
     */
    private void setupFloatingButtonDrag(android.widget.ImageButton button) {
        button.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        initialX = floatingParams.x;
                        initialY = floatingParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true; // Consume DOWN event

                    case android.view.MotionEvent.ACTION_MOVE:
                        floatingParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        floatingParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingBackButton, floatingParams);

                        // Check if actually dragging (moved more than threshold)
                        int deltaX = (int) Math.abs(event.getRawX() - initialTouchX);
                        int deltaY = (int) Math.abs(event.getRawY() - initialTouchY);
                        if (deltaX > 10 || deltaY > 10) {
                            isDragging = true;
                        }
                        return true; // Consume move event

                    case android.view.MotionEvent.ACTION_UP:
                        // Save position to preferences
                        preferencesManager.setFloatingBackX(floatingParams.x);
                        preferencesManager.setFloatingBackY(floatingParams.y);
                        Log.i(TAG, "Floating button position saved: X=" + floatingParams.x + ", Y=" + floatingParams.y);

                        // If it was a click (not dragging), perform click
                        if (!isDragging) {
                            v.performClick();
                            return true;
                        }

                        isDragging = false;
                        return true; // Consume UP event
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
        if (preferencesManager.isFullscreenEnabled()) {
            enableImmersiveMode();
        } else {
            disableImmersiveMode();
        }
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

        // НЕ устанавливаем initialScale - пусть WebView управляет масштабом сам
        // это позволит zoomIn()/zoomOut() работать правильно

        // Set user agent to mobile for better YouTube mobile experience
        webSettings.setUserAgentString(webSettings.getUserAgentString().replace("; wv", ""));

        // Enable mixed content (if needed)
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Enable remote debugging for WebView
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

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