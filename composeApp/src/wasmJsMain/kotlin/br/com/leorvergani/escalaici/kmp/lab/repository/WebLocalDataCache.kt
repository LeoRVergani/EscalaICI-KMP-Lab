package br.com.leorvergani.escalaici.kmp.lab.repository

import br.com.leorvergani.escalaici.kmp.lab.model.LabDate
import br.com.leorvergani.escalaici.kmp.lab.model.Member
import br.com.leorvergani.escalaici.kmp.lab.model.MemberRole
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallAssignment
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallStatus
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftDay
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftType
import br.com.leorvergani.escalaici.kmp.lab.model.Team
import br.com.leorvergani.escalaici.kmp.lab.model.YearResolutionSource
import kotlinx.browser.window

class WebLocalDataCache : LocalDataCache {
    override fun loadSchedule(): CacheRead<CachedSchedule> {
        val raw = read(ScheduleCacheKey) ?: return CacheRead.Missing
        return try {
            if (!isJsonObject(raw)) return CacheRead.Invalid("A escala salva está corrompida e foi ignorada.")
            val schema = jsonInt(raw, "schemaVersion")
            if (schema != ScheduleCacheSchemaVersion) return CacheRead.Invalid("A escala salva usa uma versão incompatível.")
            val periodStart = jsonString(raw, "periodStart")?.let(LabDate::parseIso)
                ?: return CacheRead.Invalid("O período da escala salva é inválido.")
            val periodEnd = jsonString(raw, "periodEnd")?.let(LabDate::parseIso)
                ?: return CacheRead.Invalid("O período da escala salva é inválido.")
            if (periodEnd < periodStart) return CacheRead.Invalid("O período da escala salva é inválido.")
            val assignmentCount = jsonArrayLength(raw, "assignments")
            if (assignmentCount <= 0) return CacheRead.Invalid("A escala salva não possui assignments válidos.")
            val days = (0 until assignmentCount).mapNotNull { index ->
                val date = jsonArrayString(raw, "assignments", index, "date")?.let(LabDate::parseIso) ?: return@mapNotNull null
                val type = jsonArrayString(raw, "assignments", index, "type")?.let { runCatching { ShiftType.valueOf(it) }.getOrNull() }
                    ?: return@mapNotNull null
                ShiftDay(
                    dayLabel = date.dayOfWeekShort(),
                    dateLabel = date.dateLabel(),
                    fullDateLabel = date.fullDateLabel(),
                    type = type,
                    date = date,
                    teamMembers = jsonNestedStringArray(raw, "assignments", index, "teamMembers"),
                    sourceStatus = jsonArrayString(raw, "assignments", index, "sourceStatus"),
                    note = jsonArrayString(raw, "assignments", index, "note"),
                    label = jsonArrayString(raw, "assignments", index, "label") ?: type.label
                )
            }
            if (days.size != assignmentCount) return CacheRead.Invalid("A escala salva possui assignments inválidos.")
            val member = Member(
                email = jsonString(raw, "memberEmail") ?: "usuario@example.invalid",
                scaleName = jsonString(raw, "memberScaleName") ?: "usuario",
                displayName = jsonString(raw, "memberDisplayName") ?: "Usuário",
                teamId = jsonString(raw, "teamId") ?: "soc",
                role = MemberRole.ANALYST
            )
            val summary = ScheduleSummary(
                member = member,
                team = Team(member.teamId, jsonString(raw, "teamName") ?: member.teamId),
                days = days,
                periodLabel = jsonString(raw, "periodLabel") ?: "Período salvo",
                pauseLabel = jsonString(raw, "pauseLabel") ?: "Pausa não calculada",
                pauseOffsetLabel = jsonString(raw, "pauseOffsetLabel") ?: "",
                sourceFileName = jsonString(raw, "originalFileName"),
                collaborators = jsonStringArray(raw, "members"),
                warnings = jsonStringArray(raw, "warnings"),
                periodStart = periodStart,
                periodEnd = periodEnd
            )
            CacheRead.Valid(
                CachedSchedule(
                    originalFileName = jsonString(raw, "originalFileName") ?: return CacheRead.Invalid("Arquivo de origem ausente."),
                    importedAt = jsonString(raw, "importedAt") ?: "",
                    resolvedYear = jsonInt(raw, "resolvedYear") ?: periodStart.year,
                    yearResolutionSource = jsonString(raw, "yearResolutionSource")
                        ?.let { runCatching { YearResolutionSource.valueOf(it) }.getOrNull() }
                        ?: YearResolutionSource.USER_CONFIRMED,
                    summary = summary
                )
            )
        } catch (_: Throwable) {
            CacheRead.Invalid("A escala salva está corrompida e foi ignorada.")
        }
    }

    override fun saveSchedule(schedule: CachedSchedule): Boolean = write(
        ScheduleCacheKey,
        buildString {
            append('{')
            field("schemaVersion", schedule.schemaVersion)
            field("sourceType", schedule.sourceType)
            field("originalFileName", schedule.originalFileName)
            field("importedAt", schedule.importedAt)
            field("periodStart", schedule.summary.periodStart?.iso())
            field("periodEnd", schedule.summary.periodEnd?.iso())
            field("teamId", schedule.summary.team.teamId)
            field("teamName", schedule.summary.team.name)
            field("scheduleType", schedule.scheduleType)
            field("resolvedYear", schedule.resolvedYear)
            field("yearResolutionSource", schedule.yearResolutionSource.name)
            field("memberEmail", schedule.summary.member.email)
            field("memberScaleName", schedule.summary.member.scaleName)
            field("memberDisplayName", schedule.summary.member.displayName)
            field("periodLabel", schedule.summary.periodLabel)
            field("pauseLabel", schedule.summary.pauseLabel)
            field("pauseOffsetLabel", schedule.summary.pauseOffsetLabel)
            stringArrayField("members", schedule.summary.collaborators)
            stringArrayField("warnings", schedule.summary.warnings)
            append("\"assignments\":[")
            schedule.summary.days.forEachIndexed { index, day ->
                if (index > 0) append(',')
                append('{')
                field("date", day.date?.iso())
                field("type", day.type.name)
                field("label", day.label)
                field("sourceStatus", day.sourceStatus)
                field("note", day.note)
                stringArrayField("teamMembers", day.teamMembers, trailingComma = false)
                append('}')
            }
            append("]}")
        }
    )

    override fun clearSchedule() = remove(ScheduleCacheKey)

    override fun loadOnCall(): CacheRead<CachedOnCall> {
        val raw = read(OnCallCacheKey) ?: return CacheRead.Missing
        return try {
            if (!isJsonObject(raw)) return CacheRead.Invalid("O plantão salvo está corrompido e foi ignorado.")
            if (jsonInt(raw, "schemaVersion") != OnCallCacheSchemaVersion) return CacheRead.Invalid("O plantão salvo usa uma versão incompatível.")
            val count = jsonArrayLength(raw, "assignments")
            if (count <= 0) return CacheRead.Invalid("O plantão salvo não possui assignments válidos.")
            val assignments = (0 until count).mapNotNull { index ->
                val startDate = jsonArrayString(raw, "assignments", index, "startDate") ?: return@mapNotNull null
                val endDate = jsonArrayString(raw, "assignments", index, "endDate") ?: return@mapNotNull null
                OnCallAssignment(
                    id = jsonArrayString(raw, "assignments", index, "id") ?: return@mapNotNull null,
                    periodId = "plantao-importado",
                    teamId = jsonString(raw, "teamId") ?: "soc",
                    memberId = jsonArrayString(raw, "assignments", index, "memberId") ?: "",
                    memberName = jsonArrayString(raw, "assignments", index, "memberName") ?: "",
                    date = startDate,
                    startDate = startDate,
                    endDate = endDate,
                    startTime = jsonArrayString(raw, "assignments", index, "startTime") ?: return@mapNotNull null,
                    endTime = jsonArrayString(raw, "assignments", index, "endTime") ?: return@mapNotNull null,
                    status = OnCallStatus.SCHEDULED,
                    notes = jsonArrayString(raw, "assignments", index, "notes")
                )
            }
            if (assignments.size != count) return CacheRead.Invalid("O plantão salvo possui assignments inválidos.")
            CacheRead.Valid(
                CachedOnCall(
                    originalFileName = jsonString(raw, "originalFileName") ?: return CacheRead.Invalid("Arquivo de origem ausente."),
                    importedAt = jsonString(raw, "importedAt") ?: "",
                    resolvedYear = jsonInt(raw, "resolvedYear") ?: 0,
                    yearResolutionSource = YearResolutionSource.FULL_DATE_IN_WORKBOOK,
                    teamId = jsonString(raw, "teamId") ?: "soc",
                    assignments = assignments,
                    warnings = jsonStringArray(raw, "warnings")
                )
            )
        } catch (_: Throwable) {
            CacheRead.Invalid("O plantão salvo está corrompido e foi ignorado.")
        }
    }

    override fun saveOnCall(onCall: CachedOnCall): Boolean = write(
        OnCallCacheKey,
        buildString {
            append('{')
            field("schemaVersion", onCall.schemaVersion)
            field("sourceType", onCall.sourceType)
            field("originalFileName", onCall.originalFileName)
            field("importedAt", onCall.importedAt)
            field("periodStart", onCall.assignments.minOfOrNull { it.startDate })
            field("periodEnd", onCall.assignments.maxOfOrNull { it.endDate })
            field("teamId", onCall.teamId)
            field("resolvedYear", onCall.resolvedYear)
            field("yearResolutionSource", onCall.yearResolutionSource.name)
            stringArrayField("warnings", onCall.warnings)
            append("\"assignments\":[")
            onCall.assignments.forEachIndexed { index, assignment ->
                if (index > 0) append(',')
                append('{')
                field("id", assignment.id)
                field("memberId", assignment.memberId)
                field("memberName", assignment.memberName)
                field("startDate", assignment.startDate)
                field("startTime", assignment.startTime)
                field("endDate", assignment.endDate)
                field("endTime", assignment.endTime)
                field("notes", assignment.notes, trailingComma = false)
                append('}')
            }
            append("]}")
        }
    )

    override fun clearOnCall() = remove(OnCallCacheKey)

    private fun read(key: String): String? = try { window.localStorage.getItem(key) } catch (_: Throwable) { null }
    private fun write(key: String, value: String): Boolean = try { window.localStorage.setItem(key, value); true } catch (_: Throwable) { false }
    private fun remove(key: String) { try { window.localStorage.removeItem(key) } catch (_: Throwable) { } }
}

private fun LabDate.iso(): String = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

private fun StringBuilder.field(name: String, value: String?, trailingComma: Boolean = true) {
    append(jsonQuote(name)).append(':').append(value?.let(::jsonQuote) ?: "null")
    if (trailingComma) append(',')
}

private fun StringBuilder.field(name: String, value: Int, trailingComma: Boolean = true) {
    append(jsonQuote(name)).append(':').append(value)
    if (trailingComma) append(',')
}

private fun StringBuilder.stringArrayField(name: String, values: List<String>, trailingComma: Boolean = true) {
    append(jsonQuote(name)).append(':').append(values.joinToString(prefix = "[", postfix = "]") { jsonQuote(it) })
    if (trailingComma) append(',')
}

private fun jsonQuote(value: String): String = buildString {
    append('"')
    value.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
    append('"')
}

private fun isJsonObject(raw: String): Boolean = js("{ try { const value = JSON.parse(raw); return value !== null && typeof value === 'object' && !Array.isArray(value); } catch (_) { return false; } }")
private fun jsonString(raw: String, key: String): String? = js("{ const value = JSON.parse(raw)[key]; return typeof value === 'string' ? value : null; }")
private fun jsonInt(raw: String, key: String): Int? = js("{ const value = JSON.parse(raw)[key]; return Number.isInteger(value) ? value : null; }")
private fun jsonArrayLength(raw: String, key: String): Int = js("{ const value = JSON.parse(raw)[key]; return Array.isArray(value) ? value.length : -1; }")
private fun jsonArrayString(raw: String, key: String, index: Int, field: String): String? = js("{ const array = JSON.parse(raw)[key]; const value = Array.isArray(array) && array[index] ? array[index][field] : null; return typeof value === 'string' ? value : null; }")
private fun jsonStringArray(raw: String, key: String): List<String> {
    val size = jsonArrayLength(raw, key)
    return if (size < 0) emptyList() else (0 until size).mapNotNull { jsonSimpleArrayString(raw, key, it) }
}
private fun jsonSimpleArrayString(raw: String, key: String, index: Int): String? = js("{ const array = JSON.parse(raw)[key]; const value = Array.isArray(array) ? array[index] : null; return typeof value === 'string' ? value : null; }")
private fun jsonNestedStringArray(raw: String, key: String, index: Int, field: String): List<String> {
    val size = jsonNestedArrayLength(raw, key, index, field)
    return if (size < 0) emptyList() else (0 until size).mapNotNull { jsonNestedArrayString(raw, key, index, field, it) }
}
private fun jsonNestedArrayLength(raw: String, key: String, index: Int, field: String): Int = js("{ const array = JSON.parse(raw)[key]; const nested = Array.isArray(array) && array[index] ? array[index][field] : null; return Array.isArray(nested) ? nested.length : -1; }")
private fun jsonNestedArrayString(raw: String, key: String, index: Int, field: String, nestedIndex: Int): String? = js("{ const array = JSON.parse(raw)[key]; const nested = Array.isArray(array) && array[index] ? array[index][field] : null; const value = Array.isArray(nested) ? nested[nestedIndex] : null; return typeof value === 'string' ? value : null; }")
