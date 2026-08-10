package br.com.leorvergani.escalaici.kmp.lab.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `buildPeriodCalendarGrid`/`competenciaHeaderLabel` (FASE 17C) - grade
 * única da competência operacional (ex. 26/07→25/08), nunca por mês civil.
 */
class PeriodCalendarGridTest {

    @Test
    fun competenciaCruzandoMeses_primeiraEUltimaDataCorretas() {
        val start = LabDate(2026, 7, 26)
        val end = LabDate(2026, 8, 25)
        val cells = buildPeriodCalendarGrid(start, end)

        val realDates = cells.mapNotNull { it.date }
        assertEquals(start, realDates.first())
        assertEquals(end, realDates.last())
        assertTrue(realDates.none { it < start || it > end }, "nenhuma data real deve ficar fora do período")
    }

    @Test
    fun viradaDeAno_26dezembroA25janeiro() {
        val start = LabDate(2026, 12, 26)
        val end = LabDate(2027, 1, 25)
        val cells = buildPeriodCalendarGrid(start, end)

        val realDates = cells.mapNotNull { it.date }
        assertEquals(start, realDates.first())
        assertEquals(end, realDates.last())
        assertEquals(6, realDates.count { it.year == 2026 }, "26 a 31 de dezembro")
        assertEquals(25, realDates.count { it.year == 2027 }, "1 a 25 de janeiro")
    }

    @Test
    fun periodoComecaNoDomingo_semFillerAntes() {
        // 2026-08-30 é domingo.
        val start = LabDate(2026, 8, 30)
        val end = LabDate(2026, 9, 5)
        val cells = buildPeriodCalendarGrid(start, end)

        assertEquals(start, cells.first().date, "sem preenchimento antes de um período que já começa no domingo")
    }

    @Test
    fun periodoComecaNaSegunda_umFillerAntes() {
        // 2026-08-31 é segunda-feira.
        val start = LabDate(2026, 8, 31)
        val end = LabDate(2026, 9, 6)
        val cells = buildPeriodCalendarGrid(start, end)

        assertNull(cells[0].date, "primeira célula deve ser filler (domingo anterior ao início)")
        assertEquals(start, cells[1].date)
    }

    @Test
    fun periodoComecaNoMeioDaSemana_fillerCorrespondenteAoOffset() {
        // 2026-08-27 é quinta-feira -> 4 células de filler antes (dom/seg/ter/qua).
        val start = LabDate(2026, 8, 27)
        val end = LabDate(2026, 9, 2)
        val cells = buildPeriodCalendarGrid(start, end)

        assertEquals(4, cells.indexOfFirst { it.date != null })
        assertEquals(start, cells[4].date)
    }

    @Test
    fun fillerAnterior_nuncaEhUmaDataReal() {
        val start = LabDate(2026, 8, 31)
        val end = LabDate(2026, 9, 6)
        val cells = buildPeriodCalendarGrid(start, end)

        val fillerAntes = cells.takeWhile { it.date == null }
        assertTrue(fillerAntes.all { it.date == null })
    }

    @Test
    fun fillerPosterior_nuncaEhUmaDataReal() {
        val start = LabDate(2026, 7, 26)
        val end = LabDate(2026, 8, 25)
        val cells = buildPeriodCalendarGrid(start, end)

        val fillerDepois = cells.takeLastWhile { it.date == null }
        assertTrue(fillerDepois.all { it.date == null })
    }

    @Test
    fun quantidadeDeCelulas_eSempreMultiploDe7() {
        val casos = listOf(
            LabDate(2026, 7, 26) to LabDate(2026, 8, 25),
            LabDate(2026, 12, 26) to LabDate(2027, 1, 25),
            LabDate(2026, 8, 30) to LabDate(2026, 9, 5),
            LabDate(2026, 8, 31) to LabDate(2026, 9, 6),
            LabDate(2026, 8, 27) to LabDate(2026, 9, 2),
        )
        casos.forEach { (start, end) ->
            val cells = buildPeriodCalendarGrid(start, end)
            assertEquals(0, cells.size % 7, "grade para $start..$end deveria ter tamanho múltiplo de 7")
        }
    }

    @Test
    fun nenhumaDataDuplicadaOuPerdida() {
        val start = LabDate(2026, 7, 26)
        val end = LabDate(2026, 8, 25)
        val cells = buildPeriodCalendarGrid(start, end)
        val realDates = cells.mapNotNull { it.date }

        assertEquals(realDates.size, realDates.toSet().size, "nao deveria haver datas duplicadas")
        assertEquals(31, realDates.size, "26/07 a 25/08 tem 31 dias")

        var expected = start
        realDates.forEach { date ->
            assertEquals(expected, date, "nenhuma data deveria ficar perdida/fora de ordem")
            expected = expected.plusDays(1)
        }
    }

    @Test
    fun header_mesmoMesEAno() {
        assertEquals("Agosto de 2026", competenciaHeaderLabel(LabDate(2026, 8, 1), LabDate(2026, 8, 25)))
    }

    @Test
    fun header_mesesDiferentesMesmoAno() {
        assertEquals("Julho — agosto de 2026", competenciaHeaderLabel(LabDate(2026, 7, 26), LabDate(2026, 8, 25)))
    }

    @Test
    fun header_mesesEAnosDiferentes() {
        assertEquals("Dezembro de 2026 — janeiro de 2027", competenciaHeaderLabel(LabDate(2026, 12, 26), LabDate(2027, 1, 25)))
    }
}
