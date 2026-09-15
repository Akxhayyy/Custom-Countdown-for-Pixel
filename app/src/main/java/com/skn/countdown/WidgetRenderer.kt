package com.skn.countdown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader

/**
 * Draws the widget face onto a Bitmap ourselves rather than relying on a
 * RemoteViews TextView, because RemoteViews cannot use a custom Typeface —
 * a plain TextView is stuck with the system font forever.
 *
 * Rendered at the widget's actual current pixel size (passed in by the
 * caller from AppWidgetManager's options) so the card reaches every edge
 * instead of being letterboxed inside a mismatched aspect ratio. Font,
 * background preset, card opacity, text color, and label/value size are
 * all per-widget, user-chosen.
 *
 * Sizing is solved directly rather than by iterative shrink-and-remeasure:
 * for a fixed string and typeface, Paint.measureText and Paint.getTextBounds
 * both scale exactly linearly with textSize, so measuring once at a
 * reference size gives an exact textSize for any target width or height via
 * a single ratio — no loop, no overshoot. Height uses getTextBounds (the
 * glyphs' actual tight bounding box) rather than Paint.getFontMetrics —
 * font metrics reserve room for the tallest ascender/lowest descender
 * anywhere in the typeface, which a string of only digits and a colon
 * never uses, so it under-fills the available space and wastes the
 * value-size slider's headroom.
 */
object WidgetRenderer {

    private const val CARD_MARGIN_FRACTION = 0.03f
    private const val CARD_CORNER_RADIUS_FRACTION = 0.12f

    private const val HORIZONTAL_TEXT_MARGIN_FRACTION = 0.02f // slim breathing room each side — text should use most of the card's width

    private const val LABEL_BASE_SIZE_FRACTION = 0.22f // of card height, before the user's label-scale slider
    private const val LABEL_MIN_TEXT_SIZE = 14f

    private const val COUNTDOWN_BASE_SIZE_FRACTION = 0.55f // of card height, before the user's value-scale slider
    private const val COUNTDOWN_MIN_TEXT_SIZE = 16f

    private const val CAPTION_SIZE_FRACTION = 0.40f // of the final countdown number size — closer to the number's own weight
    private const val CAPTION_GAP_FRACTION = 0.05f // of the final countdown number size — tight, "right underneath"
    private val CAPTION_LABELS = listOf("DAYS", "HRS", "MIN")

    private val LIGHT_TEXT_COLOR = Color.argb(235, 245, 242, 232)
    private val DARK_TEXT_COLOR = Color.argb(235, 35, 30, 20)
    private val NONE_BG_TEXT_COLOR = Color.WHITE
    private val EXPIRED_COLOR = Color.argb(255, 200, 40, 40)

    fun render(
        context: Context,
        label: String,
        countdownText: String,
        expired: Boolean,
        widthPx: Int,
        heightPx: Int,
        fontKey: String,
        backgroundKey: String,
        opacityPercent: Int,
        textColorKey: String,
        labelScalePercent: Int = 100,
        valueScalePercent: Int = 100,
        showUnitCaptions: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val preset = BackgroundPreset.byKey(backgroundKey)
        val cardMargin = widthPx.coerceAtMost(heightPx) * CARD_MARGIN_FRACTION
        val cardRect = RectF(cardMargin, cardMargin, widthPx - cardMargin, heightPx - cardMargin)

        if (preset != BackgroundPreset.NONE) {
            drawCard(canvas, cardRect, heightPx, preset, opacityPercent)
        }

        val typeface = FontChoice.byKey(fontKey).resolve(context)
        val chosenColor = TextColorChoice.byKey(textColorKey)
        val textColor = when {
            expired -> EXPIRED_COLOR // status signal always wins, regardless of chosen color
            chosenColor != TextColorChoice.AUTO -> withAlpha(chosenColor.color!!, 235)
            preset == BackgroundPreset.NONE -> NONE_BG_TEXT_COLOR
            preset.isDark -> LIGHT_TEXT_COLOR
            else -> DARK_TEXT_COLOR
        }
        val labelColor = when {
            chosenColor != TextColorChoice.AUTO -> withAlpha(chosenColor.color!!, 190)
            preset == BackgroundPreset.NONE -> NONE_BG_TEXT_COLOR
            preset.isDark -> Color.argb(190, 245, 242, 232)
            else -> Color.argb(190, 60, 55, 45)
        }

        val centerX = widthPx / 2f
        val maxTextWidth = widthPx * (1f - 2 * HORIZONTAL_TEXT_MARGIN_FRACTION)
        val hasLabel = label.isNotBlank()

        // --- Label ---
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textAlign = Paint.Align.CENTER
            this.typeface = typeface
            setShadowLayer(heightPx * 0.02f, 0f, heightPx * 0.008f, Color.argb(120, 0, 0, 0))
        }
        var labelHeight = 0f
        val labelText = label.uppercase()
        if (hasLabel) {
            val desiredLabelSize = heightPx * LABEL_BASE_SIZE_FRACTION * (labelScalePercent / 100f)
            labelPaint.textSize = desiredLabelSize
            val labelWidthAtDesired = labelPaint.measureText(labelText)
            val fittedLabelSize = if (labelWidthAtDesired > maxTextWidth && labelWidthAtDesired > 0f) {
                desiredLabelSize * (maxTextWidth / labelWidthAtDesired)
            } else {
                desiredLabelSize
            }.coerceAtLeast(LABEL_MIN_TEXT_SIZE)
            labelPaint.textSize = fittedLabelSize

            val labelFm = labelPaint.fontMetrics
            labelHeight = (labelFm.descent - labelFm.ascent) * 1.3f
            val labelBaseline = cardMargin + (labelHeight * 0.5f) - (labelFm.ascent + labelFm.descent) / 2f
            canvas.drawText(labelText, centerX, labelBaseline, labelPaint)
        }

        // --- Countdown number (+ optional DAYS/HRS/MIN captions) ---
        val countdownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            this.typeface = typeface
            setShadowLayer(heightPx * 0.025f, 0f, heightPx * 0.01f, Color.argb(130, 0, 0, 0))
        }
        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textAlign = Paint.Align.CENTER
            this.typeface = typeface
            setShadowLayer(heightPx * 0.015f, 0f, heightPx * 0.006f, Color.argb(110, 0, 0, 0))
        }

        val mainText = if (expired) "EXPIRED" else countdownText
        val segments = mainText.split(":")
        val captionsApply = showUnitCaptions && !expired && segments.size == 3

        val countdownAreaTop = cardMargin + labelHeight
        val availableHeight = (heightPx - cardMargin - countdownAreaTop).coerceAtLeast(1f)
        val desiredValueSize = heightPx * COUNTDOWN_BASE_SIZE_FRACTION * (valueScalePercent / 100f)

        // Measure once at a reference size — width and glyph-bounds height
        // both scale linearly with textSize, so this gives an exact fit in
        // one shot. Height comes from getTextBounds (tight glyph box), not
        // font metrics, so the reserved space matches what digits actually
        // use rather than the whole typeface's ascender/descender range.
        val referenceSize = 100f
        val numberBoundsAtRef = Rect()
        countdownPaint.textSize = referenceSize
        countdownPaint.getTextBounds(mainText, 0, mainText.length, numberBoundsAtRef)
        val widthAtReference = countdownPaint.measureText(mainText)
        val numberHeightAtReference = numberBoundsAtRef.height().toFloat()

        var blockHeightAtReference = numberHeightAtReference
        if (captionsApply) {
            captionPaint.textSize = referenceSize * CAPTION_SIZE_FRACTION
            val captionBoundsAtRef = Rect()
            captionPaint.getTextBounds(CAPTION_LABELS[0], 0, CAPTION_LABELS[0].length, captionBoundsAtRef)
            blockHeightAtReference += referenceSize * CAPTION_GAP_FRACTION + captionBoundsAtRef.height()
        }

        val sizeForWidth = if (widthAtReference > 0f) maxTextWidth / widthAtReference * referenceSize else desiredValueSize
        val sizeForHeight = if (blockHeightAtReference > 0f) availableHeight / blockHeightAtReference * referenceSize else desiredValueSize
        val finalValueSize = minOf(desiredValueSize, sizeForWidth, sizeForHeight).coerceAtLeast(COUNTDOWN_MIN_TEXT_SIZE)

        countdownPaint.textSize = finalValueSize
        val numberBounds = Rect()
        countdownPaint.getTextBounds(mainText, 0, mainText.length, numberBounds)
        val numberHeight = numberBounds.height().toFloat()

        // Segment widths/centers at the number's final size — needed both to
        // cap each caption to its own column (below) and to place it later.
        val totalWidth = countdownPaint.measureText(mainText)
        val leftEdge = centerX - totalWidth / 2f
        val separatorWidth = countdownPaint.measureText(":")
        val segWidths = segments.map { countdownPaint.measureText(it) }
        val segCenters = mutableListOf<Float>()
        run {
            var cumulative = 0f
            for (i in segments.indices) {
                segCenters.add(leftEdge + cumulative + segWidths[i] / 2f)
                cumulative += segWidths[i] + separatorWidth
            }
        }

        var captionHeight = 0f
        var gap = 0f
        val captionBounds = Rect()
        if (captionsApply) {
            val desiredCaptionSize = finalValueSize * CAPTION_SIZE_FRACTION
            captionPaint.textSize = desiredCaptionSize
            // Each caption word must fit under its own segment's column — a
            // short word like "DAYS" can otherwise render wider than a
            // 2-digit segment and collide with its neighbor.
            val neededScale = CAPTION_LABELS.indices.minOf { i ->
                val capWidth = captionPaint.measureText(CAPTION_LABELS[i])
                val slotWidth = segWidths.getOrElse(i) { segWidths.min() }
                if (capWidth > 0f) (slotWidth * 0.92f / capWidth).coerceAtMost(1f) else 1f
            }
            captionPaint.textSize = (desiredCaptionSize * neededScale).coerceAtLeast(10f)
            captionPaint.getTextBounds(CAPTION_LABELS[0], 0, CAPTION_LABELS[0].length, captionBounds)
            captionHeight = captionBounds.height().toFloat()
            gap = finalValueSize * CAPTION_GAP_FRACTION
        }
        val blockHeight = numberHeight + (if (captionsApply) gap + captionHeight else 0f)

        val blockTop = countdownAreaTop + (availableHeight - blockHeight) / 2f
        // baseline = top-of-glyph-box - (bounds.top), since bounds.top is the
        // (negative) offset from the baseline up to the glyph's actual top.
        val numberBaseline = blockTop - numberBounds.top

        canvas.drawText(mainText, centerX, numberBaseline, countdownPaint)

        if (captionsApply) {
            val captionTop = blockTop + numberHeight + gap
            val captionBaseline = captionTop - captionBounds.top

            for (i in segments.indices) {
                canvas.drawText(CAPTION_LABELS.getOrElse(i) { "" }, segCenters[i], captionBaseline, captionPaint)
            }
        }

        return bitmap
    }

    private fun drawCard(canvas: Canvas, cardRect: RectF, heightPx: Int, preset: BackgroundPreset, opacityPercent: Int) {
        val alpha = (opacityPercent.coerceIn(0, 100) * 255 / 100)
        val cornerRadius = heightPx * CARD_CORNER_RADIUS_FRACTION
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (preset.colors.size >= 2) {
            cardPaint.shader = LinearGradient(
                cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                withAlpha(preset.colors[0], alpha), withAlpha(preset.colors[1], alpha),
                Shader.TileMode.CLAMP
            )
        } else {
            cardPaint.color = withAlpha(preset.colors.firstOrNull() ?: Color.WHITE, alpha)
        }

        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha shl 24)
}
