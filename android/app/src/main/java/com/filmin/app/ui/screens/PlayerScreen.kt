@file:OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)

package com.filmin.app.ui.screens

import android.net.Uri
import android.view.ViewGroup
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
import com.filmin.app.data.remote.IdlixStreamExtractor
import com.filmin.app.ui.theme.AccentRed
import com.filmin.app.ui.theme.BgCard
import com.filmin.app.ui.theme.BgDark
import kotlinx.coroutines.launch
import kotlin.OptIn

@Composable
fun PlayerScreen(
    detail: MovieDetail?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val extractor = remember { IdlixStreamExtractor(context) }

    var currentServerIndex by remember { mutableIntStateOf(0) }
    var isAllServersFailed by remember { mutableStateOf(false) }
    var isVideoLoading by remember { mutableStateOf(true) }
    var isExoReady by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Mengekstrak stream video...") }

    val servers = detail?.servers ?: emptyList()

    BackHandler {
        onBackClick()
    }

    // Initialize ExoPlayer (100% Pure Native Player, NO WebView in Layout)
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

    fun playCapturedStream(streamUrl: String, embedUrl: String) {
        isVideoLoading = true
        statusText = "Menghubungkan ExoPlayer..."

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to embedUrl,
                    "Accept" to "*/*"
                )
            )

        val mediaSource: MediaSource = if (streamUrl.contains(".m3u8")) {
            HlsMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)))
        } else {
            ProgressiveMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)))
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    fun startExtractionForServer(index: Int) {
        if (servers.isEmpty() || index >= servers.size) {
            isAllServersFailed = true
            isVideoLoading = false
            return
        }

        val server = servers[index]
        isVideoLoading = true
        statusText = "Mengekstrak stream ${server.name} (${index + 1}/${servers.size})..."

        coroutineScope.launch {
            val capturedUrl = extractor.extractStreamUrl(server.url)
            if (!capturedUrl.isNullOrBlank()) {
                playCapturedStream(capturedUrl, server.url)
            } else {
                if (index + 1 < servers.size) {
                    currentServerIndex = index + 1
                    startExtractionForServer(index + 1)
                } else {
                    isAllServersFailed = true
                    isVideoLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        startExtractionForServer(0)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isVideoLoading = false
                    isExoReady = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isExoReady = false
                if (currentServerIndex + 1 < servers.size) {
                    currentServerIndex += 1
                    startExtractionForServer(currentServerIndex)
                } else {
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
                            text = if (isExoReady) "Native ExoPlayer HLS 1080p" else statusText,
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
                                text = "Ekstraksi stream HLS (.m3u8) dari server IDLIX gagal atau sedang dalam pembatasan Cloudflare. Silakan coba judul film lainnya.",
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
            } else {
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
                                text = statusText,
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
