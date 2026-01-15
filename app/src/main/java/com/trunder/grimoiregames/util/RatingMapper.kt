package com.trunder.grimoiregames.util

import com.trunder.grimoiregames.R

object RatingMapper {

    fun getDrawableFromString(ratingString: String?): Int? {
        if (ratingString == null) return null
        val upper = ratingString.uppercase()

        return when {
            // ==========================================
            // 🇪🇺 PEGI (Europa)
            // ==========================================
            // Lógica flexible: Busca "PEGI" Y el número. Da igual si es "(12)", " 12" o "-12".
            upper.contains("PEGI") && upper.contains("3") -> R.drawable.rating_pegi_3
            upper.contains("PEGI") && upper.contains("7") -> R.drawable.rating_pegi_7

            // Solución al problema "PEGI (12)": Ahora detecta el 1 y el 2 juntos
            upper.contains("PEGI") && upper.contains("12") -> R.drawable.rating_pegi_12
            upper.contains("PEGI") && upper.contains("16") -> R.drawable.rating_pegi_16
            upper.contains("PEGI") && upper.contains("18") -> R.drawable.rating_pegi_18

            // 🛠️ PARCHE DE DATOS SUCIOS IGDB (ID 9)
            // Si llega un ID 9 (ESRB E10+) etiquetado como PEGI:
            // ¿Quieres ver la realidad (ESRB) o mantener la estética (PEGI)?
            // Opción A (Realista): Muestra ESRB E10+ (Descomenta si prefieres ver el icono de ESRB)
            // upper.contains("PEGI") && upper.contains("9") -> R.drawable.rating_esrb_e10

            // Opción B (Estética): Forzamos a PEGI 7 (el más cercano) para no romper el estilo visual
            upper.contains("PEGI") && upper.contains("9") -> R.drawable.rating_pegi_7

            // ==========================================
            // 🇺🇸 ESRB (Norteamérica)
            // ==========================================
            // El orden importa: E10+ antes que E para evitar falsos positivos
            (upper.contains("ESRB") || upper.contains("EVERYONE")) && upper.contains("10") -> R.drawable.rating_esrb_e10
            upper.contains("ESRB") || upper.contains("EVERYONE") -> R.drawable.rating_esrb_e
            upper.contains("ESRB") || upper.contains("TEEN") -> R.drawable.rating_esrb_t
            upper.contains("ESRB") || upper.contains("MATURE") -> R.drawable.rating_esrb_m
            upper.contains("RP") || upper.contains("PENDING") -> R.drawable.rating_esrb_rp

            // ==========================================
            // 🇯🇵 CERO, 🇩🇪 USK, etc. (Simplificados)
            // ==========================================
            upper.contains("CERO") && upper.contains("A") -> R.drawable.rating_cero_a
            upper.contains("CERO") && upper.contains("B") -> R.drawable.rating_cero_b
            upper.contains("CERO") && upper.contains("C") -> R.drawable.rating_cero_c
            upper.contains("CERO") && upper.contains("D") -> R.drawable.rating_cero_d
            upper.contains("CERO") && upper.contains("Z") -> R.drawable.rating_cero_z

            upper.contains("USK") && (upper.contains("0") || upper.contains("AB 0")) -> R.drawable.rating_usk_0
            upper.contains("USK") && (upper.contains("6") || upper.contains("AB 6")) -> R.drawable.rating_usk_6
            upper.contains("USK") && (upper.contains("12") || upper.contains("AB 12")) -> R.drawable.rating_usk_12
            upper.contains("USK") && (upper.contains("16") || upper.contains("AB 16")) -> R.drawable.rating_usk_16
            upper.contains("USK") && (upper.contains("18") || upper.contains("AB 18")) -> R.drawable.rating_usk_18

            // Fallback Genérico para ClassInd y GRAC
            upper.contains("CLASSIND") && upper.contains("L") -> R.drawable.rating_classind_l
            upper.contains("CLASSIND") && upper.contains("10") -> R.drawable.rating_classind_10
            upper.contains("CLASSIND") && upper.contains("12") -> R.drawable.rating_classind_12
            upper.contains("CLASSIND") && upper.contains("14") -> R.drawable.rating_classind_14
            upper.contains("CLASSIND") && upper.contains("16") -> R.drawable.rating_classind_16
            upper.contains("CLASSIND") && upper.contains("18") -> R.drawable.rating_classind_18

            upper.contains("ACB") && upper.contains("PG") -> R.drawable.rating_acb_pg
            upper.contains("ACB") && upper.contains("M") -> R.drawable.rating_acb_m
            upper.contains("ACB") && upper.contains("G") -> R.drawable.rating_acb_g
            upper.contains("ACB") && (upper.contains("15") || upper.contains("MA15")) -> R.drawable.rating_acb_m15
            upper.contains("ACB") && (upper.contains("18") || upper.contains("R18")) -> R.drawable.rating_acb_r18

            else -> null
        }
    }
}