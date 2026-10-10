package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp

/**
 * Applies the profile's weight to static bundled fonts as well as variable fonts.
 * The bundled font files are static, so axis settings alone cannot change their glyphs.
 */
@Composable
@OptIn(ExperimentalTextApi::class)
internal fun ProfileWeightText(
    text: String,
    weight: Int,
    width: Float = 100f,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val clampedWeight = weight.coerceIn(100, 1000)
    val fontSizePx = with(LocalDensity.current) {
        if (style.fontSize.isSp) style.fontSize.toPx() else 24.sp.toPx()
    }
    val strokeWidth = ((clampedWeight - 100) / 900f) * (fontSizePx * 0.04f).coerceIn(1.2f, 4.5f)
    val fillStyle = style.copy(fontWeight = FontWeight.Normal, drawStyle = Fill)

    Box(modifier.graphicsLayer { scaleX = width.coerceIn(25f, 151f) / 100f }) {
        Text(text, style = fillStyle, color = color, maxLines = maxLines, overflow = overflow)
        if (strokeWidth > 0.05f) {
            Text(
                text,
                style = fillStyle.copy(drawStyle = Stroke(width = strokeWidth)),
                color = color,
                maxLines = maxLines,
                overflow = overflow
            )
        }
    }
}
