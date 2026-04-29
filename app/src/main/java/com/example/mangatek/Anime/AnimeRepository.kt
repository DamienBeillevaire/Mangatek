package com.example.mangatek

class AnimeRepository {

    private val api = RetrofitClient.apiService

    suspend fun getTopAnime(page: Int = 1): Result<AnimeListResponse> {
        return try {
            Result.success(api.getTopAnime(page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentSeasonAnime(): Result<AnimeListResponse> {
        return try {
            Result.success(api.getCurrentSeasonAnime())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchAnime(query: String, page: Int = 1): Result<AnimeListResponse> {
        return try {
            Result.success(api.searchAnime(query, page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAnimeDetail(id: Int): Result<AnimeData> {
        return try {
            Result.success(api.getAnimeDetail(id).data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

