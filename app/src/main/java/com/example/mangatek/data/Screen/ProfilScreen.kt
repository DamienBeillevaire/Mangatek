package com.example.mangatek.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mangatek.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

// =============================================================================
// ÉCRAN PROFIL — Informations du compte connecté + déconnexion
//
// STRUCTURE VISUELLE :
//   ┌─────────────────────────┐
//   │     [Avatar 100dp]      │  ← Surface circulaire avec icône Person
//   │     Nom d'utilisateur   │
//   │     📧 email            │
//   ├─────────────────────────┤
//   │     ─────────────       │  ← Divider
//   │  ┌───────────────────┐  │
//   │  │ Informations      │  │  ← Card avec ID et date d'inscription
//   │  │ ID : #1           │  │
//   │  │ Membre depuis :   │  │
//   │  └───────────────────┘  │
//   │       (espace flex)     │  ← Spacer(weight=1f) pousse le bouton vers le bas
//   │  [Se déconnecter 🔴]    │  ← Bouton rouge en bas de page
//   └─────────────────────────┘
//
// DONNÉES AFFICHÉES :
//   Toutes les données viennent de authState.currentCompte (CompteEntity en mémoire).
//   Aucun appel réseau ou BDD supplémentaire nécessaire.
// =============================================================================

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    // Raccourci vers le compte connecté (peut être null théoriquement, mais
    // cet écran n'est affiché que si isLoggedIn=true → currentCompte != null)
    val compte = authState.currentCompte

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ---- AVATAR ----
        // Surface carrée de 100dp avec coins arrondis (shapes.large) → presque circulaire
        // Pas de vraie image utilisateur → icône Person comme placeholder
        Surface(
            modifier = Modifier.size(100.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer   // Fond coloré
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- NOM D'UTILISATEUR ----
        // Opérateur Elvis ?: → affiche "Utilisateur" si currentCompte est null
        Text(
            text = compte?.username ?: "Utilisateur",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ---- EMAIL avec icône ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = compte?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Séparateur horizontal
        Divider()

        Spacer(modifier = Modifier.height(24.dp))

        // ---- CARTE INFORMATIONS DU COMPTE ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant  // Fond légèrement coloré
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Informations du compte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ID du compte
                InfoRow(label = "ID", value = "#${compte?.id ?: 0}")

                Spacer(modifier = Modifier.height(8.dp))

                // Date d'inscription — formatée à partir du timestamp (Long → String)
                compte?.createdAt?.let { timestamp ->
                    // SimpleDateFormat : formatage de la date selon la locale française
                    // Date(timestamp) : convertit milliseconds → objet Date Java
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                    val date = dateFormat.format(Date(timestamp))
                    InfoRow(label = "Membre depuis", value = date)
                }
            }
        }

        // Spacer flexible : pousse le bouton de déconnexion vers le bas de l'écran
        // weight(1f) : ce Spacer prend tout l'espace vertical restant
        Spacer(modifier = Modifier.weight(1f))

        // ---- BOUTON DE DÉCONNEXION ----
        // containerColor = error (rouge) → signale visuellement une action destructive
        Button(
            onClick = { authViewModel.signOut() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            // AutoMirrored : l'icône est retournée horizontalement en mode RTL (langues arabe, hébreu)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Se déconnecter")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// =============================================================================
// COMPOSABLE PRIVÉ — Ligne label/valeur dans la carte d'informations
// =============================================================================

/**
 * Ligne avec label à gauche et valeur à droite (layout type "formulaire en lecture").
 * Réutilisé pour l'ID et la date d'inscription.
 *
 * @param label Libellé descriptif (ex: "ID", "Membre depuis")
 * @param value Valeur correspondante (ex: "#1", "01/01/2024")
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween  // Label à gauche, valeur à droite
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant   // Grisé → moins important
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium   // Légèrement plus gras → valeur mise en avant
        )
    }
}
