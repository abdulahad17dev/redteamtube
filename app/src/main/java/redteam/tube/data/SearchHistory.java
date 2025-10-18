package redteam.tube.data;

public class SearchHistory {
    private final long id;
    private final String url;
    private final String title;
    private final long timestamp;

    public SearchHistory(long id, String url, String title, long timestamp) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
