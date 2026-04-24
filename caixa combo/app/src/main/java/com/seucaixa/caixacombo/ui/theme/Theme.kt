package com.seucaixa.caixacombo.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Cores padrão
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Error,
    onError = OnError
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark
)

// Função para criar ColorScheme dinâmico baseado nas preferências
fun createDynamicColorScheme(context: Context, darkTheme: Boolean): ColorScheme {
    val prefs = context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE)
    
    // Ler cores salvas ou usar padrão
    val primaryColor = prefs.getInt("primary_color", 0)
    val secondaryColor = prefs.getInt("secondary_color", 0)
    val tertiaryColor = prefs.getInt("tertiary_color", 0)
    val backgroundColor = prefs.getInt("background_color", 0)
    val surfaceColor = prefs.getInt("surface_color", 0)
    
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    return if (primaryColor != 0) {
        // Cores personalizadas foram salvas
        val primary = Color(primaryColor)
        val secondary = if (secondaryColor != 0) Color(secondaryColor) else primary
        val tertiary = if (tertiaryColor != 0) Color(tertiaryColor) else primary
        val background = if (backgroundColor != 0) Color(backgroundColor) else baseScheme.background
        val surface = if (surfaceColor != 0) Color(surfaceColor) else baseScheme.surface
        
        baseScheme.copy(
            primary = primary,
            onPrimary = if (darkTheme) Color(0xFF1565C0) else Color(0xFFFFFFFF),
            primaryContainer = if (darkTheme) Color(0xFF1976D2) else primary.copy(alpha = 0.12f),
            onPrimaryContainer = if (darkTheme) primary else primary,
            secondary = secondary,
            onSecondary = if (darkTheme) Color(0xFF2E7D32) else Color(0xFFFFFFFF),
            secondaryContainer = if (darkTheme) Color(0xFF388E3C) else secondary.copy(alpha = 0.12f),
            onSecondaryContainer = if (darkTheme) secondary else secondary,
            tertiary = tertiary,
            background = background,
            surface = surface
        )
    } else {
        baseScheme
    }
}

@Composable
fun CaixaComboTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Usar cores dinâmicas do SharedPreferences
    val colorScheme = remember {
        createDynamicColorScheme(context, darkTheme)
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
