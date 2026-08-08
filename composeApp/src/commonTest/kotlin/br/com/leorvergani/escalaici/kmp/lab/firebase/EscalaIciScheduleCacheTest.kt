package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TotaisRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.UsuarioRemoteDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class InMemoryKeyValueStore : RawKeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun read(key: String): String? = values[key]
    override fun write(key: String, value: String): Boolean { values[key] = value; return true }
    override fun remove(key: String) { values.remove(key) }
}

/** Isolamento por login (prompt FASE 15 secao 19/43) - cache de um usuario nunca aparece para outro. */
class EscalaIciScheduleCacheTest {

    private fun snapshot(login: String, schemaVersion: Int = ScheduleCacheSchemaVersion) = CachedEscalaSnapshot(
        schemaVersion = schemaVersion,
        login = login,
        equipeId = "EQ_SOC",
        competencia = "2026-08",
        periodoInicio = "2026-07-26",
        periodoFim = "2026-08-25",
        atualizadoEmRemoto = null,
        sincronizadoEm = "2026-08-08",
        turnosMes = TurnosMesRemoteDto(
            schemaVersion = 1, usuarioUid = login, login = login, equipeId = "EQ_SOC",
            competencia = "2026-08", periodoInicio = "2026-07-26", periodoFim = "2026-08-25",
            turnoPadrao = "M", status = TurnosMesStatus.PUBLICADA, dias = emptyMap(), totais = TotaisRemoteDto(),
        ),
        catalogo = emptyMap(),
        usuario = UsuarioRemoteDto(login = login, nome = login, email = "$login@empresa.com", cargo = "", equipeId = "EQ_SOC", nivelHierarquico = 6, turnoPadrao = "M", ativo = true),
    )

    @Test
    fun save_thenLoad_returnsTheSameSnapshot() {
        val cache = EscalaIciScheduleCache(InMemoryKeyValueStore())
        cache.save(snapshot("ana.silva"))
        assertEquals("ana.silva", cache.load("ana.silva")?.login)
    }

    @Test
    fun load_neverReturnsAnotherUsersCache() {
        val cache = EscalaIciScheduleCache(InMemoryKeyValueStore())
        cache.save(snapshot("ana.silva"))
        assertNull(cache.load("carlos.souza"))
    }

    @Test
    fun clear_removesOnlyThatUsersEntry() {
        val store = InMemoryKeyValueStore()
        val cache = EscalaIciScheduleCache(store)
        cache.save(snapshot("ana.silva"))
        cache.save(snapshot("carlos.souza"))
        cache.clear("ana.silva")
        assertNull(cache.load("ana.silva"))
        assertEquals("carlos.souza", cache.load("carlos.souza")?.login)
    }

    @Test
    fun load_discardsSnapshot_withOutdatedSchemaVersion() {
        val cache = EscalaIciScheduleCache(InMemoryKeyValueStore())
        cache.save(snapshot("ana.silva", schemaVersion = 1))
        assertNull(cache.load("ana.silva"))
    }

    @Test
    fun load_returnsNull_whenNothingCached() {
        val cache = EscalaIciScheduleCache(InMemoryKeyValueStore())
        assertNull(cache.load("marina.lima"))
    }
}
