package redteam.tube;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import redteam.tube.data.HistoryDatabase;
import redteam.tube.utils.PreferencesManager;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchFullscreen;
    private SwitchCompat switchHistory;
    private PreferencesManager preferencesManager;
    private HistoryDatabase historyDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize preferences and database
        preferencesManager = new PreferencesManager(this);
        historyDatabase = HistoryDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Load current settings
        loadSettings();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        switchFullscreen = findViewById(R.id.switchFullscreen);
        switchHistory = findViewById(R.id.switchHistory);
        LinearLayout btnViewHistory = findViewById(R.id.btnViewHistory);
        LinearLayout btnClearHistory = findViewById(R.id.btnClearHistory);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Fullscreen toggle
        switchFullscreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setFullscreenEnabled(isChecked);
            Toast.makeText(this,
                isChecked ? "Fullscreen enabled" : "Fullscreen disabled",
                Toast.LENGTH_SHORT).show();
        });

        // History toggle
        switchHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setHistoryEnabled(isChecked);
            Toast.makeText(this,
                isChecked ? "History saving enabled" : "History saving disabled",
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
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all history?")
            .setPositiveButton("Clear", (dialog, which) -> {
                historyDatabase.clearHistory();
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
