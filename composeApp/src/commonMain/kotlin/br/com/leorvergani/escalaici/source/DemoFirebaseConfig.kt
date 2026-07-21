package br.com.leorvergani.escalaici.source

data class DemoFirebaseConfig(
    val projectId: String
) {
    val isConfigured: Boolean
        get() = projectId.isNotBlank()
}

expect fun platformDemoFirebaseConfig(): DemoFirebaseConfig

fun createConfiguredDemoPublicationGateway(
    config: DemoFirebaseConfig = platformDemoFirebaseConfig()
): DemoPublicationGateway =
    if (config.isConfigured) {
        FirestoreRestGateway(projectId = config.projectId)
    } else {
        UnconfiguredDemoPublicationGateway
    }

object UnconfiguredDemoPublicationGateway : DemoPublicationGateway {
    override suspend fun loadDocumentFields(path: String) =
        error("Firebase Demo configuration not configured.")

    override suspend fun loadCollectionDocuments(path: String) =
        error("Firebase Demo configuration not configured.")
}
