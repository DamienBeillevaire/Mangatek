package com.example.mangatek

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// =============================================================================
// COUCHE VIEWMODEL — Logique métier de l'écran principal (AnimeScreen)
// Pattern MVVM : le ViewModel survit aux rotations d'écran et expose
// des StateFlow que l'UI observe via collectAsState().
//
// Responsabilités :
//   1. Chargement paginé des top animes (infinite scroll)
//   2. Recherche avec debounce (évite les requêtes en rafale pendant la frappe)
//   3. Exposition de l'état de l'UI via StateFlow immuables
// =============================================================================

class HomeViewModel : ViewModel() {

    // -------------------------------------------------------------------------
    // DÉPENDANCES
    // -------------------------------------------------------------------------

    /**
     * Repository d'accès aux données anime.
     * Instancié ici directement (pas d'injection de dépendances).
     * Dans une app de production, on utiliserait Hilt/Koin pour injecter.
     */
    private val repository = AnimeRepository()

    // -------------------------------------------------------------------------
    // STATE FLOWS — État observable par l'UI (lecture seule depuis l'extérieur)
    // Principe : MutableStateFlow en privé (modifiable par le ViewModel),
    //            StateFlow en public (lecture seule pour la Composable).
    // -------------------------------------------------------------------------

    /** Liste cumulée des animes chargés (s'agrandit page après page). */
    private val _animes = MutableStateFlow<List<AnimeData>>(emptyList())
    val animes: StateFlow<List<AnimeData>> = _animes

    /** Texte actuellement saisi dans la barre de recherche. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /** Résultats retournés par l'API pour la recherche en cours. */
    private val _searchResults = MutableStateFlow<List<AnimeData>>(emptyList())
    val searchResults: StateFlow<List<AnimeData>> = _searchResults

    /**
     * Vrai pendant que la requête de recherche est en cours.
     * Affiché comme CircularProgressIndicator dans AnimeScreen.
     */
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    /**
     * Vrai pendant que la page suivante de top animes est en cours de chargement.
     * Affiché comme CircularProgressIndicator en bas de la grille.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Vrai tant qu'il existe une page suivante à charger (selon l'API).
     * Quand false, le lazy loading s'arrête (plus d'item sentinelle en bas).
     */
    private val _hasNextPage = MutableStateFlow(true)
    val hasNextPage: StateFlow<Boolean> = _hasNextPage

    // -------------------------------------------------------------------------
    // ÉTAT INTERNE (non exposé à l'UI)
    // -------------------------------------------------------------------------

    /** Numéro de la prochaine page à charger. Incrémenté après chaque succès. */
    private var currentPage = 1

    /**
     * Handle sur la coroutine de recherche en cours.
     * Permet de l'annuler (searchJob?.cancel()) si l'utilisateur retape
     * avant la fin du délai debounce → évite les résultats obsolètes.
     */
    private var searchJob: Job? = null

    // -------------------------------------------------------------------------
    // INITIALISATION
    // -------------------------------------------------------------------------

    init {
        // Chargement automatique de la première page au démarrage du ViewModel.
        // Le ViewModel est créé une seule fois par la factory de Compose/Activity.
        loadTopAnime()
    }

    // -------------------------------------------------------------------------
    // CHARGEMENT PAGINÉ — Top Anime
    // -------------------------------------------------------------------------

    /**
     * Charge la prochaine page de top animes et l'ajoute à la liste existante.
     *
     * PROTECTION DOUBLE : la garde en début de fonction empêche :
     *  - Les appels concurrents (isLoading = true)
     *  - Les appels inutiles quand il n'y a plus de page (hasNextPage = false)
     *
     * INFINITE SCROLL : déclenché par LaunchedEffect dans AnimeScreen quand
     * l'item sentinelle (dernier élément de la grille) devient visible.
     *
     * Mise à jour de l'état :
     *  - _animes    : accumulation (ancien + nouveau) avec l'opérateur +
     *  - _hasNextPage : mis à jour depuis pagination.has_next_page de l'API
     *  - currentPage  : incrémenté pour la prochaine requête
     */
    fun loadTopAnime() {
        // Garde : ne pas charger si un chargement est déjà en cours ou s'il n'y a plus de page
        if (_isLoading.value || !_hasNextPage.value) return

        viewModelScope.launch {   // Coroutine liée au cycle de vie du ViewModel
            _isLoading.value = true

            repository.getTopAnime(currentPage)
                .onSuccess { response ->
                    // Concaténation : on garde les animes existants + on ajoute les nouveaux
                    _animes.value = _animes.value + response.data
                    _hasNextPage.value = response.pagination.has_next_page
                    currentPage++
                }
                .onFailure {
                    // TODO: exposer l'erreur à l'UI (ex: Snackbar, état d'erreur)
                    // Pour l'instant silencieux — l'utilisateur peut réessayer en scrollant
                }

            _isLoading.value = false
        }
    }

    // -------------------------------------------------------------------------
    // RECHERCHE AVEC DEBOUNCE
    // -------------------------------------------------------------------------

    /**
     * Appelé à chaque frappe dans AnimeSearchBar (via onValueChange).
     * Implémente un debounce de 400ms : la requête API n'est envoyée que si
     * l'utilisateur arrête de taper pendant 400ms.
     *
     * POURQUOI LE DEBOUNCE ?
     * Sans délai, chaque caractère tapé déclencherait une requête → surcharge API.
     * Avec 400ms, une frappe de "naruto" (6 touches) = 1 seule requête.
     *
     * GESTION DE LA JOB :
     * searchJob?.cancel() annule la coroutine précédente si elle existe.
     * Ainsi, si l'utilisateur tape "n", "na", "nar" rapidement, seul "nar"
     * (ou la dernière valeur au bout de 400ms) déclenchera la requête.
     *
     * @param query Texte saisi par l'utilisateur
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()   // Annule la recherche précédente si elle est en attente

        // Si la recherche est effacée → réinitialiser et revenir à la liste principale
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)   // Attendre 400ms de silence avant d'envoyer la requête
            _isSearching.value = true

            repository.searchAnime(query)
                .onSuccess { _searchResults.value = it.data }
                .onFailure { _searchResults.value = emptyList() }

            _isSearching.value = false
        }
    }

    // -------------------------------------------------------------------------
    // RÉINITIALISATION DE LA RECHERCHE
    // -------------------------------------------------------------------------

    /**
     * Réinitialise complètement l'état de recherche.
     * Appelé par le bouton "X" dans AnimeSearchBar (via onClear).
     * Annule aussi la coroutine de recherche en cours pour éviter
     * qu'un résultat tardif ne s'affiche après la réinitialisation.
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
        searchJob?.cancel()
    }

    // -------------------------------------------------------------------------
    // DÉTAIL FAVORI — Chargement depuis l'API au clic sur une carte favori
    // -------------------------------------------------------------------------

    private val _favoriteDetailAnime = MutableStateFlow<AnimeData?>(null)
    val favoriteDetailAnime: StateFlow<AnimeData?> = _favoriteDetailAnime

    private val _isFavoriteDetailLoading = MutableStateFlow(false)
    val isFavoriteDetailLoading: StateFlow<Boolean> = _isFavoriteDetailLoading

    fun loadFavoriteDetail(id: Int) {
        viewModelScope.launch {
            _isFavoriteDetailLoading.value = true
            repository.getAnimeDetail(id)
                .onSuccess { _favoriteDetailAnime.value = it }
                .onFailure { /* silencieux — l'utilisateur peut réessayer */ }
            _isFavoriteDetailLoading.value = false
        }
    }

    fun clearFavoriteDetail() {
        _favoriteDetailAnime.value = null
    }
}