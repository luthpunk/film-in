package com.filmin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.filmin.app.data.model.HomeFeed
import com.filmin.app.data.model.MovieItem
import com.filmin.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    feed: HomeFeed,
    isLoading: Boolean,
    onMovieClick: (String, String) -> Unit,
    onSearchClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FILM",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        Text(
                            text = "IN",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = AccentRed
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                    IconButton(onClick = onBookmarkClick) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Bookmarks", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        containerColor = BgDark
    ) { innerPadding ->
        if (isLoading) {
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
                // Hero Carousel Banner
                if (feed.hero.isNotEmpty()) {
                    HeroBanner(
                        movie = feed.hero.first(),
                        onPlayClick = { onMovieClick(it.slug, it.type) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Trending Movies Row
                ContentRowSection(
                    title = "🔥 Trending Film Terbaru",
                    movies = feed.trending,
                    onMovieClick = onMovieClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Series Row
                ContentRowSection(
                    title = "📺 Serial TV & Drama",
                    movies = feed.series,
                    onMovieClick = onMovieClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Movies Row
                ContentRowSection(
                    title = "💥 Film Action Penuh Aksi",
                    movies = feed.action,
                    onMovieClick = onMovieClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Horror Movies Row
                ContentRowSection(
                    title = "😱 Horor & Misteri",
                    movies = feed.horror,
                    onMovieClick = onMovieClick
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun HeroBanner(
    movie: MovieItem,
    onPlayClick: (MovieItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clickable { onPlayClick(movie) }
    ) {
        AsyncImage(
            model = movie.backdrop.ifBlank { movie.poster },
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BgDark.copy(alpha = 0.7f),
                            BgDark
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = AccentRed,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "4K ULTRA HD",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = AccentGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "★ ${movie.rating}",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = movie.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = movie.synopsis,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onPlayClick(movie) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tonton Sekarang", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ContentRowSection(
    title: String,
    movies: List<MovieItem>,
    onMovieClick: (String, String) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(movies) { movie ->
                MovieCardItem(movie = movie, onClick = { onMovieClick(movie.slug, movie.type) })
            }
        }
    }
}

@Composable
fun MovieCardItem(
    movie: MovieItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(135.dp)
                .height(195.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
        ) {
            AsyncImage(
                model = movie.poster,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = "★ ${movie.rating}",
                    color = AccentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = movie.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = movie.year,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}
