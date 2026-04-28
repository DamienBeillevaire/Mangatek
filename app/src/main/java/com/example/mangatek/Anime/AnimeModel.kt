package com.example.mangatek

data class AnimeListResponse(
    val data: List<AnimeData>,
    val pagination: Pagination
)

data class Pagination(
    val last_visible_page: Int,
    val has_next_page: Boolean,
    val current_page: Int
)

data class AnimeData(
    val mal_id: Int,
    val title: String,
    val title_english: String,
    val synopsis: String?,
    val score: Double?,
    val episodes: Int?,
    val status: String?,
    val year: Int?,
    val genres: List<Genre>?,
    val images: AnimeImages,
    val trailer: Trailer?
)

data class Genre(
    val name: String
)

data class AnimeImages(
    val jpg: ImageUrls
)

data class ImageUrls(
    val image_url: String,
    val large_image_url: String?
)

data class Trailer(
    val youtube_id: String?
)