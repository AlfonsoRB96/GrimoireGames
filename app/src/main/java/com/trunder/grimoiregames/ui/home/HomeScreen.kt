package com.trunder.grimoiregames.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.trunder.grimoiregames.data.entity.Game

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddGameClick: () -> Unit,
    onGameClick: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
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
            FloatingActionButton(onClick = onAddGameClick) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Juego")
            }
        }
    ) { innerPadding ->
        if (games.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tu Grimorio está vacío... ¡Añade loot!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(games) { game ->
                    GameItem(
                        game = game,
                        onClick = { onGameClick(game.id) },
                        onDeleteClick = { viewModel.deleteGame(game) }
                    )
                }
            }
        }
    }
}

// --- AQUÍ ESTÁ LA MAGIA VISUAL ---
@Composable
fun GameItem(game: Game, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // Un poco más de elevación
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Color base más limpio
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // Truco: La fila se ajusta a la altura del elemento más alto (la imagen)
        ) {

            // 1. IMAGEN (FORMATO PÓSTER) 🖼️
            AsyncImage(
                model = game.imageUrl,
                contentDescription = "Carátula de ${game.title}",
                modifier = Modifier
                    .width(100.dp) // Ancho fijo
                    .fillMaxHeight(), // ¡Ocupa toda la altura de la tarjeta!
                contentScale = ContentScale.Crop // Recorta lo que sobre para llenar el rectángulo
            )

            // 2. COLUMNA DE DATOS 📝
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp) // Padding interno del texto
            ) {
                // Título y Botón Borrar (En una fila para que el botón quede a la derecha)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), // El título ocupa lo que pueda
                        maxLines = 2 // Permitimos 2 líneas para títulos largos
                    )

                    // Botón Borrar (Pequeño y discreto)
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp) // Más pequeño
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Plataforma y Estado (Chips o Texto coloreado)
                Text(
                    text = "${game.platform} • ${game.status}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.weight(1f)) // Empuja lo siguiente hacia abajo

                // 3. ESTADÍSTICAS (Rating y Horas) 📊
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Rating
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFB300), // Color Dorado/Amarillo
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        // Si es null mostramos "-", si tiene nota la mostramos
                        text = game.rating?.toString() ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(16.dp)) // Separador

                    // Horas Jugadas
                    Icon(
                        imageVector = Icons.Filled.DateRange, // O un icono de reloj
                        contentDescription = "Horas",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${game.hoursPlayed} h",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}