package com.trunder.grimoiregames.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddGameClick: () -> Unit,
    onGameClick: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val games by viewModel.games.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()
    val searchText by viewModel.searchText.collectAsState()

    // Filtros
    val activeFilters by viewModel.filters.collectAsState()
    val availableData by viewModel.availableData.collectAsState()

    // Estados UI
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var gameToDelete by remember { mutableStateOf<Game?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // Estados internos del Diálogo de Filtros
    var selectedCategoryForFilter by remember { mutableStateOf<String?>(null) } // null = Viendo lista de categorías

    // 1. DIÁLOGO DE BORRADO
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

    // 2. NUEVO DIÁLOGO DE FILTROS AVANZADO
    if (showFilterDialog) {
        Dialog(onDismissRequest = {
            showFilterDialog = false
            selectedCategoryForFilter = null // Reset al cerrar
        }) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 600.dp).padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // --- CABECERA DEL DIÁLOGO ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCategoryForFilter != null) {
                            // Botón Atrás
                            IconButton(onClick = { selectedCategoryForFilter = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
                            }
                            Text(
                                text = selectedCategoryForFilter!!,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(48.dp)) // Equilibrio visual
                        } else {
                            // Título Principal
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

                    // --- CUERPO DEL DIÁLOGO (CAMBIA SEGÚN ESTADO) ---
                    LazyColumn(modifier = Modifier.weight(1f)) {

                        if (selectedCategoryForFilter == null) {
                            // VISTA A: LISTA DE CATEGORÍAS (Plataforma, Género, Estado...)
                            items(availableData.keys.toList()) { category ->
                                // Calculamos si hay filtros activos en esta categoría para mostrar un indicador
                                val isActive = when(category) {
                                    "Plataforma" -> activeFilters.platforms.isNotEmpty()
                                    "Género" -> activeFilters.genres.isNotEmpty()
                                    "Estado" -> activeFilters.statuses.isNotEmpty()
                                    "Desarrolladora" -> activeFilters.developers.isNotEmpty()
                                    "Distribuidora" -> activeFilters.publishers.isNotEmpty()
                                    "PEGI" -> activeFilters.pegis.isNotEmpty()
                                    "ESRB" -> activeFilters.esrbs.isNotEmpty()
                                    "Metacritic" -> activeFilters.metacriticRanges.isNotEmpty()
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
                                        if (isActive) {
                                            Spacer(Modifier.width(8.dp))
                                            Badge { Text("!") } // Indicador de filtro activo
                                        }
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "Ver opciones",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Gray
                                    )
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }

                        } else {
                            // VISTA B: LISTA DE OPCIONES (Switch, PS5, etc.)
                            val options = availableData[selectedCategoryForFilter] ?: emptyList()

                            items(options) { option ->
                                // Verificamos si está seleccionado
                                val isSelected = when(selectedCategoryForFilter) {
                                    "Plataforma" -> option in activeFilters.platforms
                                    "Género" -> option in activeFilters.genres
                                    "Estado" -> option in activeFilters.statuses
                                    "Desarrolladora" -> option in activeFilters.developers
                                    "Distribuidora" -> option in activeFilters.publishers
                                    "PEGI" -> option in activeFilters.pegis
                                    "ESRB" -> option in activeFilters.esrbs
                                    "Metacritic" -> option in activeFilters.metacriticRanges
                                    else -> false
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleFilter(selectedCategoryForFilter!!, option) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleFilter(selectedCategoryForFilter!!, option) }
                                    )
                                    Text(text = option, modifier = Modifier.padding(start = 8.dp))
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

    // --- SCAFFOLD PRINCIPAL (Igual que antes, con pequeña actualización en el botón del filtro) ---
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
                            // Lógica para contar filtros totales
                            val totalFilters = activeFilters.platforms.size + activeFilters.genres.size +
                                    activeFilters.statuses.size + activeFilters.developers.size +
                                    activeFilters.metacriticRanges.size // ...etc
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

                        // BOTÓN FILTRO
                        IconButton(onClick = { showFilterDialog = true }) {
                            val hasFilters = (activeFilters != GameFilters())
                            Icon(Icons.Default.FilterList, "Filtrar", tint = if (hasFilters) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }

                        // BOTÓN ORDENAR
                        Box {
                            IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Default.Sort, "Ordenar") }
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
        if (games.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(if (searchText.isNotEmpty() || activeFilters != GameFilters()) "No hay juegos con estos criterios" else "Tu Grimorio está vacío... ¡Añade loot!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), // 👈 AHORA SON 3 COLUMNAS
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp,
                    start = 8.dp, // Reducimos un poco los márgenes laterales (antes 12)
                    end = 8.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                games.forEach { (headerTitle, gamesInGroup) ->

                    // 1. LA CABECERA (IMPORTANTE: GridItemSpan(3))
                    // Esto asegura que el título ocupe TODO el ancho y no deje huecos
                    item(
                        span = { GridItemSpan(3) } // 👈 ¡ESTO ARREGLA EL DESAJUSTE!
                    ) {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 24.dp, bottom = 8.dp, start = 8.dp) // Alineado con las cartas
                                .fillMaxWidth()
                        )
                    }

                    // 2. LOS JUEGOS
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (platformIcon, platformColor) = when {
        game.platform.contains("Switch", true) -> Icons.Default.Gamepad to Color(0xFFE60012)
        game.platform.contains("GameCube", true) -> Icons.Default.Apps to Color(0xFF6A5ACD)
        game.platform.contains("Wii", true) -> Icons.Default.SportsEsports to Color(0xFF8DE3F5)
        game.platform.contains("PlayStation", true) -> Icons.Default.VideogameAsset to Color(0xFF003791)
        game.platform.contains("PC", true) -> Icons.Default.Computer to Color(0xFF4B4B4B)
        game.platform.contains("Xbox", true) -> Icons.Default.SportsEsports to Color(0xFF107C10)
        else -> Icons.Default.Games to Color.Black
    }

    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(1f) // Cuadrada
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box {
            // 1. Imagen de fondo
            AsyncImage(
                model = game.imageUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Iconos superiores (Igual que antes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icono Consola
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
                // Etiqueta Estado (Abreviada)
                Surface(
                    color = when(game.status) {
                        "Playing" -> Color(0xFFFFD600)
                        "Completed" -> Color(0xFF4CAF50)
                        else -> Color.DarkGray
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    val statusText = when(game.status) {
                        "Playing" -> "P"
                        "Completed" -> "C"
                        else -> "B"
                    }
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (game.status == "Playing") Color.Black else Color.White
                    )
                }
            }

            // 👇 3. LA SOMBRA (Degradado inferior)
            // Esta caja crea la "magia". Solo ocupa la parte baja y tiene un degradado suave.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp) // Altura fija para que no tape toda la imagen, solo lo necesario para 2 líneas de texto
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent, // Empieza transparente arriba
                                Color.Black.copy(alpha = 0.7f) // Termina en negro al 70% abajo del todo
                            )
                        )
                    )
            )

            // 👇 4. EL TÍTULO (Encima de la sombra)
            Text(
                text = game.title,
                modifier = Modifier
                    .align(Alignment.BottomStart) // Alineado abajo a la izquierda
                    .padding(horizontal = 8.dp, vertical = 8.dp) // Separación del borde
                    .fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start // Alineado a la izquierda queda más natural con sombra
            )
        }
    }
}