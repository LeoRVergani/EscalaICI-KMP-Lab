package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.LabDate
import java.time.LocalDate

actual fun todayLabDate(): LabDate {
    val now = LocalDate.now()
    return LabDate(now.year, now.monthValue, now.dayOfMonth)
}
