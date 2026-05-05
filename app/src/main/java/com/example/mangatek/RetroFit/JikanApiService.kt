package com.example.mangatek

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// =============================================================================
// INTERFACE API — Définition des endpoints Jikan utilisés dans l'application
//
// Retrofit génère automatiquement l'implémentation de cette interface.
// Chaque fonction est une "suspend function" → appelable depuis une coroutine.
//
// ANNOTATIONS :
//   @GET("path")     : méthode HTTP GET + chemin relatif à BASE_URL
//   @Query("param")  : ajoute ?param=value à l'URL
//   @Path("id")      : remplace {id} dans l'URL par la valeur du paramètre
// =============================================================================

interface JikanApiService {

    /**
     * Classement des meilleurs animes MyAnimeList, paginé.
     * URL finale : GET https://api.jikan.moe/v4/top/anime?page=1&limit=20
     *
     * @param page  Numéro de page (1-based)
     * @param limit Nombre d'animes par page (max 25 pour l'API Jikan)
     */
    @GET("top/anime")
    suspend fun getTopAnime(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): AnimeListResponse

    /**
     * Animes actuellement en cours de diffusion (saison actuelle).
     * URL finale : GET https://api.jikan.moe/v4/seasons/now?page=1
     * Non utilisé dans l'UI actuelle — prévu pour une future évolution.
     */
    @GET("seasons/now")
    suspend fun getCurrentSeasonAnime(
        @Query("page") page: Int = 1
    ): AnimeListResponse

    /**
     * Recherche d'animes par titre.
     * URL finale : GET https://api.jikan.moe/v4/anime?q=naruto&page=1&limit=20
     *
     * @param query Texte de recherche saisi par l'utilisateur
     * @param page  Page de résultats (toujours 1 dans l'implémentation actuelle)
     * @param limit Nombre de résultats par page
     */
    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): AnimeListResponse

    /**
     * Détail complet d'un anime par son ID MAL.
     * URL finale : GET https://api.jikan.moe/v4/anime/1
     *
     * Retourne AnimeDetailResponse (wrapper avec "data": AnimeData)
     * au lieu d'AnimeData directement (structure JSON de l'API).
     */
    @GET("anime/{id}")
    suspend fun getAnimeDetail(
        @Path("id") id: Int   // Remplace {id} dans l'URL → /anime/1, /anime/20, etc.
    ): AnimeDetailResponse
}
