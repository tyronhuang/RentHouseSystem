package tw.com.baozugong.ui

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C71), onPrimary = Color.White,
    secondary = Color(0xFF6247AA), tertiary = Color(0xFFB32668),
    background = Color(0xFFF3FAFA), surface = Color(0xFFFBFFFF),
    surfaceVariant = Color(0xFFE2F0F1), outline = Color(0xFF6F8B8D),
    error = Color(0xFFB3261E),
)

private val CyberDarkColors = darkColorScheme(
    primary = Color(0xFF00F5D4), onPrimary = Color(0xFF00201B),
    primaryContainer = Color(0xFF004D46), onPrimaryContainer = Color(0xFF76FFE8),
    secondary = Color(0xFFFF4ECD), onSecondary = Color(0xFF3A0030),
    secondaryContainer = Color(0xFF5B164D), onSecondaryContainer = Color(0xFFFFD8F2),
    tertiary = Color(0xFF8C7CFF), onTertiary = Color(0xFF16005E),
    background = Color(0xFF050914), onBackground = Color(0xFFE3F7FF),
    surface = Color(0xFF0B1220), onSurface = Color(0xFFE3F7FF),
    surfaceVariant = Color(0xFF121E32), onSurfaceVariant = Color(0xFFB7C9D9),
    outline = Color(0xFF2C7082), outlineVariant = Color(0xFF1B3E4D),
    error = Color(0xFFFF5C7A), onError = Color(0xFF3F0012),
)

@Composable
fun BaoZuGongTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) CyberDarkColors else LightColors,
        typography = Typography(
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            titleLarge = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.3.sp),
            labelLarge = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
        ),
        shapes = Shapes(
            extraSmall = CutCornerShape(topEnd = 4.dp, bottomStart = 4.dp),
            small = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp),
            medium = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
            large = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
            extraLarge = CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp),
        ),
        content = content,
    )
}
