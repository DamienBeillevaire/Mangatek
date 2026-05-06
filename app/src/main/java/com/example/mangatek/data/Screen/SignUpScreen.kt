package com.example.mangatek.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
// ÉCRAN D'INSCRIPTION — Formulaire de création de compte
//
// VALIDATIONS CÔTÉ UI (avant d'appeler le ViewModel) :
//   1. displayName non vide
//   2. email non vide
//   3. password.length >= 6  → isError sur le champ + supportingText rouge
//   4. confirmPassword == password  → isError + supportingText rouge
//   → isFormValid contrôle l'activation du bouton "S'inscrire"
//
// VALIDATIONS CÔTÉ VIEWMODEL (après appel) :
//   5. email contient "@" (validation supplémentaire)
//   6. email non déjà utilisé (vérifié en BDD)
//   → résultat dans authState.error
//
// CHAMPS DU FORMULAIRE :
//   [Nom d'utilisateur] → [Email] → [Mot de passe] → [Confirmation] → [Bouton]
//   Navigation clavier : ImeAction.Next entre les champs, ImeAction.Done sur le dernier
// =============================================================================

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit   // Retour vers LoginScreen
) {
    val authState by viewModel.authState.collectAsState()

    // ---- ÉTAT LOCAL — Données de saisie ----
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // ---- VALIDATION EN TEMPS RÉEL ----
    // Recalculé à chaque recomposition (chaque frappe) → pas de side effects
    val passwordsMatch = password == confirmPassword
    val isFormValid = displayName.isNotBlank() &&
            email.isNotBlank() &&
            password.length >= 6 &&
            passwordsMatch

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // ---- EN-TÊTE ----
        Text(
            text = "Créer un compte",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Rejoignez Mangatek",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ---- CHAMP NOM D'UTILISATEUR ----
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Nom d'utilisateur") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- CHAMP EMAIL ----
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
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
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            // isError=true : bordure rouge + texte rouge si condition remplie
            isError = password.isNotEmpty() && password.length < 6,
            // supportingText : texte d'aide/erreur sous le champ
            supportingText = {
                if (password.isNotEmpty() && password.length < 6) {
                    Text("Au moins 6 caractères")   // Affiché en rouge automatiquement si isError=true
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- CHAMP CONFIRMATION MOT DE PASSE ----
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmer le mot de passe") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Masquer" else "Afficher"
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done   // Dernier champ → soumet le formulaire
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    // Soumet seulement si tout le formulaire est valide
                    if (isFormValid) {
                        viewModel.signUp(displayName, email, password)
                    }
                    // Note : confirmPassword N'est PAS passé au ViewModel
                    // La vérification de correspondance est faite côté UI uniquement
                }
            ),
            singleLine = true,
            // Erreur si l'utilisateur a saisi quelque chose ET que les mots de passe diffèrent
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            supportingText = {
                if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                    Text("Les mots de passe ne correspondent pas")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ---- MESSAGE D'ERREUR VIEWMODEL ----
        // Erreurs provenant du ViewModel (email déjà utilisé, etc.)
        authState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---- BOUTON D'INSCRIPTION ----
        Button(
            onClick = { viewModel.signUp(displayName, email, password) },
            // Double protection : formulaire valide ET pas de chargement en cours
            enabled = isFormValid && !authState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("S'inscrire")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lien retour vers LoginScreen
        TextButton(onClick = onNavigateToLogin) {
            Text("Déjà un compte ? Se connecter")
        }
    }
}
