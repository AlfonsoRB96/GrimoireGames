package com.trunder.grimoiregames.util

import com.trunder.grimoiregames.R
import com.trunder.grimoiregames.data.remote.dto.AgeRatingCategory
import com.trunder.grimoiregames.data.remote.dto.AgeRatingCategory.*

object RatingMapper {

    // 1. PARA LA API (Uso interno o futuro)
    fun getDrawableFromCategory(category: AgeRatingCategory?): Int? {
        if (category == null) return null
        // Reutilizamos la lógica inversa para no repetir código
        // Convertimos el Enum a su String representativo y pedimos el Drawable
        val tag = getTagFromEnum(category)
        return getDrawableFromString(tag)
    }

    // 2. PARA LA UI (GameDetailScreen): Lee los Strings de Room ("PEGI 18", "ESRB T"...)
    fun getDrawableFromString(ratingTag: String?): Int? {
        if (ratingTag.isNullOrEmpty()) return null

        return when (ratingTag) {
            // === PEGI ===
            "PEGI 3" -> R.drawable.rating_pegi_3
            "PEGI 7" -> R.drawable.rating_pegi_7
            "PEGI 12" -> R.drawable.rating_pegi_12
            "PEGI 16" -> R.drawable.rating_pegi_16
            "PEGI 18" -> R.drawable.rating_pegi_18

            // === ESRB ===
            "ESRB RP" -> R.drawable.rating_esrb_rp
            "ESRB E" -> R.drawable.rating_esrb_e
            "ESRB E10+" -> R.drawable.rating_esrb_e10
            "ESRB T" -> R.drawable.rating_esrb_t
            "ESRB M" -> R.drawable.rating_esrb_m
            "ESRB AO" -> R.drawable.rating_esrb_ao

            // === CERO ===
            "CERO A" -> R.drawable.rating_cero_a
            "CERO B" -> R.drawable.rating_cero_b
            "CERO C" -> R.drawable.rating_cero_c
            "CERO D" -> R.drawable.rating_cero_d
            "CERO Z" -> R.drawable.rating_cero_z

            // === USK ===
            "USK 0" -> R.drawable.rating_usk_0
            "USK 6" -> R.drawable.rating_usk_6
            "USK 12" -> R.drawable.rating_usk_12
            "USK 16" -> R.drawable.rating_usk_16
            "USK 18" -> R.drawable.rating_usk_18

            // === GRAC ===
            "GRAC ALL" -> R.drawable.rating_grac_all
            "GRAC 12" -> R.drawable.rating_grac_12
            "GRAC 15" -> R.drawable.rating_grac_15
            "GRAC 18" -> R.drawable.rating_grac_18

            // === CLASS_IND ===
            "ClassInd L" -> R.drawable.rating_classind_l
            "ClassInd 10" -> R.drawable.rating_classind_10
            "ClassInd 12" -> R.drawable.rating_classind_12
            "ClassInd 14" -> R.drawable.rating_classind_14
            "ClassInd 16" -> R.drawable.rating_classind_16
            "ClassInd 18" -> R.drawable.rating_classind_18

            // === ACB ===
            "ACB G" -> R.drawable.rating_acb_g
            "ACB PG" -> R.drawable.rating_acb_pg
            "ACB M" -> R.drawable.rating_acb_m
            "ACB MA15+" -> R.drawable.rating_acb_m15
            "ACB R18+" -> R.drawable.rating_acb_r18
            "ACB RC" -> R.drawable.rating_acb_rc

            else -> null
        }
    }

    // Helper interno para conectar ambos mundos si fuera necesario
    private fun getTagFromEnum(cat: AgeRatingCategory): String {
        return when (cat) {
            PEGI_3 -> "PEGI 3"
            PEGI_7 -> "PEGI 7"
            PEGI_12 -> "PEGI 12"
            PEGI_16 -> "PEGI 16"
            PEGI_18 -> "PEGI 18"
            ESRB_RP, ESRB_EC -> "ESRB RP"
            ESRB_E -> "ESRB E"
            ESRB_E10 -> "ESRB E10+"
            ESRB_T -> "ESRB T"
            ESRB_M -> "ESRB M"
            ESRB_AO -> "ESRB AO"
            CERO_A -> "CERO A"
            CERO_B -> "CERO B"
            CERO_C -> "CERO C"
            CERO_D -> "CERO D"
            CERO_Z -> "CERO Z"
            GRAC_ALL, GRAC_TESTING -> "GRAC ALL"
            GRAC_12 -> "GRAC 12"
            GRAC_15 -> "GRAC 15"
            GRAC_18 -> "GRAC 18"
            USK_0 -> "USK 0"
            USK_6 -> "USK 6"
            USK_12 -> "USK 12"
            USK_16 -> "USK 16"
            USK_18 -> "USK 18"
            CLASS_IND_L -> "ClassInd L"
            CLASS_IND_10 -> "ClassInd 10"
            CLASS_IND_12 -> "ClassInd 12"
            CLASS_IND_14 -> "ClassInd 14"
            CLASS_IND_16 -> "ClassInd 16"
            CLASS_IND_18 -> "ClassInd 18"
            ACB_G -> "ACB G"
            ACB_PG -> "ACB PG"
            ACB_M -> "ACB M"
            ACB_MA15 -> "ACB MA15+"
            ACB_R18 -> "ACB R18+"
            ACB_RC -> "ACB RC"
        }
    }
}