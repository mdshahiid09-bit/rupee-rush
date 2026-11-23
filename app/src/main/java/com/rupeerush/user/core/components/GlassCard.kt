package com.rupeerush.user.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rupeerush.user.core.theme.DarkGlass
import com.rupeerush.user.core.theme.DarkGlassBorder
import com.rupeerush.user.core.theme.LightGlass
import com.rupeerush.user.core.theme.LightGlassBorder

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)

    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(DarkGlass, DarkGlass.copy(alpha = 0.15f))
            )
        } else {
            Brush.linearGradient(
                listOf(LightGlass, LightGlass.copy(alpha = 0.4f))
            )
        }
    }

    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    var cardModifier = modifier
        .clip(shape)
        .background(backgroundBrush)
        .border(1.dp, borderColor, shape)

    if (onClick != null) {
        cardModifier = cardModifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = rememberRipple(bounded = true, color = MaterialTheme.colorScheme.primary),
            onClick = onClick
        )
    }

    Box(
        modifier = cardModifier,
        content = content
    )
}
