package br.com.leorvergani.escalaici.source

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirestoreRestGateway(
    internal val projectId: String = "escalaici",
    private val client: HttpClient = HttpClient(CIO),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val authTokenProvider: FirebaseAuthTokenProvider? = null
) : FirebaseScheduleGateway, DemoPublicationGateway {
    override suspend fun loadTeam(teamId: String): FirebaseTeamDto? =
        documents("teams").mapNotNull(::team).firstOrNull { it.teamId == teamId && it.active }

    override suspend fun loadActiveSchedulePeriod(teamId: String): FirebaseSchedulePeriodDto? =
        documents("schedule_periods").mapNotNull(::schedulePeriod)
            .filter { it.teamId == teamId && it.active }
            .maxByOrNull { it.updatedAt }

    override suspend fun loadScheduleAssignments(teamId: String, periodId: String): List<FirebaseScheduleAssignmentDto> =
        documents("schedule_assignments").mapNotNull(::scheduleAssignment)
            .filter { it.teamId == teamId && it.periodId == periodId }

    override suspend fun loadMembers(teamId: String): List<FirebaseMemberDto> =
        documents("members").mapNotNull(::member).filter { it.teamId == teamId && it.active }

    override suspend fun loadActiveOnCallPeriod(teamId: String): FirebaseOnCallPeriodDto? =
        documents("oncall_periods").mapNotNull(::onCallPeriod)
            .filter { it.teamId == teamId && it.active }
            .maxByOrNull { it.updatedAt }

    override suspend fun loadOnCallAssignments(teamId: String, periodId: String): List<FirebaseOnCallAssignmentDto> =
        documents("oncall_assignments").mapNotNull(::onCallAssignment)
            .filter { it.teamId == teamId && it.periodId == periodId && it.active }

    override suspend fun checkRemoteUpdatedAt(teamId: String, onCall: Boolean): String? =
        if (onCall) loadActiveOnCallPeriod(teamId)?.updatedAt else loadActiveSchedulePeriod(teamId)?.updatedAt

    override suspend fun loadDocumentFields(path: String): JsonObject =
        fields(documentAtPath(path))

    override suspend fun loadCollectionDocuments(path: String): List<JsonObject> =
        documentsAtPath(path).map(::fields)

    private suspend fun documents(collection: String): List<JsonObject> {
        return documentsAtPath(collection)
    }

    private suspend fun documentAtPath(path: String): JsonObject {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$path"
        val response = getWithOptionalAuth(url)
        if (!response.status.isSuccess()) error(firestoreUnavailableMessage(path, response))
        return json.parseToJsonElement(response.body<String>()).jsonObject
    }

    private suspend fun documentsAtPath(path: String): List<JsonObject> {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$path?pageSize=1000"
        val response = getWithOptionalAuth(url)
        if (!response.status.isSuccess()) error(firestoreUnavailableMessage(path, response))
        val root = json.parseToJsonElement(response.body<String>()).jsonObject
        if (root["nextPageToken"]?.jsonPrimitive?.content?.isNotBlank() == true) {
            error("A coleção excede o limite seguro de leitura desta versão.")
        }
        return root["documents"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
    }

    private suspend fun getWithOptionalAuth(url: String): HttpResponse {
        val first = authenticatedGet(url)
        if (first.status != HttpStatusCode.Unauthorized || authTokenProvider == null) return first
        authTokenProvider.invalidate()
        return authenticatedGet(url)
    }

    private suspend fun authenticatedGet(url: String): HttpResponse {
        val token = when (val result = authTokenProvider?.idToken()) {
            null -> null
            is FirebaseAuthTokenResult.Success -> result.idToken
            is FirebaseAuthTokenResult.Failure -> error(result.message)
        }
        return client.get(url) {
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    private suspend fun firestoreUnavailableMessage(path: String, response: HttpResponse): String {
        val body = runCatching { response.body<String>() }.getOrDefault("")
        val detail = body.take(500).replace('\n', ' ')
        return "Firestore indisponível (${response.status.value}) em $path. $detail"
    }

    private fun fields(document: JsonObject): JsonObject = document["fields"]?.jsonObject ?: JsonObject(emptyMap())
    private fun string(fields: JsonObject, name: String): String? = value(fields[name], "stringValue") ?: value(fields[name], "timestampValue")
    private fun bool(fields: JsonObject, name: String, default: Boolean = false): Boolean =
        fields[name]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.booleanOrNull ?: default
    private fun value(element: JsonElement?, key: String): String? = element?.jsonObject?.get(key)?.jsonPrimitive?.content

    private fun team(document: JsonObject): FirebaseTeamDto? = fields(document).let { f ->
        FirebaseTeamDto(string(f, "teamId") ?: return null, string(f, "teamName") ?: return null, bool(f, "active"))
    }

    private fun member(document: JsonObject): FirebaseMemberDto? = fields(document).let { f ->
        FirebaseMemberDto(
            memberId = string(f, "memberId") ?: return null,
            teamId = string(f, "teamId") ?: return null,
            displayName = string(f, "displayName") ?: return null,
            scaleName = string(f, "scaleName") ?: return null,
            title = string(f, "title"),
            role = string(f, "role"),
            active = bool(f, "active")
        )
    }

    private fun schedulePeriod(document: JsonObject): FirebaseSchedulePeriodDto? = fields(document).let { f ->
        FirebaseSchedulePeriodDto(
            string(f, "periodId") ?: return null, string(f, "teamId") ?: return null,
            string(f, "name") ?: return null, string(f, "startDate") ?: return null,
            string(f, "endDate") ?: return null, bool(f, "active"), string(f, "updatedAt") ?: return null
        )
    }

    private fun scheduleAssignment(document: JsonObject): FirebaseScheduleAssignmentDto? = fields(document).let { f ->
        FirebaseScheduleAssignmentDto(
            string(f, "assignmentId") ?: return null, string(f, "teamId") ?: return null,
            string(f, "periodId") ?: return null, string(f, "memberId"),
            string(f, "scaleName") ?: return null, string(f, "date") ?: return null,
            string(f, "assignmentType") ?: return null, string(f, "shiftName"),
            string(f, "startDateTime"), string(f, "endDateTime"), string(f, "note")
        )
    }

    private fun onCallPeriod(document: JsonObject): FirebaseOnCallPeriodDto? = fields(document).let { f ->
        FirebaseOnCallPeriodDto(
            string(f, "periodId") ?: return null, string(f, "teamId") ?: return null,
            string(f, "name") ?: return null, string(f, "startDate") ?: return null,
            string(f, "endDate") ?: return null, bool(f, "active"), string(f, "updatedAt") ?: return null
        )
    }

    private fun onCallAssignment(document: JsonObject): FirebaseOnCallAssignmentDto? = fields(document).let { f ->
        FirebaseOnCallAssignmentDto(
            string(f, "onCallId") ?: return null, string(f, "teamId") ?: return null,
            string(f, "periodId"), string(f, "memberId"), string(f, "scaleName") ?: return null,
            string(f, "startDateTime") ?: return null, string(f, "endDateTime") ?: return null,
            string(f, "label") ?: return null, bool(f, "active"), string(f, "notes")
        )
    }
}
