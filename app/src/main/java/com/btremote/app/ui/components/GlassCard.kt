package com.btremote.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.btremote.app.ui.theme.GlassBorderDark
import com.btremote.app.ui.theme.GlassDark
import com.btremote.app.ui.theme.GlassGradientBottom
import com.btremote.app.ui.theme.GlassGradientTop

/**
 * Glassmorphism card — translucent frosted-glass surface with a subtle top-sheen gradient
 * and a thin white-alpha border. Works on top of the deep dark background.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    glowColor: Color = Color.Transparent,
    glowAlpha: Float = 0f,
    innerPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val borderColor = if (glowAlpha > 0f)
        glowColor.copy(alpha = glowAlpha)
    else
        GlassBorderDark

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GlassGradientTop,
                        GlassDark,
                        GlassGradientBottom
                    )
                )
            )
            .border(borderWidth, borderColor, shape)
            .padding(innerPadding),
        content = content
    )
}

/**
 * Neon-glow divider line.
 */
@Composable
fun NeonDivider(
    color: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 0.4f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        color.copy(alpha = alpha),
                        Color.Transparent
                    )
                )
            )
            .padding(vertical = 0.5.dp)
    )
}
