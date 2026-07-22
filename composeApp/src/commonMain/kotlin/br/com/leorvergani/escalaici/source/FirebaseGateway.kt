package br.com.leorvergani.escalaici.source

import br.com.leorvergani.escalaici.model.OnCallGroup

interface FirebaseScheduleGateway {
    suspend fun loadTeam(teamId: String): FirebaseTeamDto?
    suspend fun loadActiveSchedulePeriod(teamId: String): FirebaseSchedulePeriodDto?
    suspend fun loadScheduleAssignments(teamId: String, periodId: String): List<FirebaseScheduleAssignmentDto>
    suspend fun loadMembers(teamId: String): List<FirebaseMemberDto>
    suspend fun loadOnCallGroups(teamId: String): List<OnCallGroup>
    suspend fun loadActiveOnCallPeriod(teamId: String, groupId: String? = null): FirebaseOnCallPeriodDto?
    suspend fun loadOnCallAssignments(teamId: String, periodId: String, groupId: String? = null): List<FirebaseOnCallAssignmentDto>
    suspend fun checkRemoteUpdatedAt(teamId: String, onCall: Boolean = false): String?
}

expect fun createFirebaseScheduleGateway(): FirebaseScheduleGateway

interface FirebaseRawCacheStore {
    fun read(key: String): String?
    fun write(key: String, value: String): Boolean
    fun remove(key: String)
}

expect fun createFirebaseRawCacheStore(): FirebaseRawCacheStore
