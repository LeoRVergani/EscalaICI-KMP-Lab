package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TotaisRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `escolherEscalaAtual` (decisao pura, sem rede) - cobre os cenarios do
 * prompt FASE 15 secao 32: periodo atravessando mes, periodo
 * anterior/futuro nunca selecionado como atual, sem correspondencia.
 */
class CurrentScheduleResolverTest {

    @Test
    fun idDocumentoTurnosMes_matchesRealContractShape() {
        assertEquals("EQ_SOC_ana.silva_2026-08", idDocumentoTurnosMes("EQ_SOC", "ana.silva", "2026-08"))
    }

    @Test
    fun escolherEscalaAtual_selectsScheduleWhosePeriodCoversToday_crossingMonthBoundary() {
        val julho = escala("2026-07", "2026-06-26", "2026-07-25")
        val agosto = escala("2026-08", "2026-07-26", "2026-08-25")
        val escolhida = escolherEscalaAtual(listOf(julho, agosto), viaId = null, hojeIso = "2026-08-07", competenciaOperacional = "2026-08")
        assertEquals("2026-08", escolhida?.competencia)
    }

    @Test
    fun escolherEscalaAtual_neverSelectsPastPeriod_whenItDoesNotCoverToday() {
        val periodoPassado = escala("2026-06", "2026-05-26", "2026-06-25")
        val escolhida = escolherEscalaAtual(listOf(periodoPassado), viaId = null, hojeIso = "2026-08-07", competenciaOperacional = "2026-08")
        assertNull(escolhida)
    }

    @Test
    fun escolherEscalaAtual_neverSelectsFuturePeriod_whenItDoesNotCoverToday() {
        val periodoFuturo = escala("2026-10", "2026-09-26", "2026-10-25")
        val escolhida = escolherEscalaAtual(listOf(periodoFuturo), viaId = null, hojeIso = "2026-08-07", competenciaOperacional = "2026-08")
        assertNull(escolhida)
    }

    @Test
    fun escolherEscalaAtual_fallsBackToOperationalCompetencia_whenNoPeriodMatchesButCompetenciaDoes() {
        // Documento com periodoInicio/Fim inconsistentes com a competencia (dado real "torto"), mas a competencia bate.
        val escala = escala("2026-08", "2026-01-01", "2026-01-31")
        val escolhida = escolherEscalaAtual(listOf(escala), viaId = null, hojeIso = "2026-08-07", competenciaOperacional = "2026-08")
        assertEquals("2026-08", escolhida?.competencia)
    }

    @Test
    fun escolherEscalaAtual_mergesViaIdCandidate_withoutDuplicating() {
        val viaId = escala("2026-08", "2026-07-26", "2026-08-25")
        val escolhida = escolherEscalaAtual(candidatas = emptyList(), viaId = viaId, hojeIso = "2026-08-07", competenciaOperacional = "2026-08")
        assertEquals("2026-08", escolhida?.competencia)
    }

    @Test
    fun escolherEscalaAtual_returnsNull_whenNothingMatchesPeriodOrCompetencia() {
        val escala = escala("2026-06", "2026-05-26", "2026-06-25")
        val escolhida = escolherEscalaAtual(listOf(escala), viaId = null, hojeIso = "2026-08-07", competenciaOperacional = "2026-08")
        assertNull(escolhida)
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
