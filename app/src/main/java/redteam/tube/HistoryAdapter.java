package redteam.tube;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import redteam.tube.data.SearchHistory;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<SearchHistory> historyList;
    private final OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryClick(SearchHistory history);
        void onDeleteClick(SearchHistory history);
    }

    public HistoryAdapter(List<SearchHistory> historyList, OnHistoryClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SearchHistory history = historyList.get(position);
        holder.bind(history, listener);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvUrl;
        private final TextView tvTime;
        private final ImageButton btnDelete;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHistoryTitle);
            tvUrl = itemView.findViewById(R.id.tvHistoryUrl);
            tvTime = itemView.findViewById(R.id.tvHistoryTime);
            btnDelete = itemView.findViewById(R.id.btnDeleteHistory);
        }

        public void bind(SearchHistory history, OnHistoryClickListener listener) {
            tvTitle.setText(history.getTitle() != null && !history.getTitle().isEmpty()
                ? history.getTitle() : "YouTube");
            tvUrl.setText(history.getUrl());

            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            tvTime.setText(sdf.format(new Date(history.getTimestamp())));

            itemView.setOnClickListener(v -> listener.onHistoryClick(history));
            btnDelete.setOnClickListener(v -> listener.onDeleteClick(history));
        }
    }
}
