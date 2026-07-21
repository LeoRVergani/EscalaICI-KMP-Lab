package br.com.leorvergani.escalaici.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlantaoDisplayDecisionTest {
    @Test
    fun nonImportedOnCallReportShowsIllustrativeBadge() {
        assertTrue(shouldShowIllustrativeBadge(isImported = false))
    }

    @Test
    fun importedOnCallReportHidesIllustrativeBadge() {
        assertFalse(shouldShowIllustrativeBadge(isImported = true))
    }
}
