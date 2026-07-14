package br.com.leorvergani.escalaici.kmp.lab.source

import br.com.leorvergani.escalaici.kmp.lab.model.OnCallAssignment
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSourceType
import br.com.leorvergani.escalaici.kmp.lab.model.YearResolutionSource
import br.com.leorvergani.escalaici.kmp.lab.model.mockOnCallAssignments
import br.com.leorvergani.escalaici.kmp.lab.model.mockScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.repository.CacheRead
import br.com.leorvergani.escalaici.kmp.lab.repository.CachedOnCall
import br.com.leorvergani.escalaici.kmp.lab.repository.CachedSchedule
import br.com.leorvergani.escalaici.kmp.lab.repository.LocalDataCache
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UniversalSourcesTest {
    private val resolver = ResolvePreferredSource()
    private val summary = mockScheduleSummary()

    @Test
    fun validRemoteSourceWinsDemo() {
        val remote = success(ScheduleSourceType.FIREBASE, "remote")
        val demo = success(ScheduleSourceType.DEMO, "demo")
        assertSame(remote, resolver.resolve(listOf(demo, remote)))
    }

    @Test
    fun realRemoteCacheWinsDemo() {
        val metadata = metadata(ScheduleSourceType.FIREBASE, fromCache = true)
        val cache = DataLoadResult.OfflineCache("cache", metadata, loadedAt = "now")
        val demo = success(ScheduleSourceType.DEMO, "demo")
        assertSame(cache, resolver.resolve(listOf(demo, cache)))
    }

    @Test
    fun confirmedLocalFileWinsLocalCache() {
        val file = success(ScheduleSourceType.LOCAL_FILE, "file")
        val cache = DataLoadResult.OfflineCache("cache", metadata(ScheduleSourceType.LOCAL_CACHE, true), loadedAt = "now")
        assertSame(file, resolver.resolve(listOf(cache, file)))
    }

    @Test
    fun localCacheIsRestoredByAdapter() = runTest {
        val cache = FakeLocalDataCache(schedule = CacheRead.Valid(cachedSchedule()))
        val result = LocalScheduleCacheSource(cache) { "loaded" }.loadActive(SourceQuery())
        val restored = assertIs<DataLoadResult.OfflineCache<ScheduleSourceData>>(result)
        assertEquals(summary, restored.data.summary)
        assertTrue(restored.metadata.fromCache)
    }

    @Test
    fun emptySourceRemainsExplicit() {
        val empty = DataLoadResult.Empty(metadata(ScheduleSourceType.LOCAL_FILE), loadedAt = "now")
        assertSame(empty, resolver.resolve(listOf(empty)))
    }

    @Test
    fun recoverableErrorWithCacheIsUsable() {
        val error = DataLoadResult.RecoverableError("offline", "cached", metadata(ScheduleSourceType.FIREBASE, true), loadedAt = "now")
        assertSame(error, resolver.resolve(listOf(error, DataLoadResult.Empty(metadata(ScheduleSourceType.DEMO), loadedAt = "now"))))
    }

    @Test
    fun fatalErrorWithoutCacheRemainsFatal() {
        val fatal = DataLoadResult.FatalError("invalid", metadata(ScheduleSourceType.FIREBASE), loadedAt = "now")
        assertSame(fatal, resolver.resolve(listOf(fatal)))
    }

    @Test
    fun metadataIsPreserved() {
        val metadata = SourceMetadata(
            sourceType = ScheduleSourceType.ONEDRIVE,
            fileName = "schedule.xlsx",
            periodId = "period-1",
            sourceUpdatedAt = "source-time",
            localSyncedAt = "sync-time",
            connectivity = ConnectivityStatus.ONLINE,
            remoteSchemaVersion = 7,
            userMessage = "updated"
        )
        val result = DataLoadResult.Success("data", metadata, listOf("warning"), "loaded")
        assertEquals(metadata, resolver.resolve(listOf(result)).metadata)
    }

    @Test
    fun scheduleAndOnCallPayloadsStaySeparate() {
        val scheduleData = ScheduleSourceData(summary, metadata(ScheduleSourceType.LOCAL_FILE))
        val onCallData = OnCallSourceData(assignments = mockOnCallAssignments(), metadata = metadata(ScheduleSourceType.LOCAL_FILE))
        assertTrue(scheduleData.summary.days.isNotEmpty())
        assertTrue(onCallData.assignments.isNotEmpty())
        assertFalse(scheduleData.metadata === onCallData.metadata)
    }

    @Test
    fun resultsFromDifferentSourcesAreNeverMerged() {
        val remote = success(ScheduleSourceType.FIREBASE, listOf("remote"))
        val local = success(ScheduleSourceType.LOCAL_FILE, listOf("local"))
        val selected = assertIs<DataLoadResult.Success<List<String>>>(resolver.resolve(listOf(local, remote)))
        assertEquals(listOf("remote"), selected.data)
    }

    @Test
    fun demoIsOnlyLastFallback() {
        val demo = success(ScheduleSourceType.DEMO, "demo")
        val localCache = DataLoadResult.OfflineCache("cache", metadata(ScheduleSourceType.LOCAL_CACHE, true), loadedAt = "now")
        assertSame(localCache, resolver.resolve(listOf(demo, localCache)))
    }

    @Test
    fun invalidImportDoesNotEraseConfirmedSource() = runTest {
        val source = ConfirmedLocalScheduleSource { "now" }
        val valid = ScheduleSourceData(summary, metadata(ScheduleSourceType.LOCAL_FILE))
        val invalid = ScheduleSourceData(summary.copy(days = emptyList()), metadata(ScheduleSourceType.LOCAL_FILE))
        assertTrue(source.confirm(valid))
        assertFalse(source.confirm(invalid))
        val loaded = assertIs<DataLoadResult.Success<ScheduleSourceData>>(source.loadActive(SourceQuery()))
        assertEquals(valid.summary, loaded.data.summary)
    }

    @Test
    fun invalidatingScheduleDoesNotDeleteOnCall() = runTest {
        val onCall = cachedOnCall()
        val cache = FakeLocalDataCache(
            schedule = CacheRead.Valid(cachedSchedule()),
            onCall = CacheRead.Valid(onCall)
        )
        LocalScheduleCacheSource(cache).invalidateCache(SourceQuery())
        assertIs<CacheRead.Missing>(cache.loadSchedule())
        assertEquals(onCall, assertIs<CacheRead.Valid<CachedOnCall>>(cache.loadOnCall()).value)
    }

    @Test
    fun invalidatingOnCallDoesNotDeleteSchedule() = runTest {
        val schedule = cachedSchedule()
        val cache = FakeLocalDataCache(
            schedule = CacheRead.Valid(schedule),
            onCall = CacheRead.Valid(cachedOnCall())
        )
        LocalOnCallCacheSource(cache).invalidateCache(SourceQuery())
        assertIs<CacheRead.Missing>(cache.loadOnCall())
        assertEquals(schedule, assertIs<CacheRead.Valid<CachedSchedule>>(cache.loadSchedule()).value)
    }

    private fun <T> success(type: ScheduleSourceType, data: T) =
        DataLoadResult.Success(data, metadata(type), loadedAt = "now")

    private fun metadata(type: ScheduleSourceType, fromCache: Boolean = false) =
        SourceMetadata(type, fromCache = fromCache)

    private fun cachedSchedule() = CachedSchedule(
        originalFileName = "schedule.xlsx",
        importedAt = "2026-07-14T12:00:00Z",
        resolvedYear = 2026,
        yearResolutionSource = YearResolutionSource.USER_CONFIRMED,
        summary = summary
    )

    private fun cachedOnCall(): CachedOnCall {
        val assignments: List<OnCallAssignment> = mockOnCallAssignments()
        return CachedOnCall(
            originalFileName = "oncall.xlsx",
            importedAt = "2026-07-14T12:00:00Z",
            resolvedYear = 2026,
            yearResolutionSource = YearResolutionSource.FULL_DATE_IN_WORKBOOK,
            teamId = assignments.first().teamId,
            assignments = assignments
        )
    }
}

private class FakeLocalDataCache(
    private var schedule: CacheRead<CachedSchedule> = CacheRead.Missing,
    private var onCall: CacheRead<CachedOnCall> = CacheRead.Missing
) : LocalDataCache {
    override fun loadSchedule() = schedule
    override fun saveSchedule(schedule: CachedSchedule): Boolean { this.schedule = CacheRead.Valid(schedule); return true }
    override fun clearSchedule() { schedule = CacheRead.Missing }
    override fun loadOnCall() = onCall
    override fun saveOnCall(onCall: CachedOnCall): Boolean { this.onCall = CacheRead.Valid(onCall); return true }
    override fun clearOnCall() { onCall = CacheRead.Missing }
}
