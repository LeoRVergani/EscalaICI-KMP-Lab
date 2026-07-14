package br.com.leorvergani.escalaici.kmp.lab.source

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val FirebaseCacheSchemaVersion = 1

class FirebaseSourceCache(
    private val store: FirebaseRawCacheStore,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun loadSchedule(): FirebaseScheduleSnapshot? = decode(ScheduleKey)
    fun loadOnCall(): FirebaseOnCallSnapshot? = decode(OnCallKey)

    fun saveSchedule(snapshot: FirebaseScheduleSnapshot): Boolean =
        store.write(ScheduleKey, json.encodeToString(CacheEnvelope(FirebaseCacheSchemaVersion, snapshot)))

    fun saveOnCall(snapshot: FirebaseOnCallSnapshot): Boolean =
        store.write(OnCallKey, json.encodeToString(OnCallCacheEnvelope(FirebaseCacheSchemaVersion, snapshot)))

    fun clearSchedule() = store.remove(ScheduleKey)
    fun clearOnCall() = store.remove(OnCallKey)

    private inline fun <reified T> decode(key: String): T? {
        val raw = store.read(key) ?: return null
        return runCatching {
            when (T::class) {
                FirebaseScheduleSnapshot::class -> json.decodeFromString<CacheEnvelope>(raw).takeIf { it.schemaVersion == FirebaseCacheSchemaVersion }?.snapshot
                FirebaseOnCallSnapshot::class -> json.decodeFromString<OnCallCacheEnvelope>(raw).takeIf { it.schemaVersion == FirebaseCacheSchemaVersion }?.snapshot
                else -> null
            } as? T
        }.getOrNull()
    }

    @kotlinx.serialization.Serializable
    private data class CacheEnvelope(val schemaVersion: Int, val snapshot: FirebaseScheduleSnapshot)

    @kotlinx.serialization.Serializable
    private data class OnCallCacheEnvelope(val schemaVersion: Int, val snapshot: FirebaseOnCallSnapshot)

    private companion object {
        const val ScheduleKey = "escalaici.firebase.schedule.cache.v1"
        const val OnCallKey = "escalaici.firebase.oncall.cache.v1"
    }
}
