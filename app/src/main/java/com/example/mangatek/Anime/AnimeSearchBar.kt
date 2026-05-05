package com.example.mangatek

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

// =============================================================================
// COMPOSANT BARRE DE RECHERCHE — Champ de texte stylisé pour la recherche d'animes
//
// COMPOSANT STATELESS : ne gère pas l'état du texte en interne.
// La valeur "query" vient du HomeViewModel (searchQuery StateFlow),
// les callbacks remontent les événements vers le ViewModel.
//
// CYCLE DE VIE DE LA RECHERCHE :
//   Frappe → onQueryChange() → HomeViewModel.onSearchQueryChange()
//             → debounce 400ms → API Jikan → searchResults mis à jour
//   Clic X → onClear() → HomeViewModel.clearSearch() → retour à la liste principale
// =============================================================================

/**
 * Barre de recherche réutilisable affichée en haut de AnimeScreen.
 *
 * @param query         Texte actuellement affiché (vient de HomeViewModel.searchQuery)
 * @param onQueryChange Appelé à chaque frappe (connecté à HomeViewModel.onSearchQueryChange)
 * @param onClear       Appelé au clic sur le bouton X (connecté à HomeViewModel.clearSearch)
 * @param modifier      Modificateur optionnel pour personnaliser l'apparence depuis l'extérieur
 */
@Composable
fun AnimeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier   // Paramètre par défaut = Modifier vide → bonne pratique Compose
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,   // Chaque frappe appelle immédiatement le callback
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),

        // Texte grisé affiché quand le champ est vide
        placeholder = { Text("Rechercher un anime...") },

        // Icône 🔍 à gauche — toujours visible
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Rechercher")
        },

        // Icône ✕ à droite — visible UNIQUEMENT si du texte est saisi
        // Permet d'effacer la recherche en un clic
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                }
            }
            // Si query est vide : pas d'icône → le slot reste vide (Compose l'ignore)
        },

        // ImeAction.Search : affiche le bouton "Rechercher" sur le clavier virtuel
        // (remplace la touche Entrée par une loupe sur la plupart des claviers)
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),

        singleLine = true,   // Empêche le retour à la ligne → le texte défile horizontalement

        // Forme pill avec coins arrondis (16dp) — plus moderne qu'un rectangle
        shape = RoundedCornerShape(16.dp),

        // Personnalisation des couleurs de bordure selon l'état du focus
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,    // Couleur quand actif
            unfocusedBorderColor = MaterialTheme.colorScheme.outline   // Couleur quand inactif
        )
    )
}
