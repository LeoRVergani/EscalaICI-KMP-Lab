package br.com.leorvergani.escalaici.repository

import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.OnCallAssignment
import br.com.leorvergani.escalaici.model.OnCallPeriod
import br.com.leorvergani.escalaici.model.ScheduleAssignment
import br.com.leorvergani.escalaici.model.SchedulePeriod
import br.com.leorvergani.escalaici.model.Team
import br.com.leorvergani.escalaici.model.mockOnCallAssignments
import br.com.leorvergani.escalaici.model.mockOnCallPeriod
import br.com.leorvergani.escalaici.model.mockScheduleAssignments
import br.com.leorvergani.escalaici.model.mockSchedulePeriod
import br.com.leorvergani.escalaici.model.mockTeamMembers

/**
 * Implementacoes mock/em memoria dos contratos de `Repositories.kt`, usadas
 * apenas pelo laboratorio para validar que as interfaces sao usaveis. Nao
 * substituem nem antecipam a implementacao real (Firebase/cache local), que
 * fica para fase propria fora deste laboratorio.
 */

class MockScheduleRepository(
    private val period: SchedulePeriod = mockSchedulePeriod(),
    private val assignments: List<ScheduleAssignment> = mockScheduleAssignments()
) : ScheduleRepository {
    override suspend fun getSchedulePeriod(teamId: String): SchedulePeriod? =
        period.takeIf { it.teamId == teamId }

    override suspend fun getScheduleAssignments(periodId: String): List<ScheduleAssignment> =
        assignments.filter { it.periodId == periodId }
}

class MockMemberRepository(
    private val members: List<Member> = mockTeamMembers()
) : MemberRepository {
    override suspend fun getMember(memberId: String): Member? =
        members.firstOrNull { it.id == memberId }

    override suspend fun getMembersByTeam(teamId: String): List<Member> =
        members.filter { it.teamId == teamId }
}

class MockTeamRepository(
    private val teams: List<Team> = listOf(
        Team(teamId = "soc", name = "SOC", displayName = "SOC", members = mockTeamMembers())
    )
) : TeamRepository {
    override suspend fun getTeam(teamId: String): Team? =
        teams.firstOrNull { it.teamId == teamId }

    override suspend fun getTeams(): List<Team> = teams
}

class MockOnCallRepository(
    private val period: OnCallPeriod = mockOnCallPeriod(),
    private val assignments: List<OnCallAssignment> = mockOnCallAssignments()
) : OnCallRepository {
    override suspend fun getOnCallPeriod(teamId: String): OnCallPeriod? =
        period.takeIf { it.teamId == teamId }

    override suspend fun getOnCallAssignments(periodId: String): List<OnCallAssignment> =
        assignments.filter { it.periodId == periodId }
}

class MockAuthSessionRepository(
    private val memberId: String? = "lvergani@ici.tec.br"
) : AuthSessionRepository {
    override suspend fun currentMemberId(): String? = memberId
}

/**
 * Variante em memoria e mutavel de [AuthSessionRepository], usada pela tela
 * de login fake da FASE 9f. `signIn`/`signOut` nao fazem parte do contrato
 * multiplataforma (que so expoe leitura de sessao, conforme a spec 27 secao
 * 10) — sao apenas o jeito deste mock simular a interacao de login/logout
 * sem MSAL/Firebase reais.
 */
class InMemoryAuthSessionRepository(
    initialMemberId: String? = null
) : AuthSessionRepository {
    private var memberId: String? = initialMemberId

    override suspend fun currentMemberId(): String? = memberId

    fun signIn(memberId: String) {
        this.memberId = memberId
    }

    fun signOut() {
        memberId = null
    }
}

class MockLocalCacheRepository : LocalCacheRepository {
    private val cache = mutableMapOf<String, List<ScheduleAssignment>>()

    override suspend fun saveScheduleAssignments(periodId: String, assignments: List<ScheduleAssignment>) {
        cache[periodId] = assignments
    }

    override suspend fun loadScheduleAssignments(periodId: String): List<ScheduleAssignment>? =
        cache[periodId]
}
