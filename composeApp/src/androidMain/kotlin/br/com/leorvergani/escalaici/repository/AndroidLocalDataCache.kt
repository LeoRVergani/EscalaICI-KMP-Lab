package br.com.leorvergani.escalaici.repository

import android.content.Context
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.MemberRole
import br.com.leorvergani.escalaici.model.OnCallAssignment
import br.com.leorvergani.escalaici.model.OnCallStatus
import br.com.leorvergani.escalaici.model.ScheduleSummary
import br.com.leorvergani.escalaici.model.ShiftDay
import br.com.leorvergani.escalaici.model.ShiftType
import br.com.leorvergani.escalaici.model.Team
import br.com.leorvergani.escalaici.model.YearResolutionSource
import org.json.JSONArray
import org.json.JSONObject

class AndroidLocalDataCache(context: Context) : LocalDataCache {
    private val preferences = context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

    override fun loadSchedule(): CacheRead<CachedSchedule> {
        val raw = preferences.getString(ScheduleCacheKey, null) ?: return CacheRead.Missing
        return try {
            val json = JSONObject(raw)
            if (json.optInt("schemaVersion") != ScheduleCacheSchemaVersion) {
                return CacheRead.Invalid("A escala salva usa uma versão incompatível.")
            }
            val periodStart = json.optStringOrNull("periodStart")?.let(LabDate::parseIso)
                ?: return CacheRead.Invalid("O período da escala salva é inválido.")
            val periodEnd = json.optStringOrNull("periodEnd")?.let(LabDate::parseIso)
                ?: return CacheRead.Invalid("O período da escala salva é inválido.")
            if (periodEnd < periodStart) return CacheRead.Invalid("O período da escala salva é inválido.")

            val assignments = json.optJSONArray("assignments") ?: return CacheRead.Invalid("A escala salva não possui assignments válidos.")
            if (assignments.length() <= 0) return CacheRead.Invalid("A escala salva não possui assignments válidos.")
            val days = buildList {
                for (index in 0 until assignments.length()) {
                    val item = assignments.optJSONObject(index) ?: return CacheRead.Invalid("A escala salva possui assignments inválidos.")
                    val date = item.optStringOrNull("date")?.let(LabDate::parseIso)
                        ?: return CacheRead.Invalid("A escala salva possui assignments inválidos.")
                    val type = item.optStringOrNull("type")
                        ?.let { runCatching { ShiftType.valueOf(it) }.getOrNull() }
                        ?: return CacheRead.Invalid("A escala salva possui assignments inválidos.")
                    add(
                        ShiftDay(
                            dayLabel = date.dayOfWeekShort(),
                            dateLabel = date.dateLabel(),
                            fullDateLabel = date.fullDateLabel(),
                            type = type,
                            date = date,
                            teamMembers = item.optStringArray("teamMembers"),
                            sourceStatus = item.optStringOrNull("sourceStatus"),
                            note = item.optStringOrNull("note"),
                            label = item.optStringOrNull("label") ?: type.label
                        )
                    )
                }
            }
            val member = Member(
                email = json.optStringOrNull("memberEmail") ?: "usuario@example.invalid",
                scaleName = json.optStringOrNull("memberScaleName") ?: "usuario",
                displayName = json.optStringOrNull("memberDisplayName") ?: "Usuario",
                teamId = json.optStringOrNull("teamId") ?: "soc",
                role = MemberRole.ANALYST
            )
            val summary = ScheduleSummary(
                member = member,
                team = Team(member.teamId, json.optStringOrNull("teamName") ?: member.teamId),
                days = days,
                periodLabel = json.optStringOrNull("periodLabel") ?: "Periodo salvo",
                pauseLabel = json.optStringOrNull("pauseLabel") ?: "Pausa nao calculada",
                pauseOffsetLabel = json.optStringOrNull("pauseOffsetLabel") ?: "",
                sourceFileName = json.optStringOrNull("originalFileName"),
                remoteSourceLabel = json.optStringOrNull("remoteSourceLabel"),
                collaborators = json.optStringArray("members"),
                warnings = json.optStringArray("warnings"),
                periodStart = periodStart,
                periodEnd = periodEnd
            )
            CacheRead.Valid(
                CachedSchedule(
                    originalFileName = json.optStringOrNull("originalFileName")
                        ?: return CacheRead.Invalid("Arquivo de origem ausente."),
                    importedAt = json.optStringOrNull("importedAt") ?: "",
                    resolvedYear = json.optInt("resolvedYear", periodStart.year),
                    yearResolutionSource = json.optStringOrNull("yearResolutionSource")
                        ?.let { runCatching { YearResolutionSource.valueOf(it) }.getOrNull() }
                        ?: YearResolutionSource.USER_CONFIRMED,
                    summary = summary
                )
            )
        } catch (_: Throwable) {
            CacheRead.Invalid("A escala salva está corrompida e foi ignorada.")
        }
    }

    override fun saveSchedule(schedule: CachedSchedule): Boolean = preferences.edit()
        .putString(ScheduleCacheKey, schedule.toJson().toString())
        .commit()

    override fun clearSchedule() {
        preferences.edit().remove(ScheduleCacheKey).apply()
    }

    override fun loadOnCall(): CacheRead<CachedOnCall> {
        val raw = preferences.getString(OnCallCacheKey, null) ?: return CacheRead.Missing
        return try {
            val json = JSONObject(raw)
            if (json.optInt("schemaVersion") != OnCallCacheSchemaVersion) {
                return CacheRead.Invalid("O plantão salvo usa uma versão incompatível.")
            }
            val assignmentsJson = json.optJSONArray("assignments") ?: return CacheRead.Invalid("O plantão salvo não possui assignments válidos.")
            if (assignmentsJson.length() <= 0) return CacheRead.Invalid("O plantão salvo não possui assignments válidos.")
            val assignments = buildList {
                for (index in 0 until assignmentsJson.length()) {
                    val item = assignmentsJson.optJSONObject(index) ?: return CacheRead.Invalid("O plantão salvo possui assignments inválidos.")
                    val startDate = item.optStringOrNull("startDate") ?: return CacheRead.Invalid("O plantão salvo possui assignments inválidos.")
                    val endDate = item.optStringOrNull("endDate") ?: return CacheRead.Invalid("O plantão salvo possui assignments inválidos.")
                    add(
                        OnCallAssignment(
                            id = item.optStringOrNull("id") ?: return CacheRead.Invalid("O plantão salvo possui assignments inválidos."),
                            periodId = "plantao-importado",
                            teamId = json.optStringOrNull("teamId") ?: "soc",
                            memberId = item.optStringOrNull("memberId") ?: "",
                            memberName = item.optStringOrNull("memberName") ?: "",
                            date = startDate,
                            startDate = startDate,
                            endDate = endDate,
                            startTime = item.optStringOrNull("startTime") ?: return CacheRead.Invalid("O plantão salvo possui assignments inválidos."),
                            endTime = item.optStringOrNull("endTime") ?: return CacheRead.Invalid("O plantão salvo possui assignments inválidos."),
                            status = OnCallStatus.SCHEDULED,
                            notes = item.optStringOrNull("notes"),
                            groupId = item.optStringOrNull("groupId") ?: json.optStringOrNull("groupId")
                        )
                    )
                }
            }
            CacheRead.Valid(
                CachedOnCall(
                    originalFileName = json.optStringOrNull("originalFileName")
                        ?: return CacheRead.Invalid("Arquivo de origem ausente."),
                    importedAt = json.optStringOrNull("importedAt") ?: "",
                    resolvedYear = json.optInt("resolvedYear", 0),
                    yearResolutionSource = YearResolutionSource.FULL_DATE_IN_WORKBOOK,
                    teamId = json.optStringOrNull("teamId") ?: "soc",
                    groupId = json.optStringOrNull("groupId"),
                    assignments = assignments,
                    warnings = json.optStringArray("warnings")
                )
            )
        } catch (_: Throwable) {
            CacheRead.Invalid("O plantão salvo está corrompido e foi ignorado.")
        }
    }

    override fun saveOnCall(onCall: CachedOnCall): Boolean = preferences.edit()
        .putString(OnCallCacheKey, onCall.toJson().toString())
        .commit()

    override fun clearOnCall() {
        preferences.edit().remove(OnCallCacheKey).apply()
    }

    private fun CachedSchedule.toJson(): JSONObject =
        JSONObject()
            .put("schemaVersion", schemaVersion)
            .put("sourceType", sourceType)
            .put("originalFileName", originalFileName)
            .put("importedAt", importedAt)
            .put("periodStart", summary.periodStart?.iso())
            .put("periodEnd", summary.periodEnd?.iso())
            .put("teamId", summary.team.teamId)
            .put("teamName", summary.team.name)
            .put("scheduleType", scheduleType)
            .put("resolvedYear", resolvedYear)
            .put("yearResolutionSource", yearResolutionSource.name)
            .put("memberEmail", summary.member.email)
            .put("memberScaleName", summary.member.scaleName)
            .put("memberDisplayName", summary.member.displayName)
            .put("periodLabel", summary.periodLabel)
            .put("pauseLabel", summary.pauseLabel)
            .put("pauseOffsetLabel", summary.pauseOffsetLabel)
            .put("remoteSourceLabel", summary.remoteSourceLabel)
            .put("members", summary.collaborators.toJsonArray())
            .put("warnings", summary.warnings.toJsonArray())
            .put(
                "assignments",
                JSONArray().also { array ->
                    summary.days.forEach { day ->
                        array.put(
                            JSONObject()
                                .put("date", day.date?.iso())
                                .put("type", day.type.name)
                                .put("label", day.label)
                                .put("sourceStatus", day.sourceStatus)
                                .put("note", day.note)
                                .put("teamMembers", day.teamMembers.toJsonArray())
                        )
                    }
                }
            )

    private fun CachedOnCall.toJson(): JSONObject =
        JSONObject()
            .put("schemaVersion", schemaVersion)
            .put("sourceType", sourceType)
            .put("originalFileName", originalFileName)
            .put("importedAt", importedAt)
            .put("teamId", teamId)
            .put("groupId", groupId)
            .put("resolvedYear", resolvedYear)
            .put("yearResolutionSource", yearResolutionSource.name)
            .put("warnings", warnings.toJsonArray())
            .put(
                "assignments",
                JSONArray().also { array ->
                    assignments.forEach { assignment ->
                        array.put(
                            JSONObject()
                                .put("id", assignment.id)
                                .put("memberId", assignment.memberId)
                                .put("memberName", assignment.memberName)
                                .put("startDate", assignment.startDate)
                                .put("endDate", assignment.endDate)
                                .put("startTime", assignment.startTime)
                                .put("endTime", assignment.endTime)
                                .put("notes", assignment.notes)
                                .put("groupId", assignment.groupId)
                        )
                    }
                }
            )

    private companion object {
        const val PrefsName = "escalaici.local.data.cache"
    }
}

private fun JSONObject.optStringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null

private fun JSONObject.optStringArray(name: String): List<String> {
    val array = optJSONArray(name) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun List<String>.toJsonArray(): JSONArray =
    JSONArray().also { array -> forEach(array::put) }

private fun LabDate.iso(): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
