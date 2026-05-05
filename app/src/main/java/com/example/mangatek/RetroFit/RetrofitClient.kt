package com.example.mangatek

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// =============================================================================
// CLIENT HTTP — Configuration de Retrofit pour l'API Jikan
//
// Retrofit est une bibliothèque qui transforme une interface Kotlin (JikanApiService)
// en implémentation HTTP complète. Il gère :
//   - La construction des URLs (baseUrl + endpoint + query params)
//   - La sérialisation des requêtes et désérialisation des réponses (via Gson)
//   - L'exécution asynchrone (via suspend functions + coroutines)
//
// API Jikan : https://api.jikan.moe/v4/
//   Wrapper non officiel de MyAnimeList (MAL), gratuit et sans authentification.
//   Rate limit : ~3 requêtes/seconde, 60/minute.
// =============================================================================

/**
 * Singleton Retrofit — une seule instance partagée dans toute l'application.
 * Le mot-clé "object" en Kotlin crée un singleton de façon thread-safe.
 *
 * "by lazy" sur apiService : l'instance JikanApiService n'est créée qu'à la
 * première utilisation (lazy initialization) → pas de coût au démarrage.
 */
object RetrofitClient {

    /** URL de base de l'API Jikan v4. Toutes les URLs d'endpoint sont relatives à cette base. */
    private const val BASE_URL = "https://api.jikan.moe/v4/"

    /**
     * Instance du service API, créée une seule fois (lazy).
     *
     * Configuration Retrofit :
     *  - baseUrl()              : URL racine de l'API
     *  - GsonConverterFactory   : convertit JSON ↔ data classes Kotlin automatiquement
     *                             (les noms de champs JSON doivent correspondre aux propriétés)
     *  - create(JikanApiService): génère l'implémentation de l'interface
     */
    val apiService: JikanApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())   // JSON → data class
            .build()
            .create(JikanApiService::class.java)
    }
}
