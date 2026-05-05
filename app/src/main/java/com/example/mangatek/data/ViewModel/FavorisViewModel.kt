package com.example.mangatek.data.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mangatek.AnimeData
import com.example.mangatek.data.entity.FavorisEntity
import com.example.mangatek.data.repository.FavorisRepository
import com.example.mangatek.displayTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// =============================================================================
// VIEWMODEL FAVORIS — Gestion des animes favoris d'un utilisateur spécifique
//
// PARTICULARITÉ : ce ViewModel est scopé à un compte utilisateur (compteId).
// Il est créé dans MangatekAppWithAuth avec remember(compteId) et non via
// viewModels() standard, ce qui permet de le recréer si l'utilisateur change.
//
// FLUX DE DONNÉES (Room → StateFlow → UI) :
//   FavorisDao.getFavorisByCompte() → Flow<List<FavorisEntity>> (Room observe la BDD)
//     → .stateIn() → StateFlow<List<FavorisEntity>> (favoris)
//       → .map{} → StateFlow<Set<Int>> (favorisIds — pour vérification rapide)
// =============================================================================

class FavorisViewModel(
    private val repository: FavorisRepository,
    private val compteId: Int   // ID du compte connecté — scope les données au bon utilisateur
) : ViewModel() {

    // -------------------------------------------------------------------------
    // STATE FLOWS PRINCIPAUX
    // -------------------------------------------------------------------------

    /**
     * Liste complète des favoris du compte connecté, observée en temps réel depuis Room.
     *
     * stateIn() convertit un Flow froid (Room) en StateFlow chaud :
     *  - scope         : viewModelScope → arrêté si le ViewModel est détruit
     *  - started       : WhileSubscribed(5000) → arrête la collecte si aucun collecteur
     *                    pendant 5 secondes (économie de ressources lors des rotations)
     *  - initialValue  : emptyList() → valeur avant la première émission de Room
     *
     * Utilisé dans FavoritesScreen pour afficher la grille de favoris.
     */
    val favoris: StateFlow<List<FavorisEntity>> = repository.getFavorisByCompte(compteId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Set des IDs (mal_id) des animes en favoris.
     * Dérivé de "favoris" par transformation map{}.
     *
     * POURQUOI UN SET ? Pour la vérification O(1) dans AnimeCard et AnimeDetailScreen :
     *   isFavorite = anime.mal_id in favorisIds  ← O(1) avec Set, O(n) avec List
     *
     * Mis à jour automatiquement quand "favoris" change (Room met à jour en temps réel).
     */
    val favorisIds: StateFlow<Set<Int>> = favoris
        .map { list -> list.map { it.malId }.toSet() }  // Transforme List<FavorisEntity> en Set<Int>
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    /** État de chargement (ex: pendant le toggle favori). Non utilisé dans l'UI actuelle. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // -------------------------------------------------------------------------
    // ACTIONS
    // -------------------------------------------------------------------------

    /**
     * Ajoute ou supprime un anime des favoris (toggle).
     * Appelé depuis AnimeCard (bouton ♡) et AnimeDetailScreen (bouton ♥ dans TopAppBar).
     *
     * CONVERSION AnimeData → FavorisEntity :
     * On ne stocke que les champs nécessaires pour afficher la carte favoris
     * (titre, image, score, timestamp). On n'a pas besoin du synopsis, genres, etc.
     *
     * @param anime L'anime à ajouter/retirer des favoris
     */
    fun toggleFavoris(anime: AnimeData) {
        viewModelScope.launch {
            _isLoading.value = true

            // Construction de l'entité Room depuis l'objet API
            val favori = FavorisEntity(
                compteId = compteId,          // Scope au compte connecté
                malId = anime.mal_id,          // Clé primaire composite avec compteId
                title = anime.displayTitle,    // Titre affiché (EN ou JP)
                imageUrl = anime.images.jpg.large_image_url ?: anime.images.jpg.image_url,
                score = anime.score,
                addedAt = System.currentTimeMillis()   // Timestamp pour tri par date d'ajout
            )

            // La logique toggle (ajouter si absent, supprimer si présent) est dans le Repository
            repository.toggleFavori(favori)

            _isLoading.value = false
        }
    }

    /**
     * Supprime un favori directement depuis FavoritesScreen (bouton 🗑️).
     * Différent de toggleFavoris : ici on a déjà un FavorisEntity (depuis la BDD),
     * pas besoin de le convertir depuis AnimeData.
     *
     * @param favoris L'entité Room à supprimer
     */
    fun supprimerFavoris(favoris: FavorisEntity) {
        viewModelScope.launch {
            repository.deleteFavori(favoris.compteId, favoris.malId)
        }
    }
}

// =============================================================================
// FACTORY — Nécessaire pour les ViewModels avec paramètres dans le constructeur
//
// ViewModelProvider.Factory est utilisé quand le ViewModel prend des arguments
// que le système Android ne peut pas fournir seul.
// Ici : repository et compteId sont des paramètres métier.
//
// Utilisé dans le cas où FavorisViewModel est instancié via viewModel() standard.
// Dans l'app actuelle, il est créé manuellement avec remember(), donc cette
// Factory n'est pas utilisée — mais elle est disponible pour une évolution.
// =============================================================================

class FavorisViewModelFactory(
    private val repository: FavorisRepository,
    private val compteId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavorisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavorisViewModel(repository, compteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}