package com.example.mangatek

// =============================================================================
// COUCHE REPOSITORY — Abstraction de la source de données distante (API Jikan)
// Pattern Repository : isole la logique d'accès réseau du ViewModel.
// Le ViewModel ne connaît pas Retrofit, il ne parle qu'au Repository.
//
// FLUX DE DONNÉES :
//   ViewModel → Repository → RetrofitClient → API Jikan (https://api.jikan.moe/v4/)
//               ←  Result<T>  ←  (JSON)     ←
// =============================================================================

/**
 * Repository unique pour toutes les opérations liées aux animes.
 * Instancié directement dans HomeViewModel (pas d'injection de dépendances ici).
 *
 * Chaque méthode :
 *  1. Appelle l'API via RetrofitClient (suspend function → exécutée en coroutine)
 *  2. Enveloppe le résultat dans Result<T> pour gérer les erreurs proprement
 *  3. Retourne Result.success(data) ou Result.failure(exception)
 *
 * Avantage du pattern Result<T> : le ViewModel utilise .onSuccess{} / .onFailure{}
 * sans try/catch → code plus lisible.
 */
class AnimeRepository {

    // Instance du service Retrofit — objet singleton configuré dans RetrofitClient
    private val api = RetrofitClient.apiService

    // -------------------------------------------------------------------------
    // TOP ANIME — Chargement paginé des meilleurs animes MAL
    // -------------------------------------------------------------------------

    /**
     * Récupère une page du classement des meilleurs animes.
     * @param page Numéro de page (commence à 1). La pagination est gérée
     *             par HomeViewModel via currentPage++.
     * @return Result contenant AnimeListResponse (data + pagination) en cas de succès,
     *         ou l'exception réseau/parsing en cas d'échec.
     *
     * Endpoint : GET https://api.jikan.moe/v4/top/anime?page={page}&limit=20
     */
    suspend fun getTopAnime(page: Int = 1): Result<AnimeListResponse> {
        return try {
            Result.success(api.getTopAnime(page))
        } catch (e: Exception) {
            // Capture toutes les exceptions : IOException (réseau), HttpException (HTTP 4xx/5xx),
            // JsonSyntaxException (parsing), etc.
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // SAISON EN COURS — Animes actuellement diffusés
    // -------------------------------------------------------------------------

    /**
     * Récupère les animes de la saison actuelle.
     * Non utilisé dans l'UI actuelle mais disponible pour une évolution future
     * (ex: onglet "Cette saison").
     *
     * Endpoint : GET https://api.jikan.moe/v4/seasons/now
     */
    suspend fun getCurrentSeasonAnime(): Result<AnimeListResponse> {
        return try {
            Result.success(api.getCurrentSeasonAnime())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // RECHERCHE — Recherche d'animes par titre
    // -------------------------------------------------------------------------

    /**
     * Recherche des animes correspondant à la requête textuelle.
     * Appelé depuis HomeViewModel.onSearchQueryChange() avec un délai debounce
     * de 400ms pour éviter trop de requêtes pendant la frappe.
     *
     * @param query   Texte saisi par l'utilisateur dans AnimeSearchBar
     * @param page    Page de résultats (toujours 1 ici, pas de pagination sur la recherche)
     *
     * Endpoint : GET https://api.jikan.moe/v4/anime?q={query}&page=1&limit=20
     */
    suspend fun searchAnime(query: String, page: Int = 1): Result<AnimeListResponse> {
        return try {
            Result.success(api.searchAnime(query, page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // DÉTAIL — Récupération d'un anime par son ID MAL
    // -------------------------------------------------------------------------

    /**
     * Récupère les informations complètes d'un anime par son identifiant MAL.
     * Retourne directement AnimeData (extrait de AnimeDetailResponse.data)
     * car c'est le seul objet utile dans la réponse.
     *
     * Endpoint : GET https://api.jikan.moe/v4/anime/{id}
     *
     * Note : actuellement non appelé dans l'UI (le détail est affiché depuis
     * les données déjà chargées dans la liste). Prévu pour enrichir le détail
     * avec des champs supplémentaires non présents dans la liste.
     */
    suspend fun getAnimeDetail(id: Int): Result<AnimeData> {
        return try {
            Result.success(api.getAnimeDetail(id).data)  // On extrait .data directement
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}