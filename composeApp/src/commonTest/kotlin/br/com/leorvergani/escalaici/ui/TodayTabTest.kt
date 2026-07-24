package br.com.leorvergani.escalaici.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class TodayTabTest {
    @Test
    fun pendingChangeRequestsCountLabelUsesSingularForOne() {
        assertEquals("1 solicitação pendente", pendingChangeRequestsCountLabel(1))
    }

    @Test
    fun pendingChangeRequestsCountLabelUsesPluralForZeroAndMultiple() {
        assertEquals("0 solicitações pendentes", pendingChangeRequestsCountLabel(0))
        assertEquals("2 solicitações pendentes", pendingChangeRequestsCountLabel(2))
        assertEquals("5 solicitações pendentes", pendingChangeRequestsCountLabel(5))
    }
}
