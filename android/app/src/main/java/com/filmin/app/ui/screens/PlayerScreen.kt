@file:OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)

package com.filmin.app.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.view.View
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
    var capturedM3u8Url by remember { mutableStateOf<String?>(null) }
    var isPlayerError by remember { mutableStateOf(false) }

    val playUrl = "https://z2.idlixku.com/movie/${detail?.slug ?: ""}?play=1"

    BackHandler {
        onBackClick()
    }

    // Initialize ExoPlayer
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

    fun playNativeExoPlayer(m3u8Url: String) {
        try {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .setDefaultRequestProperties(
                    mapOf(
                        "Referer" to "https://z2.idlixku.com/",
                        "Accept" to "*/*"
                    )
                )

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
                isExoPlayerPlaying = false
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
                            text = detail?.title ?: "Pemutar Stream FilmIn",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isExoPlayerPlaying) "Pemutar Native ExoPlayer 1080p HD" else "Menyiapkan Stream Video...",
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
            if (isExoPlayerPlaying) {
                // Pure Native ExoPlayer View
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
                // Live Stream Extractor & High-Speed Stream Engine Container
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
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
                                    if ((url.contains(".m3u8") || url.contains(".mp4")) && capturedM3u8Url == null) {
                                        capturedM3u8Url = url
                                        post {
                                            playNativeExoPlayer(url)
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isVideoLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    super.onReceivedError(view, errorCode, description, failingUrl)
                                    isPlayerError = true
                                }
                            }

                            loadUrl(playUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isVideoLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentRed)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Menghubungkan Stream Video IDLIX...",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (isPlayerError && !isExoPlayerPlaying && capturedM3u8Url == null) {
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
                                text = "Server video IDLIX saat ini sedang mengalami pembatasan akses atau pemeliharaan jaringan. Silakan coba kembali beberapa saat lagi atau pilih judul film lainnya.",
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
            }
        }
    }
}
