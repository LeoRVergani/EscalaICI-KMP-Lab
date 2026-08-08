package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TotaisRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Porte de `packages/contrato/src/jornada.ts` - mesma regra de corte no dia 26 do Escala-ICI real. */
class JornadaTest {

    @Test
    fun competenciaOperacional_beforeCutoff_staysInCurrentMonth() {
        assertEquals("2026-08", Jornada.competenciaOperacional("2026-08-07"))
    }

    @Test
    fun competenciaOperacional_onOrAfterCutoff_rollsToNextMonth() {
        assertEquals("2026-09", Jornada.competenciaOperacional("2026-08-26"))
        assertEquals("2026-09", Jornada.competenciaOperacional("2026-08-31"))
    }

    @Test
    fun competenciaOperacional_rollsAcrossYearBoundary() {
        assertEquals("2027-01", Jornada.competenciaOperacional("2026-12-26"))
    }

    @Test
    fun adicionarMeses_handlesNegativeAndYearRollover() {
        assertEquals("2025-12", Jornada.adicionarMeses("2026-01", -1))
        assertEquals("2026-01", Jornada.adicionarMeses("2025-12", 1))
    }

    @Test
    fun competenciasCandidatas_returnsDistinctOrderedList() {
        val candidatas = Jornada.competenciasCandidatas("2026-08-07")
        assertEquals(listOf("2026-08", "2026-07", "2026-09"), candidatas)
    }

    @Test
    fun competenciasCandidatas_afterCutoff_includesOperationalAndCalendar() {
        val candidatas = Jornada.competenciasCandidatas("2026-08-26")
        assertEquals(listOf("2026-09", "2026-08", "2026-10"), candidatas)
    }

    @Test
    fun selecionarEscalaPorData_prefersPeriodContainingDate_evenAcrossMonths() {
        val escalas = listOf(
            escala(competencia = "2026-07", periodoInicio = "2026-06-26", periodoFim = "2026-07-25"),
            escala(competencia = "2026-08", periodoInicio = "2026-07-26", periodoFim = "2026-08-25"),
        )
        val escolhida = Jornada.selecionarEscalaPorData(escalas, "2026-08-07")
        assertEquals("2026-08", escolhida?.competencia)
    }

    @Test
    fun selecionarEscalaPorData_fallsBackToOperationalCompetencia_whenNoPeriodMatches() {
        val escalas = listOf(escala(competencia = "2026-08", periodoInicio = "2026-01-01", periodoFim = "2026-01-31"))
        val escolhida = Jornada.selecionarEscalaPorData(escalas, "2026-08-07")
        assertEquals("2026-08", escolhida?.competencia)
    }

    @Test
    fun selecionarEscalaPorData_returnsNull_whenNoEscalas() {
        assertNull(Jornada.selecionarEscalaPorData(emptyList(), "2026-08-07"))
    }

    private fun escala(competencia: String, periodoInicio: String, periodoFim: String) = TurnosMesRemoteDto(
        schemaVersion = 1,
        usuarioUid = "ana.silva",
        login = "ana.silva",
        equipeId = "EQ_TESTE",
        competencia = competencia,
        periodoInicio = periodoInicio,
        periodoFim = periodoFim,
        turnoPadrao = "M",
        status = TurnosMesStatus.PUBLICADA,
        dias = emptyMap(),
        totais = TotaisRemoteDto(),
    )
}
