package br.com.leorvergani.escalaici.kmp.lab.model

enum class ShiftType(
    val label: String,
    val shortLabel: String,
    val timeRange: String
) {
    MANHA("Manha", "M", "07:00 - 13:00"),
    TARDE("Tarde", "T", "13:00 - 19:00"),
    NOITE("Noite", "N", "19:00 - 01:00"),
    FOLGA("Folga", "F", "Descanso")
}

data class ShiftDay(
    val dayLabel: String,
    val dateLabel: String,
    val type: ShiftType
)

data class ScheduleSummary(
    val member: Member,
    val team: Team,
    val days: List<ShiftDay>
) {
    val nextShift: ShiftDay?
        get() = days.firstOrNull { it.type != ShiftType.FOLGA }

    val workedDays: Int
        get() = days.count { it.type != ShiftType.FOLGA }
}

data class Member(
    val email: String,
    val scaleName: String,
    val displayName: String
)

data class Team(
    val teamId: String,
    val name: String
)
