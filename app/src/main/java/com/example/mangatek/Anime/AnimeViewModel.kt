package com.example.mangatek

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class HomeViewModel : ViewModel() {

    private val repository = AnimeRepository()

    private val _animes = MutableStateFlow<List<AnimeData>>(emptyList())
    val animes: StateFlow<List<AnimeData>> = _animes

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<AnimeData>>(emptyList())
    val searchResults: StateFlow<List<AnimeData>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _hasNextPage = MutableStateFlow(true)
    val hasNextPage: StateFlow<Boolean> = _hasNextPage

    private var currentPage = 1
    private var searchJob: Job? = null

    init {
        loadTopAnime()
    }

    fun loadTopAnime() {
        if (_isLoading.value || !_hasNextPage.value) return

        viewModelScope.launch {
            _isLoading.value = true

            repository.getTopAnime(currentPage)
                .onSuccess { response ->
                    _animes.value = _animes.value + response.data
                    _hasNextPage.value = response.pagination.has_next_page
                    currentPage++
                }
                .onFailure {
                    // Gérer l'erreur
                }

            _isLoading.value = false
        }
    }
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            _isSearching.value = true
            repository.searchAnime(query)
                .onSuccess { _searchResults.value = it.data }
                .onFailure { _searchResults.value = emptyList() }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
        searchJob?.cancel()
    }
}