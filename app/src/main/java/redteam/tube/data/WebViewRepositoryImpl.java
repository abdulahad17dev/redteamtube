package redteam.tube.data;

import redteam.tube.domain.WebViewRepository;

public class WebViewRepositoryImpl implements WebViewRepository {

    private static final String YOUTUBE_URL = "https://www.youtube.com";

    @Override
    public String getYouTubeUrl() {
        return YOUTUBE_URL;
    }

    @Override
    public void saveHistory(String url) {
        // TODO: Implement history saving if needed
    }

    @Override
    public boolean canGoBack() {
        return true;
    }

    @Override
    public boolean canGoForward() {
        return true;
    }
}
