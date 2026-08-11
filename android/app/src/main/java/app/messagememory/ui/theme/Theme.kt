package app.messagememory.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Amber500,
    secondary = Teal400,
    background = Ink0,
    surface = Ink1,
    surfaceVariant = Ink2,
    onBackground = Slate300,
    onSurface = Slate300,
    error = Rose400,
)

private val LightColors = lightColorScheme(
    primary = Amber500,
    secondary = Teal400,
    background = LightSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    error = Rose400,
)

@Composable
fun MessageMemoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = MessageMemoryTypography, content = content)
}
