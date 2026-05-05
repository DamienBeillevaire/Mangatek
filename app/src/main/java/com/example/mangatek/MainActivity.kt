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

class MainActivity : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    // Initialiser la base de données et les repositories
    private val database by lazy { MangatekDatabase.getDatabase(applicationContext) }
    private val compteRepository by lazy { CompteRepository(database.compteDao()) }
    private val favorisRepository by lazy { FavorisRepository(database.favorisDao()) }

    // ViewModel d'authentification
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(compteRepository, applicationContext)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MangatekAppWithAuth(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    favorisRepository: FavorisRepository
) {
    val authState by authViewModel.authState.collectAsState()
    var authScreen by rememberSaveable { mutableStateOf<AuthScreen>(AuthScreen.Login) }

    // Si l'utilisateur n'est pas connecté
    if (!authState.isLoggedIn || authState.currentCompte == null) {
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
        // L'utilisateur est connecté - créer le FavorisViewModel avec l'ID du compte
        val compteId = authState.currentCompte!!.id

        // Utiliser remember pour ne créer le ViewModel qu'une fois
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MangatekApp(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    favorisViewModel: FavorisViewModel
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MENU) }

    NavigationSuiteScaffold(
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
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.MENU -> AnimeScreen(
                    viewModel = homeViewModel,
                    favorisViewModel = favorisViewModel
                )

                AppDestinations.FAVORITES -> FavoritesScreen(
                    favorisViewModel = favorisViewModel
                )

                AppDestinations.PROFILE -> ProfileScreen(
                    authViewModel = authViewModel
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    MENU("Home", Icons.Default.Menu),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

enum class AuthScreen {
    Login,
    SignUp
}