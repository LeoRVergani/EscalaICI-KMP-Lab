package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.LabDateTime
import java.time.LocalDateTime

object AndroidCurrentTimeProvider : CurrentTimeProvider {
    override fun now(): LabDateTime {
        val now = LocalDateTime.now()
        return LabDateTime(
            date = LabDate(now.year, now.monthValue, now.dayOfMonth),
            minuteOfDay = now.hour * 60 + now.minute
        )
    }
}
