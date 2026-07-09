package br.com.leorvergani.escalaici.kmp.lab.repository

import br.com.leorvergani.escalaici.kmp.lab.model.Member
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallAssignment
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallPeriod
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleAssignment
import br.com.leorvergani.escalaici.kmp.lab.model.SchedulePeriod
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftSwapRequest
import br.com.leorvergani.escalaici.kmp.lab.model.Team

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

interface ShiftSwapRepository {
    suspend fun getSwapRequests(memberId: String): List<ShiftSwapRequest>
    suspend fun requestSwap(request: ShiftSwapRequest): ShiftSwapRequest
}

interface AuthSessionRepository {
    suspend fun currentMemberId(): String?
}

interface LocalCacheRepository {
    suspend fun saveScheduleAssignments(periodId: String, assignments: List<ScheduleAssignment>)
    suspend fun loadScheduleAssignments(periodId: String): List<ScheduleAssignment>?
}
