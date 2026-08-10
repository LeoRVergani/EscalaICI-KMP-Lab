package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.CategoriaTurno
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.DiaRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TotaisRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.UsuarioRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.model.LabDate
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EscalaIciScheduleMapperTest {

    private val usuario = UsuarioRemoteDto(
        login = "ana.silva",
        nome = "Ana Silva",
        email = "ana.silva@empresa.com",
        cargo = "Analista",
        equipeId = "EQ_SOC",
        nivelHierarquico = 6,
        turnoPadrao = "M",
        ativo = true,
    )

    private val catalogo = CatalogoPadrao.CATALOGO_SOC

    @Test
    fun map_marksSummaryAsRealFirebaseData_notDemo() {
        val summary = EscalaIciScheduleMapper.map(turnosMes(dias = emptyMap()), usuario, catalogo)
        assertEquals("Firebase", summary.sourceFileName)
        assertTrue(summary.isImported)
    }

    @Test
    fun map_translatesKnownCodigoToExpectedShiftType() {
        val dias = mapOf(
            "2026-08-01" to DiaRemoteDto(c = "N", i = "19:00", f = "01:00", m = 360, vd = true, seq = 1),
        )
        val summary = EscalaIciScheduleMapper.map(turnosMes(dias), usuario, catalogo)
        val dia = summary.days.single()
        assertEquals(ShiftType.NOITE, dia.type)
        assertEquals("Noite", dia.label)
    }

    @Test
    fun map_usesCatalogDescriptionAsLabel_notHardcoded() {
        val dias = mapOf("2026-08-02" to DiaRemoteDto(c = "AFA"))
        val summary = EscalaIciScheduleMapper.map(turnosMes(dias), usuario, catalogo)
        assertEquals("Afastamento Atestado", summary.days.single().label)
        assertEquals(ShiftType.AFASTAMENTO, summary.days.single().type)
    }

    @Test
    fun map_fallsBackToIndefinido_forUnknownCodigoNotInCatalog() {
        val dias = mapOf("2026-08-03" to DiaRemoteDto(c = "ZZ"))
        val summary = EscalaIciScheduleMapper.map(turnosMes(dias), usuario, catalogo)
        assertEquals(ShiftType.INDEFINIDO, summary.days.single().type)
        assertEquals("ZZ", summary.days.single().label)
    }

    @Test
    fun map_sortsDaysChronologically() {
        val dias = mapOf(
            "2026-08-03" to DiaRemoteDto(c = "M"),
            "2026-08-01" to DiaRemoteDto(c = "M"),
            "2026-08-02" to DiaRemoteDto(c = "M"),
        )
        val summary = EscalaIciScheduleMapper.map(turnosMes(dias), usuario, catalogo)
        assertEquals(listOf("01/08", "02/08", "03/08"), summary.days.map { it.dateLabel })
    }

    @Test
    fun map_setsPeriodStartEnd_fromBackendFields_notFromDiasMinMax() {
        // FASE 17C - regra 1: a fonte da verdade do período é
        // `turnosMes.periodoInicio`/`periodoFim`, nunca um min/max
        // recalculado a partir de `dias`. Aqui `dias` só tem um dia no
        // meio do período (nem o primeiro nem o último) - se o mapper
        // ainda derivasse periodStart/periodEnd de `days`, o teste falharia.
        val dias = mapOf("2026-08-10" to DiaRemoteDto(c = "M"))
        val summary = EscalaIciScheduleMapper.map(turnosMes(dias), usuario, catalogo)

        assertEquals(LabDate(2026, 7, 26), summary.periodStart)
        assertEquals(LabDate(2026, 8, 25), summary.periodEnd)
    }

    @Test
    fun map_usesCustomCatalog_whenTeamHasItsOwn() {
        val catalogoCustom = mapOf(
            "X1" to TipoTurnoRemoteDto("X1", "Turno especial", CategoriaTurno.TRABALHO, "08:00", "16:00", 480, false, false, 0, "#123456"),
        )
        val dias = mapOf("2026-08-01" to DiaRemoteDto(c = "X1"))
        val summary = EscalaIciScheduleMapper.map(turnosMes(dias), usuario, catalogoCustom)
        assertEquals("Turno especial", summary.days.single().label)
    }

    private fun turnosMes(dias: Map<String, DiaRemoteDto>) = TurnosMesRemoteDto(
        schemaVersion = 1,
        usuarioUid = usuario.login,
        login = usuario.login,
        equipeId = usuario.equipeId,
        competencia = "2026-08",
        periodoInicio = "2026-07-26",
        periodoFim = "2026-08-25",
        turnoPadrao = "M",
        status = TurnosMesStatus.PUBLICADA,
        dias = dias,
        totais = TotaisRemoteDto(),
    )
}
