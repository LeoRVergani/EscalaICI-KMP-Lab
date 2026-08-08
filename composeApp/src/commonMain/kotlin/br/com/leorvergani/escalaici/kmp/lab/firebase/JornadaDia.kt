package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.CategoriaTurno
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.DiaRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto

/**
 * Porte de `resolverJornadaDia`/`categoriaTrabalha`
 * (`packages/contrato/src/jornada.ts`) - usado por Trocas (elegibilidade de
 * dia/colega no assistente) e por "quem trabalha nesse dia" (FASE 16,
 * secao 22). Escopo menor que o TS de proposito: sem `categoria: Categoria |
 * 'SEM_ESCALA'` nem `resolverContextoJornada`/`viraDia` - usados so por
 * Hoje/Plantao, que ja tem seu proprio caminho via [EscalaIciScheduleMapper]
 * e nao precisam ser duplicados aqui.
 */
data class JornadaDia(
    val data: String,
    val codigo: String,
    val descricao: String,
    val inicio: String?,
    val fim: String?,
    val duracaoMinutos: Int,
    val trabalha: Boolean,
) {
    /** `"07:00-13:00"` quando os dois horarios existem, string vazia caso contrario - igual `horarioDoDia` (`lib/firebase/trocasRepository.ts`). */
    val horario: String get() = if (inicio != null && fim != null) "$inicio–$fim" else ""
}

private fun categoriaTrabalha(categoria: CategoriaTurno?): Boolean =
    categoria == CategoriaTurno.TRABALHO || categoria == CategoriaTurno.PLANTAO || categoria == CategoriaTurno.EXTRA

/** `dia` nulo replica `documento?.dias[dataIso]` sendo `undefined` no TS - dia sem escala publicada para essa data. */
fun resolverJornadaDia(dia: DiaRemoteDto?, catalogo: Map<String, TipoTurnoRemoteDto>, dataIso: String): JornadaDia {
    if (dia == null) {
        return JornadaDia(
            data = dataIso,
            codigo = "",
            descricao = "Sem escala publicada",
            inicio = null,
            fim = null,
            duracaoMinutos = 0,
            trabalha = false,
        )
    }
    val tipo = catalogo[dia.c]
    val inicio = dia.i ?: tipo?.horaInicio
    val fim = dia.f ?: tipo?.horaFim
    val duracaoMinutos = dia.m ?: tipo?.duracaoMinutos ?: 0
    val trabalha = (duracaoMinutos > 0 || categoriaTrabalha(tipo?.categoria)) && inicio != null && fim != null
    return JornadaDia(
        data = dataIso,
        codigo = dia.c,
        descricao = tipo?.descricao ?: dia.c,
        inicio = inicio,
        fim = fim,
        duracaoMinutos = duracaoMinutos,
        trabalha = trabalha,
    )
}
