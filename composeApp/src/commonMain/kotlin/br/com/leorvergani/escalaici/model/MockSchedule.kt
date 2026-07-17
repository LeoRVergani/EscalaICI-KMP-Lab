package br.com.leorvergani.escalaici.model

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
        pauseOffsetLabel = "1h após o início",
        pauseWindowStart = "09:00",
        pauseWindowEnd = "11:45",
        pauseSuggestions = listOf("09:00", "09:30", "10:00", "10:30", "11:00", "11:30")
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
    source = ScheduleSourceType.DEMO,
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
        id = "oncall-assign-0",
        periodId = "oncall-2026-07",
        teamId = "soc",
        memberId = "lvergani@ici.tec.br",
        memberName = "lvergani",
        date = "2026-07-09",
        startTime = "07:00",
        endTime = "19:00",
        status = OnCallStatus.ACTIVE
    ),
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
    ),
    OnCallAssignment(
        id = "oncall-assign-2",
        periodId = "oncall-2026-07",
        teamId = "soc",
        memberId = "alamancio@ici.tec.br",
        memberName = "alamancio",
        date = "2026-07-16",
        startTime = "19:00",
        endTime = "07:00",
        status = OnCallStatus.SCHEDULED
    ),
    OnCallAssignment(
        id = "oncall-assign-3",
        periodId = "oncall-2026-07",
        teamId = "soc",
        memberId = "lvergani@ici.tec.br",
        memberName = "lvergani",
        date = "2026-07-02",
        startTime = "07:00",
        endTime = "19:00",
        status = OnCallStatus.COMPLETED
    )
)

fun mockShiftSwapRequests(): List<ShiftSwapRequest> = listOf(
    ShiftSwapRequest(
        id = "swap-1",
        requesterMemberId = "lvergani@ici.tec.br",
        targetMemberId = "alamancio@ici.tec.br",
        originalDate = "2026-07-08",
        requestedDate = "2026-07-09",
        status = SwapStatus.PENDENTE_TECNICO_DESTINO,
        requesterName = "lvergani",
        targetName = "alamancio",
        requesterShiftType = ShiftType.MANHA,
        targetShiftType = ShiftType.TARDE,
        teamName = "SOC",
        createdAt = "2026-07-07"
    ),
    ShiftSwapRequest(
        id = "swap-2",
        requesterMemberId = "altaborda@ici.tec.br",
        targetMemberId = "lvergani@ici.tec.br",
        originalDate = "2026-07-12",
        requestedDate = "2026-07-13",
        status = SwapStatus.PENDENTE_TECNICO_DESTINO,
        requesterName = "altaborda",
        targetName = "lvergani",
        requesterShiftType = ShiftType.NOITE,
        targetShiftType = ShiftType.MANHA,
        teamName = "SOC",
        createdAt = "2026-07-06"
    ),
    ShiftSwapRequest(
        id = "swap-3",
        requesterMemberId = "alamancio@ici.tec.br",
        targetMemberId = "lvergani@ici.tec.br",
        originalDate = "2026-07-03",
        requestedDate = "2026-07-04",
        status = SwapStatus.APROVADA,
        requesterName = "alamancio",
        targetName = "lvergani",
        requesterShiftType = ShiftType.TARDE,
        targetShiftType = ShiftType.MANHA,
        teamName = "SOC",
        createdAt = "2026-07-01"
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
    sourceType = ScheduleSourceType.LOCAL_FILE,
    createdAt = "2026-07-09T07:54:00",
    updatedAt = "2026-07-09T07:55:04",
    hash = null
)

/**
 * Dados mock dos modelos universais da FASE 12b (`Organization`, `OrgUnit`,
 * `Role`, `MemberTeamMembership`, `ActivityCode`, `ScheduleProfile`,
 * `BusinessHoursRule`), spec `EscalaSOC/docs/spec/
 * 34-ESCALAICI-UNIVERSAL-SETORES-E-TIPOS-DE-ESCALA.md`. Representam a
 * hierarquia ICI > GEDSI > COSI > SOC e o setor irmão N1/Service Desk (sem
 * relação hierárquica com COSI). Ainda não consumidos pela UI nem por
 * nenhum parser/Firestore real — existem para validar que os modelos
 * compilam e são utilizáveis multiplataforma.
 */
fun mockOrganizationIci(): Organization = Organization(
    id = "ici",
    name = "ICI",
    acronym = "ICI"
)

fun mockOrgUnitGedsi(): OrgUnit = OrgUnit(
    id = "gedsi",
    organizationId = "ici",
    parentId = null,
    name = "Gerência de Data Center e Segurança da Informação",
    acronym = "GEDSI",
    type = OrgUnitType.MANAGEMENT
)

fun mockOrgUnitCosi(): OrgUnit = OrgUnit(
    id = "cosi",
    organizationId = "ici",
    parentId = "gedsi",
    name = "Coordenação de Segurança da Informação",
    acronym = "COSI",
    type = OrgUnitType.COORDINATION
)

/** Setor irmão de COSI, sem relação hierárquica com GEDSI/COSI (ver spec 34 §2). */
fun mockOrgUnitN1(): OrgUnit = OrgUnit(
    id = "n1-service-desk",
    organizationId = "ici",
    parentId = null,
    name = "N1 / Service Desk",
    acronym = "N1",
    type = OrgUnitType.SECTOR
)

fun mockTeamSoc(): Team = Team(
    teamId = "soc",
    name = "SOC",
    displayName = "SOC",
    members = mockTeamMembers()
)

fun mockTeamN1(): Team = Team(
    teamId = "n1",
    name = "Técnicos N1",
    displayName = "Técnicos N1 (Service Desk)",
    members = emptyList()
)

fun mockRoles(): List<Role> = listOf(
    Role(
        id = "role-team-admin",
        name = "Administrador da equipe",
        acronym = "ADMIN",
        orgUnitId = "cosi",
        roleShortName = "Admin",
        roleDisplayName = "Administrador da equipe"
    ),
    Role(
        id = "role-analyst",
        name = "Analista",
        acronym = "ANALISTA",
        orgUnitId = "cosi",
        roleShortName = "Analista",
        roleDisplayName = "Analista"
    ),
    /** Cargo exclusivo da equipe N1 — não existe em SOC/COSI/GEDSI. */
    Role(
        id = "role-n1-tecnico",
        name = "Técnico de TI",
        acronym = "TEC",
        orgUnitId = "n1-service-desk",
        roleShortName = "Técnico",
        roleDisplayName = "Técnico de TI"
    )
)

fun mockMemberTeamMemberships(): List<MemberTeamMembership> = listOf(
    MemberTeamMembership(
        id = "membership-lvergani-soc",
        memberId = "lvergani@ici.tec.br",
        teamId = "soc",
        roleId = "role-team-admin",
        startDate = "2026-01-01",
        isPrimary = true
    ),
    MemberTeamMembership(
        id = "membership-alamancio-soc",
        memberId = "alamancio@ici.tec.br",
        teamId = "soc",
        roleId = "role-analyst",
        startDate = "2026-01-01",
        isPrimary = true
    ),
    MemberTeamMembership(
        id = "membership-douglas-n1",
        memberId = "douglas.bezerra@ici.tec.br",
        teamId = "n1",
        roleId = "role-n1-tecnico",
        startDate = "2026-01-01",
        isPrimary = true
    )
)

/**
 * Códigos reais da equipe N1 (aba `Máscara` de `Escalas Equipe N1.xls`, spec
 * 34 §3.2) — exclusivos do N1/Service Desk no contexto atual (FASE 12b-2).
 * `F`/`X`/`AUS`/`M`/`M1`-`M4`/`E`/`G`/`T` **não pertencem** a SOC/COSI/GEDSI;
 * `scheduleProfileId = "profile-n1-matrix"` amarra isso explicitamente (não
 * é só o `teamId`), e nenhum código aqui tem `teamId`/`scheduleProfileId`
 * apontando pro SOC.
 */
fun mockActivityCodesN1(): List<ActivityCode> = listOf(
    ActivityCode(id = "n1-f", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "F", label = "Folga (Cockpit, DSR, BH, aniversário)", type = ActivityCodeType.REST, countsAsWork = false, sortOrder = 0),
    ActivityCode(id = "n1-x", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "X", label = "Férias", type = ActivityCodeType.VACATION, countsAsWork = false, sortOrder = 1),
    ActivityCode(id = "n1-aus", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "AUS", label = "Ausência (atestado, declaração, falta)", type = ActivityCodeType.ABSENCE, countsAsWork = false, sortOrder = 2),
    ActivityCode(id = "n1-m", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "M", label = "Manhã", type = ActivityCodeType.WORK, countsAsWork = true, sortOrder = 3),
    ActivityCode(id = "n1-m1", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "M1", label = "Monitoramento 1", type = ActivityCodeType.MONITORING, countsAsWork = true, sortOrder = 4),
    ActivityCode(id = "n1-m2", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "M2", label = "Monitoramento 2", type = ActivityCodeType.MONITORING, countsAsWork = true, sortOrder = 5),
    ActivityCode(id = "n1-m3", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "M3", label = "Monitoramento 3", type = ActivityCodeType.MONITORING, countsAsWork = true, sortOrder = 6),
    ActivityCode(id = "n1-m4", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "M4", label = "Monitoramento 4", type = ActivityCodeType.MONITORING, countsAsWork = true, sortOrder = 7),
    ActivityCode(id = "n1-e", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "E", label = "E-mail", type = ActivityCodeType.EMAIL, countsAsWork = true, sortOrder = 8),
    ActivityCode(id = "n1-g", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "G", label = "Garantia", type = ActivityCodeType.WARRANTY, countsAsWork = true, sortOrder = 9),
    ActivityCode(id = "n1-t", teamId = "n1", scheduleProfileId = "profile-n1-matrix", code = "T", label = "Treinamento", type = ActivityCodeType.MIXED, countsAsWork = true, sortOrder = 10)
)

fun mockScheduleProfiles(): List<ScheduleProfile> = listOf(
    ScheduleProfile(
        id = "profile-soc-6x1",
        teamId = "soc",
        name = "SOC 6x1 por turnos",
        type = ScheduleProfileType.ROTATING_6X1,
        periodMode = SchedulePeriodMode.DAY_26_TO_25,
        description = "Turnos fixos Madrugada/Manhã/Tarde/Noite, ciclo do dia 26 ao dia 25.",
        // SOC usa ShiftType (Madrugada/Manhã/Tarde/Noite) — sem card de código de atividade.
        uiConfig = ScheduleUiConfig(showActivityCodeCard = false)
    ),
    ScheduleProfile(
        id = "profile-n1-matrix",
        teamId = "n1",
        name = "N1 6x1 por códigos",
        type = ScheduleProfileType.MATRIX_6X1,
        periodMode = SchedulePeriodMode.MONTHLY,
        description = "Matriz mensal por colaborador, códigos configuráveis (F, X, AUS, M1-M4, E, G, T).",
        uiConfig = ScheduleUiConfig(showActivityCodeCard = true, activityCardTitle = "Atividade do dia")
    ),
    ScheduleProfile(
        id = "profile-administrativo",
        name = "Administrativo segunda a sexta",
        type = ScheduleProfileType.BUSINESS_HOURS,
        periodMode = SchedulePeriodMode.CONTINUOUS,
        description = "08:00-18:00 com 2h de almoço, sem rodízio de fim de semana.",
        uiConfig = ScheduleUiConfig(showActivityCodeCard = false)
    )
)

fun mockBusinessHoursRule(): BusinessHoursRule = BusinessHoursRule(
    id = "rule-administrativo",
    scheduleProfileId = "profile-administrativo",
    workDays = listOf(1, 2, 3, 4, 5),
    startTime = "08:00",
    endTime = "18:00",
    breakMinutes = 120
)
