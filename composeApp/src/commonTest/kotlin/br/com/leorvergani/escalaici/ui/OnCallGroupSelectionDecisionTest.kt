package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.OnCallAssignment
import br.com.leorvergani.escalaici.model.OnCallGroup
import br.com.leorvergani.escalaici.model.OnCallStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OnCallGroupSelectionDecisionTest {
    @Test
    fun singleActiveGroupSkipsSelection() {
        val decision = assertIs<OnCallGroupSelectionDecision.UseGroup>(
            decideOnCallGroupSelection(listOf(group("cosi", "COSI")), selectedGroupId = null)
        )

        assertEquals("cosi", decision.group.id)
        assertTrue(decision.skippedSelection)
    }

    @Test
    fun multipleActiveGroupsRequireExplicitSelection() {
        val decision = assertIs<OnCallGroupSelectionDecision.RequiresSelection>(
            decideOnCallGroupSelection(
                listOf(group("monitoring", "Monitoramento"), group("field", "Campo")),
                selectedGroupId = null
            )
        )

        assertEquals(listOf("field", "monitoring"), decision.groups.map { it.id })
    }

    @Test
    fun selectedGroupShowsOnlyAssignmentsFromThatGroup() {
        val decision = assertIs<OnCallGroupSelectionDecision.UseGroup>(
            decideOnCallGroupSelection(
                listOf(group("monitoring", "Monitoramento"), group("field", "Campo")),
                selectedGroupId = "monitoring"
            )
        )
        val visibleAssignments = onCallAssignmentsForGroup(
            listOf(
                assignment("a-monitoring", "monitoring"),
                assignment("a-field", "field")
            ),
            decision.group.id
        )

        assertEquals(listOf("a-monitoring"), visibleAssignments.map { it.id })
    }

    @Test
    fun noRegisteredGroupsIsRealEmptyState() {
        assertIs<OnCallGroupSelectionDecision.NoGroups>(
            decideOnCallGroupSelection(emptyList(), selectedGroupId = null)
        )
    }

    private fun group(id: String, name: String) = OnCallGroup(
        id = id,
        teamId = "soc",
        name = name,
        active = true
    )

    private fun assignment(id: String, groupId: String) = OnCallAssignment(
        id = id,
        periodId = "period-1",
        teamId = "soc",
        memberId = "member-1",
        memberName = "Pessoa Um",
        date = "2026-07-01",
        startTime = "19:00",
        endTime = "07:00",
        status = OnCallStatus.SCHEDULED,
        groupId = groupId
    )
}
