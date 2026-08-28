package com.filmin.app.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ExtractedStreamResult(
    val streamUrl: String,
    val cookies: String,
    val embedUrl: String
)

class IdlixStreamExtractor(private val context: Context) {

    suspend fun extractStreamUrl(embedUrl: String): ExtractedStreamResult? = suspendCancellableCoroutine { continuation ->
        Handler(Looper.getMainLooper()).post {
            var isResumed = false
            val webView = WebView(context)

            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.mediaPlaybackRequiresUserGesture = false
            webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!isResumed) {
                    isResumed = true
                    try {
                        webView.stopLoading()
                        webView.destroy()
                    } catch (e: Exception) {}
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable, 8000)

            fun handleFoundStream(foundUrl: String) {
                if (!isResumed) {
                    isResumed = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    val cookies = CookieManager.getInstance().getCookie(embedUrl) ?: ""
                    Handler(Looper.getMainLooper()).post {
                        try {
                            webView.stopLoading()
                            webView.destroy()
                        } catch (e: Exception) {}
                    }
                    if (continuation.isActive) {
                        continuation.resume(
                            ExtractedStreamResult(
                                streamUrl = foundUrl,
                                cookies = cookies,
                                embedUrl = embedUrl
                            )
                        )
                    }
                }
            }

            // JavaScript Bridge for instant stream capture
            webView.addJavascriptInterface(object {
                @JavascriptInterface
                fun onStreamFound(url: String) {
                    handleFoundStream(url)
                }
            }, "AndroidBridge")

            val jsInjectScript = """
                (function() {
                    if (window.__stream_bridge_injected__) return;
                    window.__stream_bridge_injected__ = true;
                    var origFetch = window.fetch;
                    if (origFetch) {
                        window.fetch = function() {
                            var url = arguments[0];
                            if (typeof url === 'string' && (url.indexOf('.m3u8') !== -1 || url.indexOf('master') !== -1 || url.indexOf('.mp4') !== -1)) {
                                if (window.AndroidBridge) window.AndroidBridge.onStreamFound(url);
                            }
                            return origFetch.apply(this, arguments);
                        };
                    }
                    var origOpen = XMLHttpRequest.prototype.open;
                    if (origOpen) {
                        XMLHttpRequest.prototype.open = function(method, url) {
                            if (typeof url === 'string' && (url.indexOf('.m3u8') !== -1 || url.indexOf('master') !== -1 || url.indexOf('.mp4') !== -1)) {
                                if (window.AndroidBridge) window.AndroidBridge.onStreamFound(url);
                            }
                            return origOpen.apply(this, arguments);
                        };
                    }
                })();
            """.trimIndent()

            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: ""
                    if (url.contains(".m3u8") || url.contains(".mp4") || url.contains("master.m3u8") || url.contains("index.m3u8")) {
                        handleFoundStream(url)
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(jsInjectScript, null)
                }
            }

            webView.loadUrl(embedUrl)
        }
    }
}
