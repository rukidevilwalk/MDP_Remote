package com.jack.mdpremote;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class RPICam extends Fragment {

    public WebView webView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        webView.clearCache(true);
        webView.loadUrl("http://192.168.24.2:8080");

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        if(Build.VERSION.SDK_INT > 18) {
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        }

        webView.setWebViewClient(new WebViewClient());

        return webView;
    }
}
