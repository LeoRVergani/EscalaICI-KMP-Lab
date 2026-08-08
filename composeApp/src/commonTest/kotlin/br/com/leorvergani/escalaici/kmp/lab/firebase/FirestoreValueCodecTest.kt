package br.com.leorvergani.escalaici.kmp.lab.firebase

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Encoder de escrita (`:commit`, FASE 16) - cada `encodeX` deve ser lido de volta pelo decoder de leitura (`fieldsOf`/`string`/`bool`/`int`/`arrayOfMaps`) já existente desde a FASE 15. */
class FirestoreValueCodecTest {

    private fun asFields(entries: Map<String, kotlinx.serialization.json.JsonElement>): JsonObject = JsonObject(entries)

    @Test
    fun encodeString_roundTripsThroughDecodeString() {
        val fields = asFields(mapOf("nome" to FirestoreValueCodec.encodeString("Ana Silva")))
        assertEquals("Ana Silva", FirestoreValueCodec.string(fields, "nome"))
    }

    @Test
    fun encodeNull_roundTripsAsNullString() {
        val fields = asFields(mapOf("motivoRecusa" to FirestoreValueCodec.encodeNull()))
        assertNull(FirestoreValueCodec.string(fields, "motivoRecusa"))
    }

    @Test
    fun encodeBool_roundTripsThroughDecodeBool() {
        val fields = asFields(mapOf("ativo" to FirestoreValueCodec.encodeBool(true)))
        assertEquals(true, FirestoreValueCodec.bool(fields, "ativo"))
    }

    @Test
    fun encodeInt_roundTripsThroughDecodeInt() {
        val fields = asFields(mapOf("nivelHierarquico" to FirestoreValueCodec.encodeInt(6)))
        assertEquals(6, FirestoreValueCodec.int(fields, "nivelHierarquico"))
    }

    @Test
    fun encodeArrayOfMaps_roundTripsThroughDecodeArrayOfMaps() {
        val historico = listOf(
            mapOf("tipo" to FirestoreValueCodec.encodeString("SOLICITACAO_CRIADA"), "em" to FirestoreValueCodec.encodeString("2026-08-20T10:00:00.000Z")),
            mapOf("tipo" to FirestoreValueCodec.encodeString("ACEITE_DESTINATARIO"), "em" to FirestoreValueCodec.encodeString("2026-08-20T11:00:00.000Z")),
        )
        val fields = asFields(mapOf("historico" to FirestoreValueCodec.encodeArrayOfMaps(historico)))
        val decoded = FirestoreValueCodec.arrayOfMaps(fields, "historico")
        assertEquals(2, decoded.size)
        assertEquals("SOLICITACAO_CRIADA", FirestoreValueCodec.string(decoded[0], "tipo"))
        assertEquals("ACEITE_DESTINATARIO", FirestoreValueCodec.string(decoded[1], "tipo"))
    }

    @Test
    fun buildFields_composesMultipleTypesIntoOneDocument() {
        val fields = FirestoreValueCodec.buildFields {
            string("trocaId", "abc-123")
            bool("ativo", true)
            int("nivelHierarquico", 6)
            stringOrNull("motivoRecusa", null)
            arrayOfMaps("historico", listOf(mapOf("tipo" to FirestoreValueCodec.encodeString("SOLICITACAO_CRIADA"))))
        }
        val document = JsonObject(fields)
        assertEquals("abc-123", FirestoreValueCodec.string(document, "trocaId"))
        assertEquals(true, FirestoreValueCodec.bool(document, "ativo"))
        assertEquals(6, FirestoreValueCodec.int(document, "nivelHierarquico"))
        assertNull(FirestoreValueCodec.string(document, "motivoRecusa"))
        assertTrue(FirestoreValueCodec.arrayOfMaps(document, "historico").isNotEmpty())
    }

    @Test
    fun encodeMap_roundTripsThroughDecodeMap() {
        val snapshotFields = FirestoreValueCodec.buildFields { string("turnoSolicitanteOriginal", "M") }
        val fields = asFields(mapOf("snapshotValidacao" to FirestoreValueCodec.encodeMap(snapshotFields)))
        val decoded = FirestoreValueCodec.map(fields, "snapshotValidacao")
        assertEquals("M", FirestoreValueCodec.string(decoded, "turnoSolicitanteOriginal"))
    }
}
