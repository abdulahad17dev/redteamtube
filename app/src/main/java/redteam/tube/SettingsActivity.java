package redteam.tube;

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
        switchHistory = findViewById(R.id.switchHistory);
        LinearLayout btnViewHistory = findViewById(R.id.btnViewHistory);
        LinearLayout btnClearHistory = findViewById(R.id.btnClearHistory);

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
    }

    private void loadSettings() {
        // Load values first WITHOUT listeners
        switchFullscreen.setChecked(preferencesManager.isFullscreenEnabled());
        switchHistory.setChecked(preferencesManager.isHistoryEnabled());
        updateLanguageDisplay();

        // Set listeners AFTER loading values
        switchFullscreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setFullscreenEnabled(isChecked);
            Toast.makeText(this,
                isChecked ? R.string.fullscreen_enabled : R.string.fullscreen_disabled,
                Toast.LENGTH_SHORT).show();
        });

        switchHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setHistoryEnabled(isChecked);
            Toast.makeText(this,
                isChecked ? R.string.history_enabled : R.string.history_disabled,
                Toast.LENGTH_SHORT).show();
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
}
