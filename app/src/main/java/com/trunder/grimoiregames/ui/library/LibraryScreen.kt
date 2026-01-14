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
import com.trunder.grimoiregames.ui.common.PlatformResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAddGameClick: () -> Unit,
    onGameClick: (Int) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    // --- DATA STATES ---
    val games by viewModel.games.collectAsState()
    val searchText by viewModel.searchText.collectAsState()

    // Filters
    val activeFilters by viewModel.filters.collectAsState()
    val availableData by viewModel.availableData.collectAsState()

    // --- UI STATES ---
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var gameToDelete by remember { mutableStateOf<Game?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val updateProgress by viewModel.updateProgress.collectAsState()

    // Filter Dialog internal states
    var selectedCategoryForFilter by remember { mutableStateOf<String?>(null) }

    // --- REFRESH STATES ---
    val pullRefreshState = rememberPullToRefreshState()

    // 1. DELETE DIALOG
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

    // 2. FILTER DIALOG
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
                    // Header
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

                    // Body
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (selectedCategoryForFilter == null) {
                            // Category List
                            items(availableData.keys.toList()) { category ->
                                val isActive = when(category) {
                                    "Plataforma" -> activeFilters.platforms.isNotEmpty()
                                    "Género" -> activeFilters.genres.isNotEmpty()
                                    "Estado" -> activeFilters.statuses.isNotEmpty()
                                    "Desarrolladora" -> activeFilters.developers.isNotEmpty()
                                    "Distribuidora" -> activeFilters.publishers.isNotEmpty()
                                    "Clasificación por edades" -> activeFilters.ageRatings.isNotEmpty()
                                    "Tier Personal" -> activeFilters.tiers.isNotEmpty()
                                    "Metacritic" -> activeFilters.metacriticRanges.isNotEmpty()
                                    "Año de Lanzamiento" -> activeFilters.releaseYears.isNotEmpty()
                                    // 🔴 CORRECCIÓN 1: Coincidir con el nombre del ViewModel
                                    "Horas de Juego" -> activeFilters.hourRanges.isNotEmpty()
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
                            // Option List
                            val options = availableData[selectedCategoryForFilter] ?: emptyList()

                            // -> SPECIAL LOGIC FOR PERSONAL TIER <-
                            if (selectedCategoryForFilter == "Tier Personal") {
                                item {
                                    // Define colors and order
                                    val tierColors = mapOf(
                                        "S+" to Color(0xFFFF7F7F),
                                        "S" to Color(0xFFFFBF7F),
                                        "A" to Color(0xFFFFDF7F),
                                        "B" to Color(0xFFC6EF88),
                                        "C" to Color(0xFFFFFFA1),
                                        "D" to Color(0xFFD3D3D3),
                                        "F" to Color(0xFF999999),
                                        "Sin Puntuación" to Color.LightGray
                                    )

                                    Column {
                                        options.forEach { option ->
                                            val isSelected = option in activeFilters.tiers
                                            val tierColor = tierColors[option] ?: MaterialTheme.colorScheme.surfaceVariant

                                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { viewModel.toggleFilter("Tier Personal", option) },
                                                    label = {
                                                        Text(
                                                            text = option,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) Color.Black else Color.Unspecified
                                                        )
                                                    },
                                                    leadingIcon = if (isSelected) {
                                                        { Icon(Icons.Default.Check, "Seleccionado", modifier = Modifier.size(FilterChipDefaults.IconSize), tint = Color.Black) }
                                                    } else null,
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = tierColor,
                                                        selectedLabelColor = Color.Black,
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                    ),
                                                    border = FilterChipDefaults.filterChipBorder(
                                                        enabled = true,
                                                        selected = isSelected,
                                                        borderColor = if (isSelected) Color.Black else tierColor.copy(alpha = 0.5f)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // -> GENERIC LOGIC FOR OTHER FILTERS <-
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
                                        // 🔴 CORRECCIÓN 2: Coincidir con el nombre del ViewModel
                                        "Horas de Juego" -> option in activeFilters.hourRanges
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
                                    activeFilters.releaseYears.size + activeFilters.tiers.size +
                                    activeFilters.hourRanges.size // 🆕 Contamos también el filtro de horas
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
                                DropdownMenuItem(text = { Text("Tier") }, onClick = { viewModel.onSortChange(SortOption.TIER); showSortMenu = false })
                                DropdownMenuItem(text = { Text("Horas de Juego") }, onClick = { viewModel.onSortChange(SortOption.HOURS); showSortMenu = false }
                                )
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
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.refreshLibrary() },
            state = pullRefreshState,
            modifier = Modifier.padding(innerPadding)
        ) {
            if (games.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (searchText.isNotEmpty() || activeFilters != GameFilters()) "No hay juegos con estos criterios" else "Tu Grimorio está vacío... ¡Añade loot!")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
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
    val context = androidx.compose.ui.platform.LocalContext.current

    val theme = remember(game.platform) {
        PlatformResolver.getTheme(context, game.platform)
    }

    fun getRegionFlag(region: String): String {
        return when (region) {
            "NTSC-U" -> "🇺🇸"; "PAL" -> "🇪🇺"; "PAL EU" -> "🇪🇺"
            "NTSC-J" -> "🇯🇵"; "PAL DE" -> "🇩🇪"; "NTSC-K" -> "🇰🇷"
            "NTSC-BR" -> "🇧🇷"; "PAL AU" -> "🇦🇺"; else -> "🌐"
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
            AsyncImage(
                model = game.imageUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        color = theme.color.copy(alpha = 0.9f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        if (theme.iconResId != null) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = theme.iconResId),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.padding(4.dp)
                            )
                        } else {
                            Icon(
                                imageVector = theme.fallbackVector,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(5.dp)
                            )
                        }
                    }

                    Text(
                        text = getRegionFlag(game.region),
                        style = MaterialTheme.typography.titleMedium.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 1f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                blurRadius = 12f
                            )
                        )
                    )
                }

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
            )
            Text(
                text = game.title,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).fillMaxWidth(),
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
    Dialog(onDismissRequest = { }) {
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