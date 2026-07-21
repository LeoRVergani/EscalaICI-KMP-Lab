package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.ScheduleSummary

internal fun scheduleContextSourceLine(summary: ScheduleSummary): String =
    summary.remoteSourceLabel?.let { "Fonte: $it" }
        ?: "Fonte: ${summary.sourceFileName ?: "nenhuma escala importada"}"

internal fun profileScheduleStatusLine(summary: ScheduleSummary): String = when {
    summary.sourceFileName != null -> "Escala salva apenas neste dispositivo"
    summary.remoteSourceLabel != null -> "Escala publicada remotamente"
    else -> "Nenhuma escala importada"
}

internal fun profileScheduleSourceLine(summary: ScheduleSummary): String = when {
    summary.remoteSourceLabel != null -> "Fonte: ${summary.remoteSourceLabel}"
    summary.sourceFileName != null -> "Arquivo importado: ${summary.sourceFileName}"
    else -> "Fonte: dados de demonstração"
}

internal fun scheduleGenerationSourceKind(summary: ScheduleSummary): String = when {
    summary.sourceFileName != null -> "real"
    summary.remoteSourceLabel != null -> "publicada"
    else -> "ainda não importada"
}

internal fun hasPublishedOrImportedSchedule(summary: ScheduleSummary): Boolean =
    summary.sourceFileName != null || summary.remoteSourceLabel != null
