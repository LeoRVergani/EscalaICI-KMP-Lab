package br.com.leorvergani.escalaici.kmp.lab.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cobre as regras portadas exatamente do parser oficial
 * (`PlantaoWorkbookParser.kt` do app Android real, só leitura): regex de
 * data/hora, linha incompleta, fim <= início, erro quando nada é
 * encontrado, ordenação por início e depois por nome.
 */
class PlantaoWorkbookParserTest {

    private fun workbook(rows: List<List<String>>, sheetName: String = "Plantão"): ImportedWorkbook {
        return ImportedWorkbook(fileName = "plantao.xls", sheets = listOf(ImportedSheet(name = sheetName, rows = rows)))
    }

    private val header = listOf("Plantonista Segurança", "Data Início", "Data Fim")

    @Test
    fun parse_readsValidShiftsWithOfficialColumnNames() {
        val wb = workbook(
            listOf(
                header,
                listOf("Fulano de Tal", "26/07/2026 20:00", "27/07/2026 08:00")
            )
        )

        val result = PlantaoWorkbookParser.parse(wb)

        assertNull(result.error)
        assertEquals(1, result.assignments.size)
        val shift = result.assignments.single()
        assertEquals("Fulano de Tal", shift.memberName)
        assertEquals("2026-07-26", shift.date)
        assertEquals("20:00", shift.startTime)
        assertEquals("08:00", shift.endTime)
    }

    @Test
    fun parse_acceptsDashSeparatorBetweenDateAndTime() {
        val wb = workbook(listOf(header, listOf("Ciclana", "26/07/2026 - 20:00", "27/07/2026 - 08:00")))

        val result = PlantaoWorkbookParser.parse(wb)

        assertEquals(1, result.assignments.size)
    }

    @Test
    fun parse_skipsIncompleteRowWithWarning_whenTimeMissing() {
        // Sem hora, a mesma regex do app real não casa - vira linha incompleta.
        val wb = workbook(listOf(header, listOf("Fulano", "26/07/2026", "27/07/2026 08:00")))

        val result = PlantaoWorkbookParser.parse(wb)

        assertTrue(result.assignments.isEmpty())
        assertEquals("Não encontrei plantões no formato esperado: Plantonista Segurança, Data Inicio e Data Fim.", result.error)
        assertTrue(result.warnings.any { it.contains("incompleto") })
    }

    @Test
    fun parse_skipsRowWhenEndIsNotAfterStart() {
        val wb = workbook(
            listOf(
                header,
                listOf("Fulano", "26/07/2026 20:00", "26/07/2026 20:00")
            )
        )

        val result = PlantaoWorkbookParser.parse(wb)

        assertTrue(result.assignments.isEmpty())
        assertTrue(result.warnings.any { it.contains("data final menor ou igual") })
    }

    @Test
    fun parse_returnsExactOfficialErrorMessage_whenNothingFound() {
        val wb = workbook(listOf(listOf("Nome", "Outra coisa"), listOf("x", "y")))

        val result = PlantaoWorkbookParser.parse(wb)

        assertEquals(
            "Não encontrei plantões no formato esperado: Plantonista Segurança, Data Inicio e Data Fim.",
            result.error
        )
    }

    @Test
    fun parse_sortsByStartThenByNameLowercase() {
        val wb = workbook(
            listOf(
                header,
                listOf("Zeca", "26/07/2026 08:00", "26/07/2026 20:00"),
                listOf("Ana", "26/07/2026 08:00", "26/07/2026 20:00"),
                listOf("Bruno", "25/07/2026 08:00", "25/07/2026 20:00")
            )
        )

        val result = PlantaoWorkbookParser.parse(wb)

        assertEquals(listOf("Bruno", "Ana", "Zeca"), result.assignments.map { it.memberName })
    }

    @Test
    fun parse_ignoresBlankRowsSilently() {
        val wb = workbook(
            listOf(
                header,
                listOf("", "", ""),
                listOf("Fulano", "26/07/2026 20:00", "27/07/2026 08:00")
            )
        )

        val result = PlantaoWorkbookParser.parse(wb)

        assertEquals(1, result.assignments.size)
        assertTrue(result.warnings.isEmpty())
    }
}
