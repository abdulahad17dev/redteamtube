package redteam.tube;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import redteam.tube.data.HistoryDatabase;
import redteam.tube.utils.LocaleHelper;
import redteam.tube.utils.PreferencesManager;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchFullscreen;
    private SwitchCompat switchHistory;
    private TextView tvCurrentLanguage;
    private PreferencesManager preferencesManager;
    private HistoryDatabase historyDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize preferences and database
        preferencesManager = new PreferencesManager(this);
        historyDatabase = HistoryDatabase.getInstance(this);

        // Apply language setting
        LocaleHelper.setLocale(this, preferencesManager.getLanguage());

        setContentView(R.layout.activity_settings);

        // Initialize views
        initViews();

        // Load current settings
        loadSettings();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        LinearLayout btnLanguage = findViewById(R.id.btnLanguage);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);
        switchFullscreen = findViewById(R.id.switchFullscreen);
        switchHistory = findViewById(R.id.switchHistory);
        LinearLayout btnViewHistory = findViewById(R.id.btnViewHistory);
        LinearLayout btnClearHistory = findViewById(R.id.btnClearHistory);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Language selector
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        // Fullscreen toggle
        switchFullscreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setFullscreenEnabled(isChecked);
            Toast.makeText(this,
                isChecked ? R.string.fullscreen_enabled : R.string.fullscreen_disabled,
                Toast.LENGTH_SHORT).show();
        });

        // History toggle
        switchHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setHistoryEnabled(isChecked);
            Toast.makeText(this,
                isChecked ? R.string.history_enabled : R.string.history_disabled,
                Toast.LENGTH_SHORT).show();
        });

        // View history
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });

        // Clear history
        btnClearHistory.setOnClickListener(v -> showClearHistoryDialog());
    }

    private void loadSettings() {
        switchFullscreen.setChecked(preferencesManager.isFullscreenEnabled());
        switchHistory.setChecked(preferencesManager.isHistoryEnabled());
        updateLanguageDisplay();
    }

    private void updateLanguageDisplay() {
        String language = preferencesManager.getLanguage();
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

        String currentLang = preferencesManager.getLanguage();
        int checkedItem = currentLang.equals("ru") ? 1 : 0;

        new AlertDialog.Builder(this)
            .setTitle(R.string.language)
            .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                preferencesManager.setLanguage(languageCodes[which]);
                updateLanguageDisplay();
                Toast.makeText(this, R.string.restart_required, Toast.LENGTH_LONG).show();
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
}
