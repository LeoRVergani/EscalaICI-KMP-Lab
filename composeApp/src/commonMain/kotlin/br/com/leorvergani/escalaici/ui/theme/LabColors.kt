package br.com.leorvergani.escalaici.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta completa, portada 1:1 de `ui/theme/Color.kt` do app Android real
 * (`EscalaSOC`). App real e dark-only, sem light theme.
 */
internal object LabColors {
    val background = Color(0xFF070B12)
    val surface = Color(0xFF0B1827)
    val surfaceElevated = Color(0xFF111E31)
    val outline = Color(0xFF2F4668)

    val primary = Color(0xFF3B82F6)
    val primaryContainer = Color(0xFF1E3A8A)
    val onPrimary = Color(0xFFFFFFFF)
    val onPrimaryContainer = Color(0xFFDBEAFE)
    val secondary = Color(0xFF60A5FA)
    val onSecondary = Color(0xFF0D1B2A)

    val onSurface = Color(0xFFF3F4F6)
    val onSurfaceMuted = Color(0xFFAEB8C9)

    val tertiary = Color(0xFF18A874)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFF064E3B)
    val onTertiaryContainer = Color(0xFFD1FAE5)

    val orange = Color(0xFFF59E0B)
    val yellow = Color(0xFFEAB308)
    val red = Color(0xFFEF4444)
    val purple = Color(0xFF8B5CF6)
    val cyan = Color(0xFF06B6D4)
    val gray = Color(0xFF6B7280)

    val error = red
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFF450A0A)
    val onErrorContainer = Color(0xFFFEE2E2)
}
