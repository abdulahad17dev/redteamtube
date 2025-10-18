package redteam.tube;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import redteam.tube.data.HistoryDatabase;
import redteam.tube.data.SearchHistory;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private HistoryAdapter adapter;
    private HistoryDatabase historyDatabase;
    private List<SearchHistory> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize database
        historyDatabase = HistoryDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Load history
        loadHistory();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBackHistory);
        recyclerView = findViewById(R.id.recyclerViewHistory);
        tvEmpty = findViewById(R.id.tvEmptyHistory);

        btnBack.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadHistory() {
        historyList = historyDatabase.getAllHistory();

        if (historyList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);

            adapter = new HistoryAdapter(historyList, this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onHistoryClick(SearchHistory history) {
        // Return to MainActivity with URL
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("url", history.getUrl());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDeleteClick(SearchHistory history) {
        historyDatabase.deleteHistoryItem(history.getId());
        loadHistory(); // Reload list
    }
}
