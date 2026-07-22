package br.com.leorvergani.escalaici.source

import br.com.leorvergani.escalaici.model.ScheduleSourceType
import br.com.leorvergani.escalaici.model.ShiftType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FirebaseSourcesTest {
    @Test fun activePeriodMapsToSuccess() = runTest {
        val source = fixture().scheduleSource()
        val result = assertIs<DataLoadResult.Success<ScheduleSourceData>>(source.loadActive(query()))
        assertEquals(ScheduleSourceType.FIREBASE, result.metadata.sourceType)
        assertEquals(2, result.data.summary.days.size)
        assertEquals("", result.data.summary.member.email)
    }

    @Test fun missingPeriodIsEmptyWithNoActivePeriodCause() = runTest {
        val fixture = fixture().apply { gateway.schedulePeriod = null }
        val result = assertIs<DataLoadResult.Empty>(fixture.scheduleSource().loadActive(query()))
        assertEquals(ScheduleSyncCause.NO_ACTIVE_PERIOD, result.cause)
    }

    @Test fun missingTeamIsEmptyWithTeamNotFoundCause() = runTest {
        val fixture = fixture().apply { gateway.team = null }
        val result = assertIs<DataLoadResult.Empty>(fixture.scheduleSource().loadActive(query()))
        assertEquals(ScheduleSyncCause.TEAM_NOT_FOUND, result.cause)
    }

    @Test fun noAssignmentsIsEmptyWithNoAssignmentsCauseInsteadOfGenericError() = runTest {
        val fixture = fixture().apply { gateway.scheduleAssignments = mutableListOf() }
        val result = assertIs<DataLoadResult.Empty>(fixture.scheduleSource().loadActive(query()))
        assertEquals(ScheduleSyncCause.NO_ASSIGNMENTS, result.cause)
    }

    @Test fun networkFailureCauseIsClassifiedAsNetworkError() = runTest {
        val fixture = fixture().apply { gateway.fail = true }
        val result = assertIs<DataLoadResult.RecoverableError<*>>(fixture.scheduleSource().loadActive(query()))
        assertEquals(ScheduleSyncCause.NETWORK_ERROR, result.cause)
    }

    @Test fun noAssignmentsFallsBackToCacheWhenAvailableInsteadOfDiscardingIt() = runTest {
        val fixture = fixture()
        assertIs<DataLoadResult.Success<*>>(fixture.scheduleSource().loadActive(query()))
        fixture.gateway.scheduleAssignments = mutableListOf()
        val result = assertIs<DataLoadResult.OfflineCache<ScheduleSourceData>>(fixture.scheduleSource().loadActive(query()))
        assertTrue(result.metadata.fromCache)
    }

    @Test fun invalidAssignmentIsRecoverable() = runTest {
        val fixture = fixture().apply { gateway.scheduleAssignments[0] = gateway.scheduleAssignments[0].copy(date = "invalid") }
        assertIs<DataLoadResult.RecoverableError<*>>(fixture.scheduleSource().loadActive(query()))
    }

    @Test fun officialAssignmentGranularityMapsToShiftTypes() = runTest {
        val cases = listOf(
            FirebaseScheduleAssignmentDto("off-bh", "soc", "period-1", "member-1", "pessoa1", "2026-07-03", "OFF", "BH") to ShiftType.BH,
            FirebaseScheduleAssignmentDto("off-birthday", "soc", "period-1", "member-1", "pessoa1", "2026-07-03", "OFF", "Aniversário") to ShiftType.ANIVERSARIO,
            FirebaseScheduleAssignmentDto("off-common", "soc", "period-1", "member-1", "pessoa1", "2026-07-03", "OFF", null) to ShiftType.FOLGA,
            FirebaseScheduleAssignmentDto("other-hole", "soc", "period-1", "member-1", "pessoa1", "2026-07-03", "OTHER", "Sem dado importado") to ShiftType.INDEFINIDO,
            FirebaseScheduleAssignmentDto("other-inconsistent", "soc", "period-1", "member-1", "pessoa1", "2026-07-03", "OTHER", "Trabalho sem turno localizado (1)") to ShiftType.INCONSISTENCIA
        )

        cases.forEach { (remoteAssignment, expectedType) ->
            val fixture = fixture().apply { gateway.scheduleAssignments = mutableListOf(remoteAssignment) }

            val result = assertIs<DataLoadResult.Success<ScheduleSourceData>>(fixture.scheduleSource().loadActive(query()))

            assertEquals(expectedType, result.data.summary.days.single().type)
        }
    }

    @Test fun missingMemberIsRecoverable() = runTest {
        val fixture = fixture().apply { gateway.members = emptyList() }
        assertIs<DataLoadResult.RecoverableError<*>>(fixture.scheduleSource().loadActive(query()))
    }

    @Test fun duplicateAssignmentIsRejected() = runTest {
        val fixture = fixture().apply { gateway.scheduleAssignments += gateway.scheduleAssignments.first() }
        assertIs<DataLoadResult.RecoverableError<*>>(fixture.scheduleSource().loadActive(query()))
    }

    @Test fun networkErrorUsesValidCache() = runTest {
        val fixture = fixture()
        assertIs<DataLoadResult.Success<*>>(fixture.scheduleSource().loadActive(query()))
        fixture.gateway.fail = true
        val result = assertIs<DataLoadResult.OfflineCache<ScheduleSourceData>>(fixture.scheduleSource().loadActive(query()))
        assertTrue(result.metadata.fromCache)
    }

    @Test fun networkErrorWithoutCacheIsRecoverable() = runTest {
        val fixture = fixture().apply { gateway.fail = true }
        assertIs<DataLoadResult.RecoverableError<*>>(fixture.scheduleSource().loadActive(query()))
    }

    @Test fun sameUpdatedAtDoesNotRequestUpdate() = runTest {
        val fixture = fixture()
        fixture.scheduleSource().loadActive(query())
        val result = assertIs<DataLoadResult.Success<SourceUpdateStatus>>(fixture.scheduleSource().checkForUpdate(query()))
        assertFalse(result.data.updateAvailable)
    }

    @Test fun newUpdatedAtRequestsUpdate() = runTest {
        val fixture = fixture()
        fixture.scheduleSource().loadActive(query())
        fixture.gateway.schedulePeriod = fixture.gateway.schedulePeriod!!.copy(updatedAt = "2026-07-15T00:00:00Z")
        val result = assertIs<DataLoadResult.Success<SourceUpdateStatus>>(fixture.scheduleSource().checkForUpdate(query()))
        assertTrue(result.data.updateAvailable)
    }

    @Test fun corruptPartialCacheIsIgnored() = runTest {
        val fixture = fixture().apply { store.write("escalaici.firebase.schedule.cache.v1", "partial"); gateway.fail = true }
        assertIs<DataLoadResult.RecoverableError<*>>(fixture.scheduleSource().loadActive(query()))
    }

    @Test fun scheduleAndOnCallCachesAreIndependent() = runTest {
        val fixture = fixture()
        assertIs<DataLoadResult.Success<*>>(fixture.scheduleSource().loadActive(query()))
        assertIs<DataLoadResult.Success<*>>(fixture.onCallSource().loadActive(query()))
        fixture.cache.clearOnCall()
        assertNotNull(fixture.cache.loadSchedule())
        assertEquals(null, fixture.cache.loadOnCall())
    }

    @Test fun localModePreservesLocalData() {
        val firebase = successResult(ScheduleSourceType.FIREBASE, "firebase")
        val local = successResult(ScheduleSourceType.LOCAL_FILE, "local")
        val resolved = ResolvePreferredSource(mode = PreferredSourceMode.LOCAL).resolve(listOf(firebase, local))
        assertEquals("local", assertIs<DataLoadResult.Success<String>>(resolved).data)
    }

    @Test fun autoModePrefersFirebase() {
        val firebase = successResult(ScheduleSourceType.FIREBASE, "firebase")
        val local = successResult(ScheduleSourceType.LOCAL_FILE, "local")
        val resolved = ResolvePreferredSource().resolve(listOf(local, firebase))
        assertEquals("firebase", assertIs<DataLoadResult.Success<String>>(resolved).data)
    }

    @Test fun gatewayContractExposesOnlyReads() {
        val allowed = setOf("loadTeam", "loadActiveSchedulePeriod", "loadScheduleAssignments", "loadMembers", "loadActiveOnCallPeriod", "loadOnCallAssignments", "checkRemoteUpdatedAt")
        assertEquals(allowed.size, allowed.distinct().size)
        assertTrue(allowed.none { name -> listOf("add", "create", "set", "update", "delete", "batch", "transaction").any { name.startsWith(it, true) } })
    }
}

private data class Fixture(val gateway: FakeFirebaseGateway, val store: MemoryRawStore, val cache: FirebaseSourceCache) {
    fun scheduleSource() = FirebaseScheduleSource(gateway, cache) { "2026-07-14T12:00:00Z" }
    fun onCallSource() = FirebaseOnCallSource(gateway, cache) { "2026-07-14T12:00:00Z" }
}

private fun fixture(): Fixture {
    val store = MemoryRawStore()
    return Fixture(FakeFirebaseGateway(), store, FirebaseSourceCache(store))
}

private fun query() = SourceQuery(memberId = "member-1", teamId = "soc")

private fun successResult(type: ScheduleSourceType, value: String) = DataLoadResult.Success(value, SourceMetadata(type), loadedAt = "now")

private class MemoryRawStore : FirebaseRawCacheStore {
    private val values = mutableMapOf<String, String>()
    override fun read(key: String) = values[key]
    override fun write(key: String, value: String): Boolean { values[key] = value; return true }
    override fun remove(key: String) { values.remove(key) }
}

private class FakeFirebaseGateway : FirebaseScheduleGateway {
    var fail = false
    var team: FirebaseTeamDto? = FirebaseTeamDto("soc", "SOC", true)
    var schedulePeriod: FirebaseSchedulePeriodDto? = FirebaseSchedulePeriodDto("period-1", "soc", "Julho", "2026-06-26", "2026-07-25", true, "2026-07-14T00:00:00Z")
    var members = listOf(FirebaseMemberDto("member-1", "soc", "Pessoa Um", "pessoa1", "Analista", "member", true))
    var scheduleAssignments = mutableListOf(
        FirebaseScheduleAssignmentDto("a1", "soc", "period-1", "member-1", "pessoa1", "2026-07-01", "WORK_SHIFT", "Manhã"),
        FirebaseScheduleAssignmentDto("a2", "soc", "period-1", "member-1", "pessoa1", "2026-07-02", "OFF")
    )
    private val onCallPeriod = FirebaseOnCallPeriodDto("oncall-1", "soc", "Julho", "2026-07-01", "2026-07-31", true, "2026-07-14T00:00:00Z")
    private val onCallAssignments = listOf(FirebaseOnCallAssignmentDto("o1", "soc", "oncall-1", "member-1", "pessoa1", "2026-07-01T19:00:00", "2026-07-02T07:00:00", "Plantão", true))

    private fun check() { if (fail) error("network") }
    override suspend fun loadTeam(teamId: String) = team.also { check() }
    override suspend fun loadActiveSchedulePeriod(teamId: String) = schedulePeriod.also { check() }
    override suspend fun loadScheduleAssignments(teamId: String, periodId: String) = scheduleAssignments.toList().also { check() }
    override suspend fun loadMembers(teamId: String) = members.also { check() }
    override suspend fun loadActiveOnCallPeriod(teamId: String) = onCallPeriod.also { check() }
    override suspend fun loadOnCallAssignments(teamId: String, periodId: String) = onCallAssignments.also { check() }
    override suspend fun checkRemoteUpdatedAt(teamId: String, onCall: Boolean): String? { check(); return if (onCall) onCallPeriod.updatedAt else schedulePeriod?.updatedAt }
}
