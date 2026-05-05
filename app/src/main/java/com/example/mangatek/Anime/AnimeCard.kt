package com.example.mangatek

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// =============================================================================
// COMPOSANT CARTE ANIME — Élément individuel de la grille dans AnimeScreen
//
// DESIGN :
//   ┌──────────────────┐
//   │  Image (180dp)   │  ← AsyncImage (Coil)
//   ├──────────────────┤
//   │ Titre (2 lignes) │  ← TextOverflow.Ellipsis si trop long
//   │ ⭐ score   [♡/♥] │  ← score nullable + IconToggleButton favori
//   └──────────────────┘
//
// PATTERN "STATELESS COMPOSABLE" :
//   AnimeCard ne gère AUCUN état en interne.
//   - isFavorite      : reçu depuis AnimeScreen (qui le lit depuis FavorisViewModel)
//   - onFavoriteToggle: remonte l'événement vers AnimeScreen → FavorisViewModel
//   - onClick         : remonte l'événement vers AnimeScreen → selectedAnime
//
//   Avantage : la carte est réutilisable et testable sans ViewModel.
//   C'est le pattern recommandé dans l'architecture Compose (Unidirectional Data Flow).
// =============================================================================

/**
 * Carte affichant un anime dans la grille principale.
 *
 * @param anime            Données de l'anime à afficher
 * @param isFavorite       État favori actuel — vient de FavorisViewModel.favorisIds (Set<Int>)
 * @param onFavoriteToggle Callback déclenché au clic sur le bouton ♡/♥
 *                         → reçoit l'AnimeData et le passe à FavorisViewModel.toggleFavoris()
 * @param onClick          Callback déclenché au clic sur la carte entière
 *                         → met à jour selectedAnime dans AnimeScreen → affiche AnimeDetailScreen
 */
@Composable
fun AnimeCard(
    anime: AnimeData,
    isFavorite: Boolean,
    onFavoriteToggle: (AnimeData) -> Unit,
    onClick: () -> Unit
) {
    // Card cliquable (toute la surface est cliquable via onClick)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),   // Ombre légère pour effet de profondeur
        shape = MaterialTheme.shapes.medium              // Coins arrondis (défini dans le thème)
    ) {
        Column {

            // ------------------------------------------------------------------
            // IMAGE — Chargement asynchrone avec Coil
            // ------------------------------------------------------------------
            AsyncImage(
                // Priorité : grande image (400px) → image standard (225px) si large_image_url null
                model = anime.images.jpg.large_image_url ?: anime.images.jpg.image_url,
                contentDescription = anime.displayTitle,   // Accessibilité (TalkBack)
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),           // Hauteur fixe → toutes les cartes ont la même taille
                contentScale = ContentScale.Crop  // Recadrage centré, pas de déformation
                // Coil gère automatiquement : cache mémoire, cache disque, placeholder par défaut
            )

            // ------------------------------------------------------------------
            // CONTENU TEXTUEL — Titre + score + bouton favori
            // ------------------------------------------------------------------
            Column(modifier = Modifier.padding(8.dp)) {

                // Titre avec troncature si > 2 lignes
                Text(
                    text = anime.displayTitle,    // Extension property : EN si dispo, sinon JP
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,                             // Maximum 2 lignes
                    overflow = TextOverflow.Ellipsis          // "..." si texte trop long
                )

                // Ligne inférieure : score à gauche, favori à droite
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,  // Espace entre score et ♡
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score — affiché uniquement si non null (let = safe call)
                    anime.score?.let {
                        Text(
                            text = "⭐ $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bouton toggle favori ♡ / ♥
                    // IconToggleButton vs IconButton :
                    //   IconToggleButton gère un état "checked" et applique un fond circulaire
                    //   quand checked=true (comportement Material3).
                    //   checked=isFavorite : état visuel piloté par le ViewModel (source de vérité)
                    //   onCheckedChange : ne reçoit pas la nouvelle valeur car on délègue au ViewModel
                    IconToggleButton(
                        checked = isFavorite,
                        onCheckedChange = { onFavoriteToggle(anime) }  // Ignore le Boolean reçu
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite      // ♥ plein (favori)
                            else Icons.Outlined.FavoriteBorder,                       // ♡ vide (non favori)
                            contentDescription = "Favoris",
                            tint = if (isFavorite) Color.Red
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
