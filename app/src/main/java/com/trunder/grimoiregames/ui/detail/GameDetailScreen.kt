package com.trunder.grimoiregames.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    onBackClick: () -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel()
) {
    // Recogemos el juego del flujo (Flow)
    // initial = null porque al entrar puede tardar unos milisegundos en cargar
    val game by viewModel.game.collectAsState(initial = null)

    // Estados locales para la edición (se rellenarán cuando cargue el juego)
    var rating by remember { mutableFloatStateOf(0f) }
    var hours by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Backlog") }

    // Cuando 'game' cargue por primera vez, actualizamos los estados locales
    LaunchedEffect(game) {
        game?.let {
            rating = it.rating?.toFloat() ?: 0f
            hours = it.hoursPlayed.toString()
            status = it.status
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game?.title ?: "Cargando...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        // Si el juego es null (cargando), mostramos un círculo
        if (game == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val loadedGame = game!! // Ya sabemos que no es null

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()) // Hacemos scroll si la pantalla es pequeña
            ) {
                // 1. IMAGEN DE CABECERA (BANNER)
                AsyncImage(
                    model = loadedGame.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp), // Altura fija para el banner
                    contentScale = ContentScale.Crop // Recorta para llenar (efecto zoom)
                )

                Column(modifier = Modifier.padding(16.dp)) {

                    // 2. TÍTULO Y PLATAFORMA
                    Text(text = loadedGame.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(text = loadedGame.platform, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // 3. SECCIÓN DE EDICIÓN: NOTA
                    Text("Tu Valoración: ${rating.toInt()}/10", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Slider(
                            value = rating,
                            onValueChange = { rating = it },
                            valueRange = 0f..10f,
                            steps = 9, // Pasos intermedios (para que vaya de 1 en 1)
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. SECCIÓN DE EDICIÓN: HORAS
                    OutlinedTextField(
                        value = hours,
                        onValueChange = {
                            // Solo dejamos escribir números
                            if (it.all { char -> char.isDigit() }) hours = it
                        },
                        label = { Text("Horas Jugadas") },
                        leadingIcon = { Icon(Icons.Default.DateRange, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. SECCIÓN DE EDICIÓN: ESTADO (Chips)
                    Text("Estado actual:", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Backlog", "Playing", "Completed").forEach { stateOption ->
                            FilterChip(
                                selected = status == stateOption,
                                onClick = { status = stateOption },
                                label = { Text(stateOption) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 6. BOTÓN DE GUARDAR
                    Button(
                        onClick = {
                            viewModel.updateGameStats(
                                currentGame = loadedGame,
                                newRating = rating.toInt(),
                                newHours = hours.toIntOrNull() ?: 0,
                                newStatus = status
                            )
                            onBackClick() // Volvemos atrás al guardar
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GUARDAR CAMBIOS")
                    }
                }
            }
        }
    }
}