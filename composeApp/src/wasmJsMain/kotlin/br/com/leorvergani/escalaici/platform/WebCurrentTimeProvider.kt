package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.LabDateTime

object WebCurrentTimeProvider : CurrentTimeProvider {
    override fun now(): LabDateTime = LabDateTime(
        LabDate(currentYear(), currentMonth(), currentDay()),
        currentHour() * 60 + currentMinute()
    )
}

private fun currentYear(): Int = js("(new Date()).getFullYear()")
private fun currentMonth(): Int = js("(new Date()).getMonth() + 1")
private fun currentDay(): Int = js("(new Date()).getDate()")
private fun currentHour(): Int = js("(new Date()).getHours()")
private fun currentMinute(): Int = js("(new Date()).getMinutes()")
