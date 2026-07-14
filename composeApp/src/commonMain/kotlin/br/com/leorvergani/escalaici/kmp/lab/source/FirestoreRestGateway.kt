package br.com.leorvergani.escalaici.kmp.lab.source

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirestoreRestGateway(
    private val projectId: String = "escalaici",
    private val client: HttpClient = HttpClient(CIO),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : FirebaseScheduleGateway {
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

    private suspend fun documents(collection: String): List<JsonObject> {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection?pageSize=1000"
        val response: HttpResponse = client.get(url)
        if (!response.status.isSuccess()) error("Firestore indisponível (${response.status.value}).")
        val root = json.parseToJsonElement(response.body<String>()).jsonObject
        if (root["nextPageToken"]?.jsonPrimitive?.content?.isNotBlank() == true) {
            error("A coleção excede o limite seguro de leitura desta versão.")
        }
        return root["documents"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
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
