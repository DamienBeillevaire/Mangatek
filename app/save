package com.example.mangatek

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import com.example.mangatek.data.FavorisManagerDatabase
import com.example.mangatek.data.FavorisRepository
import com.example.mangatek.data.ViewModel.FavorisViewModel
import com.example.mangatek.data.ViewModel.FavorisViewModelFactory
import com.example.mangatek.ui.theme.MangatekTheme

class MainActivity : AppCompatActivity() {

    // ViewModel principal (animes, recherche)
    private val homeViewModel: HomeViewModel by viewModels()

    // ViewModel favoris — instancié avec sa factory + la BDD Room
    private val favorisViewModel: FavorisViewModel by viewModels {
        val db = FavorisManagerDatabase.getDatabase(applicationContext)
        FavorisViewModelFactory(FavorisRepository(db.favorisDao()))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main)

        findViewById<ComposeView>(R.id.compose_view).setContent {
            MangatekTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MangatekApp(
                        homeViewModel = homeViewModel,
                        favorisViewModel = favorisViewModel
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MangatekApp(
    homeViewModel: HomeViewModel,
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

                AppDestinations.PROFILE -> Text(
                    text = "Profil",
                    modifier = Modifier.padding(innerPadding)
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