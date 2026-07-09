package br.com.leorvergani.escalaici.kmp.lab.model

fun mockScheduleSummary(): ScheduleSummary {
    val member = Member(
        email = "lvergani@ici.tec.br",
        scaleName = "lvergani",
        displayName = "Leonardo Vergani"
    )
    val team = Team(
        teamId = "soc",
        name = "SOC"
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
