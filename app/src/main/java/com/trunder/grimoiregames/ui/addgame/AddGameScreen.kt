package com.trunder.grimoiregames.ui.addgame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trunder.grimoiregames.data.remote.dto.IgdbGameDto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(
    onBackClick: () -> Unit,
    onGameSaved: () -> Unit,
    viewModel: AddGameViewModel = hiltViewModel()
) {
    // ESTADO LOCAL
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

        // --- DIÁLOGO DE SELECCIÓN (PLATAFORMA + REGIÓN) ---
        if (selectedGameForPlatformSelection != null) {
            val game = selectedGameForPlatformSelection!!

            // Estado para la región dentro del diálogo (Por defecto PAL)
            var selectedRegion by remember { mutableStateOf("PAL") }
            val regions = listOf("PAL", "NTSC-U", "NTSC-J")

            val platforms = game.platforms?.map { it.name } ?: listOf("Plataforma desconocida")

            AlertDialog(
                onDismissRequest = { selectedGameForPlatformSelection = null },
                title = {
                    Text(
                        text = "Configurar Juego",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    // Usamos verticalScroll por si hay muchas plataformas
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Juego: ${game.name}")
                        Spacer(modifier = Modifier.height(16.dp))

                        // 👇 1. SELECTOR DE REGIÓN (AMPLIADO)
                        Text(
                            text = "1. Elige la versión (Región):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Lista de todas las regiones posibles
                        val regions = listOf(
                            "PAL" to "PEGI",
                            "NTSC-U" to "ESRB",
                            "NTSC-J" to "CERO",
                            "PAL DE" to "USK",
                            "PAL AU" to "ACB",
                            "NTSC-K" to "GRAC",
                            "NTSC-BR" to "ClassInd"
                        )

                        // Fila deslizante
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()), // 👈 Permite deslizar si no caben
                            horizontalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre botones
                        ) {
                            regions.forEach { (regionCode, agency) ->
                                val isSelected = (selectedRegion == regionCode)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedRegion = regionCode },
                                    label = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = regionCode, style = MaterialTheme.typography.labelMedium)
                                            Text(text = "($agency)", style = MaterialTheme.typography.labelSmall)
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        // 👇 2. SELECTOR DE PLATAFORMA (BOTONES DE ACCIÓN)
                        Text(
                            text = "2. Elige Plataforma para guardar:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        platforms.forEach { platformName ->
                            Button(
                                onClick = {
                                    // 👇 AQUI SE LLAMA AL VIEWMODEL CON TODOS LOS DATOS
                                    viewModel.onGameSelected(
                                        dto = game,
                                        selectedPlatform = platformName,
                                        selectedRegion = selectedRegion, // <-- Pasamos la región seleccionada
                                        onSuccess = {
                                            selectedGameForPlatformSelection = null
                                            onGameSaved()
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
                confirmButton = {}, // No hace falta botón confirmar, los botones de plataforma actúan como tal
                dismissButton = {
                    TextButton(onClick = { selectedGameForPlatformSelection = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}