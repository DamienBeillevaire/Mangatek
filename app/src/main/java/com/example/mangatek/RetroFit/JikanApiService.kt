package com.example.mangatek

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JikanApiService {

    // Top animes (page par page)
    @GET("top/anime")
    suspend fun getTopAnime(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): AnimeListResponse

    // Animes de la saison actuelle
    @GET("seasons/now")
    suspend fun getCurrentSeasonAnime(
        @Query("page") page: Int = 1
    ): AnimeListResponse

    // Recherche
    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): AnimeListResponse
}