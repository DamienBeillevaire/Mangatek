package com.example.mangatek

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mangatek.data.ViewModel.FavorisViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AnimeScreen(
    viewModel: HomeViewModel = viewModel(),
    favorisViewModel: FavorisViewModel
) {
    val animes        by viewModel.animes.collectAsState()
    val searchQuery   by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching   by viewModel.isSearching.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val hasNextPage   by viewModel.hasNextPage.collectAsState()
    val favorisIds    by favorisViewModel.favorisIds.collectAsState()

    val displayList = if (searchQuery.isNotEmpty()) searchResults else animes

    // Anime sélectionné pour le détail
    var selectedAnime by remember { mutableStateOf<AnimeData?>(null) }

    // Si un anime est sélectionné → afficher l'écran de détail
    selectedAnime?.let { anime ->
        AnimeDetailScreen(
            anime = anime,
            favorisViewModel = favorisViewModel,
            onBack = { selectedAnime = null }
        )
        return
    }

    // Sinon → afficher la grille
    Column(modifier = Modifier.fillMaxSize()) {

        AnimeSearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onClear = { viewModel.clearSearch() }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // Spinner pendant la recherche
                isSearching -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Aucun résultat trouvé
                searchQuery.isNotEmpty() && displayList.isEmpty() && !isSearching -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aucun résultat",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "pour \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Grille d'animes
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayList, key = { it.mal_id }) { anime ->
                            AnimeCard(
                                anime = anime,
                                isFavorite = anime.mal_id in favorisIds,
                                onFavoriteToggle = { favorisViewModel.toggleFavoris(it) },
                                onClick = { selectedAnime = anime }
                            )
                        }

                        // Pagination uniquement hors recherche
                        if (searchQuery.isEmpty() && hasNextPage) {
                            item(span = { GridItemSpan(2) }) {
                                LaunchedEffect(Unit) {
                                    viewModel.loadTopAnime()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoading) CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}