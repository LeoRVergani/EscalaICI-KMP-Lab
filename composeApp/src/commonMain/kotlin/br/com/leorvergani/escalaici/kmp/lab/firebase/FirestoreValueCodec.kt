package br.com.leorvergani.escalaici.kmp.lab.firebase

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Helpers de leitura/escrita do formato de valores da API REST do
 * Firestore (`{ "fields": { "campo": { "stringValue": "..." } } }`). Estilo
 * deliberadamente igual ao antigo `FirestoreRestGateway.kt` (removido nesta
 * fase) - manual, sem `ContentNegotiation`, so o necessario para os campos
 * do contrato `TurnosMes`/`Usuario`/`TipoTurno`.
 */
object FirestoreValueCodec {
    fun fieldsOf(document: JsonObject): JsonObject = document["fields"]?.jsonObject ?: JsonObject(emptyMap())

    fun string(fields: JsonObject, name: String): String? =
        fields[name]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content

    fun bool(fields: JsonObject, name: String, default: Boolean = false): Boolean =
        fields[name]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.booleanOrNull ?: default

    fun boolOrNull(fields: JsonObject, name: String): Boolean? =
        fields[name]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.booleanOrNull

    fun int(fields: JsonObject, name: String, default: Int = 0): Int =
        fields[name]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toIntOrNull() ?: default

    fun intOrNull(fields: JsonObject, name: String): Int? =
        fields[name]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toIntOrNull()

    fun stringList(fields: JsonObject, name: String): List<String> =
        fields[name]?.jsonObject?.get("arrayValue")?.jsonObject?.get("values")?.let { values ->
            (values as? kotlinx.serialization.json.JsonArray)?.mapNotNull {
                it.jsonObject["stringValue"]?.jsonPrimitive?.content
            }
        } ?: emptyList()

    /** Le um `mapValue` (ex.: `dias`, `totais`) como um `JsonObject` de campos internos, no mesmo formato de [fieldsOf]. */
    fun map(fields: JsonObject, name: String): JsonObject =
        fields[name]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject ?: JsonObject(emptyMap())

    /** Itera as entradas de um `mapValue` cujas chaves sao dinamicas (ex.: `dias` - uma entrada por data). */
    fun mapEntries(fields: JsonObject, name: String): Map<String, JsonObject> =
        map(fields, name).mapValues { (_, value) -> value.jsonObject["mapValue"]?.jsonObject?.get("fields")?.jsonObject ?: JsonObject(emptyMap()) }

    // --- codificacao de valores para o corpo de uma structured query (so equality/limit) ---

    fun stringFilterValue(value: String): JsonElement = buildJsonObject { put("stringValue", value) }
    fun boolFilterValue(value: Boolean): JsonElement = buildJsonObject { put("booleanValue", value) }
}
