package com.trunder.grimoiregames.ui.detail

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game?.title ?: "Detalles") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "¡Cambios guardados correctamente!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onBackClick()
                },
                icon = { Icon(Icons.Default.Save, "Guardar") },
                text = { Text("Guardar") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        game?.let { currentSafeGame ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Para que se pueda bajar
                    .padding(padding)
            ) {
                // 1. IMAGEN DE CABECERA
                AsyncImage(
                    model = currentSafeGame.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    // 2. TÍTULO Y PLATAFORMA
                    Text(
                        text = currentSafeGame.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentSafeGame.platform,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. FILA DE INFO RÁPIDA (Metacritic, Lanzamiento, Género)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoItem(label = "Metacritic", value = currentSafeGame.metacritic?.toString() ?: "N/A", isBadge = true)
                        InfoItem(label = "Lanzamiento", value = currentSafeGame.releaseDate?.take(4) ?: "TBA")
                        InfoItem(label = "Género", value = currentSafeGame.genre?.split(",")?.firstOrNull() ?: "N/A")
                        AgeRatingBadge(game = currentSafeGame)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // 4. SINOPSIS
                    Text(
                        text = "Sinopsis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentSafeGame.description ?: "No hay descripción disponible para este juego.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. DATOS TÉCNICOS
                    TechnicalDataRow(label = "Desarrollador", value = currentSafeGame.developer)
                    TechnicalDataRow(label = "Editor", value = currentSafeGame.publisher)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // 6. SECCIÓN DE PROGRESO (Recuperada)
                    Text(
                        text = "Tu Progreso",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- ESTADO DEL JUEGO ---
                    Text("Estado", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Backlog", "Playing", "Completed").forEach { status ->
                            FilterChip(
                                selected = currentSafeGame.status == status,
                                onClick = {
                                    viewModel.updateStatus(currentSafeGame, status)
                                },
                                label = { Text(status) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- PUNTUACIÓN (Estrellas o Slider) ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Puntuación: ${currentSafeGame.rating ?: 0}/10",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Slider(
                        value = (currentSafeGame.rating ?: 0).toFloat(),
                        onValueChange = { viewModel.updateRating(currentSafeGame, it.toInt()) },
                        valueRange = 0f..10f,
                        steps = 9
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- HORAS DE JUEGO ---
                    OutlinedTextField(
                        value = currentSafeGame.hoursPlayed?.toString() ?: "0",
                        onValueChange = { newValue ->
                            val hours = newValue.toIntOrNull() ?: 0
                            viewModel.updateHours(currentSafeGame, hours)
                        },
                        label = { Text("Horas Jugadas") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Icon(Icons.Default.Timer, null) }
                    )

                    Spacer(modifier = Modifier.height(80.dp)) // Espacio final
                }
            }
        } ?: run {
            // Pantalla de carga mientras llega el Flow
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, isBadge: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        if (isBadge && value != "N/A") {
            Surface(
                color = if (value.toInt() >= 75) Color(0xFF4CAF50) else Color(0xFFFFC107),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AgeRatingBadge(game: Game) {
    // 1. Intentamos coger el PEGI real. Si es nulo, cogemos el ESRB.
    val rawRating = game.pegi ?: game.esrb

    if (rawRating != null) {
        // 2. LÓGICA DE TRADUCCIÓN (De ESRB Americano a Estilo PEGI Europeo)
        val (displayText, color) = when {
            // --- VERDE (Niños) ---
            // ESRB "Everyone" es como PEGI 3 o 7
            rawRating.contains("Everyone", ignoreCase = true) || rawRating.contains("3") ->
                "PEGI 3" to Color(0xFF4CAF50) // Verde

            // --- AMARILLO/NARANJA (Adolescentes) ---
            // ESRB "Everyone 10+" es como PEGI 7 o 12
            rawRating.contains("10+", ignoreCase = true) || rawRating.contains("7") ->
                "PEGI 7" to Color(0xFFFFC107) // Amarillo

            // ESRB "Teen" es casi idéntico a PEGI 12
            rawRating.contains("Teen", ignoreCase = true) || rawRating.contains("12") ->
                "PEGI 12" to Color(0xFFFF9800) // Naranja

            // --- ROJO (Adultos) ---
            // ESRB "Mature" es PEGI 16 o 18 (Suele ser 16/18 dependiendo del juego)
            // Lo mapeamos a 16 o 18 según prefieras. Mature suele ser +17 en USA.
            rawRating.contains("Mature", ignoreCase = true) || rawRating.contains("16") ->
                "PEGI 16" to Color(0xFFF44336) // Rojo

            // --- NEGRO (Adultos estricto) ---
            // ESRB "Adults Only" es PEGI 18
            rawRating.contains("Adults Only", ignoreCase = true) || rawRating.contains("18") ->
                "PEGI 18" to Color(0xFFD32F2F) // Rojo Oscuro / Negro

            // --- CASO DESCONOCIDO (Mostramos el texto tal cual llega) ---
            else -> rawRating to Color.Gray
        }

        Surface(
            color = color,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = displayText,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun TechnicalDataRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(text = "$label: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}