package br.com.leorvergani.escalaici.kmp.lab.platform

import br.com.leorvergani.escalaici.kmp.lab.model.LabDate

/**
 * Data atual real do dispositivo/navegador — usada como âncora única de
 * "hoje" para todas as abas (Hoje, Escala, Alertas), em vez de cada tela
 * assumir que "hoje" é o primeiro dia da lista importada.
 */
expect fun todayLabDate(): LabDate

fun interface TodayProvider {
    fun today(): LabDate
}

object SystemTodayProvider : TodayProvider {
    override fun today(): LabDate = todayLabDate()
}
