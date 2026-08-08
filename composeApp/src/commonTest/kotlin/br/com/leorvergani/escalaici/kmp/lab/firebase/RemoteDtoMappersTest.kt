package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.CategoriaTurno
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** Decodifica o formato `{ fields: { campo: { stringValue: ... } } }` da API REST do Firestore - sem rede, so o parsing. */
class RemoteDtoMappersTest {

    private fun stringField(value: String) = buildJsonObject { put("stringValue", value) }
    private fun boolField(value: Boolean) = buildJsonObject { put("booleanValue", value) }
    private fun intField(value: Int) = buildJsonObject { put("integerValue", value.toString()) }

    @Test
    fun usuario_usesDocumentIdAsLogin_notTheLoginField() {
        val document = buildJsonObject {
            put("name", "projects/p/databases/(default)/documents/usuarios/ana.silva")
            putJsonObject("fields") {
                put("login", stringField("valor-desatualizado"))
                put("nome", stringField("Ana Silva"))
                put("email", stringField("ana.silva@empresa.com"))
                put("equipeId", stringField("EQ_SOC"))
                put("ativo", boolField(true))
                put("nivelHierarquico", intField(6))
            }
        }
        val usuario = RemoteDtoMappers.usuario(document)
        assertEquals("ana.silva", usuario?.login)
    }

    @Test
    fun usuario_readsAtivoFalse_ratherThanDefaultingToTrue() {
        val document = buildJsonObject {
            put("name", "projects/p/databases/(default)/documents/usuarios/carlos.souza")
            putJsonObject("fields") {
                put("nome", stringField("Carlos Souza"))
                put("email", stringField("carlos.souza@empresa.com"))
                put("equipeId", stringField("EQ_SOC"))
                put("ativo", boolField(false))
            }
        }
        val usuario = RemoteDtoMappers.usuario(document)
        assertFalse(usuario?.ativo ?: true)
    }

    @Test
    fun usuario_returnsNull_whenRequiredFieldMissing() {
        val document = buildJsonObject {
            put("name", "projects/p/databases/(default)/documents/usuarios/marina.lima")
            putJsonObject("fields") { put("nome", stringField("Marina Lima")) }
        }
        assertNull(RemoteDtoMappers.usuario(document))
    }

    @Test
    fun tipoTurno_decodesCategoriaEnum() {
        val document = buildJsonObject {
            putJsonObject("fields") {
                put("codigo", stringField("M"))
                put("descricao", stringField("Manhã"))
                put("categoria", stringField("TRABALHO"))
                put("duracaoMinutos", intField(360))
                put("viraDia", boolField(false))
                put("contaComoPlantao", boolField(false))
                put("pesoPlantao", intField(0))
                put("corHex", stringField("#FFFF00"))
            }
        }
        val tipo = RemoteDtoMappers.tipoTurno(document)
        assertEquals(CategoriaTurno.TRABALHO, tipo?.categoria)
    }

    @Test
    fun turnosMes_rejectsRascunho_asNeverPublicada() {
        val document = buildJsonObject {
            putJsonObject("fields") {
                put("schemaVersion", intField(1))
                put("usuarioUid", stringField("ana.silva"))
                put("login", stringField("ana.silva"))
                put("equipeId", stringField("EQ_SOC"))
                put("competencia", stringField("2026-08"))
                put("periodoInicio", stringField("2026-07-26"))
                put("periodoFim", stringField("2026-08-25"))
                put("turnoPadrao", stringField("M"))
                put("status", stringField("RASCUNHO"))
            }
        }
        val turnosMes = RemoteDtoMappers.turnosMes(document)
        assertEquals(TurnosMesStatus.RASCUNHO, turnosMes?.status)
    }
}
