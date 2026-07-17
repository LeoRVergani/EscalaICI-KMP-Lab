package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.LabDateTime

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

fun interface CurrentTimeProvider { fun now(): LabDateTime }
