package com.trunder.grimoiregames.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.trunder.grimoiregames.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlcDetailScreen(
    dlcName: String,
    onBackClick: () -> Unit,
    // 🧠 TRUCO DE HACKER: Reusamos el GameDetailViewModel.
    // Como la ruta tendrá "gameId", este ViewModel cargará automáticamente el juego padre. ¡Eficiencia pura!
    viewModel: GameDetailViewModel = hiltViewModel()
) {
    val gameState by viewModel.game.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del DLC") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        val game = gameState
        // Buscamos el DLC por nombre en la lista del juego cargado
        val dlc = game?.dlcs?.find { it.name == dlcName }

        if (dlc == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (game == null) CircularProgressIndicator()
                else Text("DLC no encontrado en la base de datos.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. PORTADA ÉPICA FULL SCREEN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp) // Altura dominante
                ) {
                    // Fondo borroso (Ambiente)
                    AsyncImage(
                        model = dlc.coverUrl ?: R.drawable.ic_logo_igdb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(50.dp)
                    )

                    // Imagen Principal
                    AsyncImage(
                        model = dlc.coverUrl ?: R.drawable.ic_logo_igdb,
                        contentDescription = dlc.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding() + 20.dp, bottom = 40.dp, start = 40.dp, end = 40.dp)
                    )

                    // Degradado inferior para el texto
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(200.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                                )
                            )
                    )
                }

                // 2. INFORMACIÓN
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp) // Superpuesto ligeramente
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = dlc.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Expansión para: ${game.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Fecha de lanzamiento
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lanzamiento: ${dlc.releaseDate ?: "Desconocida"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                }

                Spacer(modifier = Modifier.height(50.dp))

                Row(modifier = Modifier.padding(16.dp)) {
                    // BOTÓN: LO TENGO
                    FilterChip(
                        selected = dlc.isOwned,
                        onClick = {
                            viewModel.onDlcOwnershipChanged(game, dlc.name, true)
                        },
                        label = { Text("¡LO TENGO!") },
                        leadingIcon = {
                            if (dlc.isOwned) Icon(Icons.Default.Check, null)
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // BOTÓN: NO LO TENGO
                    FilterChip(
                        selected = !dlc.isOwned,
                        onClick = {
                            viewModel.onDlcOwnershipChanged(game, dlc.name, false)
                        },
                        label = { Text("NO LO TENGO") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}