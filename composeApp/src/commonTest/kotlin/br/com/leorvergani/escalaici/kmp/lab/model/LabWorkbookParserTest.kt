package br.com.leorvergani.escalaici.kmp.lab.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cobre as regras alinhadas na FASE 11.2 com o parser oficial
 * (`ScaleWorkbookParser.kt` do app Android real, so leitura): ranges fixos
 * de linha/coluna, validacao de data de calendario, e a assimetria de
 * separadores entre `containsCollaborator`/`teamMembersExcluding`.
 */
class LabWorkbookParserTest {

    private fun row(size: Int, vararg cells: Pair<Int, String>): List<String> {
        val values = MutableList(size) { "" }
        cells.forEach { (index, value) -> values[index] = value }
        return values
    }

    private fun workbook(escalistasRows: List<List<String>>, escalaRows: List<List<String>>): ImportedWorkbook {
        return ImportedWorkbook(
            fileName = "teste.xls",
            sheets = listOf(
                ImportedSheet(name = "Escalistas", rows = escalistasRows),
                ImportedSheet(name = "Escala", rows = escalaRows)
            )
        )
    }

    @Test
    fun parse_matchesSheetNamesIgnoringCaseAndAccents() {
        val wb = ImportedWorkbook(
            fileName = "teste.xls",
            sheets = listOf(
                ImportedSheet(
                    name = "ESCALISTAS",
                    rows = listOf(
                        row(4),
                        row(4),
                        row(4, 2 to "Colaborador", 3 to "06/07/2026"),
                        row(4, 2 to "lvergani", 3 to "1")
                    )
                ),
                ImportedSheet(
                    name = "escaLa",
                    rows = listOf(
                        row(7),
                        row(7),
                        row(7, 0 to "06/07/2026", 3 to "lvergani")
                    )
                )
            )
        )

        val preview = LabWorkbookParser.parse(wb, "lvergani")

        assertTrue(preview.errors.isEmpty(), "esperava sheets encontradas, erros: ${preview.errors}")
        assertEquals(1, preview.daysRead)
    }

    @Test
    fun parse_ignoresEscalistasStatusColumnsOutsideFixedRange() {
        val escalistas = listOf(
            row(40),
            row(40),
            row(40, 2 to "Colaborador", 3 to "06/07/2026", 33 to "07/07/2026"),
            row(40, 2 to "lvergani", 3 to "1", 33 to "X")
        )
        val escala = listOf(
            row(7),
            row(7),
            row(7, 0 to "06/07/2026", 3 to "lvergani"),
            row(7, 0 to "07/07/2026")
        )

        val preview = LabWorkbookParser.parse(workbook(escalistas, escala), "lvergani")
        val days = requireNotNull(preview.summary).days.associateBy { it.date }

        val day07 = requireNotNull(days[LabDate(2026, 7, 7)])
        assertEquals(ShiftType.INDEFINIDO, day07.type, "status na coluna 33 (fora de 3..32) nao deveria ser lido")
    }

    @Test
    fun parse_ignoresEscalaRowsOutsideFixedRange() {
        val escalistas = listOf(
            row(4),
            row(4),
            row(4, 2 to "Colaborador", 3 to "06/07/2026"),
            row(4, 2 to "lvergani", 3 to "1")
        )
        val escalaRows = MutableList(33) { row(7) }
        escalaRows[2] = row(7, 0 to "06/07/2026", 3 to "lvergani")
        // linha 32 esta fora do range fixo 2..31 usado pela aba Escala.
        escalaRows[32] = row(7, 0 to "10/07/2026", 3 to "lvergani")

        val preview = LabWorkbookParser.parse(workbook(escalistas, escalaRows), "lvergani")
        val dates = requireNotNull(preview.summary).days.mapNotNull { it.date }

        assertEquals(listOf(LabDate(2026, 7, 6)), dates, "linha 32 (fora de 2..31) nao deveria ser lida")
    }

    @Test
    fun parseDateToken_rejectsCalendarInvalidDates() {
        val escalistas = listOf(row(4), row(4), row(4, 2 to "Colaborador"), row(4, 2 to "lvergani"))
        val escala = listOf(
            row(7),
            row(7),
            // 31/02 nao existe em nenhum ano - deve ser rejeitada, nao arredondada.
            row(7, 0 to "31/02/2026", 3 to "lvergani")
        )

        val preview = LabWorkbookParser.parse(workbook(escalistas, escala), "lvergani")

        assertNull(preview.summary, "data invalida (31/02) nao deveria gerar nenhum dia")
        assertTrue(preview.warnings.any { it.contains("Nenhum dia de escala foi lido") })
    }

    @Test
    fun parseDateToken_acceptsLeapDayOnlyOnLeapYears() {
        val escalistas = listOf(row(4), row(4), row(4, 2 to "Colaborador"), row(4, 2 to "lvergani"))
        val escala = listOf(
            row(7),
            row(7),
            row(7, 0 to "29/02/2024", 3 to "lvergani")
        )

        val preview = LabWorkbookParser.parse(workbook(escalistas, escala), "lvergani")

        assertEquals(1, preview.daysRead, "29/02/2024 e valida (2024 e bissexto)")
    }

    @Test
    fun teamMembersExcluding_usesFewerSeparatorsThanCollaboratorMatch() {
        val escalistas = listOf(row(4), row(4), row(4, 2 to "Colaborador"), row(4, 2 to "lvergani"))
        val escala = listOf(
            row(7),
            row(7),
            // ';' conta para achar o colaborador (containsCollaborator),
            // mas NAO separa nomes na extracao da equipe (teamMembersExcluding).
            row(7, 0 to "06/07/2026", 3 to "lvergani;colegaB")
        )

        val preview = LabWorkbookParser.parse(workbook(escalistas, escala), "lvergani")
        val day = requireNotNull(preview.summary).days.single()

        assertEquals(ShiftType.MANHA, day.type, "';' deveria contar para achar o colaborador no turno")
        assertEquals(listOf("lvergani;colegaB"), day.teamMembers, "';' nao deveria separar nomes na extracao da equipe")
    }

    @Test
    fun labelFor_rendersNuancedLabelsMatchingOfficialParser() {
        val escalistas = listOf(
            row(6),
            row(6),
            row(6, 2 to "Colaborador", 3 to "06/07/2026", 4 to "07/07/2026", 5 to "08/07/2026"),
            row(6, 2 to "lvergani", 3 to "BH", 4 to "AN", 5 to "DF")
        )
        val escala = listOf(
            row(7),
            row(7),
            row(7, 0 to "06/07/2026"),
            row(7, 0 to "07/07/2026"),
            row(7, 0 to "08/07/2026")
        )

        val preview = LabWorkbookParser.parse(workbook(escalistas, escala), "lvergani")
        val days = requireNotNull(preview.summary).days.associateBy { it.date }

        assertEquals("Banco de horas", days.getValue(LabDate(2026, 7, 6)).label)
        assertEquals("Folga aniversário", days.getValue(LabDate(2026, 7, 7)).label)
        assertEquals("Folga / DF", days.getValue(LabDate(2026, 7, 8)).label)
    }

    @Test
    fun labelFor_marksMissingTurnWithWorkSequenceNumber() {
        val escalistas = listOf(
            row(4),
            row(4),
            row(4, 2 to "Colaborador", 3 to "06/07/2026"),
            row(4, 2 to "lvergani", 3 to "3")
        )
        val escala = listOf(
            row(7),
            row(7),
            row(7, 0 to "06/07/2026")
        )

        val preview = LabWorkbookParser.parse(workbook(escalistas, escala), "lvergani")
        val day = requireNotNull(preview.summary).days.single()

        assertEquals(ShiftType.INCONSISTENCIA, day.type)
        assertEquals("Trabalho sem turno localizado", day.label)
        assertEquals("Status origem: 3. Colaborador não encontrado nos turnos da aba Escala.", day.note)
    }
}
