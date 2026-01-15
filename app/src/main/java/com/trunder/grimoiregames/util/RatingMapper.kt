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
            upper.contains("PEGI") && upper.contains("3") -> R.drawable.rating_pegi_3
            upper.contains("PEGI") && upper.contains("7") -> R.drawable.rating_pegi_7
            upper.contains("PEGI") && upper.contains("12") -> R.drawable.rating_pegi_12
            upper.contains("PEGI") && upper.contains("16") -> R.drawable.rating_pegi_16
            upper.contains("PEGI") && upper.contains("18") -> R.drawable.rating_pegi_18

            // ==========================================
            // 🇺🇸 ESRB (Norteamérica) - 🔧 CORREGIDO ORDEN
            // ==========================================

            // 1. E10+ (Prioridad máxima por contener "10")
            (upper.contains("ESRB") || upper.contains("EVERYONE")) && upper.contains("10") -> R.drawable.rating_esrb_e10

            // 2. RP - Pending (¡Aquí estaba el bug! Ahora lo comprobamos antes)
            upper.contains("RP") || upper.contains("PENDING") -> R.drawable.rating_esrb_rp

            // 3. AO - Adults Only
            upper.contains("AO") || (upper.contains("ADULTS") && upper.contains("ONLY")) -> R.drawable.rating_esrb_ao

            // 4. M - Mature
            upper.contains("MATURE") || (upper.contains("ESRB") && upper.contains("M")) -> R.drawable.rating_esrb_m

            // 5. T - Teen
            upper.contains("TEEN") || (upper.contains("ESRB") && upper.contains("T")) -> R.drawable.rating_esrb_t

            // 6. E - Everyone (El "cajón desastre" va al final)
            // Esto captura "ESRB E", "ESRB EC" (Early Childhood) y "ESRB Everyone"
            upper.contains("ESRB") || upper.contains("EVERYONE") -> R.drawable.rating_esrb_e

            // ==========================================
            // 🇰🇷 GRAC (Corea)
            // ==========================================
            upper.contains("GRAC") && (upper.contains("ALL") || upper.contains("E")) -> R.drawable.rating_grac_all
            upper.contains("GRAC") && upper.contains("12") -> R.drawable.rating_grac_12
            upper.contains("GRAC") && upper.contains("15") -> R.drawable.rating_grac_15
            upper.contains("GRAC") && upper.contains("18") -> R.drawable.rating_grac_18

            // ==========================================
            // 🇯🇵 CERO (Japón)
            // ==========================================
            upper.contains("CERO") && upper.contains("A") -> R.drawable.rating_cero_a
            upper.contains("CERO") && upper.contains("B") -> R.drawable.rating_cero_b
            upper.contains("CERO") && upper.contains("C") -> R.drawable.rating_cero_c
            upper.contains("CERO") && upper.contains("D") -> R.drawable.rating_cero_d
            upper.contains("CERO") && upper.contains("Z") -> R.drawable.rating_cero_z

            // ==========================================
            // 🇩🇪 USK (Alemania)
            // ==========================================
            upper.contains("USK") && (upper.contains("0") || upper.contains("AB 0")) -> R.drawable.rating_usk_0
            upper.contains("USK") && (upper.contains("6") || upper.contains("AB 6")) -> R.drawable.rating_usk_6
            upper.contains("USK") && (upper.contains("12") || upper.contains("AB 12")) -> R.drawable.rating_usk_12
            upper.contains("USK") && (upper.contains("16") || upper.contains("AB 16")) -> R.drawable.rating_usk_16
            upper.contains("USK") && (upper.contains("18") || upper.contains("AB 18")) -> R.drawable.rating_usk_18

            // ==========================================
            // 🇧🇷 ClassInd (Brasil)
            // ==========================================
            upper.contains("CLASSIND") && (upper.contains("L") || upper.contains("LIVRE")) -> R.drawable.rating_classind_l
            upper.contains("CLASSIND") && upper.contains("10") -> R.drawable.rating_classind_10
            upper.contains("CLASSIND") && upper.contains("12") -> R.drawable.rating_classind_12
            upper.contains("CLASSIND") && upper.contains("14") -> R.drawable.rating_classind_14
            upper.contains("CLASSIND") && upper.contains("16") -> R.drawable.rating_classind_16
            upper.contains("CLASSIND") && upper.contains("18") -> R.drawable.rating_classind_18

            // ==========================================
            // 🇦🇺 ACB (Australia)
            // ==========================================
            upper.contains("ACB") && upper.contains("PG") -> R.drawable.rating_acb_pg
            upper.contains("ACB") && (upper.contains("MA15") || upper.contains("15")) -> R.drawable.rating_acb_m15
            upper.contains("ACB") && upper.contains("M") -> R.drawable.rating_acb_m
            upper.contains("ACB") && upper.contains("G") -> R.drawable.rating_acb_g
            upper.contains("ACB") && (upper.contains("R18") || upper.contains("18+")) -> R.drawable.rating_acb_r18
            upper.contains("ACB") && upper.contains("RC") -> R.drawable.rating_acb_rc

            else -> null
        }
    }
}