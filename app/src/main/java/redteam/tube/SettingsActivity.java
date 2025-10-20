package redteam.tube;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.os.LocaleListCompat;

import redteam.tube.data.HistoryDatabase;
import redteam.tube.utils.PreferencesManager;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchFullscreen;
    private SwitchCompat switchFullscreenDisplayLaunch; // Новый переключатель
    private SwitchCompat switchFloatingBack; // Floating back button
    private SwitchCompat switchHistory;
    private TextView tvCurrentLanguage;
    private android.widget.SeekBar seekBarPageZoom;
    private android.widget.SeekBar seekBarTextSize;
    private android.widget.SeekBar seekBarBottomBarSize;
    private TextView tvPageZoomValue;
    private TextView tvTextSizeValue;
    private TextView tvBottomBarSizeValue;
    private PreferencesManager preferencesManager;
    private HistoryDatabase historyDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize preferences and database
        preferencesManager = new PreferencesManager(this);
        historyDatabase = HistoryDatabase.getInstance(this);

        setContentView(R.layout.activity_settings);

        // Initialize views
        initViews();

        // Load current settings
        loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update language display when returning to activity
        updateLanguageDisplay();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        LinearLayout btnLanguage = findViewById(R.id.btnLanguage);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);
        switchFullscreen = findViewById(R.id.switchFullscreen);
        switchFullscreenDisplayLaunch = findViewById(R.id.switchFullscreenDisplayLaunch); // Новый переключатель
        switchFloatingBack = findViewById(R.id.switchFloatingBack); // Floating back button
        switchHistory = findViewById(R.id.switchHistory);
        LinearLayout btnViewHistory = findViewById(R.id.btnViewHistory);
        LinearLayout btnClearHistory = findViewById(R.id.btnClearHistory);

        // Zoom controls
        seekBarPageZoom = findViewById(R.id.seekBarPageZoom);
        seekBarTextSize = findViewById(R.id.seekBarTextSize);
        seekBarBottomBarSize = findViewById(R.id.seekBarBottomBarSize);
        tvPageZoomValue = findViewById(R.id.tvPageZoomValue);
        tvTextSizeValue = findViewById(R.id.tvTextSizeValue);
        tvBottomBarSizeValue = findViewById(R.id.tvBottomBarSizeValue);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Language selector
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        // View history
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });

        // Clear history
        btnClearHistory.setOnClickListener(v -> showClearHistoryDialog());

        // Apply Zoom button
        LinearLayout btnApplyZoom = findViewById(R.id.btnApplyZoom);
        btnApplyZoom.setOnClickListener(v -> applyZoomToWebView());
    }

    private void loadSettings() {
        // Load values first WITHOUT listeners
        switchFullscreen.setChecked(preferencesManager.isFullscreenEnabled());
        switchFullscreenDisplayLaunch.setChecked(preferencesManager.isFullscreenDisplayLaunchEnabled());
        switchFloatingBack.setChecked(preferencesManager.isFloatingBackEnabled());
        switchHistory.setChecked(preferencesManager.isHistoryEnabled());
        updateLanguageDisplay();

        // Set listeners AFTER loading values
        switchFullscreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setFullscreenEnabled(isChecked);
            Toast.makeText(this,
                    isChecked ? R.string.fullscreen_enabled : R.string.fullscreen_disabled,
                    Toast.LENGTH_SHORT).show();
        });

        // Новый переключатель для автозапуска в Fullscreen Display
        switchFullscreenDisplayLaunch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setFullscreenDisplayLaunchEnabled(isChecked);

            // Restart MainActivity on specific display
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("redteam.tube", "redteam.tube.MainActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Явно указываем Display ID для обоих случаев
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(isChecked ? 1003 : 1001);

            startActivity(intent, options.toBundle());
            finish();
        });

        // Floating back button переключатель
        switchFloatingBack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setFloatingBackEnabled(isChecked);

            // Отправляем broadcast для показа/скрытия floating кнопки
            Intent intent = new Intent("redteam.tube.TOGGLE_FLOATING_BACK");
            intent.putExtra("enabled", isChecked);
            sendBroadcast(intent);

            Toast.makeText(this,
                    isChecked ? "Floating Back Button Enabled" : "Floating Back Button Disabled",
                    Toast.LENGTH_SHORT).show();
        });

        switchHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setHistoryEnabled(isChecked);
            Toast.makeText(this,
                    isChecked ? R.string.history_enabled : R.string.history_disabled,
                    Toast.LENGTH_SHORT).show();
        });

        // Load zoom values and set up SeekBars
        loadZoomSettings();
    }

    private void loadZoomSettings() {
        // Load saved values (50-200%)
        int pageZoom = preferencesManager.getPageZoom();
        int textSize = preferencesManager.getTextSize();
        int bottomBarSize = preferencesManager.getBottomBarSize();

        // Convert percentage to SeekBar progress (0-150)
        // Formula: progress = percentage - 50
        int pageZoomProgress = pageZoom - 50;
        int textSizeProgress = textSize - 50;
        int bottomBarSizeProgress = bottomBarSize - 50;

        // Set SeekBar values WITHOUT triggering listeners
        seekBarPageZoom.setProgress(pageZoomProgress);
        seekBarTextSize.setProgress(textSizeProgress);
        seekBarBottomBarSize.setProgress(bottomBarSizeProgress);

        // Update text views
        tvPageZoomValue.setText(pageZoom + "%");
        tvTextSizeValue.setText(textSize + "%");
        tvBottomBarSizeValue.setText(bottomBarSize + "%");

        // Set listeners AFTER loading values
        seekBarPageZoom.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                // Convert progress (0-150) to percentage (50-200)
                int percentage = progress + 50;
                tvPageZoomValue.setText(percentage + "%");
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                int percentage = seekBar.getProgress() + 50;
                preferencesManager.setPageZoom(percentage);
                Toast.makeText(SettingsActivity.this, "Page Zoom: " + percentage + "%", Toast.LENGTH_SHORT).show();
            }
        });

        seekBarTextSize.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                int percentage = progress + 50;
                tvTextSizeValue.setText(percentage + "%");
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                int percentage = seekBar.getProgress() + 50;
                preferencesManager.setTextSize(percentage);
                Toast.makeText(SettingsActivity.this, "Text Size: " + percentage + "%", Toast.LENGTH_SHORT).show();
            }
        });

        seekBarBottomBarSize.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                int percentage = progress + 50;
                tvBottomBarSizeValue.setText(percentage + "%");
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                int percentage = seekBar.getProgress() + 50;
                preferencesManager.setBottomBarSize(percentage);
                Toast.makeText(SettingsActivity.this, "Bottom Bar Size: " + percentage + "%", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLanguageDisplay() {
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        String language = currentLocales.isEmpty() ? "system" : currentLocales.get(0).getLanguage();

        String displayText;
        switch (language) {
            case "en":
                displayText = getString(R.string.language_english);
                break;
            case "ru":
                displayText = getString(R.string.language_russian);
                break;
            default:
                displayText = getString(R.string.language_description);
                break;
        }
        tvCurrentLanguage.setText(displayText);
    }

    private void showLanguageDialog() {
        String[] languages = {
                getString(R.string.language_english),
                getString(R.string.language_russian)
        };
        String[] languageCodes = {"en", "ru"};

        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        String currentLang = currentLocales.isEmpty() ? "system" : currentLocales.get(0).getLanguage();
        int checkedItem = currentLang.equals("ru") ? 1 : 0;

        new AlertDialog.Builder(this)
                .setTitle(R.string.language)
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String selectedLanguage = languageCodes[which];
                    LocaleListCompat locales = LocaleListCompat.forLanguageTags(selectedLanguage);
                    AppCompatDelegate.setApplicationLocales(locales);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.clear, (dialog, which) -> {
                    historyDatabase.clearHistory();
                    Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Применить zoom настройки к WebView в MainActivity
     */
    private void applyZoomToWebView() {
        // Restart MainActivity для применения zoom настроек
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("redteam.tube", "redteam.tube.MainActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}
