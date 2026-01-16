package com.trunder.grimoiregames.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.trunder.grimoiregames.R
import java.util.Locale

data class PlatformTheme(
    val iconResId: Int?,
    val color: Color,
    val contentDescription: String,
    val fallbackVector: ImageVector? = null // Añadido para soporte legacy en Library
)

object PlatformResolver {

    // =================================================================
    // 🏛️ MODO BIBLIOTECA (LibraryScreen)
    // =================================================================
    // Devuelve iconos generalistas de MARCA (Nintendo, Sony, Xbox)
    // o Vectores minimalistas si no hay logo de marca.
    fun getLibraryTheme(platformName: String): PlatformTheme {
        val lower = platformName.lowercase(Locale.ROOT)

        // 1. Definimos el Vector de respaldo (El comportamiento clásico)
        val vector = when {
            lower.contains("pc") || lower.contains("windows") -> Icons.Default.Computer
            lower.contains("boy") || lower.contains("ds") || lower.contains("switch") -> Icons.Default.Smartphone // O Tv para Switch
            lower.contains("playstation") -> Icons.Default.VideogameAsset
            lower.contains("xbox") -> Icons.Default.SportsEsports
            lower.contains("wii") -> Icons.Default.SportsTennis
            else -> Icons.Default.Gamepad
        }

        // 2. Intentamos agrupar por MARCA (Si tienes el logo genérico ic_p_nintendo, úsalo)
        // Si prefieres usar SIEMPRE vectores en la Library, pon iconResId = null en todos.
        return when {
            // NINTENDO (Agrupado)
            lower.contains("super") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_snes, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("entertainment") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_nes, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("64") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_64, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("gamecube") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_gamecube, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFF000000),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("wii u") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_wiiu, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("wii") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_wii, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("switch 2") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_switch2, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFE60012),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("switch") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_switch, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFE60012),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("color") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_gbc, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("advance") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_gba, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("game boy") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_gb, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("3ds") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_3ds, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // NINTENDO (Agrupado)
            lower.contains("ds") ->
                PlatformTheme(
                    iconResId = R.drawable.ic_logo_mini_nintendo_ds, // <--- Úsalo si tienes el logo ROJO de Nintendo. Si no, pon null.
                    color = Color(0xFFFFFFFF),
                    contentDescription = "Nintendo",
                    fallbackVector = vector
                )

            // SONY
            lower.contains("playstation") || lower.contains("ps") ->
                PlatformTheme(
                    iconResId = null,
                    color = Color(0xFF003791),
                    contentDescription = "PlayStation",
                    fallbackVector = vector
                )

            // XBOX
            lower.contains("xbox") ->
                PlatformTheme(
                    iconResId = null,
                    color = Color(0xFF107C10),
                    contentDescription = "Xbox",
                    fallbackVector = vector
                )

            // RESTO (PC, SEGA...)
            else -> PlatformTheme(null, Color.Gray, platformName, vector)
        }
    }

    // =================================================================
    // 🔍 MODO DETALLE (GameDetailScreen)
    // =================================================================
    // Devuelve iconos ESPECÍFICOS de consola (Switch, NES, SNES...)
    fun getDetailTheme(platformName: String): PlatformTheme {
        val lower = platformName.lowercase(Locale.ROOT)

        return when {
            // === 1. SWITCH FAMILY (El hijo antes que el padre) ===
            lower.contains("switch 2") || lower.contains("switch2") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_switch2, Color(0xFFE60012), "Nintendo Switch 2")

            lower.contains("switch") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_switch, Color(0xFFE60012), "Nintendo Switch")

            // === 2. WII FAMILY ===
            lower.contains("wii u") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_wiiu, Color(0xFF009AC7), "Wii U")

            lower.contains("wii") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_wii, Color(0xFF009AC7), "Wii")

            // === 3. GAMECUBE ===
            lower.contains("gamecube") || lower.contains("ngc") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_gamecube, Color(0xFF6A5ACD), "GameCube")

            // === 4. NINTENDO 64 (Fix: Detectar nombre completo) ===
            // Antes fallaba porque "Nintendo 64" no contiene "n64"
            lower.contains("n64") || lower.contains("nintendo 64") || lower.contains("64") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_64, Color(0xFFFEBA34), "Nintendo 64")

            // === 5. SNES (Fix: Detectar "Super Nintendo") ===
            // Antes fallaba porque "Super Nintendo..." no contiene "snes"
            lower.contains("snes") || lower.contains("super nintendo") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_snes, Color(0xFF514689), "Super Nintendo")

            // === 6. NES (Fix: Detectar "Entertainment System") ===
            // Cuidado: "Nintendo Entertainment System" contiene "Nintendo",
            // así que si tuvieras una regla general de "Nintendo" antes, fallaría.
            lower.contains("nes") || lower.contains("nintendo entertainment system") || lower.contains("famicom") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_nes, Color(0xFF8B0000), "NES")

            // === 7. PORTÁTILES (Orden: 3DS > DS > GBA > GBC > GB) ===
            lower.contains("3ds") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_3ds, Color(0xFFCE181E), "Nintendo 3DS")

            lower.contains("ds") -> // Captura "Nintendo DS", "DSi", "DS Lite"
                PlatformTheme(R.drawable.ic_logo_nintendo_ds, Color(0xFF5DADE2), "Nintendo DS")

            lower.contains("advance") || lower.contains("gba") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_gba, Color(0xFF3F007D), "Game Boy Advance")

            // 🚨 GAME BOY COLOR (Fix: Antes que Game Boy normal)
            // Si tienes un icono específico para GBC, úsalo aquí.
            // Si no, usa el de GBA o GB pero con el texto correcto.
            lower.contains("color") || lower.contains("gbc") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_gbc, Color(0xFF009AC7), "Game Boy Color") // ⚠️ Asegúrate de tener ic_p_gbc o usa otro

            // GAME BOY CLÁSICA (El cajón de sastre de las "Boy")
            lower.contains("boy") ->
                PlatformTheme(R.drawable.ic_logo_nintendo_gb, Color(0xFF8BAC0F), "Game Boy")

            // === RESTO ===
            else -> getLibraryTheme(platformName)
        }
    }
}