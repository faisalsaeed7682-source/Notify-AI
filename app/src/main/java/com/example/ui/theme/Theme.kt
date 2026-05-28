package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF000000), 
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFCFCFF),
    surface = Color(0xFFFCFCFF),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)
  )

private val NightColorScheme =
  darkColorScheme(
    primary = Color(0xFFCC84FF),
    secondary = Color(0xFF64B5F6),
    tertiary = Color(0xFFFF8A80),
    background = Color(0xFF010006),
    surface = Color(0xFF04020C),
    surfaceVariant = Color(0xFF0F0B1E),
    onSurface = Color(0xFFF1EEFA),
    onSurfaceVariant = Color(0xFFC0BACF)
  )

private val GlassLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF0D5D68),
    secondary = Color(0xFF00695C),
    tertiary = Color(0xFFAD1457),
    background = Color.Transparent,
    surface = Color(0x3DFCFDFF),
    surfaceVariant = Color(0x4DFFFFFF),
    onSurface = Color(0xFF15151A),
    onSurfaceVariant = Color(0xFF33333E),
    onPrimary = Color.White
  )

private val GlassDarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF80DEEA),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFFFCCD5),
    background = Color.Transparent,
    surface = Color(0x2E0B0B0F),
    surfaceVariant = Color(0x3BFFFFFF),
    onSurface = Color(0xF2FFFFFF),
    onSurfaceVariant = Color(0xB3FFFFFF),
    onPrimary = Color(0xFF00363A)
  )

@Composable
fun AmbientGlowBackground(isLightMode: Boolean, content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    
    // Animate colors for a premium, continuous color-shifting glow
    val color1 by infiniteTransition.animateColor(
        initialValue = if (isLightMode) Color(0xFFC7F3FA) else Color(0xFF200F48),
        targetValue = if (isLightMode) Color(0xFFD0F0D5) else Color(0xFF071B3E),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = if (isLightMode) Color(0xFFFFE0E6) else Color(0xFF03221C),
        targetValue = if (isLightMode) Color(0xFFFFF1DB) else Color(0xFF320822),
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color2"
    )
    val color3 by infiniteTransition.animateColor(
        initialValue = if (isLightMode) Color(0xFFEFE5FD) else Color(0xFF170A28),
        targetValue = if (isLightMode) Color(0xFFE4DCFF) else Color(0xFF2C061A),
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color3"
    )

    // Smooth movement offsets of glow points
    val posX by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "posX"
    )
    val posY by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "posY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height
                
                // Solid deep space dark or ambient light base
                drawRect(color = if (isLightMode) Color(0xFFEEF2F7) else Color(0xFF06050A))
                
                // Radial ambient accent blobs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color1.copy(alpha = if (isLightMode) 0.65f else 0.5f), Color.Transparent),
                        center = Offset(width * posX, height * (1f - posY)),
                        radius = width * 1.4f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color2.copy(alpha = if (isLightMode) 0.55f else 0.45f), Color.Transparent),
                        center = Offset(width * (1f - posX), height * posY),
                        radius = width * 1.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color3.copy(alpha = if (isLightMode) 0.5f else 0.4f), Color.Transparent),
                        center = Offset(width * 0.5f, height * 0.5f),
                        radius = width * 1.3f
                    )
                )
            }
    ) {
        content()
    }
}

@Composable
fun MyApplicationTheme(
  themeMode: Int = 4,
  content: @Composable () -> Unit,
) {
  val darkTheme = isSystemInDarkTheme()
  val context = LocalContext.current
  
  // All themes map to glassmorphic variants per user design guidelines to remove flat designs
  val isDark = when (themeMode) {
      0 -> darkTheme
      1 -> false // Light is Glass Light
      2 -> true  // Dark is Glass Dark
      4 -> true  // Glass Ambient Dark
      else -> darkTheme
  }

  val colorScheme = if (isDark) GlassDarkColorScheme else GlassLightColorScheme

  AmbientGlowBackground(isLightMode = !isDark) {
      MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
