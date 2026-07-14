package br.com.leorvergani.escalaici.kmp.lab.source

import br.com.leorvergani.escalaici.kmp.lab.model.AssignmentSource
import br.com.leorvergani.escalaici.kmp.lab.model.LabDate
import br.com.leorvergani.escalaici.kmp.lab.model.Member
import br.com.leorvergani.escalaici.kmp.lab.model.MemberRole
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallAssignment
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallPeriod
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallStatus
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSourceType
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftDay
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftType
import br.com.leorvergani.escalaici.kmp.lab.model.Team

class FirebaseScheduleSource(
    private val gateway: FirebaseScheduleGateway,
    private val cache: FirebaseSourceCache,
    private val now: () -> String
) : ScheduleSource {
    override val sourceType = ScheduleSourceType.FIREBASE
    private var lastSnapshot: FirebaseScheduleSnapshot? = null

    override suspend fun loadActive(query: SourceQuery): DataLoadResult<ScheduleSourceData> {
        val loadedAt = now()
        return try {
            val teamId = query.teamId ?: return error("Equipe não informada.", loadedAt)
            val team = gateway.loadTeam(teamId) ?: return empty(teamId, loadedAt)
            val period = gateway.loadActiveSchedulePeriod(teamId) ?: return empty(teamId, loadedAt)
            val assignments = gateway.loadScheduleAssignments(teamId, period.periodId)
            val members = gateway.loadMembers(teamId)
            val snapshot = FirebaseScheduleSnapshot(team, period, members, assignments, loadedAt, loadedAt)
            val data = snapshot.toScheduleData(query.memberId)
            if (!cache.saveSchedule(snapshot)) return error("A escala foi carregada, mas não pôde ser disponibilizada offline.", loadedAt)
            lastSnapshot = snapshot
            DataLoadResult.Success(data, data.metadata, loadedAt = loadedAt)
        } catch (_: Throwable) {
            cache.loadSchedule()?.let { snapshot ->
                runCatching { snapshot.toScheduleData(query.memberId, fromCache = true) }.getOrNull()?.let { data ->
                    return DataLoadResult.OfflineCache(data, data.metadata, loadedAt = loadedAt)
                }
            }
            error("Não foi possível atualizar a escala. Tente novamente.", loadedAt)
        }
    }

    override suspend fun loadPeriod(query: SourceQuery, periodId: String): DataLoadResult<ScheduleSourceData> {
        val result = loadActive(query)
        return if (result.metadata?.periodId == periodId) result else empty(query.teamId, now())
    }

    override suspend fun checkForUpdate(query: SourceQuery): DataLoadResult<SourceUpdateStatus> {
        val loadedAt = now()
        return try {
            val teamId = query.teamId ?: return error("Equipe não informada.", loadedAt)
            val remote = gateway.checkRemoteUpdatedAt(teamId)
            val local = cache.loadSchedule()?.period?.updatedAt
            val metadata = metadata(cache.loadSchedule()?.period?.periodId, remote, loadedAt, false)
            DataLoadResult.Success(SourceUpdateStatus(remote != null && remote != local, metadata), metadata, loadedAt = loadedAt)
        } catch (_: Throwable) { error("Não foi possível verificar atualizações.", loadedAt) }
    }

    override suspend fun updateCache(data: ScheduleSourceData): Boolean = lastSnapshot?.let(cache::saveSchedule) ?: false
    override suspend fun invalidateCache(query: SourceQuery) = cache.clearSchedule()

    private fun empty(teamId: String?, at: String) = DataLoadResult.Empty(metadata(userMessage = "Nenhuma escala Firebase ativa para ${teamId ?: "a equipe"}.", at = at), loadedAt = at)
    private fun <T> error(message: String, at: String): DataLoadResult<T> = DataLoadResult.RecoverableError(message, metadata = metadata(userMessage = message, at = at), loadedAt = at)
}

class FirebaseOnCallSource(
    private val gateway: FirebaseScheduleGateway,
    private val cache: FirebaseSourceCache,
    private val now: () -> String
) : OnCallSource {
    override val sourceType = ScheduleSourceType.FIREBASE
    private var lastSnapshot: FirebaseOnCallSnapshot? = null

    override suspend fun loadActive(query: SourceQuery): DataLoadResult<OnCallSourceData> {
        val loadedAt = now()
        return try {
            val teamId = query.teamId ?: return error("Equipe não informada.", loadedAt)
            val period = gateway.loadActiveOnCallPeriod(teamId) ?: return empty(teamId, loadedAt)
            val assignments = gateway.loadOnCallAssignments(teamId, period.periodId)
            val members = gateway.loadMembers(teamId)
            val snapshot = FirebaseOnCallSnapshot(period, members, assignments, loadedAt, loadedAt)
            val data = snapshot.toOnCallData()
            if (!cache.saveOnCall(snapshot)) return error("O plantão foi carregado, mas não pôde ser disponibilizado offline.", loadedAt)
            lastSnapshot = snapshot
            DataLoadResult.Success(data, data.metadata, loadedAt = loadedAt)
        } catch (_: Throwable) {
            cache.loadOnCall()?.let { snapshot ->
                runCatching { snapshot.toOnCallData(fromCache = true) }.getOrNull()?.let { data ->
                    return DataLoadResult.OfflineCache(data, data.metadata, loadedAt = loadedAt)
                }
            }
            error("Não foi possível atualizar o plantão. Tente novamente.", loadedAt)
        }
    }

    override suspend fun loadPeriod(query: SourceQuery, periodId: String): DataLoadResult<OnCallSourceData> {
        val result = loadActive(query)
        return if (result.metadata?.periodId == periodId) result else empty(query.teamId, now())
    }

    override suspend fun checkForUpdate(query: SourceQuery): DataLoadResult<SourceUpdateStatus> {
        val loadedAt = now()
        return try {
            val teamId = query.teamId ?: return error("Equipe não informada.", loadedAt)
            val remote = gateway.checkRemoteUpdatedAt(teamId, onCall = true)
            val local = cache.loadOnCall()?.period?.updatedAt
            val metadata = metadata(cache.loadOnCall()?.period?.periodId, remote, loadedAt, false)
            DataLoadResult.Success(SourceUpdateStatus(remote != null && remote != local, metadata), metadata, loadedAt = loadedAt)
        } catch (_: Throwable) { error("Não foi possível verificar atualizações do plantão.", loadedAt) }
    }

    override suspend fun updateCache(data: OnCallSourceData): Boolean = lastSnapshot?.let(cache::saveOnCall) ?: false
    override suspend fun invalidateCache(query: SourceQuery) = cache.clearOnCall()

    private fun empty(teamId: String?, at: String) = DataLoadResult.Empty(metadata(userMessage = "Nenhum plantão Firebase ativo para ${teamId ?: "a equipe"}.", at = at), loadedAt = at)
    private fun <T> error(message: String, at: String): DataLoadResult<T> = DataLoadResult.RecoverableError(message, metadata = metadata(userMessage = message, at = at), loadedAt = at)
}

private fun FirebaseScheduleSnapshot.toScheduleData(memberId: String?, fromCache: Boolean = false): ScheduleSourceData {
    require(assignments.isNotEmpty()) { "Escala sem assignments." }
    require(assignments.distinctBy { it.assignmentId }.size == assignments.size) { "Assignment duplicado." }
    require(assignments.distinctBy { "${it.memberId}|${it.date}" }.size == assignments.size) { "Assignment duplicado por membro/data." }
    require(assignments.all { it.memberId != null && LabDate.parseIso(it.date) != null }) { "Assignment incompleto." }
    val memberMap = members.associateBy { it.memberId }
    require(assignments.all { it.memberId in memberMap }) { "Membro ausente." }
    val selected = if (memberId == null) members.firstOrNull() else memberMap[memberId]
        ?: error("Membro selecionado não encontrado no Firebase.")
    requireNotNull(selected) { "Nenhum membro ativo." }
    val domainMember = selected.toMember()
    val memberAssignments = assignments.filter { it.memberId == selected.memberId }.sortedBy { it.date }
    require(memberAssignments.isNotEmpty()) { "Nenhum assignment para o membro selecionado." }
    val days = memberAssignments.map { assignment ->
        val date = requireNotNull(LabDate.parseIso(assignment.date))
        val type = assignment.toShiftType()
        val sameDay = assignments.filter { it.date == assignment.date && it.assignmentType == "WORK_SHIFT" }
        ShiftDay(
            dayLabel = date.dayOfWeekShort(), dateLabel = date.dateLabel(), fullDateLabel = date.fullDateLabel(),
            type = type, date = date, teamMembers = sameDay.map { it.scaleName }.filter { it != selected.scaleName },
            membersByShift = sameDay.groupBy { it.toShiftType() }.mapValues { (_, values) -> values.map { it.scaleName } },
            sourceStatus = assignment.assignmentType, note = assignment.note, label = type.label
        )
    }
    val metadata = metadata(period.periodId, period.updatedAt, cachedAt, fromCache)
    return ScheduleSourceData(
        ScheduleSummary(
            member = domainMember,
            team = Team(team.teamId, team.teamName, members = members.map { it.toMember() }),
            days = days,
            periodLabel = "${period.startDate} a ${period.endDate}",
            pauseLabel = "Pausa conforme o turno",
            pauseOffsetLabel = "",
            collaborators = members.map { it.scaleName },
            sourceFileName = null,
            warnings = emptyList()
        ),
        metadata
    )
}

private fun FirebaseOnCallSnapshot.toOnCallData(fromCache: Boolean = false): OnCallSourceData {
    require(assignments.isNotEmpty()) { "Plantão sem assignments." }
    require(assignments.distinctBy { it.onCallId }.size == assignments.size) { "Plantão duplicado." }
    val membersById = members.associateBy { it.memberId }
    val mapped = assignments.map { dto ->
        val memberId = dto.memberId ?: error("Plantão sem membro.")
        require(memberId in membersById) { "Membro do plantão ausente." }
        val startDate = dto.startDateTime.take(10)
        val endDate = dto.endDateTime.take(10)
        val startTime = dto.startDateTime.substringAfter('T').take(5)
        val endTime = dto.endDateTime.substringAfter('T').take(5)
        require(LabDate.parseIso(startDate) != null && LabDate.parseIso(endDate) != null && startTime.length == 5 && endTime.length == 5)
        OnCallAssignment(dto.onCallId, dto.periodId ?: period.periodId, dto.teamId, memberId, dto.scaleName, startDate, startDate, endDate, startTime, endTime, OnCallStatus.SCHEDULED, dto.notes)
    }
    val metadata = metadata(period.periodId, period.updatedAt, cachedAt, fromCache)
    return OnCallSourceData(OnCallPeriod(period.periodId, period.teamId, period.startDate, period.endDate, period.updatedAt), mapped, metadata)
}

internal fun FirebaseMemberDto.toMember() = Member(
    email = "", scaleName = scaleName, displayName = displayName, id = memberId, teamId = teamId,
    role = when (role?.lowercase()) { "system_admin", "team_admin", "admin" -> MemberRole.ADMIN; "responsible", "lead" -> MemberRole.LEAD; else -> MemberRole.ANALYST },
    active = active
)

private fun FirebaseScheduleAssignmentDto.toShiftType(): ShiftType = when (assignmentType) {
    "OFF" -> ShiftType.FOLGA
    "VACATION" -> ShiftType.FERIAS
    "WORK_SHIFT" -> when (shiftName?.lowercase()) {
        "madrugada" -> ShiftType.MADRUGADA
        "manhã", "manha" -> ShiftType.MANHA
        "tarde" -> ShiftType.TARDE
        "noite" -> ShiftType.NOITE
        else -> error("Turno desconhecido.")
    }
    else -> error("Tipo de assignment desconhecido.")
}

private fun metadata(
    periodId: String? = null,
    updatedAt: String? = null,
    at: String = "",
    fromCache: Boolean = false,
    userMessage: String? = null
) = SourceMetadata(
    sourceType = ScheduleSourceType.FIREBASE,
    periodId = periodId,
    sourceUpdatedAt = updatedAt,
    localSyncedAt = at,
    connectivity = if (fromCache) ConnectivityStatus.OFFLINE else ConnectivityStatus.ONLINE,
    fromCache = fromCache,
    remoteSchemaVersion = FirebaseCacheSchemaVersion,
    userMessage = userMessage ?: if (fromCache) "Dados Firebase disponíveis offline." else "Fonte: Firebase"
)
