package br.com.leorvergani.escalaici.source

import br.com.leorvergani.escalaici.model.ShiftType

internal fun shiftTypeFromAssignment(
    assignmentType: String?,
    shiftName: String?,
    unknownWorkShift: ShiftType? = ShiftType.INDEFINIDO,
    unknownAssignmentType: ShiftType? = ShiftType.INDEFINIDO
): ShiftType? = when (assignmentType) {
    "WORK_SHIFT" -> workShiftTypeFrom(shiftName) ?: unknownWorkShift
    "VACATION" -> ShiftType.FERIAS
    "ABSENCE" -> ShiftType.AFASTAMENTO
    "OFF" -> when (shiftName) {
        "BH" -> ShiftType.BH
        "Aniversário" -> ShiftType.ANIVERSARIO
        else -> ShiftType.FOLGA
    }
    "OTHER" -> when {
        shiftName == "Sem dado importado" -> ShiftType.INDEFINIDO
        shiftName?.startsWith("Trabalho sem turno localizado") == true -> ShiftType.INCONSISTENCIA
        else -> ShiftType.INDEFINIDO
    }
    "TRAINING" -> ShiftType.INDEFINIDO
    else -> unknownAssignmentType
}

private fun workShiftTypeFrom(shiftName: String?): ShiftType? = when (shiftName?.lowercase()) {
    "madrugada" -> ShiftType.MADRUGADA
    "manhã", "manha", "morning" -> ShiftType.MANHA
    "tarde", "afternoon" -> ShiftType.TARDE
    "noite", "night" -> ShiftType.NOITE
    "comercial" -> ShiftType.COMERCIAL
    else -> null
}
