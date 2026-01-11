package com.trunder.grimoiregames.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.trunder.grimoiregames.data.entity.Game
import androidx.compose.material.icons.filled.*
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
    var showMenu by remember { mutableStateOf(false) }
    // Estado para guardar el juego que se ha pulsado prolongadamente
    var gameToDelete by remember { mutableStateOf<Game?>(null) }

    // Lógica del Diálogo de Confirmación
    if (gameToDelete != null) {
        AlertDialog(
            onDismissRequest = { gameToDelete = null },
            title = { Text("¿Eliminar juego?") },
            text = { Text("¿Estás seguro de que quieres borrar '${gameToDelete?.title}' de tu biblioteca?") },
            confirmButton = {
                TextButton(onClick = {
                    gameToDelete?.let { viewModel.deleteGame(it) }
                    gameToDelete = null
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { gameToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grimoire Games") },
                actions = {
                    // BOTÓN DE ORDENAR
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Ordenar")
                        }

                        // EL MENÚ DESPLEGABLE
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Opción 1: Alfabético
                            DropdownMenuItem(
                                text = { Text("Alfabético (A-Z)") },
                                onClick = {
                                    viewModel.onSortChange(SortOption.TITLE)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    if (currentSort == SortOption.TITLE)
                                        Icon(Icons.Default.Check, null)
                                }
                            )

                            // Opción 2: Por Plataforma
                            DropdownMenuItem(
                                text = { Text("Por Plataforma") },
                                onClick = {
                                    viewModel.onSortChange(SortOption.PLATFORM)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    if (currentSort == SortOption.PLATFORM)
                                        Icon(Icons.Default.Check, null)
                                }
                            )

                            // Opción 3: Por Estado
                            DropdownMenuItem(
                                text = { Text("Por Estado") },
                                onClick = {
                                    viewModel.onSortChange(SortOption.STATUS)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    if (currentSort == SortOption.STATUS)
                                        Icon(Icons.Default.Check, null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGameClick) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Juego")
            }
        }
    ) { innerPadding ->
        if (games.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tu Grimorio está vacío... ¡Añade loot!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                // Iteramos sobre el Mapa (Cabecera -> ListaDeJuegos)
                games.forEach { (headerTitle, gamesInGroup) ->

                    // 1. LA CABECERA (Ocupa 2 espacios, ancho completo)
                    item(
                        span = { GridItemSpan(2) } // 👈 Esto hace que ocupe todo el ancho
                    ) {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 24.dp, bottom = 8.dp, start = 4.dp)
                                .fillMaxWidth()
                        )
                    }

                    // 2. LOS JUEGOS DE ESE GRUPO
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

@OptIn(ExperimentalFoundationApi::class) // Necesario para combinedClickable
@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    onLongClick: () -> Unit // 👈 Nueva acción
) {

    val (platformIcon, platformColor) = when {
        // FAMILIA NINTENDO (Rojo)
        game.platform.contains("Switch", ignoreCase = true) ->
            Icons.Default.Gamepad to Color(0xFFE60012)
        game.platform.contains("GameCube", ignoreCase = true) ->
            Icons.Default.Apps to Color(0xFF6A5ACD) // Morado GameCube
        game.platform.contains("Wii", ignoreCase = true) ->
            Icons.Default.SportsEsports to Color(0xFF8DE3F5)

        // FAMILIA PLAYSTATION (Azul)
        game.platform.contains("PlayStation 5", ignoreCase = true) ||
                game.platform.contains("PlayStation 4", ignoreCase = true) ->
            Icons.Default.VideogameAsset to Color(0xFF003791)
        game.platform.contains("PlayStation 2", ignoreCase = true) ->
            Icons.Default.SettingsInputComponent to Color(0xFF003791)

        // PC Y OTROS (Gris/Verde)
        game.platform.contains("PC", ignoreCase = true) ->
            Icons.Default.Computer to Color(0xFF4B4B4B)
        game.platform.contains("Xbox", ignoreCase = true) ->
            Icons.Default.SportsEsports to Color(0xFF107C10)

        else -> Icons.Default.Games to Color.Black
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(220.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            // Imagen de la carátula
            AsyncImage(
                model = game.imageUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth() //
                    .padding(8.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. ICONO DE LA CONSOLA (Esquina Izquierda)
                Surface(
                    color = platformColor.copy(alpha = 0.9f),
                    shape = CircleShape,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = platformIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }

                // 2. ETIQUETA DE ESTADO (Esquina Derecha)
                Surface(
                    color = when(game.status) {
                        "Playing" -> Color(0xFFFFD600)
                        "Completed" -> Color(0xFF4CAF50)
                        else -> Color.DarkGray
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = game.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (game.status == "Playing") Color.Black else Color.White
                    )
                }
            }

            // Título sobre fondo degradado
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 300f
                        )
                    )
            ) {
                Text(
                    text = game.title,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}