package com.example.mangatek

// =============================================================================
// COUCHE MODÈLE — Représentation des données de l'API Jikan (MyAnimeList)
// Ces data classes sont sérialisées/désérialisées automatiquement par Gson
// (configuré dans RetrofitClient) à partir des réponses JSON de l'API.
// =============================================================================

/**
 * Réponse de l'API pour une liste d'animes (ex: top anime, recherche).
 * L'API Jikan renvoie toujours un objet racine avec "data" + "pagination".
 *
 * Exemple d'endpoint : GET /top/anime?page=1
 */
data class AnimeListResponse(
    val data: List<AnimeData>,       // La liste des animes retournés
    val pagination: Pagination       // Métadonnées de pagination
)

/**
 * Informations de pagination fournies par l'API Jikan.
 * Utilisé dans HomeViewModel pour savoir s'il faut charger la page suivante.
 */
data class Pagination(
    val last_visible_page: Int,   // Dernière page disponible
    val has_next_page: Boolean,   // Vrai s'il existe une page suivante → déclenche le lazy loading
    val current_page: Int         // Page actuellement chargée
)

/**
 * Représentation d'un anime individuel tel que renvoyé par l'API Jikan.
 * Tous les champs nullable (?) peuvent être absents dans la réponse JSON —
 * Gson les mettra à null automatiquement sans lever d'exception.
 *
 * Cette classe est utilisée partout : grille, détail, favoris (via conversion).
 */
data class AnimeData(
    val mal_id: Int,               // Identifiant unique MyAnimeList — utilisé comme clé Room & liste
    val title: String,             // Titre japonais (toujours présent)
    val title_english: String?,    // Titre anglais (souvent null pour les animes peu connus)
    val synopsis: String?,         // Description de l'anime
    val score: Double?,            // Note /10 sur MAL (null si pas encore noté)
    val episodes: Int?,            // Nombre d'épisodes (null si en cours ou inconnu)
    val status: String?,           // Ex: "Finished Airing", "Currently Airing"
    val year: Int?,                // Année de diffusion
    val genres: List<Genre>?,      // Liste des genres (Action, Romance, etc.)
    val images: AnimeImages        // URLs des images (toujours présent)
)

/**
 * Genre d'un anime (ex: "Action", "Comedy", "Romance").
 * L'API renvoie un objet complet {mal_id, type, name, url} mais on ne
 * mappe que le champ "name" dont on a besoin → les autres champs sont ignorés par Gson.
 */
data class Genre(
    val name: String
)

/**
 * Conteneur d'images — l'API Jikan fournit plusieurs formats (jpg, webp).
 * On n'utilise ici que le format "jpg".
 */
data class AnimeImages(
    val jpg: ImageUrls
)

/**
 * URLs des images en format JPG.
 * - image_url      : taille standard (~225×318px) — utilisée comme fallback
 * - large_image_url: grande taille (~400×565px) — utilisée en priorité (détail, carte)
 */
data class ImageUrls(
    val image_url: String,
    val large_image_url: String?   // Peut être null pour certains animes peu populaires
)

// =============================================================================
// EXTENSION PROPERTY — Logique de présentation du titre
// =============================================================================

/**
 * Propriété d'extension calculée sur AnimeData.
 * Retourne le titre anglais s'il est disponible et non vide,
 * sinon retourne le titre original (japonais).
 *
 * Utilisé partout dans l'UI pour afficher le "meilleur" titre disponible.
 * Syntaxe Kotlin : accessible comme une propriété normale → anime.displayTitle
 */
val AnimeData.displayTitle: String
    get() = title_english?.takeIf { it.isNotBlank() } ?: title

// =============================================================================
// RÉPONSE DÉTAIL — Pour l'endpoint GET /anime/{id}
// =============================================================================

/**
 * Réponse de l'API pour un anime unique (endpoint de détail).
 * Structure identique à AnimeListResponse mais avec un seul objet "data"
 * au lieu d'un tableau.
 *
 * Utilisé dans AnimeRepository.getAnimeDetail() — non utilisé directement
 * dans l'UI actuelle (le détail s'affiche depuis les données déjà chargées).
 */
data class AnimeDetailResponse(
    val data: AnimeData
)