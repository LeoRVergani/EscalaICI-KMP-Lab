package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto

/**
 * Porte literal de `packages/contrato/src/jornada.ts` (`origin/main` do
 * Escala-ICI, confirmado sem alteracoes na auditoria da FASE 15) - a regra
 * de corte no dia 26 e a selecao do periodo vigente NAO sao inventadas
 * aqui, sao as mesmas usadas pelo app real (`apps/app/src/EmployeeApp.tsx`).
 */
object Jornada {
    private val competenciaRegex = Regex("""^(\d{4})-(\d{2})$""")
    private val dataIsoRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")

    fun adicionarMeses(competencia: String, quantidade: Int): String {
        val match = competenciaRegex.matchEntire(competencia)
            ?: throw IllegalArgumentException("Competencia invalida: $competencia")
        val ano = match.groupValues[1].toInt()
        val mes = match.groupValues[2].toInt()
        val totalMeses = ano * 12 + (mes - 1) + quantidade
        val novoAno = totalMeses.floorDiv(12)
        val novoMes = totalMeses.mod(12) + 1
        return "$novoAno-${novoMes.toString().padStart(2, '0')}"
    }

    fun competenciaOperacional(dataIso: String, diaCorte: Int = 26): String {
        val partes = partesData(dataIso)
        val calendario = "${partes.ano}-${partes.mes.toString().padStart(2, '0')}"
        return if (partes.dia >= diaCorte) adicionarMeses(calendario, 1) else calendario
    }

    /** Ordem igual ao real: operacional, calendario, operacional-1mes, operacional+1mes (sem duplicatas). */
    fun competenciasCandidatas(dataIso: String, diaCorte: Int = 26): List<String> {
        val operacional = competenciaOperacional(dataIso, diaCorte)
        val partes = partesData(dataIso)
        val calendario = "${partes.ano}-${partes.mes.toString().padStart(2, '0')}"
        return listOf(operacional, calendario, adicionarMeses(operacional, -1), adicionarMeses(operacional, 1)).distinct()
    }

    /**
     * Primeiro tenta achar a escala cujo periodo contem a data (cobre
     * ciclos que atravessam o mes, ex.: 26/07 a 25/08); senao cai para a
     * competencia operacional; senao a competencia mais recente
     * disponivel - mesma ordem de `selecionarEscalaPorData`.
     */
    fun selecionarEscalaPorData(escalas: List<TurnosMesRemoteDto>, dataIso: String): TurnosMesRemoteDto? {
        escalas.firstOrNull { it.periodoInicio <= dataIso && dataIso <= it.periodoFim }?.let { return it }
        val competencia = competenciaOperacional(dataIso)
        return escalas.firstOrNull { it.competencia == competencia }
            ?: escalas.maxByOrNull { it.competencia }
    }

    private data class PartesData(val ano: Int, val mes: Int, val dia: Int)

    private fun partesData(dataIso: String): PartesData {
        val match = dataIsoRegex.matchEntire(dataIso)
            ?: throw IllegalArgumentException("Data ISO invalida: $dataIso")
        return PartesData(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
    }
}
