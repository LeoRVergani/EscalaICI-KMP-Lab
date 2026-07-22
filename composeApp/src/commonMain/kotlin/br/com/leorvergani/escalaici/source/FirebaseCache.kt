package br.com.leorvergani.escalaici.source

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val FirebaseCacheSchemaVersion = 1

class FirebaseSourceCache(
    private val store: FirebaseRawCacheStore,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun loadSchedule(): FirebaseScheduleSnapshot? = decode(ScheduleKey)
    fun loadOnCall(groupId: String? = null): FirebaseOnCallSnapshot? = decode(onCallKey(groupId))

    fun saveSchedule(snapshot: FirebaseScheduleSnapshot): Boolean =
        store.write(ScheduleKey, json.encodeToString(CacheEnvelope(FirebaseCacheSchemaVersion, snapshot)))

    fun saveOnCall(snapshot: FirebaseOnCallSnapshot, groupId: String? = snapshot.period.groupId): Boolean =
        store.write(onCallKey(groupId), json.encodeToString(OnCallCacheEnvelope(FirebaseCacheSchemaVersion, snapshot)))

    fun clearSchedule() = store.remove(ScheduleKey)
    fun clearOnCall(groupId: String? = null) = store.remove(onCallKey(groupId))

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

        fun onCallKey(groupId: String?): String {
            val normalized = groupId
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.mapNotNull { char -> if (char.isLetterOrDigit() || char == '-' || char == '_') char else null }
                ?.joinToString("")
                ?.takeIf { it.isNotEmpty() }
            return if (normalized == null) OnCallKey else "$OnCallKey.$normalized"
        }
    }
}
