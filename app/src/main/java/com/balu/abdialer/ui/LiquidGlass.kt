package com.balu.abdialer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp

/**
 * Original AB Dialer liquid-glass surface.
 *
 * The shared glass system combines translucent depth, directional light,
 * an ambient edge glow and a fine specular rim. Keep the effect centralized
 * so every dialer surface can evolve together without copied UI assets.
 */
fun Modifier.liquidGlass(
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    tint: Color = Color.White.copy(alpha = 0.12f),
    highlight: Color = Color.White.copy(alpha = 0.32f),
    borderAlpha: Float = 0.34f,
    elevation: Float = 10f
): Modifier = this
    .shadow(
        elevation = elevation.dp,
        shape = shape,
        clip = false
    )
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = tint.alpha * 0.30f),
                tint.copy(alpha = tint.alpha * 1.35f),
                tint.copy(alpha = tint.alpha * 0.72f),
                Color.Black.copy(alpha = tint.alpha * 0.16f)
            ),
            tileMode = TileMode.Clamp
        ),
        shape = shape
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha * 1.20f),
                highlight.copy(alpha = borderAlpha * 0.55f),
                Color.Transparent,
                Color.White.copy(alpha = borderAlpha * 0.32f)
            ),
            tileMode = TileMode.Clamp
        ),
        shape = shape
    )
    .drawWithCache {
        val radius = 28.dp.toPx()
        val ambient = Brush.radialGradient(
            colors = listOf(
                highlight.copy(alpha = 0.12f),
                Color.Transparent
            ),
            radius = size.maxDimension * 0.95f
        )
        val specular = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.06f),
                Color.Transparent,
                Color.Transparent
            ),
            tileMode = TileMode.Clamp
        )
        val lowerEdge = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.06f),
                Color.Transparent
            )
        )
        onDrawWithContent {
            drawRoundRect(
                brush = ambient,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
            )
            drawRoundRect(
                brush = specular,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
            )
            drawContent()
            drawRoundRect(
                brush = lowerEdge,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }
    }

/**
 * A restrained moving specular reflection for primary glass controls.
 * Use on a small number of hero controls to keep animation inexpensive.
 */
@Composable
fun Modifier.liquidGlassSweep(durationMillis: Int = 2600): Modifier {
    val transition = rememberInfiniteTransition(label = "liquid-glass-sweep")
    val progress by transition.animateFloat(
        initialValue = -0.65f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid-glass-sweep-progress"
    )

    return this.drawWithCache {
        val startX = size.width * progress
        val reflection = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.02f),
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent
            ),
            start = Offset(startX, 0f),
            end = Offset(startX + size.width * 0.48f, size.height),
            tileMode = TileMode.Clamp
        )
        onDrawWithContent {
            drawContent()
            drawRoundRect(
                brush = reflection,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension / 2f)
            )
        }
    }
}

@Composable
fun BoxScope.GlassContent() {
    // Marker composable for future shared glass effects.
}
