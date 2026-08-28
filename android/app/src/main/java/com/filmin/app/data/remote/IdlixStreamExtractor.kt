package com.filmin.app.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume

class IdlixStreamExtractor(private val context: Context) {

    private val m3u8Regex = Pattern.compile("https?://[^\s\"']+\\.(m3u8|mp4)[^\s\"']*")

    suspend fun extractStreamUrl(embedUrl: String): String? = suspendCancellableCoroutine { continuation ->
        Handler(Looper.getMainLooper()).post {
            var isResumed = false
            val webView = WebView(context)

            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

            // Timeout safety after 12 seconds
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
            timeoutHandler.postDelayed(timeoutRunnable, 12000)

            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: ""
                    
                    if (url.contains(".m3u8") || url.contains(".mp4") || url.contains("master.m3u8") || url.contains("index.m3u8")) {
                        if (!isResumed) {
                            isResumed = true
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    webView.stopLoading()
                                    webView.destroy()
                                } catch (e: Exception) {}
                            }
                            if (continuation.isActive) {
                                continuation.resume(url)
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if (url != null && (url.contains(".m3u8") || url.contains(".mp4"))) {
                        if (!isResumed) {
                            isResumed = true
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    webView.stopLoading()
                                    webView.destroy()
                                } catch (e: Exception) {}
                            }
                            if (continuation.isActive) {
                                continuation.resume(url)
                            }
                        }
                    }
                }
            }

            webView.loadUrl(embedUrl)
        }
    }
}
