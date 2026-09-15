package com.skn.countdown

/**
 * A small curated set of solid/gradient card looks rather than a full color
 * wheel — keeps the picker simple and every option pre-checked for text
 * contrast. [colors] is 1 entry for a solid fill, 2 for a diagonal
 * gradient, or empty for NONE (no card at all — countdown text sits
 * directly on the wallpaper).
 */
enum class BackgroundPreset(val key: String, val label: String, val colors: List<Int>, val isDark: Boolean) {
    BUTTER_PAPER("butter_paper", "Butter Paper", listOf(0xFFFFFBEF.toInt()), false),
    FROSTED_DARK("frosted_dark", "Frosted Dark", listOf(0xFF1A1A1A.toInt()), true),
    SOLID_WHITE("solid_white", "Solid White", listOf(0xFFFFFFFF.toInt()), false),
    SOLID_BLACK("solid_black", "Solid Black", listOf(0xFF000000.toInt()), true),
    SUNSET("sunset", "Sunset", listOf(0xFFFF7E5F.toInt(), 0xFFFEB47B.toInt()), false),
    OCEAN("ocean", "Ocean", listOf(0xFF2193B0.toInt(), 0xFF6DD5ED.toInt()), true),
    MIDNIGHT("midnight", "Midnight", listOf(0xFF0F2027.toInt(), 0xFF2C5364.toInt()), true),
    NONE("none", "Transparent (no card)", emptyList(), false);

    companion object {
        fun byKey(key: String): BackgroundPreset = entries.firstOrNull { it.key == key } ?: BUTTER_PAPER
    }
}
