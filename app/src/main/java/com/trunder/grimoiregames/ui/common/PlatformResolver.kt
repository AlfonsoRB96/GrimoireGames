package com.trunder.grimoiregames.ui.common

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

data class PlatformTheme(
    val iconResId: Int?,       // Si tenemos un PNG/SVG en drawable
    val fallbackVector: ImageVector, // Si no tenemos nada, usamos un icono material
    val color: Color
)

object PlatformResolver {

    fun getTheme(context: Context, platformName: String): PlatformTheme {
        // 1. Limpiamos el nombre para que sea un nombre de archivo válido
        // Ej: "Super Nintendo (SNES)" -> "snes"
        // Ej: "PlayStation 5" -> "playstation_5"
        val normalizedName = normalizePlatformName(platformName)

        // 2. Buscamos si existe un archivo "ic_p_[nombre]" en drawable
        // El prefijo "ic_p_" es para tenerlos ordenados (icon_platform)
        val resourceName = "ic_p_$normalizedName"
        val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)

        // 3. Resolvemos el color
        val color = resolveColor(platformName, normalizedName)

        return PlatformTheme(
            iconResId = if (resId != 0) resId else null,
            fallbackVector = resolveFallbackVector(platformName),
            color = color
        )
    }

    private fun normalizePlatformName(name: String): String {
        return name.lowercase(Locale.ROOT)
            .replace("entertainment system", "") // Quitar apellidos largos
            .replace("(", "")
            .replace(")", "")
            .trim()
            .replace(" ", "_")
            .replace("-", "_")
            // Truco: Si el nombre contiene "snes" aunque sea largo, forzamos "snes"
            .let { if (it.contains("snes")) "snes" else it }
            .let { if (it.contains("nes")) "nes" else it }
            .let { if (it.contains("switch")) "switch" else it }
            .let { if (it.contains("megadrive") || it.contains("genesis")) "megadrive" else it }
    }

    private fun resolveFallbackVector(name: String): ImageVector {
        val n = name.lowercase()
        return when {
            n.contains("pc") || n.contains("windows") -> Icons.Default.Computer
            n.contains("boy") || n.contains("ds") -> Icons.Default.Smartphone // Portátiles
            n.contains("playstation") -> Icons.Default.VideogameAsset
            n.contains("xbox") -> Icons.Default.SportsEsports
            n.contains("wii") -> Icons.Default.SportsTennis
            else -> Icons.Default.Gamepad // Genérico
        }
    }

    private fun resolveColor(originalName: String, normalized: String): Color {
        // Colores manuales para los grandes
        return when {
            normalized.contains("switch") -> Color(0xFFE60012) // Rojo Nintendo
            normalized.contains("gamecube") -> Color(0xFF6A5ACD) // Morado
            normalized.contains("wii") -> Color(0xFF009AC7) // Azulito Wii
            normalized.contains("playstation") -> Color(0xFF003791) // Azul Sony
            normalized.contains("xbox") -> Color(0xFF107C10) // Verde Xbox
            normalized.contains("pc") || normalized.contains("windows") -> Color(0xFF333333)
            normalized.contains("snes") -> Color(0xFF514689) // Gris/Morado SNES USA
            normalized.contains("nes") -> Color(0xFF8B0000) // Rojo oscuro
            normalized.contains("megadrive") || normalized.contains("genesis") -> Color(0xFF000000)

            // GENERADOR DE COLOR AUTOMÁTICO PARA EL RESTO
            // Si metes "Atari Jaguar", generará un color único basado en su nombre
            // para que no salga siempre negro.
            else -> generateColorFromString(originalName)
        }
    }

    private fun generateColorFromString(text: String): Color {
        val hash = text.hashCode()
        // Generamos un color pastel oscuro para que las letras blancas se lean bien
        val r = (hash and 0xFF0000 shr 16)
        val g = (hash and 0x00FF00 shr 8)
        val b = (hash and 0x0000FF)
        return Color(r, g, b).copy(alpha = 1f).let {
            // Aseguramos que no sea demasiado claro (truco rápido)
            if (it.red > 0.8f && it.green > 0.8f) Color.DarkGray else it
        }
    }
}