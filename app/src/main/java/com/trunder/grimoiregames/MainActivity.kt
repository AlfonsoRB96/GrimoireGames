package com.trunder.grimoiregames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trunder.grimoiregames.ui.addgame.AddGameScreen
import com.trunder.grimoiregames.ui.home.HomeScreen
import com.trunder.grimoiregames.ui.theme.GrimoireGamesTheme
import dagger.hilt.android.AndroidEntryPoint

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

                    // RUTA 1: PANTALLA PRINCIPAL
                    composable("home") {
                        HomeScreen(
                            onAddGameClick = { navController.navigate("add_game") }
                        )
                    }

                    // RUTA 2: PANTALLA DE AÑADIR (BUSCADOR)
                    composable("add_game") {
                        AddGameScreen(
                            onBackClick = { navController.popBackStack() },
                            onGameSaved = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}