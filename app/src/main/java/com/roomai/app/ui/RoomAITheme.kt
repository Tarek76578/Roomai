package com.roomai.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RoomAIWarm = Color(0xFFF6F3EE)
private val RoomAICream = Color(0xFFFFFCF8)
private val RoomAIDark = Color(0xFF1F2824)
private val RoomAIGreen = Color(0xFF52665D)
private val RoomAITerracotta = Color(0xFFC98262)
private val RoomAIMuted = Color(0xFF737A76)
private val RoomAIOutline = Color(0xFFE3DED6)

private val LightColors = lightColorScheme(
    primary = RoomAIDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E8E3),
    onPrimaryContainer = RoomAIDark,
    secondary = RoomAIGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6ECE8),
    onSecondaryContainer = RoomAIDark,
    tertiary = RoomAITerracotta,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4DDD3),
    onTertiaryContainer = Color(0xFF4A2115),
    background = RoomAIWarm,
    onBackground = RoomAIDark,
    surface = RoomAICream,
    onSurface = RoomAIDark,
    surfaceVariant = Color(0xFFEDE9E3),
    onSurfaceVariant = RoomAIMuted,
    outline = RoomAIOutline
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5EEE9),
    onPrimary = Color(0xFF1F2824),
    primaryContainer = Color(0xFF384A43),
    onPrimaryContainer = Color(0xFFE5EEE9),
    secondary = Color(0xFFB9C9C1),
    onSecondary = Color(0xFF203029),
    secondaryContainer = Color(0xFF34463F),
    onSecondaryContainer = Color(0xFFD9E7E0),
    tertiary = Color(0xFFE4A88E),
    onTertiary = Color(0xFF432014),
    tertiaryContainer = Color(0xFF653D2D),
    onTertiaryContainer = Color(0xFFFFDBCC),
    background = Color(0xFF121815),
    onBackground = Color(0xFFE8ECE9),
    surface = Color(0xFF19201C),
    onSurface = Color(0xFFE8ECE9),
    surfaceVariant = Color(0xFF2A322E),
    onSurfaceVariant = Color(0xFFB9C1BC),
    outline = Color(0xFF46514B)
)

private val RoomShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val RoomTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.2).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.7).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        lineHeight = 21.sp
    )
)

@Composable
fun RoomAITheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RoomTypography,
        shapes = RoomShapes,
        content = content
    )
}
