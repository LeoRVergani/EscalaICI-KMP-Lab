package br.com.leorvergani.escalaici.kmp.lab.firebase

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

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

    /** Le um `arrayValue` de `mapValue`s (ex.: `historico` de uma troca) como uma lista de `JsonObject` de campos, no mesmo formato de [fieldsOf]. */
    fun arrayOfMaps(fields: JsonObject, name: String): List<JsonObject> =
        fields[name]?.jsonObject?.get("arrayValue")?.jsonObject?.get("values")?.let { values ->
            (values as? kotlinx.serialization.json.JsonArray)?.mapNotNull {
                it.jsonObject["mapValue"]?.jsonObject?.get("fields")?.jsonObject
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

    // --- codificacao de valores para o corpo de `:commit` (escrita) - FASE 16, Trocas ---

    fun encodeString(value: String): JsonElement = buildJsonObject { put("stringValue", value) }
    fun encodeNull(): JsonElement = buildJsonObject { put("nullValue", JsonNull) }
    fun encodeBool(value: Boolean): JsonElement = buildJsonObject { put("booleanValue", value) }

    /** `integerValue` na API REST do Firestore e uma string decimal, nao um numero JSON. */
    fun encodeInt(value: Int): JsonElement = buildJsonObject { put("integerValue", value.toString()) }

    /** Um `mapValue` cujos campos internos ja estao no formato `{campo: {tipoValue: ...}}` (ex.: uma entrada de `historico`). */
    fun encodeMap(fields: Map<String, JsonElement>): JsonElement = buildJsonObject {
        putJsonObject("mapValue") {
            putJsonObject("fields") { fields.forEach { (name, value) -> put(name, value) } }
        }
    }

    /** Um `arrayValue` de `mapValue`s (ex.: `historico` inteiro) - cada item e um conjunto de campos ja codificados. */
    fun encodeArrayOfMaps(items: List<Map<String, JsonElement>>): JsonElement = buildJsonObject {
        putJsonObject("arrayValue") {
            putJsonArray("values") { items.forEach { item -> add(encodeMap(item)) } }
        }
    }

    /**
     * Builder para o mapa de campos de um documento/escrita (`{fields: {...}}`) - espelha em escrita
     * o mesmo estilo de leitura de [fieldsOf]/[string]/[bool]/[int] acima.
     */
    fun buildFields(builder: FirestoreFieldsBuilder.() -> Unit): Map<String, JsonElement> {
        val fieldsBuilder = FirestoreFieldsBuilder()
        fieldsBuilder.builder()
        return fieldsBuilder.fields
    }

    class FirestoreFieldsBuilder internal constructor() {
        internal val fields = linkedMapOf<String, JsonElement>()

        fun string(name: String, value: String) {
            fields[name] = encodeString(value)
        }

        fun stringOrNull(name: String, value: String?) {
            fields[name] = if (value == null) encodeNull() else encodeString(value)
        }

        fun bool(name: String, value: Boolean) {
            fields[name] = encodeBool(value)
        }

        fun int(name: String, value: Int) {
            fields[name] = encodeInt(value)
        }

        fun arrayOfMaps(name: String, items: List<Map<String, JsonElement>>) {
            fields[name] = encodeArrayOfMaps(items)
        }

        fun map(name: String, value: Map<String, JsonElement>) {
            fields[name] = encodeMap(value)
        }
    }
}
