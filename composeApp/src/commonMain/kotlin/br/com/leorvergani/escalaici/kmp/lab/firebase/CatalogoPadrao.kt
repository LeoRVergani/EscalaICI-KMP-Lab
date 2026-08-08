package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.CategoriaTurno
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto

/**
 * Porte literal de `CATALOGO_SOC` (`packages/contrato/src/catalogo.ts`,
 * `origin/main` do Escala-ICI) - usado so como fallback quando a query em
 * `tiposTurno` para a equipe vier vazia, exatamente como
 * `readRepository.ts:listarCatalogo()` faz no app real. Nao e um catalogo
 * inventado pelo KMP; e o mesmo fallback da fonte de verdade.
 */
object CatalogoPadrao {
    val CATALOGO_SOC: Map<String, TipoTurnoRemoteDto> = listOf(
        TipoTurnoRemoteDto("MD", "Madrugada", CategoriaTurno.TRABALHO, "01:00", "07:00", 360, false, false, 0, "#FFFF00", listOf("MADRUGADA", "MD")),
        TipoTurnoRemoteDto("M", "Manhã", CategoriaTurno.TRABALHO, "07:00", "13:00", 360, false, false, 0, "#FFFF00", listOf("MANHA", "MANHÃ", "M")),
        TipoTurnoRemoteDto("T", "Tarde", CategoriaTurno.TRABALHO, "13:00", "19:00", 360, false, false, 0, "#FFFF00", listOf("TARDE", "T")),
        TipoTurnoRemoteDto("N", "Noite", CategoriaTurno.TRABALHO, "19:00", "01:00", 360, true, false, 0, "#FFFF00", listOf("NOITE", "N")),
        TipoTurnoRemoteDto("X", "Férias", CategoriaTurno.AUSENCIA, null, null, 0, false, false, 0, "#0070C0", listOf("X", "FERIAS", "FÉRIAS")),
        TipoTurnoRemoteDto("DF", "DSR - Final de Semana", CategoriaTurno.DESCANSO, null, null, 0, false, false, 0, "#FF3399", listOf("DF")),
        TipoTurnoRemoteDto("DU", "DSR - Dia útil", CategoriaTurno.DESCANSO, null, null, 0, false, false, 0, "#00B050", listOf("DU")),
        TipoTurnoRemoteDto("BH", "Compensação BH", CategoriaTurno.COMPENSACAO, null, null, 0, false, false, 0, "#FFD966", listOf("BH")),
        TipoTurnoRemoteDto("FOLGA", "Folga - Feriado", CategoriaTurno.DESCANSO, null, null, 0, false, false, 0, "#CC99FF", listOf("FOLGA")),
        TipoTurnoRemoteDto("AN", "Folga Aniversário", CategoriaTurno.DESCANSO, null, null, 0, false, false, 0, "#99CCFF", listOf("AN")),
        TipoTurnoRemoteDto("HE", "Hora Extra", CategoriaTurno.EXTRA, null, null, 0, false, false, 0, "#00B0F0", listOf("HE")),
        TipoTurnoRemoteDto("AFA", "Afastamento Atestado", CategoriaTurno.AUSENCIA, null, null, 0, false, false, 0, "#404040", listOf("#", "AT", "ATESTADO", "AFASTAMENTO")),
    ).associateBy { it.codigo }
}
