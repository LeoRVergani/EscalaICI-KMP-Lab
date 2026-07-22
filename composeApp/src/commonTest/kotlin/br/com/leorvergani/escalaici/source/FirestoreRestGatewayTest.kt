package br.com.leorvergani.escalaici.source

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FirestoreRestGatewayTest {
    @Test
    fun corporateGatewayDefaultProjectRemainsEscalaici() {
        val gateway = FirestoreRestGateway()

        assertEquals("escalaici", gateway.projectId)
    }

    @Test
    fun configuredDemoGatewayUsesDevProjectId() {
        val gateway = assertIs<FirestoreRestGateway>(
            createConfiguredDemoPublicationGateway(
                DemoFirebaseConfig(projectId = "escala-ici-dev")
            )
        )

        assertEquals("escala-ici-dev", gateway.projectId)
    }

    @Test
    fun demoFirestoreReadUsesDevProjectUrlWithoutFirebaseAuthToken() = runTest {
        var requestedUrl = ""
        var authorizationHeader: String? = null
        val gateway = FirestoreRestGateway(
            projectId = "escala-ici-dev",
            client = HttpClient(MockEngine) { engine {
                addHandler { request ->
                    requestedUrl = request.url.toString()
                    authorizationHeader = request.headers[HttpHeaders.Authorization]
                    respond(
                        content = """{"fields":{"workspaceId":{"stringValue":"demo-v1"}}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            } }
        )

        gateway.loadDocumentFields("workspaces/demo-v1")

        assertTrue("/projects/escala-ici-dev/" in requestedUrl)
        assertTrue("/projects/escalaici/" !in requestedUrl)
        assertEquals(null, authorizationHeader)
    }

    @Test
    fun optionalAuthTokenProviderStillAddsBearerHeaderWhenConfigured() = runTest {
        var authorizationHeader: String? = null
        val gateway = FirestoreRestGateway(
            projectId = "escala-ici-dev",
            client = HttpClient(MockEngine) { engine {
                addHandler { request ->
                    authorizationHeader = request.headers[HttpHeaders.Authorization]
                    respond(
                        content = """{"fields":{"workspaceId":{"stringValue":"demo-v1"}}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            } },
            authTokenProvider = StaticTokenProvider("id-token-1")
        )

        gateway.loadDocumentFields("workspaces/demo-v1")

        assertEquals("Bearer id-token-1", authorizationHeader)
    }

    @Test
    fun onCallGroupsAreReadFromFirestoreCollection() = runTest {
        val gateway = FirestoreRestGateway(
            projectId = "escala-ici-dev",
            client = HttpClient(MockEngine) { engine {
                addHandler {
                    respond(
                        content = """
                            {
                              "documents": [
                                {
                                  "fields": {
                                    "groupId": {"stringValue": "cosi"},
                                    "teamId": {"stringValue": "soc"},
                                    "name": {"stringValue": "COSI"},
                                    "active": {"booleanValue": true}
                                  }
                                },
                                {
                                  "fields": {
                                    "groupId": {"stringValue": "noc"},
                                    "teamId": {"stringValue": "noc"},
                                    "name": {"stringValue": "NOC"},
                                    "active": {"booleanValue": true}
                                  }
                                }
                              ]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            } }
        )

        val groups = gateway.loadOnCallGroups("soc")

        assertEquals(1, groups.size)
        assertEquals("cosi", groups.single().id)
        assertEquals("COSI", groups.single().name)
    }

    @Test
    fun missingDemoConfigCreatesUnavailableGatewayForFixtureFallback() = runTest {
        val gateway = createConfiguredDemoPublicationGateway(DemoFirebaseConfig(projectId = ""))
        val repository = br.com.leorvergani.escalaici.identity.DemoPublicationRepository(
            resolver = DemoPublicationResolver(gateway),
            fixtureProvider = {
                br.com.leorvergani.escalaici.identity.DemoFixturePackage(
                    schemaVersion = 1,
                    workspace = br.com.leorvergani.escalaici.identity.DemoFixtureWorkspace(
                        workspaceId = "demo-v1",
                        workspaceType = "DEMO",
                        scenarioId = "test",
                        seedVersion = 1,
                        publicationRevision = 2,
                        externalEffectsAllowed = false,
                        notificationsEnabled = false
                    ),
                    teams = emptyList(),
                    members = emptyList(),
                    memberTeamMemberships = emptyList(),
                    teamManagerAssignments = emptyList(),
                    scheduleChangeRequests = emptyList(),
                    schedulePeriods = emptyList(),
                    scheduleAssignments = emptyList(),
                    publicationRecords = emptyList()
                )
            }
        )

        val data = repository.data()

        assertEquals(br.com.leorvergani.escalaici.identity.DemoDataOrigin.LOCAL_FIXTURE, data.state.origin)
        assertEquals(ScheduleSyncCause.AUTH_REQUIRED, data.state.fallbackCause)
    }
}

private class StaticTokenProvider(private val token: String) : FirebaseAuthTokenProvider {
    override suspend fun idToken(): FirebaseAuthTokenResult = FirebaseAuthTokenResult.Success(token)
    override fun invalidate() = Unit
}
