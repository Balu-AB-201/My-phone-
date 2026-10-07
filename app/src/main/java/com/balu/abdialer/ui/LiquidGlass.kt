package com.balu.abdialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp

/**
 * Original AB Dialer liquid-glass surface.
 *
 * This is deliberately implemented in our own code rather than copied from
 * another application. The effect combines translucent material, depth,
 * directional highlights and a fine specular edge.
 */
fun Modifier.liquidGlass(
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    tint: Color = Color.White.copy(alpha = 0.12f),
    highlight: Color = Color.White.copy(alpha = 0.32f),
    borderAlpha: Float = 0.34f
): Modifier = this
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                tint.copy(alpha = tint.alpha * 1.25f),
                Color.White.copy(alpha = tint.alpha * 0.32f),
                tint
            ),
            tileMode = TileMode.Clamp
        ),
        shape = shape
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                highlight.copy(alpha = borderAlpha),
                Color.Transparent,
                Color.White.copy(alpha = borderAlpha * 0.45f)
            )
        ),
        shape = shape
    )
    .drawWithCache {
        val specular = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.Transparent,
                Color.Transparent
            )
        )
        onDrawWithContent {
            drawRoundRect(
                brush = specular,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())
            )
            drawContent()
        }
    }

@Composable
fun BoxScope.GlassContent() {
    // Marker composable for future shared glass effects.
}
