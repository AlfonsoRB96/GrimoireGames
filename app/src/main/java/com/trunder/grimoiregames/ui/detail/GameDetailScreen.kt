package com.trunder.grimoiregames.ui.detail

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
                    // Envoltorio horizontal
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp) // Un poco de aire arriba y abajo
                    ) {
                        // 2.1. Nombre de la Plataforma
                        Text(
                            text = currentSafeGame.platform,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp)) // Espacio entre texto y etiqueta

                        // 2.2. Etiqueta de Región
                        // Usamos currentSafeGame.region en vez de game!! para ser más seguros
                        RegionBadge(region = currentSafeGame.region)
                    }

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
                    val translatedDescription by viewModel.translatedDescription.collectAsState()
                    val isTranslating by viewModel.isTranslating.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sinopsis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        // LÓGICA DEL BOTÓN:
                        // Solo lo mostramos si aún NO tenemos la traducción
                        if (translatedDescription == null) {
                            if (isTranslating) {
                                // Si está cargando, mostramos ruedita pequeña
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                // Si no, mostramos botón "Traducir"
                                TextButton(
                                    onClick = {
                                        // Al pulsar, mandamos el texto original al ViewModel
                                        viewModel.translateDescription(currentSafeGame.description)
                                    },
                                    // Ajustamos padding para que no ocupe mucho
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        // Asegúrate de tener: import androidx.compose.material.icons.filled.Translate
                                        // Si no te sale el icono Translate, usa Icons.Default.Language o Icons.Default.Refresh
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Traducir", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // QUÉ TEXTO MOSTRAR:
                    // Si hay traducción, mostramos esa. Si no, la original. Si es null, el mensaje por defecto.
                    val descriptionToShow = translatedDescription ?: currentSafeGame.description ?: "No hay descripción disponible."

                    // Animamos suavemente el cambio de texto (Opcional, queda bonito)
                    AnimatedContent(
                        targetState = descriptionToShow,
                        label = "textChange"
                    ) { text ->
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
                    // C. CLASIFICACION POR EDADES
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clasificación", color = Color.Gray)

                        // 👇 AQUÍ VA EL BADGE
                        AgeRatingBadge(ratingString = currentSafeGame.ageRating)
                    }

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

                    // --- NUEVO SISTEMA DE TIERS ---
                    TierSelector(
                        currentRating = currentSafeGame.rating,
                        onTierSelected = { newRating ->
                            viewModel.updateRating(currentSafeGame, newRating)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- HORAS DE JUEGO ---
                    OutlinedTextField(
                        // LÓGICA: Si es 0 o null, ponemos texto vacío "" para que no moleste.
                        // Si tiene horas (ej: 15), mostramos "15".
                        value = if ((currentSafeGame.hoursPlayed ?: 0) == 0) "" else currentSafeGame.hoursPlayed.toString(),

                        onValueChange = { newValue ->
                            // Solo dejamos escribir números (evitamos letras o símbolos)
                            if (newValue.all { it.isDigit() }) {
                                val hours = newValue.toIntOrNull() ?: 0
                                viewModel.updateHours(currentSafeGame, hours)
                            }
                        },
                        label = { Text("Horas Jugadas") },
                        placeholder = { Text("0") }, // Esto se ve clarito cuando está vacío
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
fun AgeRatingBadge(ratingString: String?) {
    if (ratingString == null) return

    // Lógica de colores según el texto
    val color = when {
        // VERDE (Apto para todos)
        ratingString.contains("PEGI 3") || ratingString.contains("PEGI 7") ||
                ratingString.contains("ESRB E") || ratingString.contains("CERO A") -> Color(0xFF4CAF50)

        // AMARILLO/NARANJA (Adolescentes)
        ratingString.contains("PEGI 12") || ratingString.contains("PEGI 16") ||
                ratingString.contains("ESRB T") || ratingString.contains("CERO B") || ratingString.contains("CERO C") -> Color(0xFFFFC107)

        // ROJO (Adultos)
        ratingString.contains("PEGI 18") || ratingString.contains("ESRB M") ||
                ratingString.contains("CERO D") || ratingString.contains("CERO Z") -> Color(0xFFF44336)

        else -> Color.Gray
    }

    Surface(
        color = color, // Fondo de color sólido
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = ratingString,
            color = Color.White, // Texto blanco para contraste
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

@Composable
fun TierSelector(
    currentRating: Int?,
    onTierSelected: (Int) -> Unit
) {
    // Definimos los Tiers: Texto, Valor numérico y Color
    data class Tier(val label: String, val value: Int, val color: Color)

    val tiers = listOf(
        Tier("S+", 10, Color(0xFFFF7F7F)), // Rojo salmón
        Tier("S", 9, Color(0xFFFFBF7F)),   // Naranja suave
        Tier("A", 8, Color(0xFFFFDF7F)),   // Amarillo anaranjado
        Tier("B", 7, Color(0xFFC6EF88)),   // Verde lima
        Tier("C", 5, Color(0xFFFFFFA1)),   // Amarillo pálido
        Tier("D", 3, Color(0xFFD3D3D3))    // Gris
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Tu Puntuación (Tier)", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tiers.forEach { tier ->
                // Lógica: Si la nota coincide (o se acerca), está activo.
                // Usamos un rango pequeño para agrupar notas antiguas si tenías un 6, por ejemplo.
                val isSelected = currentRating == tier.value

                TierBadge(
                    text = tier.label,
                    color = tier.color,
                    isSelected = isSelected,
                    onClick = { onTierSelected(tier.value) }
                )
            }
        }
    }
}

@Composable
fun TierBadge(
    text: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Si no está seleccionado, lo mostramos gris apagado y con transparencia
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
            .size(width = 48.dp, height = 48.dp) // Cuadrados uniformes
            .clickable { onClick() }
            .graphicsLayer(scaleX = scale, scaleY = scale) // Efecto pop
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun RegionBadge(region: String) {
    val (containerColor, contentColor) = when (region) {
        "NTSC-J" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer // Rojo (Japón)
        "NTSC-U" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer // Azul/Morado (USA)
        "PAL DE" -> Color(0xFFFFF3E0) to Color(0xFFE65100) // Naranja (Alemania/USK)
        "PAL AU" -> Color(0xFFE0F2F1) to Color(0xFF00695C) // Verde azulado (Australia)
        "NTSC-K" -> Color(0xFFE8EAF6) to Color(0xFF283593) // Indigo (Corea)
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer // PAL EU (Standard)
    }

    Surface(
        color = containerColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Text(
            text = region,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}