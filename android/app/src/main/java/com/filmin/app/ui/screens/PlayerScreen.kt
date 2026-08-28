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
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.filmin.app.data.model.MovieDetail
import com.filmin.app.ui.theme.AccentRed
import com.filmin.app.ui.theme.BgCard
import com.filmin.app.ui.theme.BgDark
import kotlin.OptIn

@Composable
fun PlayerScreen(
    detail: MovieDetail?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var currentServerIndex by remember { mutableIntStateOf(0) }
    var isAllServersFailed by remember { mutableStateOf(false) }
    var isVideoLoading by remember { mutableStateOf(true) }
    var currentServerName by remember { mutableStateOf("Server 1 (Otomatis)") }

    val servers = detail?.servers ?: emptyList()

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

    fun playCurrentServer() {
        if (servers.isEmpty() || currentServerIndex >= servers.size) {
            isAllServersFailed = true
            isVideoLoading = false
            return
        }

        val server = servers[currentServerIndex]
        currentServerName = server.name
        isVideoLoading = true

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://z2.idlixku.com/",
                    "Accept" to "*/*"
                )
            )

        val mediaSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(server.url)))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    LaunchedEffect(currentServerIndex) {
        playCurrentServer()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isVideoLoading = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Auto failover to next server if current server fails!
                if (currentServerIndex + 1 < servers.size) {
                    currentServerIndex += 1
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
                            text = "Menghubungkan: $currentServerName",
                            fontSize = 12.sp,
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
                // Clean Informative Dialog when all servers fail (NO Fallback to Webview!)
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
            } else {
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
                                text = "Memutar Otomatis (Mencoba Server ${currentServerIndex + 1}/${servers.size})...",
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
