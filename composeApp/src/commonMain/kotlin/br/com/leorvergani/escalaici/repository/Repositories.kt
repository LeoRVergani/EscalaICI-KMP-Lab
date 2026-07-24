package br.com.leorvergani.escalaici.repository

import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.OnCallAssignment
import br.com.leorvergani.escalaici.model.OnCallPeriod
import br.com.leorvergani.escalaici.model.ScheduleAssignment
import br.com.leorvergani.escalaici.model.SchedulePeriod
import br.com.leorvergani.escalaici.model.Team

/**
 * FASE 9e — contratos de repository descritos na spec
 * `docs/spec/27-KMP-PWA-IOS-ESTRATEGIA.md` (secao 4, repositorio Android
 * principal). Interfaces multiplataforma puras: nenhuma delas conhece
 * Firebase, MSAL, Room/SQLDelight ou qualquer SDK de plataforma.
 *
 * As implementacoes reais (Firebase, cache local etc.) ficam fora do
 * laboratorio nesta fase; aqui existem apenas os contratos e implementacoes
 * mock em `MockRepositories.kt`, usadas para provar que os contratos
 * compilam e sao usaveis multiplataforma.
 */

interface ScheduleRepository {
    suspend fun getSchedulePeriod(teamId: String): SchedulePeriod?
    suspend fun getScheduleAssignments(periodId: String): List<ScheduleAssignment>
}

interface MemberRepository {
    suspend fun getMember(memberId: String): Member?
    suspend fun getMembersByTeam(teamId: String): List<Member>
}

interface TeamRepository {
    suspend fun getTeam(teamId: String): Team?
    suspend fun getTeams(): List<Team>
}

interface OnCallRepository {
    suspend fun getOnCallPeriod(teamId: String): OnCallPeriod?
    suspend fun getOnCallAssignments(periodId: String): List<OnCallAssignment>
}

interface AuthSessionRepository {
    suspend fun currentMemberId(): String?
}

interface LocalCacheRepository {
    suspend fun saveScheduleAssignments(periodId: String, assignments: List<ScheduleAssignment>)
    suspend fun loadScheduleAssignments(periodId: String): List<ScheduleAssignment>?
}
