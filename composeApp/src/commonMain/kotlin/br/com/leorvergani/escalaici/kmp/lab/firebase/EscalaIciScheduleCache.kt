package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.UsuarioRemoteDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Versao do formato de cache - o cache v1 antigo (schema Firebase antigo) e descartado, nao migrado (prompt FASE 15 secao 19). */
const val ScheduleCacheSchemaVersion = 2

@Serializable
data class CachedEscalaSnapshot(
    val schemaVersion: Int = ScheduleCacheSchemaVersion,
    val login: String,
    val equipeId: String,
    val competencia: String,
    val periodoInicio: String,
    val periodoFim: String,
    val atualizadoEmRemoto: String?,
    val sincronizadoEm: String,
    val turnosMes: TurnosMesRemoteDto,
    val catalogo: Map<String, TipoTurnoRemoteDto>,
    val usuario: UsuarioRemoteDto,
)

/**
 * Cache offline por usuario - chave `firebase_schedule_cache_v2:{login}`
 * (prompt FASE 15 secao 19, literal ao contrato real por login). Nunca
 * reaproveitado entre usuarios diferentes: cada login tem sua propria
 * entrada, e o logout limpa exatamente a entrada do usuario que saiu (a
 * proxima pessoa a logar nunca ve cache de outra pessoa).
 */
class EscalaIciScheduleCache(
    private val store: RawKeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun load(login: String): CachedEscalaSnapshot? {
        val raw = store.read(keyFor(login)) ?: return null
        return runCatching { json.decodeFromString<CachedEscalaSnapshot>(raw) }
            .getOrNull()
            ?.takeIf { it.schemaVersion == ScheduleCacheSchemaVersion && it.login == login }
    }

    fun save(snapshot: CachedEscalaSnapshot) {
        store.write(keyFor(snapshot.login), json.encodeToString(snapshot))
    }

    fun clear(login: String) {
        store.remove(keyFor(login))
    }

    private fun keyFor(login: String) = "firebase_schedule_cache_v2:$login"
}
