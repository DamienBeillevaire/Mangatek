package com.example.mangatek.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mangatek.viewmodel.AuthViewModel

// =============================================================================
// ÉCRAN DE CONNEXION — Formulaire d'authentification
//
// ÉTAT LOCAL (remember{}) vs ÉTAT GLOBAL (ViewModel) :
//   - email, password, passwordVisible : état local (données de saisie temporaires)
//     → remember{} suffit, pas besoin de survivre aux rotations
//     → réinitialisés à chaque recréation de l'écran (comportement voulu)
//   - authState (isLoading, error, isLoggedIn) : état global dans AuthViewModel
//     → survit aux rotations, partagé avec d'autres composables
//
// NAVIGATION :
//   - Connexion réussie → authState.isLoggedIn = true → MangatekAppWithAuth
//     recompose et affiche MangatekApp (géré en dehors de cet écran)
//   - Clic "S'inscrire" → onNavigateToSignUp() → authScreen = AuthScreen.SignUp
//     dans MangatekAppWithAuth
//
// UX — ACCESSIBILITÉ CLAVIER :
//   ImeAction.Next + focusManager.moveFocus(Down) → Tab entre les champs
//   ImeAction.Done → soumet le formulaire si valide
// =============================================================================

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit   // Callback vers MangatekAppWithAuth pour changer d'écran auth
) {
    // État global d'auth observé depuis le ViewModel
    val authState by viewModel.authState.collectAsState()

    // ---- ÉTAT LOCAL — Données de saisie (vie limitée à cet écran) ----
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Contrôle l'affichage du mot de passe en clair ou masqué (•••)
    var passwordVisible by remember { mutableStateOf(false) }

    // FocusManager : permet de déplacer le focus clavier programmatiquement
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center   // Contenu centré verticalement
    ) {

        // ---- EN-TÊTE ----
        Text(
            text = "Mangatek",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connectez-vous à votre compte",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ---- CHAMP EMAIL ----
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,  // Clavier optimisé pour email (@, .com, etc.)
                imeAction = ImeAction.Next          // Bouton "Suivant" sur le clavier
            ),
            keyboardActions = KeyboardActions(
                // ImeAction.Next → déplace le focus vers le champ suivant (mot de passe)
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- CHAMP MOT DE PASSE ----
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },

            // Icône œil pour afficher/masquer le mot de passe
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility     // 👁️ (visible)
                        else Icons.Default.VisibilityOff,                                // 🙈 (masqué)
                        contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                    )
                }
            },

            // VisualTransformation : contrôle l'affichage du texte dans le champ
            // PasswordVisualTransformation() → affiche des bullets (•••)
            // VisualTransformation.None → affiche le texte en clair
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,  // Désactive la suggestion automatique
                imeAction = ImeAction.Done             // Bouton "Valider" sur le clavier
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()   // Ferme le clavier
                    // Soumet le formulaire uniquement si les deux champs sont remplis
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.signIn(email, password)
                    }
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ---- MESSAGE D'ERREUR ----
        // Affiché uniquement si authState.error != null (let = safe call)
        // Ex: "Email ou mot de passe incorrect"
        authState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,      // Rouge
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---- BOUTON DE CONNEXION ----
        Button(
            onClick = { viewModel.signIn(email, password) },
            // Désactivé si : champs vides OU chargement en cours (évite les doubles soumissions)
            enabled = email.isNotBlank() && password.isNotBlank() && !authState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Affichage conditionnel : spinner pendant le chargement, texte sinon
            if (authState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary  // Blanc sur fond primary
                )
            } else {
                Text("Se connecter")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- LIEN VERS L'INSCRIPTION ----
        // TextButton : bouton sans fond ni bordure (style "lien")
        TextButton(onClick = onNavigateToSignUp) {
            Text("Pas encore de compte ? S'inscrire")
        }
    }
}
