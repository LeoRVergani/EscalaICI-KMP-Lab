package br.com.leorvergani.escalaici.kmp.lab.source

import br.com.leorvergani.escalaici.kmp.lab.model.OnCallAssignment
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallPeriod
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSourceType
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary

enum class ConnectivityStatus { ONLINE, OFFLINE, UNKNOWN }

data class SourceMetadata(
    val sourceType: ScheduleSourceType,
    val fileName: String? = null,
    val periodId: String? = null,
    val sourceUpdatedAt: String? = null,
    val localSyncedAt: String? = null,
    val connectivity: ConnectivityStatus = ConnectivityStatus.UNKNOWN,
    val fromCache: Boolean = false,
    val remoteSchemaVersion: Int? = null,
    val userMessage: String? = null
)

sealed interface DataLoadResult<out T> {
    val metadata: SourceMetadata?
    val warnings: List<String>
    val loadedAt: String
    val canRetry: Boolean

    data class Success<T>(
        val data: T,
        override val metadata: SourceMetadata,
        override val warnings: List<String> = emptyList(),
        override val loadedAt: String,
        override val canRetry: Boolean = false
    ) : DataLoadResult<T>

    data class Empty(
        override val metadata: SourceMetadata,
        override val warnings: List<String> = emptyList(),
        override val loadedAt: String,
        override val canRetry: Boolean = true
    ) : DataLoadResult<Nothing>

    data class OfflineCache<T>(
        val data: T,
        override val metadata: SourceMetadata,
        override val warnings: List<String> = emptyList(),
        override val loadedAt: String,
        override val canRetry: Boolean = true
    ) : DataLoadResult<T>

    data class RecoverableError<T>(
        val message: String,
        val cachedData: T? = null,
        override val metadata: SourceMetadata? = null,
        override val warnings: List<String> = emptyList(),
        override val loadedAt: String,
        override val canRetry: Boolean = true
    ) : DataLoadResult<T>

    data class FatalError(
        val message: String,
        override val metadata: SourceMetadata? = null,
        override val warnings: List<String> = emptyList(),
        override val loadedAt: String,
        override val canRetry: Boolean = false
    ) : DataLoadResult<Nothing>
}

data class SourceQuery(
    val memberId: String? = null,
    val teamId: String? = null,
    val scheduleProfileId: String? = null
)

data class ScheduleSourceData(
    val summary: ScheduleSummary,
    val metadata: SourceMetadata
)

data class OnCallSourceData(
    val period: OnCallPeriod? = null,
    val assignments: List<OnCallAssignment>,
    val metadata: SourceMetadata
)

data class SourceUpdateStatus(
    val updateAvailable: Boolean,
    val metadata: SourceMetadata,
    val message: String? = null
)

interface ScheduleSource {
    val sourceType: ScheduleSourceType
    suspend fun loadActive(query: SourceQuery): DataLoadResult<ScheduleSourceData>
    suspend fun loadPeriod(query: SourceQuery, periodId: String): DataLoadResult<ScheduleSourceData>
    suspend fun checkForUpdate(query: SourceQuery): DataLoadResult<SourceUpdateStatus>
    suspend fun updateCache(data: ScheduleSourceData): Boolean
    suspend fun invalidateCache(query: SourceQuery)
}

interface OnCallSource {
    val sourceType: ScheduleSourceType
    suspend fun loadActive(query: SourceQuery): DataLoadResult<OnCallSourceData>
    suspend fun loadPeriod(query: SourceQuery, periodId: String): DataLoadResult<OnCallSourceData>
    suspend fun checkForUpdate(query: SourceQuery): DataLoadResult<SourceUpdateStatus>
    suspend fun updateCache(data: OnCallSourceData): Boolean
    suspend fun invalidateCache(query: SourceQuery)
}
