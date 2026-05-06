package com.example.mangatek

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mangatek.data.entity.FavorisEntity
import com.example.mangatek.data.ViewModel.FavorisViewModel

// =============================================================================
// ÉCRAN FAVORIS — Liste des animes favoris de l'utilisateur connecté
//
// CET ÉCRAN A DEUX MODES, comme AnimeScreen :
//   Mode liste  : grille des favoris (FavoriteCard)
//   Mode détail : AnimeDetailScreen (return early si selectedAnime != null)
//
// PARTICULARITÉ — Conversion FavorisEntity → AnimeData :
//   Les favoris sont stockés en tant que FavorisEntity (Room).
//   Pour afficher AnimeDetailScreen, il faut un AnimeData.
//   La conversion se fait dans FavoriteCard.onClick via un AnimeData reconstruit
//   à partir des champs stockés dans FavorisEntity.
//
// SOURCE DE DONNÉES :
//   FavorisViewModel.favoris → StateFlow<List<FavorisEntity>> → collectAsState()
//   Room observe la BDD et réémet automatiquement quand un favori est ajouté/supprimé
//   depuis n'importe quel autre écran (AnimeScreen, AnimeDetailScreen).
// =============================================================================

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FavoritesScreen(favorisViewModel: FavorisViewModel, homeViewModel: HomeViewModel) {

    val favoris by favorisViewModel.favoris.collectAsState()
    val favoriteDetail by homeViewModel.favoriteDetailAnime.collectAsState()
    val isDetailLoading by homeViewModel.isFavoriteDetailLoading.collectAsState()

    // ---- NAVIGATION VERS LE DÉTAIL ----
    favoriteDetail?.let { anime ->
        AnimeDetailScreen(
            anime = anime,
            favorisViewModel = favorisViewModel,
            onBack = { homeViewModel.clearFavoriteDetail() }
        )
        return
    }

    // ---- CHARGEMENT DU DÉTAIL EN COURS ----
    if (isDetailLoading) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // ---- ÉTAT VIDE ----
    if (favoris.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Aucun favori", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Appuie sur ♡ pour en ajouter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // ---- GRILLE DES FAVORIS ----
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding()
        ) {
            items(favoris, key = { it.malId }) { fav ->
                FavoriteCard(
                    fav = fav,
                    onDelete = { favorisViewModel.supprimerFavoris(fav) },
                    onClick = { homeViewModel.loadFavoriteDetail(fav.malId) }
                )
            }
        }
    }
}

// =============================================================================
// COMPOSANT CARTE FAVORI — Version simplifiée de AnimeCard pour l'écran favoris
//
// DIFFÉRENCES avec AnimeCard :
//   - Reçoit un FavorisEntity (données Room) au lieu d'AnimeData (données API)
//   - Pas de bouton toggle ♡ (on est déjà dans les favoris) → bouton 🗑️ à la place
//   - Pas de onClick sur la carte entière (pas de navigation vers le détail depuis ici)
//
// DONNÉES STOCKÉES vs DONNÉES API :
//   FavorisEntity ne contient que : title, imageUrl, score, addedAt.
//   Synopsis, genres, épisodes etc. ne sont PAS stockés → pour afficher le détail,
//   il faudrait faire un appel API supplémentaire (non implémenté actuellement).
// =============================================================================

/**
 * Carte affichant un anime favori dans FavoritesScreen.
 *
 * @param fav      Entité Room contenant les données du favori (titre, image, score)
 * @param onDelete Callback déclenché au clic sur 🗑️ → appelle FavorisViewModel.supprimerFavoris()
 */
@Composable
fun FavoriteCard(fav: FavorisEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            // Image du favori (URL stockée dans Room au moment de l'ajout en favori)
            AsyncImage(
                model = fav.imageUrl,
                contentDescription = fav.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(8.dp)) {
                // Titre (peut être sur 2 lignes maximum)
                Text(
                    text = fav.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score — affiché uniquement si non null
                    fav.score?.let {
                        Text(
                            text = "⭐ $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bouton suppression 🗑️ (rouge = action destructive)
                    // Contrairement à AnimeCard qui a un toggle ♡,
                    // ici on ne peut que supprimer (on est déjà dans les favoris)
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error   // Rouge = couleur d'erreur/danger
                        )
                    }
                }
            }
        }
    }
}
