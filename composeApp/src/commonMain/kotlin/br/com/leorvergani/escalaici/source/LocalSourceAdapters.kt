package br.com.leorvergani.escalaici.source

import br.com.leorvergani.escalaici.model.OnCallPeriod
import br.com.leorvergani.escalaici.model.ScheduleSourceType
import br.com.leorvergani.escalaici.model.YearResolutionSource
import br.com.leorvergani.escalaici.repository.CacheRead
import br.com.leorvergani.escalaici.repository.CachedOnCall
import br.com.leorvergani.escalaici.repository.CachedSchedule
import br.com.leorvergani.escalaici.repository.LocalDataCache
import br.com.leorvergani.escalaici.repository.OnCallCacheSchemaVersion
import br.com.leorvergani.escalaici.repository.ScheduleCacheSchemaVersion

class ConfirmedLocalScheduleSource(
    private val now: () -> String = { "" }
) : ScheduleSource {
    override val sourceType = ScheduleSourceType.LOCAL_FILE
    private var confirmed: ScheduleSourceData? = null

    fun confirm(data: ScheduleSourceData): Boolean {
        if (data.summary.days.isEmpty()) return false
        confirmed = data.copy(metadata = data.metadata.copy(sourceType = sourceType, fromCache = false))
        return true
    }

    override suspend fun loadActive(query: SourceQuery): DataLoadResult<ScheduleSourceData> = confirmed?.let {
        DataLoadResult.Success(it, it.metadata, it.summary.warnings, now())
    } ?: DataLoadResult.Empty(SourceMetadata(sourceType), loadedAt = now())

    override suspend fun loadPeriod(query: SourceQuery, periodId: String): DataLoadResult<ScheduleSourceData> {
        val data = confirmed
        return if (data != null && data.metadata.periodId == periodId) DataLoadResult.Success(data, data.metadata, data.summary.warnings, now())
        else DataLoadResult.Empty(SourceMetadata(sourceType, periodId = periodId), loadedAt = now())
    }

    override suspend fun checkForUpdate(query: SourceQuery): DataLoadResult<SourceUpdateStatus> {
        val metadata = confirmed?.metadata ?: SourceMetadata(sourceType)
        return DataLoadResult.Success(SourceUpdateStatus(false, metadata), metadata, loadedAt = now())
    }

    override suspend fun updateCache(data: ScheduleSourceData): Boolean = confirm(data)
    override suspend fun invalidateCache(query: SourceQuery) { confirmed = null }
}

class ConfirmedLocalOnCallSource(
    private val now: () -> String = { "" }
) : OnCallSource {
    override val sourceType = ScheduleSourceType.LOCAL_FILE
    private var confirmed: OnCallSourceData? = null

    fun confirm(data: OnCallSourceData): Boolean {
        if (data.assignments.isEmpty()) return false
        confirmed = data.copy(metadata = data.metadata.copy(sourceType = sourceType, fromCache = false))
        return true
    }

    override suspend fun loadActive(query: SourceQuery): DataLoadResult<OnCallSourceData> = confirmed?.let {
        DataLoadResult.Success(it, it.metadata, loadedAt = now())
    } ?: DataLoadResult.Empty(SourceMetadata(sourceType), loadedAt = now())

    override suspend fun loadPeriod(query: SourceQuery, periodId: String): DataLoadResult<OnCallSourceData> {
        val data = confirmed
        return if (data != null && data.metadata.periodId == periodId) DataLoadResult.Success(data, data.metadata, loadedAt = now())
        else DataLoadResult.Empty(SourceMetadata(sourceType, periodId = periodId), loadedAt = now())
    }

    override suspend fun checkForUpdate(query: SourceQuery): DataLoadResult<SourceUpdateStatus> {
        val metadata = confirmed?.metadata ?: SourceMetadata(sourceType)
        return DataLoadResult.Success(SourceUpdateStatus(false, metadata), metadata, loadedAt = now())
    }

    override suspend fun updateCache(data: OnCallSourceData): Boolean = confirm(data)
    override suspend fun invalidateCache(query: SourceQuery) { confirmed = null }
}

class LocalScheduleCacheSource(
    private val cache: LocalDataCache,
    private val now: () -> String = { "" }
) : ScheduleSource {
    override val sourceType = ScheduleSourceType.LOCAL_CACHE

    override suspend fun loadActive(query: SourceQuery): DataLoadResult<ScheduleSourceData> = when (val cached = cache.loadSchedule()) {
        is CacheRead.Valid -> cached.value.toLoadResult(now())
        CacheRead.Missing -> DataLoadResult.Empty(metadata(), loadedAt = now())
        is CacheRead.Invalid -> DataLoadResult.RecoverableError(cached.safeMessage, metadata = metadata(), loadedAt = now())
    }

    override suspend fun loadPeriod(query: SourceQuery, periodId: String): DataLoadResult<ScheduleSourceData> {
        val result = loadActive(query)
        val actualPeriod = result.metadata?.periodId
        return if (actualPeriod == null || actualPeriod == periodId) result
        else DataLoadResult.Empty(metadata(periodId = periodId), loadedAt = now())
    }

    override suspend fun checkForUpdate(query: SourceQuery): DataLoadResult<SourceUpdateStatus> {
        val metadata = metadata()
        return DataLoadResult.Success(SourceUpdateStatus(false, metadata), metadata, loadedAt = now())
    }

    override suspend fun updateCache(data: ScheduleSourceData): Boolean {
        val start = data.summary.periodStart ?: return false
        return cache.saveSchedule(
            CachedSchedule(
                schemaVersion = ScheduleCacheSchemaVersion,
                sourceType = ScheduleSourceType.LOCAL_FILE.name,
                originalFileName = data.metadata.fileName ?: data.summary.sourceFileName ?: return false,
                importedAt = data.metadata.sourceUpdatedAt ?: now(),
                resolvedYear = start.year,
                yearResolutionSource = YearResolutionSource.USER_CONFIRMED,
                summary = data.summary
            )
        )
    }

    override suspend fun invalidateCache(query: SourceQuery) = cache.clearSchedule()

    private fun CachedSchedule.toLoadResult(loadedAt: String): DataLoadResult<ScheduleSourceData> {
        val metadata = metadata(
            fileName = originalFileName,
            periodId = summary.periodStart?.yearMonth()?.periodMonthLabel(),
            sourceUpdatedAt = importedAt,
            localSyncedAt = importedAt
        )
        val data = ScheduleSourceData(summary, metadata)
        return DataLoadResult.OfflineCache(data, metadata, summary.warnings, loadedAt)
    }

    private fun metadata(
        fileName: String? = null,
        periodId: String? = null,
        sourceUpdatedAt: String? = null,
        localSyncedAt: String? = null
    ) = SourceMetadata(
        sourceType = sourceType,
        fileName = fileName,
        periodId = periodId,
        sourceUpdatedAt = sourceUpdatedAt,
        localSyncedAt = localSyncedAt,
        connectivity = ConnectivityStatus.OFFLINE,
        fromCache = true,
        remoteSchemaVersion = ScheduleCacheSchemaVersion,
        userMessage = "Escala restaurada do cache local."
    )
}

class LocalOnCallCacheSource(
    private val cache: LocalDataCache,
    private val now: () -> String = { "" }
) : OnCallSource {
    override val sourceType = ScheduleSourceType.LOCAL_CACHE

    override suspend fun loadActive(query: SourceQuery): DataLoadResult<OnCallSourceData> = when (val cached = cache.loadOnCall()) {
        is CacheRead.Valid -> cached.value.toLoadResult(now())
        CacheRead.Missing -> DataLoadResult.Empty(metadata(), loadedAt = now())
        is CacheRead.Invalid -> DataLoadResult.RecoverableError(cached.safeMessage, metadata = metadata(), loadedAt = now())
    }

    override suspend fun loadPeriod(query: SourceQuery, periodId: String): DataLoadResult<OnCallSourceData> {
        val result = loadActive(query)
        return if (result.metadata?.periodId == periodId) result
        else DataLoadResult.Empty(metadata(periodId = periodId), loadedAt = now())
    }

    override suspend fun checkForUpdate(query: SourceQuery): DataLoadResult<SourceUpdateStatus> {
        val metadata = metadata()
        return DataLoadResult.Success(SourceUpdateStatus(false, metadata), metadata, loadedAt = now())
    }

    override suspend fun updateCache(data: OnCallSourceData): Boolean {
        if (data.assignments.isEmpty()) return false
        return cache.saveOnCall(
            CachedOnCall(
                schemaVersion = OnCallCacheSchemaVersion,
                sourceType = ScheduleSourceType.LOCAL_FILE.name,
                originalFileName = data.metadata.fileName ?: return false,
                importedAt = data.metadata.sourceUpdatedAt ?: now(),
                resolvedYear = data.assignments.first().startDate.substringBefore("-").toIntOrNull() ?: return false,
                yearResolutionSource = YearResolutionSource.FULL_DATE_IN_WORKBOOK,
                teamId = data.assignments.first().teamId,
                assignments = data.assignments,
                warnings = emptyList()
            )
        )
    }

    override suspend fun invalidateCache(query: SourceQuery) = cache.clearOnCall()

    private fun CachedOnCall.toLoadResult(loadedAt: String): DataLoadResult<OnCallSourceData> {
        val periodId = assignments.firstOrNull()?.periodId
        val metadata = metadata(originalFileName, periodId, importedAt, importedAt)
        val period = assignments.takeIf { it.isNotEmpty() }?.let {
            OnCallPeriod(
                id = periodId ?: "plantao-cache",
                teamId = teamId,
                startDate = it.minOf { assignment -> assignment.startDate },
                endDate = it.maxOf { assignment -> assignment.endDate },
                updatedAt = importedAt
            )
        }
        val data = OnCallSourceData(period, assignments, metadata)
        return DataLoadResult.OfflineCache(data, metadata, warnings, loadedAt)
    }

    private fun metadata(
        fileName: String? = null,
        periodId: String? = null,
        sourceUpdatedAt: String? = null,
        localSyncedAt: String? = null
    ) = SourceMetadata(
        sourceType = sourceType,
        fileName = fileName,
        periodId = periodId,
        sourceUpdatedAt = sourceUpdatedAt,
        localSyncedAt = localSyncedAt,
        connectivity = ConnectivityStatus.OFFLINE,
        fromCache = true,
        remoteSchemaVersion = OnCallCacheSchemaVersion,
        userMessage = "Plantão restaurado do cache local."
    )
}
