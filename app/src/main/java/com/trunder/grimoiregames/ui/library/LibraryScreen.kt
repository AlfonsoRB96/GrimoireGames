package com.trunder.grimoiregames.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.trunder.grimoiregames.data.entity.Game
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.collections.get
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class) // Necesario para PullToRefresh
@Composable
fun LibraryScreen(
    onAddGameClick: () -> Unit,
    onGameClick: (Int) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel() // O LibraryViewModel según como lo hayas renombrado
) {
    // --- ESTADOS DE DATOS ---
    val games by viewModel.games.collectAsState()
    val searchText by viewModel.searchText.collectAsState()

    // Filtros
    val activeFilters by viewModel.filters.collectAsState()
    val availableData by viewModel.availableData.collectAsState()

    // --- ESTADOS DE UI ---
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var gameToDelete by remember { mutableStateOf<Game?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val updateProgress by viewModel.updateProgress.collectAsState()

    // Estados internos del Diálogo de Filtros
    var selectedCategoryForFilter by remember { mutableStateOf<String?>(null) }

    // --- ESTADOS NUEVOS PARA REFRESCAR ---
    // Estado del componente visual de Material 3
    val pullRefreshState = rememberPullToRefreshState()

    // 1. DIÁLOGO DE BORRADO (Sin cambios)
    if (gameToDelete != null) {
        AlertDialog(
            onDismissRequest = { gameToDelete = null },
            title = { Text("¿Eliminar juego?") },
            text = { Text("¿Estás seguro de que quieres borrar '${gameToDelete?.title}' de tu biblioteca?") },
            confirmButton = {
                TextButton(onClick = { gameToDelete?.let { viewModel.deleteGame(it) }; gameToDelete = null }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { gameToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    // 2. DIÁLOGO DE FILTROS (Sin cambios)
    if (showFilterDialog) {
        Dialog(onDismissRequest = {
            showFilterDialog = false
            selectedCategoryForFilter = null
        }) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 600.dp).padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Cabecera
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCategoryForFilter != null) {
                            IconButton(onClick = { selectedCategoryForFilter = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
                            }
                            Text(
                                text = selectedCategoryForFilter!!,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(48.dp))
                        } else {
                            Text(
                                text = "Categorías de Filtro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { viewModel.clearAllFilters() }) {
                                Text("Limpiar todo", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Cuerpo
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (selectedCategoryForFilter == null) {
                            items(availableData.keys.toList()) { category ->
                                val isActive = when(category) {
                                    "Plataforma" -> activeFilters.platforms.isNotEmpty()
                                    "Género" -> activeFilters.genres.isNotEmpty()
                                    "Estado" -> activeFilters.statuses.isNotEmpty()
                                    "Desarrolladora" -> activeFilters.developers.isNotEmpty()
                                    "Distribuidora" -> activeFilters.publishers.isNotEmpty()
                                    "Clasificación por edades" -> activeFilters.ageRatings.isNotEmpty()
                                    "Metacritic" -> activeFilters.metacriticRanges.isNotEmpty()
                                    "Año de Lanzamiento" -> activeFilters.releaseYears.isNotEmpty()
                                    else -> false
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCategoryForFilter = category }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = category, style = MaterialTheme.typography.bodyLarge)
                                        if (isActive) { Spacer(Modifier.width(8.dp)); Badge { Text("!") } }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "Ver", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        } else {
                            val options = availableData[selectedCategoryForFilter] ?: emptyList()
                            items(options) { option ->
                                val isSelected = when(selectedCategoryForFilter) {
                                    "Plataforma" -> option in activeFilters.platforms
                                    "Género" -> option in activeFilters.genres
                                    "Estado" -> option in activeFilters.statuses
                                    "Desarrolladora" -> option in activeFilters.developers
                                    "Distribuidora" -> option in activeFilters.publishers
                                    "Clasificación por edades" -> option in activeFilters.ageRatings
                                    "Metacritic" -> option in activeFilters.metacriticRanges
                                    "Año de Lanzamiento" -> option in activeFilters.releaseYears
                                    else -> false
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.toggleFilter(selectedCategoryForFilter!!, option) },
                                        label = { Text(option) },
                                        leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, "Seleccionado", modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showFilterDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver Resultados")
                    }
                }
            }
        }
    }

    if (showUpdateDialog) {
        UpdateProgressDialog(progress = updateProgress)
    }

    // --- SCAFFOLD ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchText, onValueChange = viewModel::onSearchTextChange,
                            placeholder = { Text("Buscar por nombre...") }, singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                            ), modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text("Grimoire Games")
                            val totalFilters = activeFilters.platforms.size + activeFilters.genres.size +
                                    activeFilters.statuses.size + activeFilters.developers.size +
                                    activeFilters.releaseYears.size
                            if (totalFilters > 0) {
                                Text("Filtros activos: $totalFilters", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = { isSearching = false; viewModel.onSearchTextChange("") }) {
                            Icon(Icons.Default.Close, "Cerrar")
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) { Icon(Icons.Default.Search, "Buscar") }
                        IconButton(onClick = { showFilterDialog = true }) {
                            val hasFilters = (activeFilters != GameFilters())
                            Icon(Icons.Default.FilterList, "Filtrar", tint = if (hasFilters) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Ordenar") }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(text = { Text("Alfabético") }, onClick = { viewModel.onSortChange(SortOption.TITLE); showSortMenu = false })
                                DropdownMenuItem(text = { Text("Plataforma") }, onClick = { viewModel.onSortChange(SortOption.PLATFORM); showSortMenu = false })
                                DropdownMenuItem(text = { Text("Estado") }, onClick = { viewModel.onSortChange(SortOption.STATUS); showSortMenu = false })
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGameClick) { Icon(Icons.Default.Add, "Añadir") }
        }
    ) { innerPadding ->

        // 👇 AQUI INTEGRAMOS EL PULL TO REFRESH
        // Envolvemos todo el contenido (Lista o Vacío) en la caja de refresco
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.refreshLibrary() }, // Asegúrate de tener esta función en tu ViewModel
            state = pullRefreshState,
            modifier = Modifier.padding(innerPadding) // El padding del Scaffold va aquí
        ) {
            if (games.isEmpty()) {
                // Hacemos que la caja vacía sea scrolleable para que funcione el gesto de refrescar aunque esté vacía
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()), // Importante para el gesto
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (searchText.isNotEmpty() || activeFilters != GameFilters()) "No hay juegos con estos criterios" else "Tu Grimorio está vacío... ¡Añade loot!")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    // Quitamos el innerPadding del contentPadding porque ya lo aplicamos al PullToRefreshBox
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 80.dp,
                        start = 8.dp, end = 8.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    games.forEach { (headerTitle, gamesInGroup) ->
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 8.dp).fillMaxWidth()
                            )
                        }
                        items(gamesInGroup) { game ->
                            GameCard(
                                game = game,
                                onClick = { onGameClick(game.id) },
                                onLongClick = { gameToDelete = game }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(game: Game, onClick: () -> Unit, onLongClick: () -> Unit) {
    // 1. Lógica de colores de plataforma
    val (platformIcon, platformColor) = when {
        game.platform.contains("Switch", true) -> Icons.Default.Gamepad to Color(0xFFE60012)
        game.platform.contains("GameCube", true) -> Icons.Default.Apps to Color(0xFF6A5ACD)
        game.platform.contains("Wii", true) -> Icons.Default.SportsEsports to Color(0xFF8DE3F5)
        game.platform.contains("PlayStation", true) -> Icons.Default.VideogameAsset to Color(0xFF003791)
        game.platform.contains("PC", true) -> Icons.Default.Computer to Color(0xFF4B4B4B)
        game.platform.contains("Xbox", true) -> Icons.Default.SportsEsports to Color(0xFF107C10)
        else -> Icons.Default.Games to Color.Black
    }

    // 2. Función auxiliar para la bandera (Local)
    fun getRegionFlag(region: String): String {
        return when (region) {
            "NTSC-U"  -> "🇺🇸"
            "PAL"     -> "🇪🇺"
            "PAL EU"  -> "🇪🇺" // Por si acaso usas la variante larga
            "NTSC-J"  -> "🇯🇵"
            "PAL DE"  -> "🇩🇪"
            "NTSC-K"  -> "🇰🇷"
            "NTSC-BR" -> "🇧🇷"
            "PAL AU"  -> "🇦🇺"
            else      -> "🌐"
        }
    }

    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box {
            // FONDO: Imagen
            AsyncImage(
                model = game.imageUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // CAPA SUPERIOR: Iconos y Estado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .align(Alignment.TopCenter),
                // Alineamos arriba para que el badge de estado no se mueva si la columna de la izq crece
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // IZQUIERDA: Columna con Icono Plataforma + Bandera
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp) // Espacio entre icono y bandera
                ) {
                    // Icono Plataforma
                    Surface(
                        color = platformColor.copy(alpha = 0.9f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = platformIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(5.dp)
                        )
                    }

                    // Bandera Región (Estilo pegatina pequeña)
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f), // Fondo oscuro semitransparente para legibilidad
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = getRegionFlag(game.region),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // DERECHA: Estado (Playing, Completed...)
                Surface(
                    color = when (game.status) {
                        "Playing" -> Color(0xFFFFD600)
                        "Completed" -> Color(0xFF4CAF50)
                        else -> Color.DarkGray
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (game.status == "Playing") "P" else if (game.status == "Completed") "C" else "B",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (game.status == "Playing") Color.Black else Color.White
                    )
                }
            }

            // CAPA INFERIOR: Gradiente y Título
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            Text(
                text = game.title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
fun UpdateProgressDialog(
    progress: Int
) {
    Dialog(onDismissRequest = { /* No dejamos cerrar tocando fuera para que no cancelen el proceso */ }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Actualizando Biblioteca",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Barra de progreso lineal
                // Convertimos el int (0..100) a float (0.0..1.0) para el componente
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Obteniendo últimas notas y datos...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

fun getRegionFlag(region: String): String {
    return when (region) {
        "NTSC-U"  -> "🇺🇸" // Estados Unidos
        "PAL"     -> "🇪🇺" // Europa (Unión Europea)
        "NTSC-J"  -> "🇯🇵" // Japón
        "PAL DE"  -> "🇩🇪" // Alemania
        "NTSC-K"  -> "🇰🇷" // Corea del Sur
        "NTSC-BR" -> "🇧🇷" // Brasil
        "PAL AU"  -> "🇦🇺" // Australia
        else      -> "🌐" // Globo (para desconocidos)
    }
}