package com.rukatv.iptv.resolver

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class StreamInterceptor(private val context: Context) {

    companion object {
        private const val TAG = "StreamInterceptor"
        private const val TIMEOUT_MS = 20L * 1000
    }

    private var webView: WebView? = null
    private var handler = Handler(Looper.getMainLooper())
    private var callback: ((String?) -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun intercept(url: String, cb: (String?) -> Unit) {
        callback = cb

        handler.post {
            try {
                webView?.destroy()

                webView = WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(1, 1)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowContentAccess = true
                        allowFileAccess = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onMpdUrl(url: String) {
                            Log.d(TAG, "JS bridge: $url")
                            notifyResult(url)
                        }
                    }, "AndroidBridge")

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                            val msg = cm?.message() ?: return false
                            Log.d(TAG, "Console: $msg")

                            if (msg.contains("MPD URL:")) {
                                val mpdUrl = msg.substringAfter("MPD URL:").trim()
                                if (mpdUrl.startsWith("http")) {
                                    Log.d(TAG, "MPD capturado: $mpdUrl")
                                    notifyResult(mpdUrl)
                                }
                            }
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var origLog = console.log;
                                    console.log = function() {
                                        origLog.apply(console, arguments);
                                        var args = Array.from(arguments).join(' ');
                                        if (args.indexOf('MPD URL:') >= 0) {
                                            try { AndroidBridge.onMpdUrl(args.split('MPD URL:')[1].trim()); } catch(e) {}
                                        }
                                    };
                                })();
                                """.trimIndent(),
                                null
                            )
                        }

                        override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                            super.onPageFinished(view, loadedUrl)
                            Log.d(TAG, "Page loaded: $loadedUrl")

                            view?.evaluateJavascript(
                                """
                                (function() {
                                    try {
                                        if (typeof mpdBase !== 'undefined') return mpdBase;
                                    } catch(e) {}
                                    try {
                                        var scripts = document.querySelectorAll('script');
                                        for (var i = 0; i < scripts.length; i++) {
                                            var t = scripts[i].textContent;
                                            var m = t.match(/var mpdBase = `([^`]+)`/);
                                            if (m) return m[1];
                                        }
                                    } catch(e) {}
                                    return '';
                                })();
                                """.trimIndent()
                            ) { result ->
                                val cleaned = result?.trim('"', '\'') ?: ""
                                if (cleaned.startsWith("http") && cleaned.contains(".mpd")) {
                                    Log.d(TAG, "MPD via JS eval: $cleaned")
                                    notifyResult(cleaned)
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return false
                        }
                    }

                    loadUrl(url)
                }

                handler.postDelayed({
                    Log.w(TAG, "Timeout, no MPD captured")
                    notifyResult(null)
                }, TIMEOUT_MS)

            } catch (e: Exception) {
                Log.e(TAG, "intercept error", e)
                notifyResult(null)
            }
        }
    }

    @Synchronized
    private fun notifyResult(url: String?) {
        handler.removeCallbacksAndMessages(null)
        callback?.let { cb ->
            callback = null
            cb(url)
        }
        try {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        } catch (_: Exception) {}
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        callback = null
        try {
            webView?.destroy()
            webView = null
        } catch (_: Exception) {}
    }
}
