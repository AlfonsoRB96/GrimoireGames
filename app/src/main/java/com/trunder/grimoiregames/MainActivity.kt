package com.trunder.grimoiregames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trunder.grimoiregames.ui.addgame.AddGameScreen
import com.trunder.grimoiregames.ui.library.HomeScreen
import com.trunder.grimoiregames.ui.theme.GrimoireGamesTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.trunder.grimoiregames.ui.detail.GameDetailScreen
import com.trunder.grimoiregames.ui.home.HomeScreen
import com.trunder.grimoiregames.ui.settings.SettingsScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrimoireGamesTheme {
                // 1. EL CONTROLADOR DE NAVEGACIÓN
                // (Es el cerebro que sabe dónde estamos y a dónde vamos)
                val navController = rememberNavController()

                // 2. EL MAPA DE RUTAS
                NavHost(navController = navController, startDestination = "home") {

                    // 1. PANTALLA PRINCIPAL (DASHBOARD)
                    composable("home") {
                        HomeScreen(
                            onNavigateToLibrary = { navController.navigate("library") },
                            onNavigateToActivity = { /* TODO: Pantalla 2 */ },
                            onNavigateToWishlist = { /* TODO: Pantalla 3 */ },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }

                    // RUTA 2: BIBLIOTECA
                    composable("library") {
                        HomeScreen(
                            onAddGameClick = { navController.navigate("add_game") },
                            // ¡NUEVO! Al hacer clic en un juego, vamos a "detail/ID"
                            onGameClick = { gameId ->
                                navController.navigate("detail/$gameId")
                            }
                        )
                    }

                    // RUTA 3: AÑADIR
                    composable("add_game") {
                        AddGameScreen(
                            onBackClick = { navController.popBackStack() },
                            onGameSaved = { navController.popBackStack() }
                        )
                    }

                    // RUTA 4: DETALLE
                    // Definimos que esta ruta espera un argumento llamado "gameId"
                    composable(
                        route = "detail/{gameId}",
                        arguments = listOf(navArgument("gameId") { type = NavType.IntType })
                    ) {
                        GameDetailScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    // 5. SETTINGS
                    composable("settings") {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}