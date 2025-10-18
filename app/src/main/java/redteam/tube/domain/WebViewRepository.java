package redteam.tube.domain;

public interface WebViewRepository {
    String getYouTubeUrl();
    void saveHistory(String url);
    boolean canGoBack();
    boolean canGoForward();
}
