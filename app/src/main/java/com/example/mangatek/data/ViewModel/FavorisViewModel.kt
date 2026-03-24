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

class FavorisViewModel(
    private val repository: FavorisRepository
) : ViewModel() {

    val favoris: StateFlow<List<FavorisEntity>> = repository.allFavoris
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @RequiresApi(Build.VERSION_CODES.O)
    fun ajouterFavoris(
        date: String
    ) {

        viewModelScope.launch {
            repository.insert(
                FavorisEntity(
                    date = LocalDate.now().toString()
                )
            )
        }
    }

    fun supprimerFavoris(favoris: FavorisEntity) {
        viewModelScope.launch {
            repository.delete(favoris)
        }
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