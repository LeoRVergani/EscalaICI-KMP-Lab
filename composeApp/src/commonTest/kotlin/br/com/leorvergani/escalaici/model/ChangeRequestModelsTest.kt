package br.com.leorvergani.escalaici.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ChangeRequestModelsTest {
    private fun sampleRequest(status: String, requestType: String) = ScheduleChangeRequest(
        id = "request-1",
        workspaceId = "ici-dev",
        publicationRevision = 2,
        memberId = "member-1",
        teamId = "team-1",
        periodId = "period-1",
        assignmentId = "assignment-1",
        status = status,
        assignedManagerMemberId = "manager-1",
        requestType = requestType,
        reason = "Motivo de teste",
        createdAt = "2026-07-20T00:00:00Z",
    )

    @Test
    fun statusTypedParsesAllKnownDashboardValues() {
        val known = mapOf(
            "DRAFT" to ChangeRequestStatus.DRAFT,
            "PENDING" to ChangeRequestStatus.PENDING,
            "APPROVED" to ChangeRequestStatus.APPROVED,
            "REJECTED" to ChangeRequestStatus.REJECTED,
            "CANCELLED" to ChangeRequestStatus.CANCELLED,
            "EXPIRED" to ChangeRequestStatus.EXPIRED,
        )
        known.forEach { (raw, expected) ->
            assertEquals(expected, sampleRequest(status = raw, requestType = "OTHER").statusTyped)
        }
    }

    @Test
    fun statusTypedFallsBackToUnknownForUnrecognizedValue() {
        assertEquals(ChangeRequestStatus.UNKNOWN, sampleRequest(status = "algo_futuro", requestType = "OTHER").statusTyped)
    }

    @Test
    fun requestTypeTypedParsesAllKnownDashboardValues() {
        val known = mapOf(
            "SHIFT_CHANGE" to ChangeRequestType.SHIFT_CHANGE,
            "DAY_OFF_CHANGE" to ChangeRequestType.DAY_OFF_CHANGE,
            "SWAP_WITH_MEMBER" to ChangeRequestType.SWAP_WITH_MEMBER,
            "SCHEDULE_CORRECTION" to ChangeRequestType.SCHEDULE_CORRECTION,
            "OTHER" to ChangeRequestType.OTHER,
        )
        known.forEach { (raw, expected) ->
            assertEquals(expected, sampleRequest(status = "PENDING", requestType = raw).requestTypeTyped)
        }
    }

    @Test
    fun requestTypeTypedFallsBackToUnknownForUnrecognizedValue() {
        assertEquals(ChangeRequestType.UNKNOWN, sampleRequest(status = "PENDING", requestType = "algo_futuro").requestTypeTyped)
    }

    @Test
    fun labelsMatchDashboardTextsExactly() {
        assertEquals("Rascunho", ChangeRequestStatus.DRAFT.label())
        assertEquals("Pendente", ChangeRequestStatus.PENDING.label())
        assertEquals("Aprovada", ChangeRequestStatus.APPROVED.label())
        assertEquals("Recusada", ChangeRequestStatus.REJECTED.label())
        assertEquals("Cancelada", ChangeRequestStatus.CANCELLED.label())
        assertEquals("Expirada", ChangeRequestStatus.EXPIRED.label())

        assertEquals("Troca de turno", ChangeRequestType.SHIFT_CHANGE.label())
        assertEquals("Troca de folga", ChangeRequestType.DAY_OFF_CHANGE.label())
        assertEquals("Troca com colega", ChangeRequestType.SWAP_WITH_MEMBER.label())
        assertEquals("Correção de escala", ChangeRequestType.SCHEDULE_CORRECTION.label())
        assertEquals("Outro", ChangeRequestType.OTHER.label())
    }
}
