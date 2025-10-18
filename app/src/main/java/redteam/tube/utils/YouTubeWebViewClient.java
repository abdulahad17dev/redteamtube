package redteam.tube.utils;

import android.graphics.Bitmap;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class YouTubeWebViewClient extends WebViewClient {

    private final OnPageLoadListener listener;

    public interface OnPageLoadListener {
        void onPageStarted(String url);
        void onPageFinished(String url);
        void onPageError();
    }

    public YouTubeWebViewClient(OnPageLoadListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (listener != null) {
            listener.onPageStarted(url);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (listener != null) {
            listener.onPageFinished(url);
        }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        if (listener != null) {
            listener.onPageError();
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        // Allow all YouTube URLs to load in the WebView
        String url = request.getUrl().toString();
        if (url.contains("youtube.com") || url.contains("youtu.be") ||
            url.contains("googlevideo.com") || url.contains("ytimg.com")) {
            return false; // Let WebView handle it
        }
        // For external links, you could handle them differently
        return false;
    }
}
