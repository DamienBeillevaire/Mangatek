package com.example.mangatek

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mangatek.data.ViewModel.FavorisViewModel

// =============================================================================
// ÉCRAN DÉTAIL — Fiche complète d'un anime
//
// Affiché en remplacement de AnimeScreen quand l'utilisateur clique sur une carte.
// Navigation gérée par un état local dans AnimeScreen (pas de NavController) :
//   selectedAnime != null → AnimeDetailScreen s'affiche
//   onBack()             → selectedAnime = null → retour à la grille
//
// STRUCTURE VISUELLE :
//   ┌─────────────────────────┐
//   │ TopAppBar (titre + ♡)   │
//   ├─────────────────────────┤
//   │ Image hero (280dp)      │
//   │   + dégradé bas→haut    │
//   ├─────────────────────────┤
//   │ Titre complet           │
//   │ Chips infos (score,ep..)│
//   │ Genres                  │
//   │ Synopsis (scrollable)   │
//   └─────────────────────────┘
// =============================================================================

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)   // TopAppBar est encore en API expérimentale
@Composable
fun AnimeDetailScreen(
    anime: AnimeData,                    // Données de l'anime à afficher (passées depuis la grille)
    favorisViewModel: FavorisViewModel,  // Pour lire/modifier l'état favori
    onBack: () -> Unit                   // Callback → remet selectedAnime à null dans AnimeScreen
) {
    // Observe le Set d'IDs favoris pour déduire si CET anime est favori
    val favorisIds by favorisViewModel.favorisIds.collectAsState()
    val isFavorite = anime.mal_id in favorisIds  // Vérification O(1) dans un Set<Int>

    // État de scroll pour le contenu (titre, chips, synopsis)
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                // Titre tronqué à 1 ligne si trop long
                title = {
                    Text(
                        text = anime.displayTitle,   // Extension property → EN ou JP
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                // Bouton retour ←
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                // Bouton favori ♡ / ♥ en haut à droite
                actions = {
                    IconButton(onClick = { favorisViewModel.toggleFavoris(anime) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite    // ♥ rouge
                            else Icons.Outlined.FavoriteBorder,                    // ♡ neutre
                            contentDescription = "Favoris",
                            tint = if (isFavorite) Color.Red
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->  // innerPadding évite le chevauchement avec la TopAppBar

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)   // Tout le contenu est scrollable
                .padding(innerPadding)
        ) {

            // ------------------------------------------------------------------
            // IMAGE HERO — Grande image avec dégradé de fondu en bas
            // ------------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                // Image principale (Coil charge l'image depuis l'URL de façon asynchrone)
                AsyncImage(
                    // Priorité : grande image → image standard (fallback si large_image_url null)
                    model = anime.images.jpg.large_image_url ?: anime.images.jpg.image_url,
                    contentDescription = anime.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop   // Recadrage centré (pas de déformation)
                )

                // Dégradé vertical superposé sur l'image :
                // Transparent en haut → couleur de fond en bas
                // Crée une transition visuelle douce entre l'image et le texte en dessous
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    // alpha=0.9 → quasi opaque en bas (fond de l'app)
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                                ),
                                startY = 120f   // Le dégradé commence à 120px depuis le haut
                            )
                        )
                )
            }

            // ------------------------------------------------------------------
            // CONTENU TEXTUEL — Titre, chips, genres, synopsis
            // ------------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                // Titre complet (peut être sur plusieurs lignes ici, contrairement à la TopAppBar)
                Text(
                    text = anime.displayTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chips d'informations rapides : ⭐ score | 📺 épisodes | 📅 année | statut
                InfoChipsRow(anime = anime)

                Spacer(modifier = Modifier.height(16.dp))

                // Genres — n'affiche la section que si la liste n'est pas vide
                if (!anime.genres.isNullOrEmpty()) {
                    Text(
                        text = "Genres",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    GenreChips(genres = anime.genres)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Synopsis — n'affiche la section que si le synopsis est présent
                if (!anime.synopsis.isNullOrBlank()) {
                    Text(
                        text = "Synopsis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = anime.synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// =============================================================================
// COMPOSABLES PRIVÉS — Sous-composants réutilisables de cet écran
// =============================================================================

/**
 * Ligne horizontale de "chips" d'information (score, épisodes, année, statut).
 * Chaque champ nullable est affiché uniquement s'il est présent (let).
 * Les chips défilent horizontalement si trop nombreux (Row sans scroll → peut dépasser).
 */
@Composable
private fun InfoChipsRow(anime: AnimeData) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // let{} → exécuté seulement si la valeur n'est pas null
        anime.score?.let    { InfoChip(label = "⭐ $it") }
        anime.episodes?.let { InfoChip(label = "📺 $it épisodes") }
        anime.year?.let     { InfoChip(label = "📅 $it") }
        anime.status?.let   { InfoChip(label = it) }
    }
}

/**
 * Chip individuelle avec fond coloré (secondaryContainer).
 * Utilisée pour afficher une information courte avec un style "badge".
 *
 * @param label Texte à afficher dans la chip (ex: "⭐ 8.5", "📺 24 épisodes")
 */
@Composable
private fun InfoChip(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),              // Coins très arrondis → forme pill
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Rangée de chips de genres (fond primaryContainer).
 * Affiche au maximum 5 genres pour éviter le débordement horizontal.
 * take(5) → prend les 5 premiers éléments de la liste.
 *
 * @param genres Liste des genres de l'anime (ex: [Action, Adventure, Fantasy])
 */
@Composable
private fun GenreChips(genres: List<Genre>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        genres.take(5).forEach { genre ->     // Max 5 genres affichés
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer   // Couleur différente des InfoChip
            ) {
                Text(
                    text = genre.name,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}