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
import com.trunder.grimoiregames.data.remote.dto.IgdbGameDto // 👈 IMPORTANTE
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(
    onBackClick: () -> Unit,
    onGameSaved: () -> Unit,
    viewModel: AddGameViewModel = hiltViewModel()
) {
    // ESTADO LOCAL: Ahora usa IgdbGameDto
    var selectedGameForPlatformSelection by remember { mutableStateOf<IgdbGameDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Añadir Juego (IGDB)") },
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 1. BARRA DE BÚSQUEDA
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = { viewModel.onQueryChange(it) },
                label = { Text("Buscar videojuego...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. INDICADOR DE ERROR
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

            // 3. LISTA DE RESULTADOS
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(viewModel.searchResults) { gameDto ->

                        // 👇 LÓGICA DE FECHA (Timestamp -> Año)
                        val releaseYear = remember(gameDto.firstReleaseDate) {
                            gameDto.firstReleaseDate?.let { timestamp ->
                                val date = Date(timestamp * 1000)
                                SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
                            } ?: "TBA"
                        }

                        ListItem(
                            headlineContent = { Text(gameDto.name) },
                            supportingContent = {
                                Text("Lanzamiento: $releaseYear")
                            },
                            modifier = Modifier.clickable {
                                selectedGameForPlatformSelection = gameDto
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        // --- DIÁLOGO DE SELECCIÓN DE PLATAFORMA ---
        if (selectedGameForPlatformSelection != null) {
            val game = selectedGameForPlatformSelection!!

            // 👇 LÓGICA DE PLATAFORMAS DE IGDB
            // IGDB devuelve una lista plana: platforms[].name
            val platforms = game.platforms?.map { it.name } ?: listOf("Plataforma desconocida")

            AlertDialog(
                onDismissRequest = { selectedGameForPlatformSelection = null },
                title = { Text("Elige Plataforma") },
                text = {
                    Column {
                        Text("¿Dónde vas a jugar a ${game.name}?")
                        Spacer(modifier = Modifier.height(10.dp))

                        // Lista de botones
                        platforms.forEach { platformName ->
                            Button(
                                onClick = {
                                    // 👇 CAMBIO IMPORTANTE
                                    // Le pasamos el juego, la plataforma Y LO QUE TIENE QUE HACER AL ACABAR
                                    viewModel.onGameSelected(
                                        dto = game,
                                        selectedPlatform = platformName,
                                        onSuccess = {
                                            // Esto se ejecutará SOLO cuando el ViewModel diga "Ya he acabado"
                                            selectedGameForPlatformSelection = null
                                            onGameSaved() // Volvemos atrás
                                        }
                                    )
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
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedGameForPlatformSelection = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}