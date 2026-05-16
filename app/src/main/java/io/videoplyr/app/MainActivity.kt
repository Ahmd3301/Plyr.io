package io.videoplyr.app

import android.annotation.SuppressLint
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
        
        // إنشاء الواجهة برمجياً لضمان عدم حدوث خطأ في ملف XML
        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(Color.BLACK)
        
        webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(-1, -1)
        rootLayout.addView(webView)

        val customViewContainer = FrameLayout(this)
        customViewContainer.id = View.generateViewId()
        customViewContainer.visibility = View.GONE
        customViewContainer.setBackgroundColor(Color.BLACK)
        rootLayout.addView(customViewContainer)

        setContentView(rootLayout)

        setupFullscreen()
        setupWebView(customViewContainer)
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
    private fun setupWebView(container: FrameLayout) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
                container.visibility = View.VISIBLE
                container.addView(view)
                webView.visibility = View.GONE
            }

            override fun onHideCustomView() {
                container.visibility = View.GONE
                container.removeView(customView)
                webView.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()
            }
        }

        webView.addJavascriptInterface(object {
            @JavascriptInterface fun onVideoReady() {}
        }, "Android")
        
        webView.loadUrl("file:///android_asset/player.html")
    }

    private fun handleIntent() {
        intent.data?.let { uri ->
            if (uri.scheme == "videoplyrio") {
                if (uri.host == "open") {
                    val url = uri.getQueryParameter("url")
                    val title = uri.getQueryParameter("title") ?: "Video"
                    webView.evaluateJavascript("loadVideo('$url', '$title')", null)
                } else if (uri.host == "playlist") {
                    val base64 = uri.getQueryParameter("data") ?: ""
                    val json = String(Base64.decode(base64, Base64.DEFAULT))
                    webView.evaluateJavascript("loadPlaylist('$json')", null)
                }
            }
        }
    }
}
