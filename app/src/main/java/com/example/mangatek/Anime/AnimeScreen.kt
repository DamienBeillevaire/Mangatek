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
import androidx.compose.foundation.layout.statusBarsPadding
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

// =============================================================================
// ÉCRAN PRINCIPAL — Grille d'animes avec recherche et infinite scroll
//
// ARCHITECTURE DE NAVIGATION INTERNE :
//   AnimeScreen affiche SOIT la grille, SOIT le détail d'un anime.
//   La navigation est gérée localement par un état "selectedAnime" (pas de NavController).
//   Si selectedAnime != null → on affiche AnimeDetailScreen et on sort (return early).
//   Si selectedAnime == null → on affiche la grille normale.
//
// SOURCES DE DONNÉES (deux ViewModel distincts) :
//   - AnimeViewModel     : gère la liste/recherche d'animes (données réseau)
//   - FavorisViewModel  : gère les favoris (données Room/locale)
// =============================================================================

@RequiresApi(Build.VERSION_CODES.O)   // Requis par AnimeDetailScreen (utilise l'API O)
@Composable
fun AnimeScreen(
    viewModel: HomeViewModel = viewModel(),   // Injecté automatiquement par Compose
    favorisViewModel: FavorisViewModel        // Passé depuis MainActivity (partagé entre écrans)
) {
    // -------------------------------------------------------------------------
    // COLLECTE DES ÉTATS — Convertit les StateFlow en State Compose
    // collectAsState() s'abonne au Flow et redessine la Composable à chaque émission.
    // -------------------------------------------------------------------------
    val animes        by viewModel.animes.collectAsState()         // Liste cumulée (pagination)
    val searchQuery   by viewModel.searchQuery.collectAsState()    // Texte de recherche en cours
    val searchResults by viewModel.searchResults.collectAsState()  // Résultats de l'API search
    val isSearching   by viewModel.isSearching.collectAsState()    // Spinner de recherche
    val isLoading     by viewModel.isLoading.collectAsState()      // Spinner de pagination
    val hasNextPage   by viewModel.hasNextPage.collectAsState()    // Faut-il charger plus ?
    val favorisIds    by favorisViewModel.favorisIds.collectAsState() // Set<Int> des IDs favoris

    // -------------------------------------------------------------------------
    // LISTE À AFFICHER — Logique de basculement liste principale / résultats recherche
    // Si une recherche est active → on affiche searchResults
    // Sinon → on affiche la liste paginée "animes"
    // -------------------------------------------------------------------------
    val displayList = if (searchQuery.isNotEmpty()) searchResults else animes

    // -------------------------------------------------------------------------
    // ÉTAT LOCAL DE NAVIGATION — Anime sélectionné pour la vue détail
    // remember{} : survit aux recompositions mais PAS aux rotations d'écran
    // (acceptable ici : la rotation revient simplement à la liste)
    // -------------------------------------------------------------------------
    var selectedAnime by remember { mutableStateOf<AnimeData?>(null) }

    // -------------------------------------------------------------------------
    // NAVIGATION VERS LE DÉTAIL — Pattern "return early"
    // Si un anime est sélectionné, on affiche immédiatement l'écran de détail
    // et on sort de la fonction (le reste du code ne s'exécute pas).
    // Le bouton retour dans AnimeDetailScreen appelle onBack = { selectedAnime = null }
    // ce qui retourne à la grille.
    // -------------------------------------------------------------------------
    selectedAnime?.let { anime ->
        AnimeDetailScreen(
            anime = anime,
            favorisViewModel = favorisViewModel,
            onBack = { selectedAnime = null }  // Réinitialise → revient à la grille
        )
        return   // ← Sort de AnimeScreen, AnimeDetailScreen prend tout l'écran
    }

    // -------------------------------------------------------------------------
    // VUE PRINCIPALE — Barre de recherche + contenu principal
    // -------------------------------------------------------------------------
    Column(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
    ) {

        // Barre de recherche en haut de l'écran (toujours visible)
        AnimeSearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChange(it) },  // Déclenche le debounce
            onClear = { viewModel.clearSearch() }                    // Bouton X
        )

        // Zone de contenu principale (prend tout l'espace restant)
        Box(modifier = Modifier.fillMaxSize()) {
            when {

                // CAS 1 : Recherche en cours → afficher un spinner centré
                isSearching -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // CAS 2 : Recherche terminée mais aucun résultat → message informatif
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

                // CAS 3 : Afficher la grille d'animes (liste normale OU résultats de recherche)
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),            // 2 colonnes fixes
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ---- Grille des animes ----
                        // key = { it.mal_id } → optimisation : Compose identifie chaque item
                        // par son ID unique pour minimiser les recompositions lors de mises à jour.
                        items(displayList, key = { it.mal_id }) { anime ->
                            AnimeCard(
                                anime = anime,
                                isFavorite = anime.mal_id in favorisIds,  // O(1) grâce au Set
                                onFavoriteToggle = { favorisViewModel.toggleFavoris(it) },
                                onClick = { selectedAnime = anime }  // Déclenche la navigation détail
                            )
                        }

                        // ---- Item sentinelle pour l'infinite scroll (hors recherche) ----
                        // Cet item n'est ajouté que si on est en mode liste normale ET s'il
                        // reste des pages à charger.
                        // span = GridItemSpan(2) → cet item occupe les 2 colonnes (pleine largeur)
                        if (searchQuery.isEmpty() && hasNextPage) {
                            item(span = { GridItemSpan(2) }) {
                                // LaunchedEffect(Unit) : déclenché une seule fois quand cet item
                                // devient visible à l'écran → charge la page suivante.
                                LaunchedEffect(Unit) {
                                    viewModel.loadTopAnime()
                                }
                                // Indicateur de chargement (visible pendant le chargement réseau)
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