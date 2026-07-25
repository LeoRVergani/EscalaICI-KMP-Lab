package br.com.leorvergani.escalaici.source

import br.com.leorvergani.escalaici.auth.CorporateIdentity
import br.com.leorvergani.escalaici.identity.DemoDataOrigin
import br.com.leorvergani.escalaici.identity.DefaultOrganizationIdentityResolver
import br.com.leorvergani.escalaici.identity.DemoFixtureMember
import br.com.leorvergani.escalaici.identity.DemoFixtureMembership
import br.com.leorvergani.escalaici.identity.DemoFixturePackage
import br.com.leorvergani.escalaici.identity.DemoFixtureScheduleAssignment
import br.com.leorvergani.escalaici.identity.DemoFixtureSchedulePeriod
import br.com.leorvergani.escalaici.identity.DemoFixtureTeam
import br.com.leorvergani.escalaici.identity.DemoFixtureWorkspace
import br.com.leorvergani.escalaici.identity.DemoPersona
import br.com.leorvergani.escalaici.identity.DemoPersonaCatalog
import br.com.leorvergani.escalaici.identity.DemoPublicationRepository
import br.com.leorvergani.escalaici.identity.InMemoryMemberDirectoryRepository
import br.com.leorvergani.escalaici.identity.InMemoryMemberRepository
import br.com.leorvergani.escalaici.identity.InMemoryMembershipRepository
import br.com.leorvergani.escalaici.identity.InMemoryTeamRepository
import br.com.leorvergani.escalaici.identity.OrganizationResolutionResult
import br.com.leorvergani.escalaici.identity.OrganizationWorkspace
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoMemberDirectoryRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoMemberRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoMembershipRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoTeamRepository
import br.com.leorvergani.escalaici.identity.isDemoAuthorizedForIdentity
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.ShiftType
import br.com.leorvergani.escalaici.platform.TodayProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DemoPublicationResolverTest {
    @Test
    fun activeRevisionLoadsConsistentSnapshotFromRevisionCollections() = runTest {
        val gateway = FakeDemoGateway(revisions = mapOf(7 to revisionCollections(7)))

        val result = assertIs<DemoPublicationLoadResult.Success>(DemoPublicationResolver(gateway).loadActiveSnapshot())

        assertEquals(7, result.snapshot.pointer.activeRevision)
        assertEquals("team-demo-soc", result.snapshot.teams.single().teamId)
        assertEquals("member-demo-1", result.snapshot.members.single().id)
        assertEquals(7, result.snapshot.memberships.single().publicationRevision)
    }

    @Test
    fun parsesCorporateLoginWrittenByDashboardOfficialPublication() = runTest {
        // Auditoria de contrato FASE 14f-3: o Dashboard grava corporateLogin em todo membro
        // publicado (DemoMemberDto.corporateLogin), mas essa leitura nunca extraia o campo -
        // a resolucao de identidade por login sempre caia para comparar contra scaleName.
        val collections = revisionCollections(7).toMutableMap()
        collections[DemoPublicationCollections.MEMBERS] = listOf(
            member(7).let { doc ->
                buildJsonObject {
                    doc.forEach { (key, value) -> put(key, value) }
                    putField("corporateLogin", "pessoa.demo")
                }
            }
        )

        val result = assertIs<DemoPublicationLoadResult.Success>(
            DemoPublicationResolver(FakeDemoGateway(revisions = mapOf(7 to collections))).loadActiveSnapshot()
        )

        assertEquals("pessoa.demo", result.snapshot.members.single().corporateLogin)
    }

    @Test
    fun buildsOnlyWorkspaceRevisionPathsForDemoPublication() = runTest {
        val gateway = FakeDemoGateway(revisions = mapOf(7 to revisionCollections(7)))

        assertIs<DemoPublicationLoadResult.Success>(DemoPublicationResolver(gateway).loadActiveSnapshot())

        assertEquals("workspaces/demo-v1", gateway.documentPaths.first())
        assertEquals("workspaces/demo-v1", gateway.documentPaths.last())
        DemoPublicationCollections.all.forEach { collection ->
            assertTrue("workspaces/demo-v1/revisions/7/$collection" in gateway.collectionPaths)
        }
        val rootCollections = setOf("teams", "members", "member_team_memberships", "team_manager_assignments", "schedule_periods", "schedule_assignments", "schedule_change_requests")
        assertFalse(gateway.collectionPaths.any { it in rootCollections })
    }

    @Test
    fun buildsOnlyWorkspaceRevisionPathsForCorporatePublication() = runTest {
        val gateway = FakeDemoGateway(
            pointerWorkspaceId = "ici-dev",
            revisions = mapOf(7 to revisionCollections(7, workspaceId = "ici-dev", teamId = "team-corp"))
        )

        assertIs<DemoPublicationLoadResult.Success>(
            DemoPublicationResolver(gateway, workspaceId = "ici-dev").loadActiveSnapshot()
        )

        assertEquals("workspaces/ici-dev", gateway.documentPaths.first())
        assertEquals("workspaces/ici-dev", gateway.documentPaths.last())
        DemoPublicationCollections.all.forEach { collection ->
            assertTrue("workspaces/ici-dev/revisions/7/$collection" in gateway.collectionPaths)
        }
        assertFalse(gateway.collectionPaths.any { it == "teams" || it == "members" })
    }

    @Test
    fun workspaceMismatchRejectsWholeSnapshot() = runTest {
        val collections = revisionCollections(7).toMutableMap()
        collections[DemoPublicationCollections.MEMBERS] = listOf(member(7, workspaceId = "wrong-workspace"))

        val result = assertIs<DemoPublicationLoadResult.Failure>(
            DemoPublicationResolver(FakeDemoGateway(revisions = mapOf(7 to collections))).loadActiveSnapshot()
        )

        assertEquals(ScheduleSyncCause.INVALID_REMOTE_DATA, result.cause)
    }

    @Test
    fun publicationRevisionMismatchRejectsWholeSnapshot() = runTest {
        val collections = revisionCollections(7).toMutableMap()
        collections[DemoPublicationCollections.ASSIGNMENTS] = listOf(assignment(6))

        val result = assertIs<DemoPublicationLoadResult.Failure>(
            DemoPublicationResolver(FakeDemoGateway(revisions = mapOf(7 to collections))).loadActiveSnapshot()
        )

        assertEquals(ScheduleSyncCause.INVALID_REMOTE_DATA, result.cause)
    }

    @Test
    fun membershipResolvesMemberAndTeamWithoutMemberTeamId() = runTest {
        val repository = DemoPublicationRepository(
            resolver = DemoPublicationResolver(FakeDemoGateway(revisions = mapOf(7 to revisionCollections(7)))),
            fixtureProvider = { emptyFixture() }
        )

        val member = RemoteFirstDemoMemberRepository(repository).getMembersByTeam("team-demo-soc").single()
        val memberships = RemoteFirstDemoMembershipRepository(repository).getMemberships("member-demo-1")

        assertEquals("member-demo-1", member.id)
        assertEquals("", member.teamId)
        assertEquals("team-demo-soc", memberships.single().teamId)
    }

    @Test
    fun mapsCommercialRemoteWorkShiftAsWorkShiftIgnoringCase() {
        listOf("Comercial", "comercial", "COMERCIAL").forEach { shiftName ->
            val assignment = assignment(revision = 7, shiftName = shiftName).toDemoScheduleAssignment(revision = 7)

            assertEquals(ShiftType.COMERCIAL, assignment.shiftType)
            assertTrue(assignment.shiftType.isWorkShift)
        }
    }

    @Test
    fun keepsUnknownRemoteWorkShiftUndefined() {
        val assignment = assignment(revision = 7, shiftName = "turno-inexistente-xyz").toDemoScheduleAssignment(revision = 7)

        assertEquals(ShiftType.INDEFINIDO, assignment.shiftType)
    }

    @Test
    fun mapsOfficialAssignmentGranularityFromDashboardPublication() {
        val cases = listOf(
            Triple("OFF", "BH", ShiftType.BH),
            Triple("OFF", "Aniversário", ShiftType.ANIVERSARIO),
            Triple("OFF", null, ShiftType.FOLGA),
            Triple("OTHER", "Sem dado importado", ShiftType.INDEFINIDO),
            Triple("OTHER", "Trabalho sem turno localizado (1)", ShiftType.INCONSISTENCIA),
            Triple("ABSENCE", null, ShiftType.AFASTAMENTO),
            Triple("TRAINING", null, ShiftType.INDEFINIDO)
        )

        cases.forEach { (assignmentType, shiftName, expectedType) ->
            val assignment = assignment(
                revision = 7,
                assignmentType = assignmentType,
                shiftName = shiftName
            ).toDemoScheduleAssignment(revision = 7)

            assertEquals(expectedType, assignment.shiftType)
        }
    }

    @Test
    fun nonActivePointerStatusIsRejectedDefensively() = runTest {
        val result = assertIs<DemoPublicationLoadResult.Failure>(
            DemoPublicationResolver(
                FakeDemoGateway(pointerStatus = "PREPARING", revisions = mapOf(7 to revisionCollections(7)))
            ).loadActiveSnapshot()
        )

        assertEquals(ScheduleSyncCause.INVALID_REMOTE_DATA, result.cause)
    }

    @Test
    fun pointerChangeRetriesOnceAndPublishesSecondRevision() = runTest {
        val gateway = FakeDemoGateway(
            pointerRevisions = mutableListOf(7, 8, 8, 8),
            revisions = mapOf(7 to revisionCollections(7), 8 to revisionCollections(8, teamId = "team-demo-v8"))
        )

        val result = assertIs<DemoPublicationLoadResult.Success>(DemoPublicationResolver(gateway).loadActiveSnapshot())

        assertEquals(8, result.snapshot.pointer.activeRevision)
        assertEquals("team-demo-v8", result.snapshot.teams.single().teamId)
    }

    @Test
    fun secondPointerChangeReturnsControlledError() = runTest {
        val gateway = FakeDemoGateway(
            pointerRevisions = mutableListOf(7, 8, 8, 9),
            revisions = mapOf(7 to revisionCollections(7), 8 to revisionCollections(8))
        )

        val result = assertIs<DemoPublicationLoadResult.Failure>(DemoPublicationResolver(gateway).loadActiveSnapshot())

        assertEquals(ScheduleSyncCause.INVALID_REMOTE_DATA, result.cause)
    }

    @Test
    fun collectionFailureDiscardsRemoteAndFallsBackToFixture() = runTest {
        val repository = DemoPublicationRepository(
            resolver = DemoPublicationResolver(FakeDemoGateway(failCollection = DemoPublicationCollections.MEMBERS)),
            fixtureProvider = { emptyFixture(revision = 3) }
        )

        val data = repository.data()

        assertEquals(DemoDataOrigin.LOCAL_FIXTURE, data.state.origin)
        assertEquals(3, data.state.publicationRevision)
        assertEquals(ScheduleSyncCause.UNKNOWN, data.state.fallbackCause)
    }

    @Test
    fun permissionDeniedFallsBackToFixtureWithPermissionCause() = runTest {
        val repository = DemoPublicationRepository(
            resolver = DemoPublicationResolver(FakeDemoGateway(failDocument = true, failMessage = "403 Forbidden")),
            fixtureProvider = { emptyFixture(revision = 4) }
        )

        val data = repository.data()

        assertEquals(DemoDataOrigin.LOCAL_FIXTURE, data.state.origin)
        assertEquals(4, data.state.publicationRevision)
        assertEquals(ScheduleSyncCause.PERMISSION_DENIED, data.state.fallbackCause)
    }

    @Test
    fun authorizedDemoResolutionUsesLocalFixtureWhenRemotePointerIsUnavailable() = runTest {
        val persona = DemoPersonaCatalog.personas[0]
        val repository = DemoPublicationRepository(
            resolver = DemoPublicationResolver(
                FakeDemoGateway(failDocument = true, failMessage = "404 Not Found")
            ),
            fixtureProvider = { fixtureForPersona(persona, revision = 9) }
        )
        val authorized = isDemoAuthorizedForIdentity(
            identity = CorporateIdentity(
                tenantId = "tenant",
                objectId = "object-not-allowed",
                username = "lvergani",
                displayName = "Leandro Vergani",
                email = "lvergani@ici.tec.br",
                accountId = "account"
            ),
            isDevelopmentBuild = true,
            allowedDeveloperObjectIdsProvider = { error("Firestore pointer unavailable") }
        )

        assertTrue(authorized)
        val result = DefaultOrganizationIdentityResolver(
            corporateMemberDirectoryRepository = InMemoryMemberDirectoryRepository(emptyList()),
            corporateMembershipRepository = InMemoryMembershipRepository(emptyList()),
            corporateMemberRepository = InMemoryMemberRepository(emptyList()),
            corporateTeamRepository = InMemoryTeamRepository(emptyList()),
            demoMemberDirectoryRepository = RemoteFirstDemoMemberDirectoryRepository(repository),
            demoMembershipRepository = RemoteFirstDemoMembershipRepository(repository),
            demoMemberRepository = RemoteFirstDemoMemberRepository(repository),
            demoTeamRepository = RemoteFirstDemoTeamRepository(repository),
            demoDataSourceStateProvider = { repository.state() },
            todayProvider = TodayProvider { LabDate(2026, 7, 18) }
        ).resolveDemoPersona(persona)
        val state = repository.state()

        assertIs<OrganizationResolutionResult.Resolved>(result)
        assertEquals(persona.memberId, result.context.memberId)
        assertEquals("Equipe Demo Fixture", result.context.primaryTeamName)
        assertEquals(DemoDataOrigin.LOCAL_FIXTURE, state.origin)
        assertEquals(9, state.publicationRevision)
        assertEquals(ScheduleSyncCause.UNKNOWN, state.fallbackCause)
    }

    @Test
    fun cancellationDuringLoadPropagatesInsteadOfBecomingAFailure() = runTest {
        // Guarda de regressao da causa raiz real (spec 67, Checkpoint H addendum): uma
        // CancellationException (ex.: LaunchedEffect cancelado por corporateAuthState mudar de
        // novo por causa do refresh silencioso de token) NUNCA pode virar um
        // DemoPublicationLoadResult.Failure "normal" - isso quebraria a concorrencia estruturada
        // e, pior, deixaria uma falha falsa cacheada pra sempre em DemoPublicationRepository.
        val gateway = FakeDemoGateway(failDocument = true, throwCancellation = true)

        assertFailsWith<CancellationException> {
            DemoPublicationResolver(gateway).loadActiveSnapshot()
        }
    }

    @Test
    fun genuineRemoteFailureWithoutFixtureIsNeverCachedForever() = runTest {
        // Guarda de regressao: sem fixtureProvider (caso real do workspace corporativo em
        // MainActivity), uma falha genuina de rede/permissao NAO pode grudar pra sempre no
        // repositorio - a proxima chamada a data() precisa poder tentar de novo, senao o app
        // fica preso mostrando "escala nao publicada" pelo resto da sessao mesmo depois da
        // causa real (rede, permissao, etc.) se resolver sozinha.
        val gateway = FakeDemoGateway(failDocumentCallsRemaining = 1, revisions = mapOf(7 to revisionCollections(7)))
        val repository = DemoPublicationRepository(
            resolver = DemoPublicationResolver(gateway),
            fixtureProvider = null
        )

        val firstAttempt = repository.data()
        assertEquals(DemoDataOrigin.REMOTE_UNAVAILABLE, firstAttempt.state.origin)

        val secondAttempt = repository.data()
        assertEquals(DemoDataOrigin.REMOTE_PUBLICATION, secondAttempt.state.origin)
        assertEquals("member-demo-1", secondAttempt.members.single().id)
    }

    private fun revisionCollections(
        revision: Int,
        teamId: String = "team-demo-soc",
        workspaceId: String = "demo-v1"
    ): Map<String, List<JsonObject>> = mapOf(
        DemoPublicationCollections.TEAMS to listOf(team(revision, teamId, workspaceId)),
        DemoPublicationCollections.MEMBERS to listOf(member(revision, workspaceId)),
        DemoPublicationCollections.MEMBERSHIPS to listOf(membership(revision, teamId, workspaceId)),
        DemoPublicationCollections.MANAGERS to listOf(manager(revision, teamId, workspaceId)),
        DemoPublicationCollections.PERIODS to listOf(period(revision, teamId, workspaceId)),
        DemoPublicationCollections.ASSIGNMENTS to listOf(assignment(revision, teamId, workspaceId)),
        DemoPublicationCollections.REQUESTS to listOf(request(revision, teamId, workspaceId))
    )

    private fun emptyFixture(revision: Int = 1) = DemoFixturePackage(
        schemaVersion = 1,
        workspace = DemoFixtureWorkspace("demo-v1", "DEMO", "test", 1, revision, false, false),
        teams = emptyList(),
        members = emptyList(),
        memberTeamMemberships = emptyList(),
        teamManagerAssignments = emptyList(),
        scheduleChangeRequests = emptyList(),
        schedulePeriods = emptyList(),
        scheduleAssignments = emptyList(),
        publicationRecords = emptyList()
    )

    private fun fixtureForPersona(persona: DemoPersona, revision: Int) = DemoFixturePackage(
        schemaVersion = 1,
        workspace = DemoFixtureWorkspace(
            workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
            workspaceType = "DEMO",
            scenarioId = "fallback-test",
            seedVersion = 1,
            publicationRevision = revision,
            externalEffectsAllowed = false,
            notificationsEnabled = false
        ),
        teams = listOf(
            DemoFixtureTeam(
                id = "team-demo-fixture",
                workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                name = "Equipe Demo Fixture",
                acronym = "FIX",
                active = true,
                schemaVersion = 1
            )
        ),
        members = listOf(
            DemoFixtureMember(
                id = persona.memberId,
                workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                displayName = persona.displayName,
                corporateLogin = persona.fictitiousLogin,
                emailNormalized = persona.fictitiousEmail,
                active = true,
                schemaVersion = 1
            )
        ),
        memberTeamMemberships = listOf(
            DemoFixtureMembership(
                id = "membership-demo-fixture",
                workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                memberId = persona.memberId,
                teamId = "team-demo-fixture",
                startDate = "2020-01-01",
                endDate = null,
                active = true,
                isPrimary = true,
                schemaVersion = 1
            )
        ),
        teamManagerAssignments = emptyList(),
        scheduleChangeRequests = emptyList(),
        schedulePeriods = listOf(
            DemoFixtureSchedulePeriod(
                id = "period-demo-fixture",
                workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                teamId = "team-demo-fixture",
                name = "Julho 2026",
                startDate = "2026-07-01",
                endDate = "2026-07-31",
                active = true,
                publicationRevision = revision,
                schemaVersion = 1
            )
        ),
        scheduleAssignments = listOf(
            DemoFixtureScheduleAssignment(
                id = "assignment-demo-fixture",
                workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                periodId = "period-demo-fixture",
                teamId = "team-demo-fixture",
                memberId = persona.memberId,
                date = "2026-07-18",
                assignmentType = "WORK_SHIFT",
                shiftName = "manha",
                startTime = null,
                endTime = null,
                schemaVersion = 1
            )
        ),
        publicationRecords = emptyList()
    )
}

private class FakeDemoGateway(
    private val pointerWorkspaceId: String = "demo-v1",
    private val pointerRevisions: MutableList<Int> = mutableListOf(7),
    private val pointerStatus: String = "ACTIVE",
    private val revisions: Map<Int, Map<String, List<JsonObject>>> = mapOf(7 to emptyMap()),
    private val failDocument: Boolean = false,
    private val failDocumentCallsRemaining: Int = 0,
    private val throwCancellation: Boolean = false,
    private val failCollection: String? = null,
    private val failMessage: String = "network"
) : DemoPublicationGateway {
    val documentPaths = mutableListOf<String>()
    val collectionPaths = mutableListOf<String>()
    private var remainingFailures = failDocumentCallsRemaining

    override suspend fun loadDocumentFields(path: String): JsonObject {
        documentPaths += path
        if (failDocument || remainingFailures > 0) {
            remainingFailures -= 1
            if (throwCancellation) throw CancellationException("cancelled")
            error(failMessage)
        }
        val revision = if (pointerRevisions.size > 1) pointerRevisions.removeAt(0) else pointerRevisions.first()
        return workspacePointer(revision, pointerStatus, pointerWorkspaceId)
    }

    override suspend fun loadCollectionDocuments(path: String): List<JsonObject> {
        collectionPaths += path
        val collection = path.substringAfterLast("/")
        if (collection == failCollection) error(failMessage)
        val revision = path.substringAfter("/revisions/").substringBefore("/").toInt()
        return revisions.getValue(revision).getValue(collection)
    }
}

private fun workspacePointer(revision: Int, status: String, workspaceId: String = "demo-v1") = buildJsonObject {
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("status", status)
}

private fun team(revision: Int, id: String, workspaceId: String = "demo-v1") = buildJsonObject {
    putField("id", id)
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("name", "SOC Demo")
}

private fun member(revision: Int, workspaceId: String = "demo-v1") = buildJsonObject {
    putField("id", "member-demo-1")
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("displayName", "Pessoa Demo")
    putField("emailNormalized", "pessoa.demo@example.invalid")
    putBoolField("active", true)
}

private fun membership(revision: Int, teamId: String, workspaceId: String = "demo-v1") = buildJsonObject {
    putField("id", "membership-demo-1")
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("memberId", "member-demo-1")
    putField("teamId", teamId)
    putField("startDate", "2020-01-01")
    putBoolField("active", true)
    putBoolField("isPrimary", true)
}

private fun manager(revision: Int, teamId: String, workspaceId: String = "demo-v1") = buildJsonObject {
    putField("id", "manager-demo-1")
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("managerMemberId", "member-demo-1")
    putField("teamId", teamId)
    putField("role", "PRIMARY_MANAGER")
    putBoolField("active", true)
    putField("validFrom", "2020-01-01")
}

private fun period(revision: Int, teamId: String, workspaceId: String = "demo-v1") = buildJsonObject {
    putField("id", "period-demo-1")
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("teamId", teamId)
    putField("startDate", "2026-07-01")
    putField("endDate", "2026-07-31")
}

private fun assignment(
    revision: Int,
    teamId: String = "team-demo-soc",
    workspaceId: String = "demo-v1",
    assignmentType: String = "WORK_SHIFT",
    shiftName: String? = "manha"
) = buildJsonObject {
    putField("id", "assignment-demo-1")
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("periodId", "period-demo-1")
    putField("teamId", teamId)
    putField("memberId", "member-demo-1")
    putField("date", "2026-07-01")
    putField("assignmentType", assignmentType)
    if (shiftName != null) putField("shiftName", shiftName)
}

private fun request(revision: Int, teamId: String, workspaceId: String = "demo-v1") = buildJsonObject {
    putField("id", "request-demo-1")
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("requesterMemberId", "member-demo-1")
    putField("requesterTeamId", teamId)
    putField("assignedManagerMemberId", "member-demo-1")
    putField("schedulePeriodId", "period-demo-1")
    putField("status", "PENDING")
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putField(name: String, value: String) {
    put(name, buildJsonObject { put("stringValue", value) })
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putField(name: String, value: Int) {
    put(name, buildJsonObject { put("integerValue", value.toString()) })
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putBoolField(name: String, value: Boolean) {
    put(name, buildJsonObject { put("booleanValue", value) })
}
