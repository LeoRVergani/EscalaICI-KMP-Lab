package br.com.leorvergani.escalaici.kmp.lab.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

internal val LabColorScheme = darkColorScheme(
    primary = LabColors.primary,
    onPrimary = Color.White,
    secondary = Color(0xFF60A5FA),
    tertiary = LabColors.tertiary,
    background = LabColors.background,
    onBackground = LabColors.onSurface,
    surface = LabColors.surface,
    onSurface = LabColors.onSurface,
    surfaceVariant = LabColors.surfaceElevated,
    onSurfaceVariant = LabColors.onSurfaceMuted,
    outline = LabColors.outline,
    error = Color(0xFFEF4444)
)
