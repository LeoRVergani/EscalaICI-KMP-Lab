package br.com.leorvergani.escalaici.kmp.lab.platform

import br.com.leorvergani.escalaici.kmp.lab.model.LabDate
import java.time.LocalDate

actual fun todayLabDate(): LabDate {
    val now = LocalDate.now()
    return LabDate(now.year, now.monthValue, now.dayOfMonth)
}
