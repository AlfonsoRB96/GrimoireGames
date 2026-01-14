package com.trunder.grimoiregames.ui.detail

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    // 1. OBSERVAMOS EL JUEGO DESDE EL VIEWMODEL
    val gameState by viewModel.game.collectAsState(initial = null)

    // Estado local UI
    var showScoreDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gameState?.title ?: "Cargando...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { padding ->
        if (gameState == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val currentSafeGame = gameState!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
            ) {
                // 1. CABECERA CINEMÁTICA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    // CAPA A: FONDO AMBIENTAL (Blur + Oscuro)
                    AsyncImage(
                        model = currentSafeGame.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(radius = 25.dp)
                            .drawWithContent {
                                drawContent()
                                drawRect(Color.Black.copy(alpha = 0.5f))
                            }
                    )

                    // CAPA B: PÓSTER PRINCIPAL (Nítido y completo)
                    AsyncImage(
                        model = currentSafeGame.imageUrl,
                        contentDescription = currentSafeGame.title,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight()
                            // 🔴 CAMBIO AQUÍ: Usamos RectangleShape para esquinas cuadradas
                            .shadow(elevation = 16.dp, shape = RectangleShape)
                            .clip(RectangleShape)
                    )
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // ... (El resto del código sigue exactamente igual) ...
                    // 2. TÍTULO Y PLATAFORMA
                    Text(
                        text = currentSafeGame.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Plataforma + Badge Región
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = currentSafeGame.platform,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RegionBadge(region = currentSafeGame.region)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. FILA DE INFO RÁPIDA
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pressScoreToDisplay = currentSafeGame.metacriticPress
                            ?: currentSafeGame.opencriticPress
                            ?: currentSafeGame.igdbPress

                        val pressLabel = when {
                            currentSafeGame.metacriticPress != null -> "Metacritic"
                            currentSafeGame.opencriticPress != null -> "OpenCritic"
                            else -> "IGDB Score"
                        }

                        val scoreColor = getScoreColor(pressScoreToDisplay, pressLabel)
                        val userScoreToDisplay = currentSafeGame.metacriticUser ?: currentSafeGame.igdbUser

                        // A. NOTAS
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showScoreDialog = true }
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RatingBadge(
                                score = pressScoreToDisplay,
                                label = pressLabel,
                                icon = Icons.Default.Newspaper,
                                customColor = scoreColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            RatingBadge(
                                score = userScoreToDisplay,
                                label = "User Score",
                                icon = Icons.Default.Person,
                                customColor = scoreColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Ver más",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // B. INFO TEXTUAL
                        InfoItem(label = "Año", value = currentSafeGame.releaseDate?.take(4) ?: "TBA")
                        InfoItem(label = "Género", value = currentSafeGame.genre?.split(",")?.firstOrNull() ?: "N/A")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // 4. SINOPSIS
                    val translatedDescription by viewModel.translatedDescription.collectAsState()
                    val isTranslating by viewModel.isTranslating.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sinopsis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                        if (translatedDescription == null) {
                            if (isTranslating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(
                                    onClick = { viewModel.translateDescription(currentSafeGame.description) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Translate, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Traducir", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val descriptionToShow = translatedDescription ?: currentSafeGame.description ?: "No hay descripción disponible."
                    AnimatedContent(targetState = descriptionToShow, label = "textChange") { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Justify,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. DATOS TÉCNICOS
                    TechnicalDataRow(label = "Desarrollador", value = currentSafeGame.developer)
                    TechnicalDataRow(label = "Editor", value = currentSafeGame.publisher)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clasificación", color = Color.Gray)
                        AgeRatingBadge(ratingString = currentSafeGame.ageRating)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // 6. PROGRESO PERSONAL
                    Text("Tu Progreso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Estado", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Backlog", "Playing", "Completed").forEach { status ->
                            FilterChip(
                                selected = currentSafeGame.status == status,
                                onClick = { viewModel.updateStatus(currentSafeGame, status) },
                                label = { Text(status) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // TIER SELECTOR
                    TierSelector(
                        currentRating = currentSafeGame.userRating,
                        onTierSelected = { newRating -> viewModel.onUserRatingChanged(currentSafeGame, newRating) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // HORAS DE JUEGO
                    var hoursText by remember(currentSafeGame.hoursPlayed) {
                        mutableStateOf(if ((currentSafeGame.hoursPlayed ?: 0) == 0) "" else currentSafeGame.hoursPlayed.toString())
                    }

                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                hoursText = newValue
                                val hours = newValue.toIntOrNull() ?: 0
                                viewModel.onPlayTimeChanged(currentSafeGame, hours)
                            }
                        },
                        label = { Text("Horas Jugadas") },
                        placeholder = { Text("0", color = Color.Gray.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        prefix = { Icon(Icons.Default.Timer, null) }
                    )

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            if (showScoreDialog) {
                ScoreComparisonDialog(game = currentSafeGame, onDismiss = { showScoreDialog = false })
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AgeRatingBadge(ratingString: String?) {
    if (ratingString == null) return

    val color = when {
        ratingString.contains("PEGI 3") || ratingString.contains("PEGI 7") ||
                ratingString.contains("ESRB E") || ratingString.contains("CERO A") -> Color(0xFF4CAF50)

        ratingString.contains("PEGI 12") || ratingString.contains("PEGI 16") ||
                ratingString.contains("ESRB T") || ratingString.contains("CERO B") || ratingString.contains("CERO C") -> Color(0xFFFFC107)

        ratingString.contains("PEGI 18") || ratingString.contains("ESRB M") ||
                ratingString.contains("CERO D") || ratingString.contains("CERO Z") -> Color(0xFFF44336)

        else -> Color.Gray
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = ratingString,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
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
fun RatingBadge(
    score: Int?,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    customColor: Color? = null // 👇 Nuevo parámetro opcional
) {
    val scoreText = score?.toString() ?: "--"

    // LÓGICA DE COLOR:
    // Si nos pasan un 'customColor' (Metacritic/OpenCritic/IGDB), usamos ese.
    // Si es null, calculamos el color semáforo según la nota (para User Score).
    val finalColor = customColor ?: when {
        score == null -> Color.Gray
        score >= 75 -> Color(0xFF4CAF50) // Verde
        score >= 50 -> Color(0xFFFFC107) // Amarillo
        else -> Color(0xFFF44336)        // Rojo
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Círculo con la nota
        Surface(
            color = finalColor.copy(alpha = 0.15f), // Fondo suavito del color elegido
            shape = CircleShape,
            border = BorderStroke(2.dp, finalColor),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = finalColor // Texto del mismo color fuerte
                )
            }
        }
        // Texto debajo (Prensa/Fans)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.Gray
            )
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
fun ScoreComparisonDialog(
    game: Game,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tabla de Puntuaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Cabecera
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Prensa", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                    Text("Usuarios", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                }

                HorizontalDivider()

                // 1. METACRITIC (Color dinámico estándar)
                ScoreRow(
                    sourceName = "Metacritic",
                    pressScore = game.metacriticPress,
                    userScore = game.metacriticUser,
                    pressColor = getScoreColor(game.metacriticPress, "Metacritic"), // 👈 Dinámico
                    userColor = getScoreColor(game.metacriticUser, "Metacritic")    // 👈 Dinámico
                )

                // 2. OPENCRITIC (Color dinámico OpenCritic)
                ScoreRow(
                    sourceName = "OpenCritic",
                    pressScore = game.opencriticPress,
                    userScore = null, // OpenCritic no tiene user score
                    pressColor = getScoreColor(game.opencriticPress, "OpenCritic"), // 👈 Dinámico (Mighty/Strong...)
                    userColor = Color.Gray
                )

                // 3. IGDB (Color dinámico estándar)
                ScoreRow(
                    sourceName = "IGDB",
                    pressScore = game.igdbPress,
                    userScore = game.igdbUser,
                    pressColor = getScoreColor(game.igdbPress, "IGDB"),
                    userColor = getScoreColor(game.igdbUser, "IGDB")
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// Actualiza ScoreRow para aceptar colores separados
@Composable
fun ScoreRow(sourceName: String, pressScore: Int?, userScore: Int?, pressColor: Color, userColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nombre de la fuente (Usamos el color de prensa como distintivo o gris)
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            // Bolita identificativa (Opcional: usar color fijo de marca aquí o el dinámico)
            Surface(color = pressColor, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(sourceName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }

        // Nota Prensa
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ScorePill(score = pressScore, isPress = true, forcedColor = pressColor)
        }

        // Nota Usuario
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ScorePill(score = userScore, isPress = false, forcedColor = userColor)
        }
    }
}

// Actualiza ScorePill para aceptar color forzado
@Composable
fun ScorePill(score: Int?, isPress: Boolean, forcedColor: Color? = null) {
    if (score == null) {
        Text("--", color = Color.Gray)
    } else {
        // Si nos pasan color forzado, lo usamos. Si no, calculamos semáforo básico.
        val color = forcedColor ?: if (score >= 75) Color(0xFF4CAF50) else if (score >= 50) Color(0xFFFFC107) else Color(0xFFF44336)

        Surface(
            color = if (isPress) color else Color.Transparent,
            border = if (!isPress) BorderStroke(1.dp, color) else null,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = score.toString(),
                color = if (isPress) Color.White else color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun TierSelector(currentRating: Int?, onTierSelected: (Int) -> Unit) {
    // Definición de Tiers
    data class Tier(val label: String, val value: Int, val color: Color)
    val tiers = listOf(
        Tier("S+", 10, Color(0xFFFF7F7F)),
        Tier("S", 9, Color(0xFFFFBF7F)),
        Tier("A", 8, Color(0xFFFFDF7F)),
        Tier("B", 7, Color(0xFFC6EF88)),
        Tier("C", 5, Color(0xFFFFFFA1)),
        Tier("D", 3, Color(0xFFD3D3D3))
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Tu Puntuación (Tier)", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            tiers.forEach { tier ->
                val isSelected = currentRating == tier.value
                TierBadge(text = tier.label, color = tier.color, isSelected = isSelected, onClick = { onTierSelected(tier.value) })
            }
        }
    }
}

@Composable
fun TierBadge(text: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) color else Color.LightGray.copy(alpha = 0.3f)
    val textColor = if (isSelected) Color.Black.copy(alpha = 0.8f) else Color.Gray
    val border = if (isSelected) BorderStroke(2.dp, Color.Black.copy(alpha = 0.5f)) else null
    val elevation = if (isSelected) 6.dp else 0.dp
    val scale = if (isSelected) 1.1f else 1.0f

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = border,
        shadowElevation = elevation,
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() }
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun RegionBadge(region: String) {
    val (containerColor, contentColor) = when (region) {
        "NTSC-J" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "NTSC-U" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "PAL DE" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "PAL AU" -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        "NTSC-K" -> Color(0xFFE8EAF6) to Color(0xFF283593)
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Text(
            text = region,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// Función para calcular el color según la fuente y la nota
fun getScoreColor(score: Int?, source: String): Color {
    if (score == null) return Color.Gray

    return when (source) {
        "OpenCritic" -> when {
            // Escala Oficial OpenCritic
            score >= 84 -> Color(0xFFFC430A) // Mighty (Naranja Fuego)
            score >= 75 -> Color(0xFF9B59B6) // Strong (Morado)
            score >= 60 -> Color(0xFF3498DB) // Fair (Azul)
            else -> Color(0xFFE74C3C)        // Weak (Rojo)
        }
        "Metacritic" -> when {
            // AHORA SÍ: Tonos "Modernos" (Flat Design) que coinciden con la web actual
            score >= 75 -> Color(0xFF2ECC71) // Verde Esmeralda (Como en tu captura)
            score >= 50 -> Color(0xFFF1C40F) // Amarillo Sólido
            else -> Color(0xFFE74C3C)        // Rojo Alizarin
        }
        "IGDB Score", "IGDB" -> when {
            // Tonos "Clásicos" / Retro (o el estilo que prefieras para IGDB)
            score >= 75 -> Color(0xFF66CC33) // Verde Lima
            score >= 50 -> Color(0xFFFFCC33) // Amarillo Huevo
            else -> Color(0xFFFF0000)        // Rojo Puro
        }
        else -> Color.Gray
    }
}