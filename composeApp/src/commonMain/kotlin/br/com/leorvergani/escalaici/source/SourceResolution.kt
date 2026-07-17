package br.com.leorvergani.escalaici.source

import br.com.leorvergani.escalaici.model.ScheduleSourceType

enum class PreferredSourceMode { AUTO, LOCAL, FIREBASE, ONEDRIVE, DROPBOX }

class SourcePriorityPolicy(
    private val remoteOrder: List<ScheduleSourceType> = listOf(
        ScheduleSourceType.FIREBASE,
        ScheduleSourceType.ONEDRIVE,
        ScheduleSourceType.DROPBOX
    )
) {
    fun <T> priorityOf(result: DataLoadResult<T>): Int {
        val metadata = result.metadata ?: return Int.MAX_VALUE
        val remoteIndex = remoteOrder.indexOf(metadata.sourceType)
        return when {
            remoteIndex >= 0 && !metadata.fromCache -> remoteIndex
            remoteIndex >= 0 -> 100 + remoteIndex
            metadata.sourceType == ScheduleSourceType.LOCAL_FILE -> 200
            metadata.sourceType == ScheduleSourceType.LOCAL_CACHE -> 300
            metadata.sourceType == ScheduleSourceType.DEMO -> 400
            else -> 500
        }
    }
}

class ResolvePreferredSource(
    private val policy: SourcePriorityPolicy = SourcePriorityPolicy(),
    private val mode: PreferredSourceMode = PreferredSourceMode.AUTO
) {
    fun <T> resolve(results: List<DataLoadResult<T>>): DataLoadResult<T> {
        require(results.isNotEmpty()) { "Ao menos um resultado de fonte é obrigatório." }
        val eligible = when (mode) {
            PreferredSourceMode.AUTO -> results
            PreferredSourceMode.LOCAL -> results.filter { it.metadata?.sourceType in setOf(ScheduleSourceType.LOCAL_FILE, ScheduleSourceType.LOCAL_CACHE, ScheduleSourceType.DEMO) }
            PreferredSourceMode.FIREBASE -> results.filter { it.metadata?.sourceType == ScheduleSourceType.FIREBASE }
            PreferredSourceMode.ONEDRIVE -> results.filter { it.metadata?.sourceType == ScheduleSourceType.ONEDRIVE }
            PreferredSourceMode.DROPBOX -> results.filter { it.metadata?.sourceType == ScheduleSourceType.DROPBOX }
        }.ifEmpty { results }
        return eligible
            .withIndex()
            .filter { (_, result) -> result.hasUsableData() }
            .minWithOrNull(compareBy<IndexedValue<DataLoadResult<T>>> { policy.priorityOf(it.value) }.thenBy { it.index })
            ?.value
            ?: eligible.firstOrNull { it is DataLoadResult.RecoverableError }
            ?: eligible.firstOrNull { it is DataLoadResult.Empty }
            ?: eligible.first()
    }
}

private fun <T> DataLoadResult<T>.hasUsableData(): Boolean = when (this) {
    is DataLoadResult.Success -> true
    is DataLoadResult.OfflineCache -> true
    is DataLoadResult.RecoverableError -> cachedData != null
    is DataLoadResult.Empty, is DataLoadResult.FatalError -> false
}

class LoadActiveSchedule(
    private val resolver: ResolvePreferredSource = ResolvePreferredSource()
) {
    suspend operator fun invoke(sources: List<ScheduleSource>, query: SourceQuery): DataLoadResult<ScheduleSourceData> =
        resolver.resolve(sources.map { it.loadActive(query) })
}

class LoadActiveOnCall(
    private val resolver: ResolvePreferredSource = ResolvePreferredSource()
) {
    suspend operator fun invoke(sources: List<OnCallSource>, query: SourceQuery): DataLoadResult<OnCallSourceData> =
        resolver.resolve(sources.map { it.loadActive(query) })
}

class RefreshSchedule {
    suspend operator fun invoke(source: ScheduleSource, query: SourceQuery): DataLoadResult<ScheduleSourceData> =
        source.loadActive(query)
}

class RefreshOnCall {
    suspend operator fun invoke(source: OnCallSource, query: SourceQuery): DataLoadResult<OnCallSourceData> =
        source.loadActive(query)
}
