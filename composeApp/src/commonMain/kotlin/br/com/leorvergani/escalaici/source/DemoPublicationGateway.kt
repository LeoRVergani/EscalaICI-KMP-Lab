package br.com.leorvergani.escalaici.source

import kotlinx.serialization.json.JsonObject

interface DemoPublicationGateway {
    suspend fun loadDocumentFields(path: String): JsonObject
    suspend fun loadCollectionDocuments(path: String): List<JsonObject>
}

expect fun createDemoPublicationGateway(): DemoPublicationGateway
