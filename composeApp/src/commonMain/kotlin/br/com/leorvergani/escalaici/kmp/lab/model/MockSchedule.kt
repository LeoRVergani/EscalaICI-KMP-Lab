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
        ShiftDay("Seg", "06/07", ShiftType.MANHA),
        ShiftDay("Ter", "07/07", ShiftType.MANHA),
        ShiftDay("Qua", "08/07", ShiftType.FOLGA),
        ShiftDay("Qui", "09/07", ShiftType.NOITE),
        ShiftDay("Sex", "10/07", ShiftType.NOITE),
        ShiftDay("Sab", "11/07", ShiftType.TARDE),
        ShiftDay("Dom", "12/07", ShiftType.FOLGA)
    )

    return ScheduleSummary(
        member = member,
        team = team,
        days = days
    )
}
