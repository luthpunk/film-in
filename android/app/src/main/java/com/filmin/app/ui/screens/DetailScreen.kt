package com.filmin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.filmin.app.data.model.MovieDetail
import com.filmin.app.data.model.StreamServer
import com.filmin.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    detail: MovieDetail?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onPlayServerClick: (String, String) -> Unit // (serverUrl, serverName)
) {
    var selectedServerIndex by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = detail?.title ?: "Detail Film", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        containerColor = BgDark
    ) { innerPadding ->
        if (isLoading || detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
            ) {
                // Banner Backdrop
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    AsyncImage(
                        model = detail.backdrop.ifBlank { detail.poster },
                        contentDescription = detail.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, BgDark)
                                )
                            )
                    )
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Badges
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = AccentRed, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = detail.quality,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = AccentGold.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "★ ${detail.rating}",
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = detail.year, color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = detail.duration, color = TextSecondary, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = detail.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Genres Row
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(detail.genres) { genre ->
                            Surface(
                                color = BgCard,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = genre,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stream Server Selectors
                    Text(
                        text = "Pilih Server Stream:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(detail.servers.size) { idx ->
                            val srv = detail.servers[idx]
                            val isSelected = selectedServerIndex == idx
                            Surface(
                                color = if (isSelected) AccentRed else BgCard,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { selectedServerIndex = idx }
                            ) {
                                Text(
                                    text = srv.name,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Big Play Video Button
                    val currentServer = detail.servers.getOrNull(selectedServerIndex) ?: detail.servers.firstOrNull()
                    Button(
                        onClick = {
                            if (currentServer != null) {
                                onPlayServerClick(currentServer.url, currentServer.name)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Putar Video (${currentServer?.name ?: "Server 1"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Synopsis
                    Text(
                        text = "Sinopsis",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = detail.synopsis,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Director & Cast
                    Text(
                        text = "Sutradara & Pemeran",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Sutradara: ${detail.director.ifBlank { "Lin Zhenzhao" }}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        detail.cast.take(4).forEach { actor ->
                            Surface(
                                color = BgCard,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = actor,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
