package com.example.mangatek.data.ViewModel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mangatek.data.FavorisEntity
import com.example.mangatek.data.FavorisRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.mangatek.AnimeData
import com.example.mangatek.displayTitle
import kotlinx.coroutines.flow.map

class FavorisViewModel(
    private val repository: FavorisRepository
) : ViewModel() {

    val favoris: StateFlow<List<FavorisEntity>> = repository.allFavoris
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Set des IDs favoris pour vérifier rapidement si un anime est favori
    val favorisIds: StateFlow<Set<Int>> = favoris
        .map { list -> list.map { it.malId }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleFavoris(anime: AnimeData) {
        viewModelScope.launch {
            val isFav = favoris.value.any { it.malId == anime.mal_id }
            if (isFav) {
                repository.delete(FavorisEntity(
                    malId = anime.mal_id,
                    title = anime.displayTitle,
                    imageUrl = anime.images.jpg.large_image_url ?: anime.images.jpg.image_url,
                    score = anime.score,
                    addedAt = ""
                ))
            } else {
                repository.insert(FavorisEntity(
                    malId = anime.mal_id,
                    title = anime.displayTitle,
                    imageUrl = anime.images.jpg.large_image_url ?: anime.images.jpg.image_url,
                    score = anime.score,
                    addedAt = LocalDate.now().toString()
                ))
            }
        }
    }

    fun supprimerFavoris(favoris: FavorisEntity) {
        viewModelScope.launch { repository.delete(favoris) }
    }
}

class FavorisViewModelFactory(
    private val repository: FavorisRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavorisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavorisViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel inconnue")
    }
}