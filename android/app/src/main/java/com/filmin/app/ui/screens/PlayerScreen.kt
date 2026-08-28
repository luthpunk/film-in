@file:OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)

package com.filmin.app.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.filmin.app.data.model.MovieDetail
import com.filmin.app.ui.theme.AccentRed
import com.filmin.app.ui.theme.BgCard
import com.filmin.app.ui.theme.BgDark
import kotlin.OptIn

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    detail: MovieDetail?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var isExoPlayerPlaying by remember { mutableStateOf(false) }
    var isVideoLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("Melewati Cloudflare & Mengekstrak Stream...") }
    var isAllServersFailed by remember { mutableStateOf(false) }
    var currentServerIndex by remember { mutableIntStateOf(0) }

    val slug = detail?.slug ?: ""
    val imdbId = detail?.servers?.firstOrNull()?.url?.let {
        if (it.contains("imdb=")) it.substringAfter("imdb=") else slug
    } ?: slug

    val serverUrls = remember(detail) {
        listOf(
            "https://z2.idlixku.com/movie/$slug?play=1",
            "https://vidsrc.me/embed/movie?imdb=$imdbId",
            "https://vidsrc.to/embed/movie/$imdbId"
        )
    }

    BackHandler {
        onBackClick()
    }

    // Initialize ExoPlayer (100% Pure Native Player)
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    fun playNativeExoPlayer(m3u8Url: String, cookies: String, embedUrl: String) {
        try {
            val reqProperties = mutableMapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to embedUrl,
                "Origin" to "https://z2.idlixku.com",
                "Accept" to "*/*"
            )
            if (cookies.isNotBlank()) {
                reqProperties["Cookie"] = cookies
            }

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(reqProperties["User-Agent"] ?: "")
                .setDefaultRequestProperties(reqProperties)

            val mediaSource: MediaSource = if (m3u8Url.contains(".m3u8")) {
                HlsMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(m3u8Url)))
            } else {
                ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(m3u8Url)))
            }

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            isExoPlayerPlaying = true
            isVideoLoading = false
        } catch (e: Exception) {
            isExoPlayerPlaying = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isVideoLoading = false
                    isExoPlayerPlaying = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (currentServerIndex + 1 < serverUrls.size) {
                    currentServerIndex += 1
                    isExoPlayerPlaying = false
                    isVideoLoading = true
                } else {
                    isExoPlayerPlaying = false
                    isAllServersFailed = true
                    isVideoLoading = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = detail?.title ?: "Pemutar Video Native",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isExoPlayerPlaying) "Native ExoPlayer HLS 1080p HD" else statusText,
                            fontSize = 11.sp,
                            color = AccentRed
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (isAllServersFailed) {
                // Informative Dialog (NO WebView Fallback)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = BgCard,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Video Tidak Dapat Diputar",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Ekstraksi stream HLS (.m3u8) dari server IDLIX gagal atau sedang dalam pemeliharaan jaringan. Silakan coba judul film lainnya.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            Button(
                                onClick = onBackClick,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Kembali ke Detail Film", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else if (isExoPlayerPlaying) {
                // 100% Pure ExoPlayer View (NO WebView in UI)
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Active Stream Interceptor Engine (Attached to Window Hierarchy)
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(1, 1)

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            }

                            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val url = request?.url?.toString() ?: ""
                                    if (url.contains(".m3u8") || url.contains(".mp4") || url.contains("master.m3u8") || url.contains("index.m3u8")) {
                                        val cookies = CookieManager.getInstance().getCookie(serverUrls[currentServerIndex]) ?: ""
                                        post {
                                            playNativeExoPlayer(url, cookies, serverUrls[currentServerIndex])
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }
                            }

                            loadUrl(serverUrls[currentServerIndex])
                        }
                    },
                    modifier = Modifier.size(1.dp)
                )

                if (isVideoLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentRed)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Menghubungkan Stream Server ${currentServerIndex + 1}/${serverUrls.size}...",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
