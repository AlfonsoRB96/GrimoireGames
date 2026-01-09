package com.trunder.grimoiregames.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trunder.grimoiregames.data.entity.Game

// @Composable: Esto es una función que DIBUJA UI.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    // hiltViewModel(): ¡Magia! Hilt busca el ViewModel que creamos y lo inyecta aquí.
    onAddGameClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // ESTADO: Observamos la lista de juegos.
    // collectAsState: Convierte el flujo de datos en un estado que Compose entiende.
    // Si la lista cambia en la BBDD, esta variable cambia y la pantalla se redibuja sola.
    val games by viewModel.games.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grimoire Games") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGameClick) { // <--- USAMOS LA NAVEGACIÓN
                Icon(Icons.Default.Add, contentDescription = "Añadir Juego")
            }
        }
    ) { innerPadding ->
        // EL CUERPO DE LA PANTALLA
        if (games.isEmpty()) {
            // Estado Vacío
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tu Grimorio está vacío... ¡Añade loot!")
            }
        } else {
            // Lista de Juegos (Como un RecyclerView pero en 3 líneas)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(games) { game ->
                    GameItem(
                        game = game,
                        onDeleteClick = { viewModel.deleteGame(game) }
                    )
                }
            }
        }
    }
}

// COMPONENTE: CARTA DE JUEGO INDIVIDUAL
@Composable
fun GameItem(game: Game, onDeleteClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${game.platform} • ${game.status}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Borrar",
                    tint = Color.Red
                )
            }
        }
    }
}