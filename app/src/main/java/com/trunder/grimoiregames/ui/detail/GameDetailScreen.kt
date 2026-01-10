package com.trunder.grimoiregames.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.trunder.grimoiregames.data.entity.Game

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    onBackClick: () -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel()
) {
    val game by viewModel.game.collectAsState(initial = null)

    // Estados de edición
    var rating by remember { mutableFloatStateOf(0f) }
    var hours by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Backlog") }

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
        if (game == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val loadedGame = game!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. BANNER
                AsyncImage(
                    model = loadedGame.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(16.dp)) {

                    // 2. TÍTULO Y METADATOS PRINCIPALES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loadedGame.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = loadedGame.platform,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Badge de Metacritic (Si existe)
                        loadedGame.metacritic?.let { score ->
                            MetacriticBadge(score)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. INFORMACIÓN TÉCNICA (Grid Pequeño)
                    InfoRow("Género", loadedGame.genre)
                    InfoRow("Desarrollador", loadedGame.developer)
                    InfoRow("Lanzamiento", loadedGame.releaseDate)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // 4. SINOPSIS (Descripción)
                    if (!loadedGame.description.isNullOrBlank()) {
                        Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = loadedGame.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // 5. ZONA DE GESTIÓN (Tus datos)
                    Text("Tu Progreso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Rating Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${rating.toInt()}/10", fontWeight = FontWeight.Bold)
                        Slider(
                            value = rating,
                            onValueChange = { rating = it },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }

                    // Horas Input
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { if (it.all { c -> c.isDigit() }) hours = it },
                        label = { Text("Horas Jugadas") },
                        leadingIcon = { Icon(Icons.Default.DateRange, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Backlog", "Playing", "Completed").forEach { stateOption ->
                            FilterChip(
                                selected = status == stateOption,
                                onClick = { status = stateOption },
                                label = { Text(stateOption) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.updateGameStats(
                                loadedGame,
                                rating.toInt(),
                                hours.toIntOrNull() ?: 0,
                                status
                            )
                            onBackClick()
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

// --- COMPONENTES AUXILIARES PARA QUE EL CÓDIGO QUEDE LIMPIO ---

@Composable
fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(text = "$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MetacriticBadge(score: Int) {
    // Verde si > 75, Amarillo si > 50, Rojo si menos
    val color = when {
        score >= 75 -> Color(0xFF66CC33)
        score >= 50 -> Color(0xFFFFCC33)
        else -> Color(0xFFFF0000)
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = score.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}