package com.skn.countdown

import android.content.Context
import android.graphics.Typeface

/**
 * Curated bundled fonts (bold weight only — this is a countdown display,
 * not body text), loaded from assets/fonts/ rather than res/font/ because
 * android.graphics.Typeface.Builder only accepts an AssetManager + path,
 * not a font resource id. All the non-system ones are variable-weight-only
 * font files (Google Fonts' current distribution format), so a bold cut is
 * requested at render time via font variation settings rather than by
 * bundling a separate static-weight file per family.
 */
enum class FontChoice(val key: String, val label: String, private val assetPath: String?, private val isVariable: Boolean) {
    SYSTEM("system", "System Default", null, false),
    OSWALD("oswald", "Oswald (condensed)", "fonts/oswald_variable.ttf", true),
    ANTON("anton", "Anton (impact)", "fonts/anton_regular.ttf", false),
    ORBITRON("orbitron", "Orbitron (digital)", "fonts/orbitron_variable.ttf", true),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono", "fonts/jetbrains_mono_variable.ttf", true),
    PLAYFAIR_DISPLAY("playfair_display", "Playfair Display (serif)", "fonts/playfair_display_variable.ttf", true),
    CAVEAT("caveat", "Caveat (handwriting)", "fonts/caveat_variable.ttf", true);

    fun resolve(context: Context): Typeface {
        val path = assetPath ?: return Typeface.DEFAULT_BOLD
        return try {
            if (isVariable) {
                Typeface.Builder(context.assets, path)
                    .setFontVariationSettings("'wght' 700")
                    .build() ?: Typeface.DEFAULT_BOLD
            } else {
                Typeface.createFromAsset(context.assets, path)
            }
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }

    companion object {
        fun byKey(key: String): FontChoice = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
