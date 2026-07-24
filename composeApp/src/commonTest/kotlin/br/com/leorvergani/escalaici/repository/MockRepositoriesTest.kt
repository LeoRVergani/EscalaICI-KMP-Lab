package br.com.leorvergani.escalaici.repository

import br.com.leorvergani.escalaici.model.ScheduleAssignment
import br.com.leorvergani.escalaici.model.ShiftType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockRepositoriesTest {

    @Test
    fun scheduleRepository_filtersAssignmentsByPeriod() = runTest {
        val repository = MockScheduleRepository()
        val period = repository.getSchedulePeriod("soc")

        assertEquals("soc", period?.teamId)
        val assignments = repository.getScheduleAssignments(requireNotNull(period).id)
        assertTrue(assignments.isNotEmpty())
        assertTrue(assignments.all { it.periodId == period.id })
    }

    @Test
    fun memberRepository_findsMemberByIdAndTeam() = runTest {
        val repository = MockMemberRepository()

        val member = repository.getMember("lvergani@ici.tec.br")
        assertEquals("Leonardo Vergani", member?.displayName)

        val teamMembers = repository.getMembersByTeam("soc")
        assertTrue(teamMembers.isNotEmpty())
        assertTrue(teamMembers.all { it.teamId == "soc" })
    }

    @Test
    fun teamRepository_returnsKnownTeam() = runTest {
        val repository = MockTeamRepository()

        val team = repository.getTeam("soc")
        assertEquals("SOC", team?.name)
        assertTrue(repository.getTeams().isNotEmpty())
    }

    @Test
    fun onCallRepository_filtersAssignmentsByPeriod() = runTest {
        val repository = MockOnCallRepository()

        val period = repository.getOnCallPeriod("soc")
        assertEquals("soc", period?.teamId)
        val assignments = repository.getOnCallAssignments(requireNotNull(period).id)
        assertTrue(assignments.all { it.periodId == period.id })
    }

    @Test
    fun authSessionRepository_returnsConfiguredMemberId() = runTest {
        assertEquals("lvergani@ici.tec.br", MockAuthSessionRepository().currentMemberId())
        assertNull(MockAuthSessionRepository(memberId = null).currentMemberId())
    }

    @Test
    fun localCacheRepository_roundTripsSavedAssignments() = runTest {
        val repository = MockLocalCacheRepository()
        assertNull(repository.loadScheduleAssignments("period-x"))

        val assignments = listOf(
            ScheduleAssignment(
                id = "a1",
                periodId = "period-x",
                teamId = "soc",
                memberId = "lvergani@ici.tec.br",
                memberName = "lvergani",
                date = "2026-07-06",
                shiftType = ShiftType.MANHA
            )
        )
        repository.saveScheduleAssignments("period-x", assignments)

        assertEquals(assignments, repository.loadScheduleAssignments("period-x"))
    }
}
