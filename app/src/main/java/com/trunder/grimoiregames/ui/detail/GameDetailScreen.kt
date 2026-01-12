package com.trunder.grimoiregames.ui.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var showScoreDialog by remember { mutableStateOf(false) }

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

                    // 3. FILA DE INFO RÁPIDA (Notas, Lanzamiento, Género, PEGI)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp), // Un poco de aire vertical
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // A. LAS NOTAS (Prensa + Fans juntas) - AHORA CON CLICK
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp)) // Redondeamos las esquinas del área tocable
                                .clickable { showScoreDialog = true } // <--- ¡AQUÍ ESTÁ LA CLAVE!
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) // Un fondo sutil para que parezca un botón
                                .padding(horizontal = 8.dp, vertical = 4.dp) // Un poco de aire interno
                        ) {
                            // Nota Prensa
                            RatingBadge(
                                score = currentSafeGame.metacriticPress, // Tu cambio: Metacritic por defecto
                                label = "Score",
                                icon = Icons.Default.Newspaper
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Nota Fans
                            RatingBadge(
                                score = currentSafeGame.metacriticUser,
                                label = "User Score",
                                icon = Icons.Default.Person
                            )

                            // Icono pequeño para indicar que hay "más cosas" (desplegable)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Ver más notas",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // B. INFO TEXTUAL (Se mantiene igual)
                        // Usamos Box o Row simples para alinear si InfoItem ocupa mucho
                        InfoItem(label = "Año", value = currentSafeGame.releaseDate?.take(4) ?: "TBA")
                        InfoItem(label = "Género", value = currentSafeGame.genre?.split(",")?.firstOrNull() ?: "N/A")
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
                    // C. PEGI (Se mantiene igual)
                    AgeRatingBadge(game = currentSafeGame)

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

            // 👇 AQUÍ SE DIBUJA EL MODAL SI LA VARIABLE ES TRUE
            if (showScoreDialog) {
                ScoreComparisonDialog(
                    game = currentSafeGame,
                    onDismiss = { showScoreDialog = false }
                )
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
    // Recuperamos el dato real tal cual viene de la BD / RAWG
    val ratingText = game.pegi ?: game.esrb

    if (ratingText != null) {
        val color = when {
            // --- VERDE (Apto para todos) ---
            // PEGI 3 / ESRB Everyone
            ratingText.contains("3") || ratingText.contains("Everyone", ignoreCase = true) ->
                Color(0xFF4CAF50)

            // --- AMARILLO (Mayores de 7/10) ---
            // PEGI 7 / ESRB Everyone 10+
            ratingText.contains("7") || ratingText.contains("10") ->
                Color(0xFFFFC107)

            // --- NARANJA (Adolescentes) ---
            // PEGI 12 / ESRB Teen
            ratingText.contains("12") || ratingText.contains("Teen", ignoreCase = true) ->
                Color(0xFFFF9800)

            // --- ROJO (Maduro) ---
            // PEGI 16 / ESRB Mature
            ratingText.contains("16") || ratingText.contains("Mature", ignoreCase = true) ->
                Color(0xFFF44336)

            // --- NEGRO (Adultos) ---
            // PEGI 18 / ESRB Adults Only
            ratingText.contains("18") || ratingText.contains("Adults", ignoreCase = true) ->
                Color.Black

            else -> Color.Gray
        }

        Surface(
            color = color,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = ratingText, // 👈 Mostramos el texto REAL (Ej: "Mature")
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

@Composable
fun RatingBadge(score: Int?, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val scoreText = score?.toString() ?: "--"

    // Color semáforo según la nota
    val color = when {
        score == null -> Color.Gray
        score >= 75 -> Color(0xFF4CAF50) // Verde
        score >= 50 -> Color(0xFFFFC107) // Amarillo
        else -> Color(0xFFF44336)        // Rojo
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Círculo con la nota
        Surface(
            color = color.copy(alpha = 0.2f), // Fondo suave
            shape = CircleShape,
            border = BorderStroke(2.dp, color),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        // Texto debajo (Prensa/Fans)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ScorePill(score: Int?, isPress: Boolean) {
    if (score == null) {
        // Si no hay nota, mostramos guiones grises
        Text("--", color = Color.Gray)
    } else {
        // Colores semáforo
        val color = if (score >= 75) Color(0xFF4CAF50) // Verde
        else if (score >= 50) Color(0xFFFFC107)        // Amarillo
        else Color(0xFFF44336)                         // Rojo

        // Dibujamos la "pastilla"
        Surface(
            // Si es Prensa -> Fondo sólido. Si es Usuario -> Solo borde.
            color = if (isPress) color else Color.Transparent,
            border = if (!isPress) BorderStroke(1.dp, color) else null,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = score.toString(),
                // Si es Prensa -> Texto Blanco. Si es Usuario -> Texto del color de la nota.
                color = if (isPress) Color.White else color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// --- COMPONENTES DEL MODAL (Pégalos al final del archivo) ---

@Composable
fun ScoreComparisonDialog(
    game: Game,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tabla de Puntuaciones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Cabecera de la tabla
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f)) // Hueco para el nombre
                    Text("Prensa", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                    Text("Usuarios", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                }

                HorizontalDivider()

                // 1. METACRITIC (Amarillo)
                ScoreRow(
                    sourceName = "Metacritic",
                    pressScore = game.metacriticPress,
                    userScore = game.metacriticUser,
                    color = Color(0xFFFFCC33)
                )

                // 2. OPENCRITIC (Naranja)
                ScoreRow(
                    sourceName = "OpenCritic",
                    pressScore = game.opencriticPress,
                    userScore = null, // OpenCritic no suele tener user score relevante en API pública
                    color = Color(0xFFFC430A)
                )

                // 3. IGDB (Morado)
                ScoreRow(
                    sourceName = "IGDB",
                    pressScore = game.igdbPress,
                    userScore = game.igdbUser,
                    color = Color(0xFF9147FF)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
fun ScoreRow(sourceName: String, pressScore: Int?, userScore: Int?, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nombre de la fuente (con bolita de color)
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(sourceName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }

        // Nota Prensa
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ScorePill(score = pressScore, isPress = true)
        }

        // Nota Usuario
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ScorePill(score = userScore, isPress = false)
        }
    }
}