package com.trunder.grimoiregames.data.remote.dto

// IDs oficiales de IGDB para 'organization'
enum class AgeRatingOrganization(val id: Int) {
    ESRB(1),
    PEGI(2),
    CERO(3),
    USK(4),
    GRAC(5),
    CLASS_IND(6),
    ACB(7);

    companion object {
        fun fromId(id: Int?): AgeRatingOrganization? = entries.find { it.id == id }
    }
}

// Vinculamos cada categoría con su organización esperada
enum class AgeRatingCategory(val id: Int, val org: AgeRatingOrganization) {
    // PEGI (ID Org: 2)
    PEGI_3(1, AgeRatingOrganization.PEGI),
    PEGI_7(2, AgeRatingOrganization.PEGI),
    PEGI_12(3, AgeRatingOrganization.PEGI),
    PEGI_16(4, AgeRatingOrganization.PEGI),
    PEGI_18(5, AgeRatingOrganization.PEGI),

    // ESRB (ID Org: 1)
    ESRB_RP(6, AgeRatingOrganization.ESRB),
    ESRB_EC(7, AgeRatingOrganization.ESRB),
    ESRB_E(8, AgeRatingOrganization.ESRB),
    ESRB_E10(9, AgeRatingOrganization.ESRB),
    ESRB_T(10, AgeRatingOrganization.ESRB),
    ESRB_M(11, AgeRatingOrganization.ESRB),
    ESRB_AO(12, AgeRatingOrganization.ESRB),

    // CERO (ID Org: 3)
    CERO_A(13, AgeRatingOrganization.CERO),
    CERO_B(14, AgeRatingOrganization.CERO),
    CERO_C(15, AgeRatingOrganization.CERO),
    CERO_D(16, AgeRatingOrganization.CERO),
    CERO_Z(17, AgeRatingOrganization.CERO),

    // USK (ID Org: 4)
    USK_0(18, AgeRatingOrganization.USK),
    USK_6(19, AgeRatingOrganization.USK),
    USK_12(20, AgeRatingOrganization.USK),
    USK_16(21, AgeRatingOrganization.USK),
    USK_18(22, AgeRatingOrganization.USK),

    // GRAC (ID Org: 5)
    GRAC_ALL(23, AgeRatingOrganization.GRAC),
    GRAC_12(24, AgeRatingOrganization.GRAC),
    GRAC_15(25, AgeRatingOrganization.GRAC),
    GRAC_18(26, AgeRatingOrganization.GRAC),
    GRAC_TESTING(27, AgeRatingOrganization.GRAC),

    // CLASS_IND (ID Org: 6)
    CLASS_IND_L(28, AgeRatingOrganization.CLASS_IND),
    CLASS_IND_10(29, AgeRatingOrganization.CLASS_IND),
    CLASS_IND_12(30, AgeRatingOrganization.CLASS_IND),
    CLASS_IND_14(31, AgeRatingOrganization.CLASS_IND),
    CLASS_IND_16(32, AgeRatingOrganization.CLASS_IND),
    CLASS_IND_18(33, AgeRatingOrganization.CLASS_IND),

    // ACB (ID Org: 7)
    ACB_G(34, AgeRatingOrganization.ACB),
    ACB_PG(35, AgeRatingOrganization.ACB),
    ACB_M(36, AgeRatingOrganization.ACB),
    ACB_MA15(37, AgeRatingOrganization.ACB),
    ACB_R18(38, AgeRatingOrganization.ACB),
    ACB_RC(39, AgeRatingOrganization.ACB);

    companion object {
        fun fromId(id: Int?): AgeRatingCategory? = entries.find { it.id == id }
    }
}