package com.filmin.app.data.remote

import com.filmin.app.data.model.MovieDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedStreamResult(
    val streamUrl: String,
    val cookies: String,
    val embedUrl: String
)

class IdlixStreamExtractor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private fun fetchHtml(url: String, referer: String = "https://z2.idlixku.com/"): Pair<String, String> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Referer", referer)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val cookies = response.headers.values("Set-Cookie").joinToString("; ")
                val body = response.body?.string() ?: ""
                Pair(body, cookies)
            }
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    suspend fun extractStreamUrl(detail: MovieDetail?, serverIndex: Int): ExtractedStreamResult? = withContext(Dispatchers.IO) {
        if (detail == null) return@withContext null
        val slug = detail.slug
        val imdbId = detail.servers.getOrNull(serverIndex)?.url?.let {
            if (it.contains("imdb=")) it.substringAfter("imdb=") else slug
        } ?: slug

        when (serverIndex) {
            0 -> {
                // Server 1: IDLIX Direct Embed
                val playUrl = "https://z2.idlixku.com/movie/$slug?play=1"
                val (html, cookies) = fetchHtml(playUrl)
                val m3u8Match = Regex("https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*").find(html)?.value
                    ?: Regex("https?://[^\"'\\s]+\\.mp4[^\"'\\s]*").find(html)?.value

                if (!m3u8Match.isNullOrBlank()) {
                    return@withContext ExtractedStreamResult(
                        streamUrl = m3u8Match,
                        cookies = cookies,
                        embedUrl = playUrl
                    )
                }
            }
            1 -> {
                // Server 2: VidSrc Provider (IMDb ID)
                val vidsrcUrl = "https://vidsrc.to/embed/movie/$imdbId"
                val (html, cookies) = fetchHtml(vidsrcUrl, "https://vidsrc.to/")
                val iframeMatch = Regex("href=[\"'](https?://vsembed\\.ru/embed/[^\"']+)[\"']").find(html)?.groupValues?.get(1)
                    ?: Regex("src=[\"'](https?://[^\"']+/embed/[^\"']+)[\"']").find(html)?.groupValues?.get(1)

                val targetUrl = iframeMatch ?: vidsrcUrl
                val (embedHtml, embedCookies) = fetchHtml(targetUrl, vidsrcUrl)
                val m3u8Match = Regex("https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*").find(embedHtml)?.value
                    ?: Regex("https?://[^\"'\\s]+\\.mp4[^\"'\\s]*").find(embedHtml)?.value

                if (!m3u8Match.isNullOrBlank()) {
                    return@withContext ExtractedStreamResult(
                        streamUrl = m3u8Match,
                        cookies = embedCookies.ifBlank { cookies },
                        embedUrl = targetUrl
                    )
                }
            }
            2 -> {
                // Server 3: AutoEmbed Provider
                val autoembedUrl = "https://autoembed.co/movie/imdb/$imdbId"
                val (html, cookies) = fetchHtml(autoembedUrl, "https://autoembed.co/")
                val m3u8Match = Regex("https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*").find(html)?.value
                    ?: Regex("https?://[^\"'\\s]+\\.mp4[^\"'\\s]*").find(html)?.value

                if (!m3u8Match.isNullOrBlank()) {
                    return@withContext ExtractedStreamResult(
                        streamUrl = m3u8Match,
                        cookies = cookies,
                        embedUrl = autoembedUrl
                    )
                }
            }
        }
        null
    }
}
