package br.com.leorvergani.escalaici.ui.theme

import androidx.compose.ui.graphics.Color
import br.com.leorvergani.escalaici.model.ShiftType

internal fun ShiftType.shiftColor(): Color = when (this) {
    ShiftType.MADRUGADA -> Color(0xFF6366F1)
    ShiftType.MANHA -> Color(0xFFFACC15)
    ShiftType.TARDE -> Color(0xFFF97316)
    ShiftType.NOITE -> Color(0xFF1D4ED8)
    ShiftType.FOLGA -> Color(0xFF16A34A)
    ShiftType.FERIAS -> Color(0xFF14B8A6)
    ShiftType.BH -> Color(0xFFF59E0B)
    ShiftType.ANIVERSARIO -> Color(0xFFEC4899)
    ShiftType.HORA_EXTRA -> Color(0xFF22C55E)
    ShiftType.AFASTAMENTO -> Color(0xFF94A3B8)
    ShiftType.INCONSISTENCIA -> Color(0xFFEF4444)
    ShiftType.INDEFINIDO -> Color(0xFF6B7280)
}

/**
 * Cor de "container" (fundo suave) de cada turno, portada 1:1 de
 * `ui/theme/Color.kt` do app real (ex.: `ShiftManhaContainer`). Turnos sem
 * container definido no app real (Inconsistencia/Aniversario/HoraExtra/
 * Indefinido) usam um fallback derivado por alpha da propria cor do turno.
 */
internal fun ShiftType.shiftContainerColor(): Color = when (this) {
    ShiftType.MANHA -> Color(0xFF713F12).copy(alpha = 0.30f)
    ShiftType.TARDE -> Color(0xFF451A03).copy(alpha = 0.3f)
    ShiftType.NOITE -> LabColors.primaryContainer.copy(alpha = 0.3f)
    ShiftType.MADRUGADA -> Color(0xFF2E1065).copy(alpha = 0.3f)
    ShiftType.FOLGA -> Color(0xFF064E3B).copy(alpha = 0.3f)
    ShiftType.FERIAS -> Color(0xFF134E4A).copy(alpha = 0.30f)
    ShiftType.BH -> Color(0xFF451A03).copy(alpha = 0.3f)
    ShiftType.AFASTAMENTO -> Color(0xFF1F2937).copy(alpha = 0.3f)
    else -> shiftColor().copy(alpha = 0.3f)
}

/** Cor de texto/icone sobre o container do turno, portada 1:1 do app real. */
internal fun ShiftType.shiftOnColor(): Color = when (this) {
    ShiftType.MANHA -> Color(0xFFFEF9C3)
    ShiftType.TARDE -> Color(0xFFFEF3C7)
    ShiftType.NOITE -> LabColors.onPrimaryContainer
    ShiftType.MADRUGADA -> Color(0xFFEDE9FE)
    ShiftType.FOLGA -> Color(0xFFD1FAE5)
    ShiftType.FERIAS -> Color(0xFFCCFBF1)
    ShiftType.BH -> Color(0xFFFEF3C7)
    ShiftType.AFASTAMENTO -> LabColors.onSurface
    else -> LabColors.onSurface
}
