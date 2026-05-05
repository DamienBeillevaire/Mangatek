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

class FavorisViewModel(
    private val repository: FavorisRepository,
    private val compteId: Int  // ID du compte connecté
) : ViewModel() {

    // Récupérer uniquement les favoris du compte connecté
    val favoris: StateFlow<List<FavorisEntity>> = repository.getFavorisByCompte(compteId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Set des IDs favoris pour vérifier rapidement
    val favorisIds: StateFlow<Set<Int>> = favoris
        .map { list -> list.map { it.malId }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Toggle un favori (ajouter ou supprimer)
     */
    fun toggleFavoris(anime: AnimeData) {
        viewModelScope.launch {
            _isLoading.value = true

            val favori = FavorisEntity(
                compteId = compteId,
                malId = anime.mal_id,
                title = anime.displayTitle,
                imageUrl = anime.images.jpg.large_image_url ?: anime.images.jpg.image_url,
                score = anime.score,
                addedAt = System.currentTimeMillis()
            )

            repository.toggleFavori(favori)

            _isLoading.value = false
        }
    }

    /**
     * Supprimer un favori
     */
    fun supprimerFavoris(favoris: FavorisEntity) {
        viewModelScope.launch {
            repository.deleteFavori(favoris.compteId, favoris.malId)
        }
    }
}

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