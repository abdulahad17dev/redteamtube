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

    // Display ID поддержка
    private int currentDisplayId = 0;
    private boolean isLaunchedInFullscreenDisplay = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize preferences and database
        preferencesManager = new PreferencesManager(this);
        historyDatabase = HistoryDatabase.getInstance(this);

        // Определяем Display ID на котором запущено приложение
        detectDisplayId();

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

        // Load URL from intent or default
        String url = getIntent().getStringExtra("url");
        if (url == null || url.isEmpty()) {
            url = repository.getYouTubeUrl();
        }
        webView.loadUrl(url);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFullscreenMonitoring();

        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.clearCache(true);
            webView.onPause();
            webView.removeAllViews();
            webView.destroyDrawingCache();
            webView.destroy();
            webView = null;
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
}