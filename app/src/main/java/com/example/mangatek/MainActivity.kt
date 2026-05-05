package com.example.mangatek

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mangatek.data.ViewModel.FavorisViewModel
import com.example.mangatek.data.database.MangatekDatabase
import com.example.mangatek.data.repository.CompteRepository
import com.example.mangatek.data.repository.FavorisRepository
import com.example.mangatek.ui.auth.LoginScreen
import com.example.mangatek.ui.auth.SignUpScreen
import com.example.mangatek.ui.profile.ProfileScreen
import com.example.mangatek.ui.theme.MangatekTheme
import com.example.mangatek.viewmodel.AuthViewModel
import com.example.mangatek.viewmodel.AuthViewModelFactory

// =============================================================================
// POINT D'ENTRÉE — Activity principale de l'application
//
// ARCHITECTURE GLOBALE :
//
//   MainActivity
//     ├── MangatekDatabase (Room, singleton)
//     │     ├── CompteDao  → CompteRepository  → AuthViewModel
//     │     └── FavorisDao → FavorisRepository → FavorisViewModel
//     │
//     ├── HomeViewModel (chargement/recherche animes)
//     │
//     └── MangatekAppWithAuth (Composable racine)
//           ├── [Non connecté] LoginScreen / SignUpScreen
//           └── [Connecté]     MangatekApp (NavigationSuiteScaffold)
//                               ├── AnimeScreen
//                               ├── FavoritesScreen
//                               └── ProfileScreen
//
// GESTION DES VIEWMODELS :
//   - HomeViewModel et AuthViewModel : créés par viewModels() → cycle de vie Activity
//   - FavorisViewModel : créé par remember(compteId) → recréé si l'utilisateur change
// =============================================================================

class MainActivity : AppCompatActivity() {

    // ---- ViewModels Activity-scoped ----
    // viewModels() délègue la création au ViewModelStore → survit aux rotations

    /** ViewModel pour le chargement et la recherche d'animes (API Jikan). */
    private val homeViewModel: HomeViewModel by viewModels()

    // ---- Initialisation paresseuse de la base de données ----
    // by lazy{} → créé à la première utilisation, pas au démarrage de l'Activity

    /** Instance singleton de la base de données Room. */
    private val database by lazy { MangatekDatabase.getDatabase(applicationContext) }

    /** Repository d'accès aux données de comptes utilisateurs. */
    private val compteRepository by lazy { CompteRepository(database.compteDao()) }

    /** Repository d'accès aux données de favoris. */
    private val favorisRepository by lazy { FavorisRepository(database.favorisDao()) }

    /**
     * ViewModel d'authentification — nécessite une Factory car son constructeur
     * prend des paramètres (compteRepository, context) que viewModels() ne peut
     * pas fournir seul. AuthViewModelFactory crée l'instance avec les bons paramètres.
     */
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(compteRepository, applicationContext)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // enableEdgeToEdge() : étend le contenu sous les barres système (status bar, nav bar)
        // Le padding est ensuite géré par Scaffold/innerPadding dans chaque écran.
        enableEdgeToEdge()

        // setContent{} : remplace le setContentView() classique par du Compose
        setContent {
            MangatekTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MangatekAppWithAuth(
                        homeViewModel = homeViewModel,
                        authViewModel = authViewModel,
                        favorisRepository = favorisRepository
                    )
                }
            }
        }
    }
}

// =============================================================================
// COMPOSABLE RACINE — Gestion du flux d'authentification
//
// LOGIQUE :
//   1. Observe authState depuis AuthViewModel
//   2. Si l'utilisateur N'EST PAS connecté → affiche Login ou SignUp
//   3. Si l'utilisateur EST connecté → affiche MangatekApp (l'app principale)
//
// TRANSITION AUTOMATIQUE :
//   Quand authViewModel.signIn() réussit, authState.isLoggedIn passe à true,
//   ce qui provoque une recomposition et affiche MangatekApp sans action supplémentaire.
// =============================================================================

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MangatekAppWithAuth(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    favorisRepository: FavorisRepository
) {
    val authState by authViewModel.authState.collectAsState()

    // État local : quelle page d'auth afficher (Login ou SignUp)
    // rememberSaveable : survit aux rotations d'écran (contrairement à remember)
    var authScreen by rememberSaveable { mutableStateOf<AuthScreen>(AuthScreen.Login) }

    if (!authState.isLoggedIn || authState.currentCompte == null) {
        // ---- UTILISATEUR NON CONNECTÉ ----
        when (authScreen) {
            AuthScreen.Login -> {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToSignUp = { authScreen = AuthScreen.SignUp }
                )
            }
            AuthScreen.SignUp -> {
                SignUpScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { authScreen = AuthScreen.Login }
                )
            }
        }
    } else {
        // ---- UTILISATEUR CONNECTÉ ----

        // Récupère l'ID du compte connecté pour scoper les favoris
        val compteId = authState.currentCompte!!.id

        /**
         * FavorisViewModel créé avec remember(compteId) :
         * - Si compteId ne change pas → même instance réutilisée
         * - Si compteId change (changement de compte) → nouvelle instance créée
         * Cela garantit que chaque utilisateur ne voit que SES favoris.
         *
         * Note : ce ViewModel n'est PAS géré par le ViewModelStore de l'Activity,
         * il sera détruit quand MangatekAppWithAuth quitte la composition.
         */
        val favorisViewModel = remember(compteId) {
            FavorisViewModel(favorisRepository, compteId)
        }

        MangatekApp(
            homeViewModel = homeViewModel,
            authViewModel = authViewModel,
            favorisViewModel = favorisViewModel
        )
    }
}

// =============================================================================
// COMPOSABLE APP PRINCIPALE — Navigation entre les 3 onglets
//
// Utilise NavigationSuiteScaffold (Material3 Adaptive) qui affiche automatiquement :
//   - Une BottomNavigationBar sur téléphone (portrait)
//   - Une NavigationRail sur tablette ou paysage
//   - Un NavigationDrawer sur grand écran
// =============================================================================

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MangatekApp(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    favorisViewModel: FavorisViewModel
) {
    // État de navigation : quel onglet est actif
    // rememberSaveable → survit aux rotations (l'onglet sélectionné est conservé)
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MENU) }

    NavigationSuiteScaffold(
        // Construction des items de navigation à partir de l'enum AppDestinations
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        // Contenu affiché selon l'onglet sélectionné
        Scaffold(modifier = Modifier.fillMaxSize()) { _ ->  // innerPadding ignoré (géré par chaque écran)
            when (currentDestination) {

                // Onglet 1 : Grille d'animes + recherche
                AppDestinations.MENU -> AnimeScreen(
                    viewModel = homeViewModel,
                    favorisViewModel = favorisViewModel
                )

                // Onglet 2 : Liste des animes favoris de l'utilisateur
                AppDestinations.FAVORITES -> FavoritesScreen(
                    favorisViewModel = favorisViewModel
                )

                // Onglet 3 : Profil utilisateur + déconnexion
                AppDestinations.PROFILE -> ProfileScreen(
                    authViewModel = authViewModel
                )
            }
        }
    }
}

// =============================================================================
// ENUMS — Définition des destinations de navigation
// =============================================================================

/**
 * Destinations de la navigation principale (onglets).
 * Chaque entrée définit :
 *  - label : texte affiché sous l'icône
 *  - icon  : icône Material Icons affichée dans la barre de navigation
 *
 * L'ordre des entrées détermine l'ordre des onglets dans la NavigationBar.
 */
enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    MENU("Home", Icons.Default.Menu),             // Écran principal (liste animes)
    FAVORITES("Favorites", Icons.Default.Favorite), // Écran favoris
    PROFILE("Profile", Icons.Default.AccountBox),   // Écran profil
}

/**
 * États possibles du flux d'authentification.
 * Contrôle quel écran afficher quand l'utilisateur n'est pas connecté.
 */
enum class AuthScreen {
    Login,    // Écran de connexion (par défaut)
    SignUp    // Écran d'inscription
}