package com.trunder.grimoiregames.ui.addgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trunder.grimoiregames.data.remote.dto.GameDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(
    onBackClick: () -> Unit,
    onGameSaved: () -> Unit,
    viewModel: AddGameViewModel = hiltViewModel()
) {
    // ESTADO LOCAL: ¿Qué juego hemos pulsado para elegir plataforma?
    var selectedGameForPlatformSelection by remember { mutableStateOf<GameDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Añadir Juego") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize() // Asegura que ocupe toda la pantalla
                .padding(padding)
                .padding(16.dp)
        ) {
            // 1. BARRA DE BÚSQUEDA
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = { viewModel.onQueryChange(it) },
                label = { Text("Buscar en RAWG...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. INDICADOR DE CARGA
            if (viewModel.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = viewModel.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 👇 QUE LA LISTA SOLO SALGA SI NO HAY ERROR
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // 3. LISTA DE RESULTADOS
                LazyColumn {
                    items(viewModel.searchResults) { gameDto ->
                        ListItem(
                            headlineContent = { Text(gameDto.name) },
                            supportingContent = {
                                Text("Lanzamiento: ${gameDto.releaseDate ?: "N/A"}")
                            },
                            modifier = Modifier.clickable {
                                // EN LUGAR DE GUARDAR DIRECTAMENTE, ABRIMOS EL DIÁLOGO
                                selectedGameForPlatformSelection = gameDto
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        // --- EL DIÁLOGO DE SELECCIÓN DE PLATAFORMA ---
        if (selectedGameForPlatformSelection != null) {
            val game = selectedGameForPlatformSelection!!

            // Extraemos los nombres de las plataformas de la estructura rara de RAWG
            // Si viene null o vacío, ponemos "PC" por defecto
            val platforms = game.platforms?.map { it.platform.name } ?: listOf("PC", "Console")

            AlertDialog(
                onDismissRequest = { selectedGameForPlatformSelection = null },
                title = { Text("Elige Plataforma") },
                text = {
                    Column {
                        Text("¿Dónde vas a jugar a ${game.name}?")
                        Spacer(modifier = Modifier.height(10.dp))
                        // Lista de botones para cada plataforma
                        platforms.forEach { platformName ->
                            Button(
                                onClick = {
                                    viewModel.onGameSelected(game, platformName)
                                    selectedGameForPlatformSelection = null // Cerramos diálogo
                                    onGameSaved() // Volvemos atrás
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(platformName)
                            }
                        }
                    }
                },
                confirmButton = {}, // No necesitamos botón genérico
                dismissButton = {
                    TextButton(onClick = { selectedGameForPlatformSelection = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}