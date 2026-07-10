package br.com.leorvergani.escalaici.kmp.lab.ui.theme

import androidx.compose.ui.graphics.Color
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftType

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
