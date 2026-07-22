package br.com.leorvergani.escalaici.repository

import br.com.leorvergani.escalaici.model.OnCallAssignment
import br.com.leorvergani.escalaici.model.ScheduleSummary
import br.com.leorvergani.escalaici.model.YearResolutionSource

const val ScheduleCacheSchemaVersion = 1
const val OnCallCacheSchemaVersion = 1
const val ScheduleCacheKey = "escalaici.web.schedule.cache.v1"
const val OnCallCacheKey = "escalaici.web.oncall.cache.v1"

data class CachedSchedule(
    val schemaVersion: Int = ScheduleCacheSchemaVersion,
    val sourceType: String = "LOCAL_FILE",
    val originalFileName: String,
    val importedAt: String,
    val resolvedYear: Int,
    val yearResolutionSource: YearResolutionSource,
    val scheduleType: String = "ESCALA_NORMAL",
    val summary: ScheduleSummary
)

data class CachedOnCall(
    val schemaVersion: Int = OnCallCacheSchemaVersion,
    val sourceType: String = "LOCAL_FILE",
    val originalFileName: String,
    val importedAt: String,
    val resolvedYear: Int,
    val yearResolutionSource: YearResolutionSource,
    val teamId: String,
    val groupId: String? = null,
    val assignments: List<OnCallAssignment>,
    val warnings: List<String> = emptyList()
)

sealed interface CacheRead<out T> {
    data object Missing : CacheRead<Nothing>
    data class Valid<T>(val value: T) : CacheRead<T>
    data class Invalid(val safeMessage: String) : CacheRead<Nothing>
}

interface LocalDataCache {
    fun loadSchedule(): CacheRead<CachedSchedule>
    fun saveSchedule(schedule: CachedSchedule): Boolean
    fun clearSchedule()

    fun loadOnCall(): CacheRead<CachedOnCall>
    fun saveOnCall(onCall: CachedOnCall): Boolean
    fun clearOnCall()
}

object UnavailableLocalDataCache : LocalDataCache {
    override fun loadSchedule(): CacheRead<CachedSchedule> = CacheRead.Missing
    override fun saveSchedule(schedule: CachedSchedule): Boolean = false
    override fun clearSchedule() = Unit
    override fun loadOnCall(): CacheRead<CachedOnCall> = CacheRead.Missing
    override fun saveOnCall(onCall: CachedOnCall): Boolean = false
    override fun clearOnCall() = Unit
}
