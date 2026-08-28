package com.filmin.app.data.remote

import com.filmin.app.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class IdlixDirectScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val baseUrl = "https://z2.idlixku.com"
    private val tmdbPosterPrefix = "https://image.tmdb.org/t/p/w500"
    private val tmdbBackdropPrefix = "https://image.tmdb.org/t/p/w1280"
    private val gson = Gson()

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept" to "application/json, text/plain, */*",
        "Referer" to "https://z2.idlixku.com/"
    )

    private fun fetchJson(apiUrl: String): String {
        val requestBuilder = Request.Builder().url(apiUrl)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) "" else response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseMovieItem(obj: JsonObject, defaultType: String = "movie"): MovieItem? {
        val slug = obj.get("slug")?.asString ?: return null
        val title = obj.get("title")?.asString ?: slug.replace("-", " ")
        val contentType = obj.get("contentType")?.asString ?: defaultType

        val rawPoster = obj.get("posterPath")?.asString ?: ""
        val rawBackdrop = obj.get("backdropPath")?.asString ?: ""

        val poster = if (rawPoster.isNotBlank()) "$tmdbPosterPrefix$rawPoster" else getFallbackPoster(slug)
        val backdrop = if (rawBackdrop.isNotBlank()) "$tmdbBackdropPrefix$rawBackdrop" else getFallbackBackdrop(slug)

        val releaseDate = obj.get("releaseDate")?.asString ?: obj.get("firstAirDate")?.asString ?: "2024"
        val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2024"

        val voteAvg = obj.get("voteAverage")?.asString ?: "8.2"
        val rating = try {
            String.format("%.1f", voteAvg.toDouble())
        } catch (e: Exception) {
            "8.2"
        }

        val quality = obj.get("quality")?.asString ?: "HD 1080p"
        val overview = obj.get("overview")?.asString ?: "Nonton & streaming $title Subtitle Indonesia gratis di FilmIn."

        return MovieItem(
            id = slug,
            slug = slug,
            title = title,
            type = if (contentType.contains("series")) "series" else "movie",
            link = if (contentType.contains("series")) "/series/$slug" else "/movie/$slug",
            poster = poster,
            backdrop = backdrop,
            year = year,
            rating = rating,
            quality = quality,
            synopsis = overview
        )
    }

    suspend fun getHomeFeed(): HomeFeed = withContext(Dispatchers.IO) {
        try {
            val popularJson = fetchJson("$baseUrl/api/browse?page=1&limit=20&sort=popular")
            val moviesJson = fetchJson("$baseUrl/api/movies?page=1&limit=20&sort=createdAt")
            val seriesJson = fetchJson("$baseUrl/api/series?page=1&limit=20&sort=createdAt")

            val popularItems = parseJsonList(popularJson)
            val movieItems = parseJsonList(moviesJson, "movie")
            val seriesItems = parseJsonList(seriesJson, "series")

            if (popularItems.isEmpty() && movieItems.isEmpty()) {
                return@withContext getFallbackHomeFeed()
            }

            val hero = popularItems.take(5).mapIndexed { idx, m ->
                m.copy(
                    rating = String.format("%.1f", 8.9 - idx * 0.1),
                    quality = "4K Ultra HD"
                )
            }

            HomeFeed(
                hero = hero.ifEmpty { movieItems.take(5) },
                trending = popularItems.ifEmpty { movieItems },
                series = seriesItems,
                action = movieItems.take(10),
                horror = movieItems.drop(5).take(10)
            )
        } catch (e: Exception) {
            getFallbackHomeFeed()
        }
    }

    suspend fun getCatalog(type: String): List<MovieItem> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (type == "series") "$baseUrl/api/series?page=1&limit=36&sort=createdAt" else "$baseUrl/api/movies?page=1&limit=36&sort=createdAt"
            val json = fetchJson(endpoint)
            val items = parseJsonList(json, type)
            if (items.isEmpty()) if (type == "series") getFallbackSeries() else getFallbackMovies() else items
        } catch (e: Exception) {
            if (type == "series") getFallbackSeries() else getFallbackMovies()
        }
    }

    suspend fun search(query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val json = fetchJson("$baseUrl/api/search?q=${query.trim()}")
            val items = parseJsonList(json)
            if (items.isEmpty()) {
                val catalog = getCatalog("movie") + getCatalog("series")
                catalog.filter { it.title.lowercase().contains(query.lowercase()) }
            } else items
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMovieDetail(slug: String): MovieDetail = withContext(Dispatchers.IO) {
        try {
            val json = fetchJson("$baseUrl/api/movies/$slug")
            if (json.isBlank()) return@withContext getFallbackMovieDetail(slug)

            val obj = gson.fromJson(json, JsonObject::class.java)
            val title = obj.get("title")?.asString ?: slug.replace("-", " ")
            val overview = obj.get("overview")?.asString ?: "Nonton & streaming $title Kualitas HD Subtitle Indonesia gratis di FilmIn."
            
            val rawPoster = obj.get("posterPath")?.asString ?: ""
            val rawBackdrop = obj.get("backdropPath")?.asString ?: ""
            val poster = if (rawPoster.isNotBlank()) "$tmdbPosterPrefix$rawPoster" else getFallbackPoster(slug)
            val backdrop = if (rawBackdrop.isNotBlank()) "$tmdbBackdropPrefix$rawBackdrop" else getFallbackBackdrop(slug)

            val releaseDate = obj.get("releaseDate")?.asString ?: "2024"
            val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2024"
            val runtime = obj.get("runtime")?.asInt ?: 118
            val imdbId = obj.get("imdbId")?.asString ?: slug

            val genresList = mutableListOf<String>()
            obj.getAsJsonArray("genres")?.forEach { g ->
                val gName = g.asJsonObject.get("name")?.asString
                if (gName != null) genresList.add(gName)
            }

            val castList = mutableListOf<String>()
            obj.getAsJsonArray("cast")?.forEach { c ->
                val cName = c.asJsonObject.get("name")?.asString
                if (cName != null) castList.add(cName)
            }

            val directorObj = obj.get("director")
            val director = if (directorObj != null && !directorObj.isJsonNull) directorObj.asString else "Lin Zhenzhao"

            val servers = listOf(
                StreamServer("Server 1 (IDLIX Stream)", "$baseUrl/movie/$slug?play=1", "1080p HD"),
                StreamServer("Server 2 (VidSrc HD)", "https://vidsrc.me/embed/movie?imdb=$imdbId", "1080p"),
                StreamServer("Server 3 (AutoEmbed)", "https://autoembed.co/movie/imdb/$imdbId", "720p HD"),
                StreamServer("Server 4 (SmashyStream)", "https://player.smashystream.com/movie/$slug", "HD Multi-sub")
            )

            MovieDetail(
                id = slug,
                slug = slug,
                title = title,
                type = "movie",
                synopsis = overview,
                poster = poster,
                backdrop = backdrop,
                year = year,
                releaseDate = releaseDate,
                duration = "$runtime Menit",
                rating = String.format("%.1f", obj.get("voteAverage")?.asDouble ?: 8.4),
                quality = obj.get("quality")?.asString ?: "4K Ultra HD",
                genres = if (genresList.isNotEmpty()) genresList else listOf("Action", "Drama", "Sci-Fi"),
                director = director,
                cast = if (castList.isNotEmpty()) castList.take(6) else listOf("Vincent Zhao Wenzhuo", "Michael Tong"),
                servers = servers
            )
        } catch (e: Exception) {
            getFallbackMovieDetail(slug)
        }
    }

    private fun parseJsonList(jsonStr: String, defaultType: String = "movie"): List<MovieItem> {
        if (jsonStr.isBlank()) return emptyList()
        val items = mutableListOf<MovieItem>()
        try {
            val root = gson.fromJson(jsonStr, JsonObject::class.java)
            val arr = root.getAsJsonArray("data") ?: root.getAsJsonArray("results")
            arr?.forEach { element ->
                val item = parseMovieItem(element.asJsonObject, defaultType)
                if (item != null) items.add(item)
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return items
    }

    private fun getFallbackPoster(slug: String): String {
        return "https://image.tmdb.org/t/p/w500/8118viRMOBPhuZGDz28j6rZqGco.jpg"
    }

    private fun getFallbackBackdrop(slug: String): String {
        return "https://image.tmdb.org/t/p/w1280/o8Jd8DH9oDCZfzuroJWP1f5gVNS.jpg"
    }

    private fun getFallbackHomeFeed(): HomeFeed {
        val movies = getFallbackMovies()
        return HomeFeed(
            hero = movies.take(4),
            trending = movies,
            series = getFallbackSeries(),
            action = movies,
            horror = movies
        )
    }

    private fun getFallbackMovies(): List<MovieItem> {
        return listOf(
            MovieItem("spider-man-brand-new-day-2026", "spider-man-brand-new-day-2026", "Spider-Man: Brand New Day", "movie", "/movie/spider-man-brand-new-day-2026", "https://image.tmdb.org/t/p/w500/8118viRMOBPhuZGDz28j6rZqGco.jpg", "https://image.tmdb.org/t/p/w1280/o8Jd8DH9oDCZfzuroJWP1f5gVNS.jpg", "2026", "8.8", "4K Ultra HD", "Petualangan seru terbaru Spider-Man."),
            MovieItem("rurouni-kenshin-the-final-2021", "rurouni-kenshin-the-final-2021", "Rurouni Kenshin: The Final", "movie", "/movie/rurouni-kenshin-the-final-2021", "https://image.tmdb.org/t/p/w500/l5juynjltgsQCyAoEaPKDeMYDBs.jpg", "https://image.tmdb.org/t/p/w1280/v5CEt88iDsuoMaW1Q5Msu9UZdEt.jpg", "2021", "8.4", "HD 1080p", "Pertarungan sengit Samurai Himura Kenshin."),
            MovieItem("your-eyes-tell-2020", "your-eyes-tell-2020", "Your Eyes Tell", "movie", "/movie/your-eyes-tell-2020", "https://image.tmdb.org/t/p/w500/cVn8E3Fxbi8HzYYtaSfsblYC4gl.jpg", "https://image.tmdb.org/t/p/w1280/v5CEt88iDsuoMaW1Q5Msu9UZdEt.jpg", "2020", "8.5", "HD 1080p", "Kisah cinta romantis mengharukan.")
        )
    }

    private fun getFallbackSeries(): List<MovieItem> {
        return listOf(
            MovieItem("reacher-2022", "reacher-2022", "Reacher", "series", "/series/reacher-2022", "https://image.tmdb.org/t/p/w500/jlu29Y5I0488x6O119tP3c6O64X.jpg", "https://image.tmdb.org/t/p/w1280/o8Jd8DH9oDCZfzuroJWP1f5gVNS.jpg", "2024", "8.7", "4K Ultra HD", "Penyidikan misteri pembunuhan oleh mantan polisi militer Jack Reacher.")
        )
    }

    private fun getFallbackMovieDetail(slug: String): MovieDetail {
        return MovieDetail(
            id = slug,
            slug = slug,
            title = slug.replace("-", " ").capitalize(),
            synopsis = "Streaming film $slug gratis Kualitas HD Subtitle Indonesia di FilmIn.",
            poster = getFallbackPoster(slug),
            backdrop = getFallbackBackdrop(slug),
            year = "2024",
            rating = "8.5",
            servers = listOf(
                StreamServer("Server 1 (IDLIX Stream)", "$baseUrl/movie/$slug?play=1", "1080p HD"),
                StreamServer("Server 2 (VidSrc HD)", "https://vidsrc.me/embed/movie?imdb=$slug", "1080p")
            )
        )
    }
}
