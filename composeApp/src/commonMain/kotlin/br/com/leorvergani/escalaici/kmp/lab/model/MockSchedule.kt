package br.com.leorvergani.escalaici.kmp.lab.model

fun mockScheduleSummary(): ScheduleSummary {
    val member = Member(
        email = "lvergani@ici.tec.br",
        scaleName = "lvergani",
        displayName = "Leonardo Vergani",
        teamId = "soc",
        role = MemberRole.ANALYST,
        active = true
    )
    val team = Team(
        teamId = "soc",
        name = "SOC",
        displayName = "SOC",
        members = mockTeamMembers()
    )
    val days = listOf(
        mockDay(6, 7, ShiftType.MANHA, listOf("alamancio", "altaborda")),
        mockDay(7, 7, ShiftType.MANHA, listOf("alamancio", "altaborda")),
        mockDay(8, 7, ShiftType.MANHA, listOf("mmoura", "pribeiro")),
        mockDay(9, 7, ShiftType.MANHA, listOf("woliveira", "msantos")),
        mockDay(10, 7, ShiftType.MANHA, listOf("rferreira", "jcastro")),
        mockDay(11, 7, ShiftType.FOLGA, note = "Descanso programado"),
        mockDay(12, 7, ShiftType.MANHA, listOf("alamancio", "altaborda"))
    )

    return ScheduleSummary(
        member = member,
        team = team,
        days = days,
        periodLabel = "26 jun. - 25 jul. 2026",
        pauseLabel = "08:00 - 08:15",
        pauseOffsetLabel = "1h após o início"
    )
}

private fun mockDay(
    day: Int,
    month: Int,
    type: ShiftType,
    teamMembers: List<String> = emptyList(),
    note: String? = null
): ShiftDay {
    val date = LabDate(2026, month, day)
    return ShiftDay(
        dayLabel = date.dayOfWeekShort(),
        dateLabel = date.dateLabel(),
        fullDateLabel = date.fullDateLabel(),
        type = type,
        date = date,
        teamMembers = teamMembers,
        membersByShift = if (type.isWorkShift) {
            mapOf(type to (listOf("lvergani") + teamMembers))
        } else {
            emptyMap()
        },
        note = note
    )
}

fun mockTeamMembers(): List<Member> = listOf(
    Member(
        email = "lvergani@ici.tec.br",
        scaleName = "lvergani",
        displayName = "Leonardo Vergani",
        teamId = "soc",
        role = MemberRole.LEAD,
        active = true
    ),
    Member(
        email = "alamancio@ici.tec.br",
        scaleName = "alamancio",
        displayName = "Alan Amancio",
        teamId = "soc",
        role = MemberRole.ANALYST,
        active = true
    ),
    Member(
        email = "altaborda@ici.tec.br",
        scaleName = "altaborda",
        displayName = "Alexandre Taborda",
        teamId = "soc",
        role = MemberRole.ANALYST,
        active = true
    )
)

/**
 * Dados mock dos modelos puros da FASE 9c (`SchedulePeriod`, `ScheduleAssignment`,
 * `OnCallPeriod`, `OnCallAssignment`, `ShiftSwapRequest`, `ImportJob`,
 * `SourceFileRecord`). Ainda nao sao consumidos pela UI do laboratorio; existem
 * para validar que os modelos compilam e sao utilizaveis multiplataforma.
 */
fun mockSchedulePeriod(): SchedulePeriod = SchedulePeriod(
    id = "period-2026-07",
    teamId = "soc",
    startDate = "2026-06-26",
    endDate = "2026-07-25",
    source = ScheduleSourceType.MOCK,
    updatedAt = "2026-07-09T08:00:00"
)

fun mockScheduleAssignments(): List<ScheduleAssignment> = listOf(
    ScheduleAssignment(
        id = "assign-1",
        periodId = "period-2026-07",
        teamId = "soc",
        memberId = "lvergani@ici.tec.br",
        memberName = "lvergani",
        date = "2026-07-06",
        shiftType = ShiftType.MANHA,
        startTime = "07:00",
        endTime = "13:00",
        source = AssignmentSource.MANUAL
    ),
    ScheduleAssignment(
        id = "assign-2",
        periodId = "period-2026-07",
        teamId = "soc",
        memberId = "lvergani@ici.tec.br",
        memberName = "lvergani",
        date = "2026-07-11",
        shiftType = ShiftType.FOLGA,
        source = AssignmentSource.MANUAL,
        notes = "Descanso programado"
    )
)

fun mockOnCallPeriod(): OnCallPeriod = OnCallPeriod(
    id = "oncall-2026-07",
    teamId = "soc",
    startDate = "2026-06-26",
    endDate = "2026-07-25",
    updatedAt = "2026-07-09T08:00:00"
)

fun mockOnCallAssignments(): List<OnCallAssignment> = listOf(
    OnCallAssignment(
        id = "oncall-assign-1",
        periodId = "oncall-2026-07",
        teamId = "soc",
        memberId = "altaborda@ici.tec.br",
        memberName = "altaborda",
        date = "2026-07-12",
        startTime = "19:00",
        endTime = "07:00",
        status = OnCallStatus.SCHEDULED
    )
)

fun mockShiftSwapRequests(): List<ShiftSwapRequest> = listOf(
    ShiftSwapRequest(
        id = "swap-1",
        requesterMemberId = "lvergani@ici.tec.br",
        targetMemberId = "alamancio@ici.tec.br",
        originalDate = "2026-07-08",
        requestedDate = "2026-07-09",
        status = SwapStatus.PENDING
    )
)

fun mockImportJob(): ImportJob = ImportJob(
    id = "import-1",
    sourceFileId = "source-1",
    status = ImportStatus.SUCCESS,
    createdAt = "2026-07-09T07:55:00",
    finishedAt = "2026-07-09T07:55:04"
)

fun mockSourceFileRecord(): SourceFileRecord = SourceFileRecord(
    id = "source-1",
    fileName = "Escala-SOC-Controle-Julho.xls",
    sourceType = ScheduleSourceType.IMPORTED_XLS,
    createdAt = "2026-07-09T07:54:00",
    updatedAt = "2026-07-09T07:55:04",
    hash = null
)
