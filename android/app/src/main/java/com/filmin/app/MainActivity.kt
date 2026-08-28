package com.filmin.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.filmin.app.data.model.HomeFeed
import com.filmin.app.data.model.MovieDetail
import com.filmin.app.data.model.MovieItem
import com.filmin.app.data.remote.IdlixDirectScraper
import com.filmin.app.ui.screens.*
import com.filmin.app.ui.theme.FilmInTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val scraper = IdlixDirectScraper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FilmInTheme {
                var homeFeed by remember { mutableStateOf<HomeFeed?>(null) }
                var isHomeLoading by remember { mutableStateOf(true) }

                var currentDetail by remember { mutableStateOf<MovieDetail?>(null) }
                var isDetailLoading by remember { mutableStateOf(false) }

                var searchResults by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
                var isSearching by remember { mutableStateOf(false) }

                var bookmarks by remember { mutableStateOf<List<MovieItem>>(emptyList()) }

                val navController = rememberNavController()

                // Fetch Home Feed on Launch directly on IO thread
                LaunchedEffect(Unit) {
                    isHomeLoading = true
                    homeFeed = scraper.getHomeFeed()
                    isHomeLoading = false
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            feed = homeFeed ?: HomeFeed(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                            isLoading = isHomeLoading,
                            onMovieClick = { slug, type ->
                                isDetailLoading = true
                                currentDetail = null
                                navController.navigate("detail/$slug/$type")
                                lifecycleScope.launch {
                                    currentDetail = scraper.getMovieDetail(slug)
                                    isDetailLoading = false
                                }
                            },
                            onSearchClick = {
                                searchResults = homeFeed?.trending ?: emptyList()
                                navController.navigate("search")
                            },
                            onBookmarkClick = {
                                navController.navigate("bookmarks")
                            }
                        )
                    }

                    composable(
                        route = "detail/{slug}/{type}",
                        arguments = listOf(
                            navArgument("slug") { type = NavType.StringType },
                            navArgument("type") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val slug = backStackEntry.arguments?.getString("slug") ?: ""
                        val type = backStackEntry.arguments?.getString("type") ?: "movie"

                        LaunchedEffect(slug) {
                            if (currentDetail == null || currentDetail?.slug != slug) {
                                isDetailLoading = true
                                currentDetail = scraper.getMovieDetail(slug)
                                isDetailLoading = false
                            }
                        }

                        DetailScreen(
                            detail = currentDetail,
                            isLoading = isDetailLoading,
                            onBackClick = { navController.popBackStack() },
                            onPlayServerClick = { streamUrl, serverName ->
                                val encodedUrl = Uri.encode(streamUrl)
                                val encodedName = Uri.encode(serverName)
                                navController.navigate("player/$encodedUrl/$encodedName")
                            }
                        )
                    }

                    composable(
                        route = "player/{streamUrl}/{serverName}",
                        arguments = listOf(
                            navArgument("streamUrl") { type = NavType.StringType },
                            navArgument("serverName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val rawUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
                        val streamUrl = Uri.decode(rawUrl)
                        val serverName = Uri.decode(backStackEntry.arguments?.getString("serverName") ?: "Server Stream")

                        PlayerScreen(
                            streamUrl = streamUrl,
                            serverName = serverName,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable("search") {
                        SearchScreen(
                            results = searchResults,
                            isSearching = isSearching,
                            onSearchQueryChange = { query ->
                                lifecycleScope.launch {
                                    isSearching = true
                                    searchResults = scraper.search(query)
                                    isSearching = false
                                }
                            },
                            onMovieClick = { slug, type ->
                                isDetailLoading = true
                                currentDetail = null
                                navController.navigate("detail/$slug/$type")
                                lifecycleScope.launch {
                                    currentDetail = scraper.getMovieDetail(slug)
                                    isDetailLoading = false
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable("bookmarks") {
                        BookmarkScreen(
                            bookmarks = bookmarks,
                            onMovieClick = { slug, type ->
                                isDetailLoading = true
                                currentDetail = null
                                navController.navigate("detail/$slug/$type")
                                lifecycleScope.launch {
                                    currentDetail = scraper.getMovieDetail(slug)
                                    isDetailLoading = false
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
