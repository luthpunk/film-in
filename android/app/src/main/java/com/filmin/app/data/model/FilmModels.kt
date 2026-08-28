package com.filmin.app.data.model

data class MovieItem(
    val id: String,
    val slug: String,
    val title: String,
    val type: String = "movie", // "movie" or "series"
    val link: String,
    val poster: String,
    val backdrop: String = "",
    val year: String = "2024",
    val rating: String = "8.0",
    val quality: String = "HD 1080p",
    val synopsis: String = ""
)

data class StreamServer(
    val name: String,
    val url: String,
    val quality: String = "1080p HD",
    val type: String = "iframe"
)

data class MovieDetail(
    val id: String,
    val slug: String,
    val title: String,
    val type: String = "movie",
    val synopsis: String,
    val poster: String,
    val backdrop: String,
    val year: String,
    val releaseDate: String = "",
    val duration: String = "115 Menit",
    val rating: String = "8.4",
    val quality: String = "4K Ultra HD",
    val genres: List<String> = emptyList(),
    val director: String = "",
    val cast: List<String> = emptyList(),
    val trailerUrl: String = "",
    val servers: List<StreamServer> = emptyList()
)

data class EpisodeItem(
    val season: Int,
    val episode: Int,
    val title: String,
    val link: String,
    val streamUrl: String
)

data class SeasonItem(
    val seasonNumber: Int,
    val episodes: List<EpisodeItem>
)

data class SeriesDetail(
    val id: String,
    val slug: String,
    val title: String,
    val type: String = "series",
    val synopsis: String,
    val poster: String,
    val backdrop: String,
    val rating: String = "8.7",
    val seasons: List<SeasonItem> = emptyList()
)

data class HomeFeed(
    val hero: List<MovieItem>,
    val trending: List<MovieItem>,
    val series: List<MovieItem>,
    val action: List<MovieItem>,
    val horror: List<MovieItem>
)
