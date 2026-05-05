package com.example.mangatek.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mangatek.data.entity.CompteEntity
import com.example.mangatek.data.repository.CompteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// =============================================================================
// STATE — Représentation de l'état d'authentification
// =============================================================================

/**
 * Data class immuable représentant l'état complet du système d'authentification.
 * Exposé en tant que StateFlow depuis AuthViewModel.
 *
 * Pattern "UI State" : un seul objet contient tout l'état → simplifie la gestion.
 * copy() de Kotlin permet de modifier un seul champ sans recréer tout l'objet.
 *
 * @param isLoading      Vrai pendant une opération async (signIn, signUp)
 * @param currentCompte  Le compte connecté (null si déconnecté)
 * @param error          Message d'erreur à afficher (null si pas d'erreur)
 * @param isLoggedIn     Vrai si l'utilisateur est authentifié
 */
data class AuthState(
    val isLoading: Boolean = false,
    val currentCompte: CompteEntity? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

// =============================================================================
// VIEWMODEL AUTHENTIFICATION — Inscription, connexion, déconnexion
//
// PERSISTANCE DE SESSION :
//   SharedPreferences stocke l'ID du compte connecté entre les sessions.
//   Au démarrage, checkSavedSession() relit cet ID et recharge le compte depuis Room.
//   Cela évite à l'utilisateur de se reconnecter à chaque lancement de l'app.
//
// CYCLE D'AUTHENTIFICATION :
//   Démarrage → checkSavedSession()
//     ├── ID trouvé → compte chargé → isLoggedIn = true → MangatekApp
//     └── Pas d'ID  → AuthState par défaut → LoginScreen
//
//   signIn(email, password) → vérification BDD → saveSession() → isLoggedIn = true
//   signUp(...) → création compte BDD → saveSession() → isLoggedIn = true
//   signOut() → clearSession() → isLoggedIn = false → LoginScreen
// =============================================================================

class AuthViewModel(
    private val compteRepository: CompteRepository,
    private val context: Context   // Nécessaire pour accéder aux SharedPreferences
) : ViewModel() {

    // -------------------------------------------------------------------------
    // STATE FLOW
    // -------------------------------------------------------------------------

    private val _authState = MutableStateFlow(AuthState())
    /** État d'authentification observable par l'UI (MangatekAppWithAuth, LoginScreen, etc.). */
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    // asStateFlow() : wrapping en StateFlow read-only (empêche le cast en MutableStateFlow)

    // -------------------------------------------------------------------------
    // PERSISTANCE — SharedPreferences
    // -------------------------------------------------------------------------

    /**
     * Stockage local simple (clé-valeur) pour persister l'ID du compte entre sessions.
     * Mode PRIVATE : accessible uniquement par cette application.
     *
     * Clé stockée : "current_compte_id" → valeur par défaut -1 si absent.
     *
     * Alternative plus sécurisée : EncryptedSharedPreferences (pas implémentée ici).
     */
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // -------------------------------------------------------------------------
    // INITIALISATION
    // -------------------------------------------------------------------------

    init {
        // Vérifie au démarrage si une session était déjà active
        checkSavedSession()
    }

    /**
     * Recharge la session persistée depuis les SharedPreferences.
     * Si un ID est trouvé, recharge le compte depuis Room et connecte l'utilisateur.
     *
     * Cas d'usage : l'utilisateur avait fermé l'app sans se déconnecter.
     */
    private fun checkSavedSession() {
        viewModelScope.launch {
            val savedCompteId = prefs.getInt("current_compte_id", -1)  // -1 = pas de session

            if (savedCompteId != -1) {
                // ID trouvé → on recharge le compte depuis Room pour vérifier qu'il existe encore
                compteRepository.getCompteById(savedCompteId)?.let { compte ->
                    _authState.value = AuthState(
                        currentCompte = compte,
                        isLoggedIn = true
                    )
                }
                // Si le compte n'existe plus en BDD (let non exécuté), authState reste à sa valeur
                // initiale → l'utilisateur devra se reconnecter
            }
        }
    }

    // -------------------------------------------------------------------------
    // INSCRIPTION
    // -------------------------------------------------------------------------

    /**
     * Crée un nouveau compte utilisateur avec validation côté client puis serveur (Room).
     *
     * VALIDATIONS (dans l'ordre) :
     *  1. username non vide
     *  2. email valide (contient "@") — validation simplifiée
     *  3. password ≥ 6 caractères
     *  4. email non déjà utilisé (vérifié par CompteRepository → BDD)
     *
     * Le mot de passe est hashé (SHA-256) dans CompteRepository avant stockage.
     *
     * @param username Nom d'utilisateur affiché sur le profil
     * @param email    Email unique utilisé pour la connexion
     * @param password Mot de passe en clair (sera hashé avant stockage)
     */
    fun signUp(username: String, email: String, password: String) {
        viewModelScope.launch {
            // Début de chargement + effacement de l'erreur précédente
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            when {
                // Validation 1 : nom d'utilisateur requis
                username.isBlank() -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Le nom d'utilisateur est requis"
                    )
                }
                // Validation 2 : email valide
                email.isBlank() || !email.contains("@") -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Email invalide"
                    )
                }
                // Validation 3 : mot de passe assez long
                password.length < 6 -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Le mot de passe doit contenir au moins 6 caractères"
                    )
                }
                // Toutes les validations passées → tentative de création en BDD
                else -> {
                    // createCompte() retourne null si l'email est déjà utilisé
                    val compteId = compteRepository.createCompte(username, email, password)

                    if (compteId != null) {
                        // Succès : recharger le compte complet depuis Room
                        val compte = compteRepository.getCompteById(compteId.toInt())
                        saveSession(compteId.toInt())  // Persiste la session
                        _authState.value = AuthState(
                            isLoading = false,
                            currentCompte = compte,
                            isLoggedIn = true    // ← déclenche navigation vers MangatekApp
                        )
                    } else {
                        // Échec : email déjà utilisé
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = "Cet email est déjà utilisé"
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // CONNEXION
    // -------------------------------------------------------------------------

    /**
     * Authentifie l'utilisateur avec son email et mot de passe.
     * La vérification se fait entièrement en local (Room) :
     *  1. Recherche du compte par email
     *  2. Comparaison du hash SHA-256 du mot de passe saisi avec le hash stocké
     *
     * @param email    Email de l'utilisateur
     * @param password Mot de passe en clair (hashé dans CompteRepository)
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            // CompteRepository.login() gère le hashing et la comparaison
            val compte = compteRepository.login(email, password)

            if (compte != null) {
                // Authentification réussie
                saveSession(compte.id)
                _authState.value = AuthState(
                    isLoading = false,
                    currentCompte = compte,
                    isLoggedIn = true   // ← déclenche navigation vers MangatekApp
                )
            } else {
                // Échec : email inconnu ou mot de passe incorrect
                // On ne précise pas lequel pour des raisons de sécurité (pas de user enumeration)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Email ou mot de passe incorrect"
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // DÉCONNEXION
    // -------------------------------------------------------------------------

    /**
     * Déconnecte l'utilisateur :
     *  1. Supprime la session des SharedPreferences
     *  2. Réinitialise l'état → isLoggedIn = false → retour à LoginScreen
     *
     * Note : les données Room (favoris, compte) ne sont PAS supprimées.
     * L'utilisateur peut se reconnecter et retrouver ses favoris.
     */
    fun signOut() {
        clearSession()
        _authState.value = AuthState(isLoggedIn = false)   // Reset complet
    }

    /**
     * Efface le message d'erreur courant.
     * Appelé avant une nouvelle tentative ou quand l'utilisateur ferme le message.
     */
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    // -------------------------------------------------------------------------
    // GESTION DE SESSION (SharedPreferences)
    // -------------------------------------------------------------------------

    /** Sauvegarde l'ID du compte dans les SharedPreferences. */
    private fun saveSession(compteId: Int) {
        prefs.edit().putInt("current_compte_id", compteId).apply()
        // .apply() → asynchrone (ne bloque pas le thread principal)
        // .commit() → synchrone (à éviter sur le thread principal)
    }

    /** Efface toutes les préférences d'auth (utilisé à la déconnexion). */
    private fun clearSession() {
        prefs.edit().clear().apply()
    }
}

// =============================================================================
// FACTORY — Permet d'injecter repository et context dans AuthViewModel
//
// Sans Factory : ViewModelProvider ne saurait pas quels arguments passer au constructeur.
// Avec Factory : on définit explicitement comment créer l'instance.
// =============================================================================

class AuthViewModelFactory(
    private val compteRepository: CompteRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(compteRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}