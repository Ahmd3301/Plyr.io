package io.videoplyr.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupFullscreen()
        setupWebView()
        handleIntent()
    }

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
                findViewById<FrameLayout>(R.id.customViewContainer).apply {
                    visibility = View.VISIBLE
                    addView(view)
                }
                webView.visibility = View.GONE
            }

            override fun onHideCustomView() {
                findViewById<FrameLayout>(R.id.customViewContainer).apply {
                    visibility = View.GONE
                    removeView(customView)
                }
                webView.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()
            }
        }

        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.loadUrl("file:///android_asset/player.html")
    }

    private fun handleIntent() {
        val data: Uri? = intent.data
        data?.let { uri ->
            if (uri.scheme == "videoplyrio") {
                when (uri.host) {
                    "open" -> {
                        val url = uri.getQueryParameter("url")
                        val title = uri.getQueryParameter("title") ?: "Video"
                        webView.evaluateJavascript("loadVideo('$url', '$title')", null)
                    }
                    "playlist" -> {
                        val base64 = uri.getQueryParameter("data")
                        val json = String(Base64.decode(base64, Base64.DEFAULT))
                        webView.evaluateJavascript("loadPlaylist('$json')", null)
                    }
                }
            }
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun onVideoReady() { /* Player Ready */ }
    }

    override fun onBackPressed() {
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
