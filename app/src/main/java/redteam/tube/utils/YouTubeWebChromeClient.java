package redteam.tube.utils;

import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class YouTubeWebChromeClient extends WebChromeClient {

    private final ProgressBar progressBar;
    private View customView;
    private CustomViewCallback customViewCallback;
    private final FullscreenListener fullscreenListener;

    public interface FullscreenListener {
        void onEnterFullscreen(View view);
        void onExitFullscreen();
    }

    public YouTubeWebChromeClient(ProgressBar progressBar, FullscreenListener fullscreenListener) {
        this.progressBar = progressBar;
        this.fullscreenListener = fullscreenListener;
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

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        super.onShowCustomView(view, callback);

        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }

        customView = view;
        customViewCallback = callback;

        if (fullscreenListener != null) {
            fullscreenListener.onEnterFullscreen(view);
        }
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();

        if (customView == null) {
            return;
        }

        if (fullscreenListener != null) {
            fullscreenListener.onExitFullscreen();
        }

        customView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
    }
}
