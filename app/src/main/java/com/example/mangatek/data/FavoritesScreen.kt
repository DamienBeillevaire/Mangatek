package com.example.mangatek

import android.os.Build
import androidx.annotation.RequiresApi
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

@RequiresApi(Build.VERSION_CODES.O)   // Requis par AnimeDetailScreen
@Composable
fun FavoritesScreen(favorisViewModel: FavorisViewModel) {

    // Liste des favoris observée en temps réel depuis Room via FavorisViewModel
    val favoris by favorisViewModel.favoris.collectAsState()

    // État local de navigation (identique au pattern utilisé dans AnimeScreen)
    var selectedAnime by remember { mutableStateOf<AnimeData?>(null) }

    // ---- NAVIGATION VERS LE DÉTAIL (return early) ----
    // Si un anime est sélectionné → affiche AnimeDetailScreen et sort de la fonction
    selectedAnime?.let { anime ->
        AnimeDetailScreen(
            anime = anime,
            favorisViewModel = favorisViewModel,
            onBack = { selectedAnime = null }  // Retour à la liste des favoris
        )
        return
    }

    // ---- ÉTAT VIDE ----
    // Affiché quand l'utilisateur n'a aucun favori
    if (favoris.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        // Identique à la grille d'AnimeScreen (2 colonnes, espacement 12dp)
        // mais avec FavoriteCard au lieu d'AnimeCard
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // key = { it.malId } : identifiant unique de chaque item pour optimiser
            // les recompositions lors des suppressions (animation de retrait correcte)
            items(favoris, key = { it.malId }) { fav ->
                FavoriteCard(
                    fav = fav,
                    // Lambda de suppression : passe le FavorisEntity au ViewModel
                    onDelete = { favorisViewModel.supprimerFavoris(fav) }
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
fun FavoriteCard(fav: FavorisEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
