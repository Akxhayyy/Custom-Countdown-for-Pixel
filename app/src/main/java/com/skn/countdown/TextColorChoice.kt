package com.skn.countdown

import android.graphics.Color

/**
 * "Auto" keeps the existing behavior — light or dark text picked to
 * contrast with whatever background preset is chosen. Anything else pins
 * the text to a fixed color regardless of background (your problem to
 * manage if you pick a bad combo).
 */
enum class TextColorChoice(val key: String, val label: String, val color: Int?) {
    AUTO("auto", "Auto (matches background)", null),
    WHITE("white", "White", 0xFFFFFFFF.toInt()),
    BLACK("black", "Black", 0xFF101010.toInt()),
    CREAM("cream", "Cream", 0xFFFFF6E0.toInt()),
    GOLD("gold", "Gold", 0xFFFFC107.toInt()),
    RED("red", "Red", 0xFFE53935.toInt()),
    ORANGE("orange", "Orange", 0xFFFB8C00.toInt()),
    GREEN("green", "Green", 0xFF43A047.toInt()),
    BLUE("blue", "Blue", 0xFF1E88E5.toInt()),
    PURPLE("purple", "Purple", 0xFF8E24AA.toInt()),
    PINK("pink", "Pink", 0xFFD81B60.toInt());

    companion object {
        fun byKey(key: String): TextColorChoice = entries.firstOrNull { it.key == key } ?: AUTO
    }
}
