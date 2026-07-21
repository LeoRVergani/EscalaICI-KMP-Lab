package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.mockScheduleSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ScheduleSourceDisplayDecisionTest {
    @Test
    fun remoteSourceContextNeverDuplicatesFontePrefix() {
        val summary = mockScheduleSummary().copy(
            sourceFileName = null,
            remoteSourceLabel = "publicacao remota rev. 3"
        )

        val contextLine = scheduleContextSourceLine(summary)

        assertEquals("Fonte: publicacao remota rev. 3", contextLine)
        assertFalse(contextLine.contains("Fonte: Fonte:"))
        assertEquals("Fonte: publicacao remota rev. 3", profileScheduleSourceLine(summary))
        assertEquals("Escala publicada remotamente", profileScheduleStatusLine(summary))
    }

    @Test
    fun importedXlsStillShowsImportedFileLabel() {
        val summary = mockScheduleSummary().copy(
            sourceFileName = "Escala-SOC-Controle-Atual.xls",
            remoteSourceLabel = null
        )

        assertEquals("Arquivo importado: Escala-SOC-Controle-Atual.xls", profileScheduleSourceLine(summary))
        assertEquals("Escala salva apenas neste dispositivo", profileScheduleStatusLine(summary))
    }
}
