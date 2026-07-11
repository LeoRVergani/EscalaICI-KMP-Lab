package br.com.leorvergani.escalaici.kmp.lab.platform

import br.com.leorvergani.escalaici.kmp.lab.model.LabDate

actual fun todayLabDate(): LabDate = LabDate(currentYear(), currentMonth(), currentDay())

private fun currentYear(): Int = js("(new Date()).getFullYear()")

private fun currentMonth(): Int = js("(new Date()).getMonth() + 1")

private fun currentDay(): Int = js("(new Date()).getDate()")
