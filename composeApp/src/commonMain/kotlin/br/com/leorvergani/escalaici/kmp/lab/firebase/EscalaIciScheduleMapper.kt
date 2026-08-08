package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.CategoriaTurno
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.DiaRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.UsuarioRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.model.LabDate
import br.com.leorvergani.escalaici.kmp.lab.model.Member
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftDay
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftType
import br.com.leorvergani.escalaici.kmp.lab.model.Team

/**
 * Converte `TurnosMesRemoteDto` + catalogo de `tiposTurno` no
 * `ScheduleSummary` ja usado pela UI (`model/ScheduleModels.kt`) - a UI
 * (Hoje/Escala/Perfil) nao muda, so passa a receber dados reais do
 * Firestore em vez do mock.
 *
 * Compromisso deliberado e documentado: `ShiftType` continua sendo o enum
 * fechado ja existente (Madrugada/Manha/Tarde/Noite/Folga/...), em vez de
 * um modelo totalmente dirigido pelo catalogo remoto (o que o prompt da
 * FASE 15 secao 15 pediria de forma literal) - substituir esse enum
 * exigiria reescrever cores/icones/textos usados em varias telas
 * (`ShiftColors`, `TodayTab`, `ScheduleTab`) sem um navegador/emulador
 * disponivel nesta sessao para validar visualmente a mudanca, risco alto
 * demais para esta fase. O texto exibido (`ShiftDay.label`) ja vem do
 * catalogo remoto (`TipoTurno.descricao`), so o `type`/cor/icone usam o
 * mapeamento fixo abaixo. Ver relatorio final da FASE 15 para o
 * acompanhamento desta pendencia.
 */
object EscalaIciScheduleMapper {

    fun map(turnosMes: TurnosMesRemoteDto, usuario: UsuarioRemoteDto, catalogo: Map<String, TipoTurnoRemoteDto>): ScheduleSummary {
        val member = Member(
            email = usuario.email,
            scaleName = usuario.nome,
            displayName = usuario.nome,
            id = usuario.login,
            teamId = usuario.equipeId,
            active = usuario.ativo,
        )
        val team = Team(teamId = usuario.equipeId, name = usuario.equipeId, displayName = usuario.equipeId)
        val days = turnosMes.dias.entries
            .sortedBy { it.key }
            .map { (dataIso, dia) -> buildShiftDay(dataIso, dia, catalogo) }

        return ScheduleSummary(
            member = member,
            team = team,
            days = days,
            periodLabel = periodLabel(turnosMes.periodoInicio, turnosMes.periodoFim),
            pauseLabel = "--:--",
            pauseOffsetLabel = "Sem sugestão disponível",
            sourceFileName = "Firebase",
            competencia = turnosMes.competencia,
        )
    }

    private fun buildShiftDay(dataIso: String, dia: DiaRemoteDto, catalogo: Map<String, TipoTurnoRemoteDto>): ShiftDay {
        val date = LabDate.parseIso(dataIso)
        val tipo = catalogo[dia.c]
        val type = shiftTypeFor(dia.c, tipo?.categoria ?: CategoriaTurno.TRABALHO)
        return ShiftDay(
            dayLabel = date?.dayOfWeekShort() ?: "",
            dateLabel = date?.dateLabel() ?: dataIso,
            fullDateLabel = date?.fullDateLabel() ?: dataIso,
            type = type,
            date = date,
            label = tipo?.descricao ?: dia.c,
        )
    }

    /**
     * "Quem trabalha nesse dia" (FASE 16, seção 22) - mesmo mapeamento
     * código/categoria -> [ShiftType] usado em [buildShiftDay], aplicado a
     * toda a equipe (não só ao usuário atual) a partir do
     * [TeamScheduleSnapshot] já carregado por Trocas. Corrige "Equipe não
     * localizada na escala": antes essa seção só existia para escalas
     * importadas de XLS (`ShiftDay.teamMembers`/`membersByShift`, sempre
     * vazios para escalas do Firebase).
     */
    fun quemTrabalhaPorTurno(snapshot: TeamScheduleSnapshot, dataIso: String): Map<ShiftType, List<String>> =
        snapshot.usuariosAtivos
            .mapNotNull { usuario ->
                val jornada = snapshot.jornadaDoDia(usuario.login, dataIso)
                if (!jornada.trabalha) return@mapNotNull null
                val categoria = snapshot.catalogo[jornada.codigo]?.categoria ?: CategoriaTurno.TRABALHO
                shiftTypeFor(jornada.codigo, categoria) to usuario.nome
            }
            .groupBy({ it.first }, { it.second })

    private fun periodLabel(periodoInicio: String, periodoFim: String): String {
        val start = LabDate.parseIso(periodoInicio)
        val end = LabDate.parseIso(periodoFim)
        return if (start != null && end != null) "${start.periodToken()} — ${end.periodToken()}" else "$periodoInicio — $periodoFim"
    }

    /** Mapeamento fixo codigo/categoria -> `ShiftType` existente - ver nota de compromisso na doc da classe. */
    private fun shiftTypeFor(codigo: String, categoria: CategoriaTurno): ShiftType = when (codigo.uppercase()) {
        "MD" -> ShiftType.MADRUGADA
        "M" -> ShiftType.MANHA
        "T" -> ShiftType.TARDE
        "N" -> ShiftType.NOITE
        "X" -> ShiftType.FERIAS
        "DF", "DU", "FOLGA" -> ShiftType.FOLGA
        "BH" -> ShiftType.BH
        "AN" -> ShiftType.ANIVERSARIO
        "HE" -> ShiftType.HORA_EXTRA
        "AFA" -> ShiftType.AFASTAMENTO
        else -> when (categoria) {
            CategoriaTurno.TRABALHO, CategoriaTurno.PLANTAO -> ShiftType.INDEFINIDO
            CategoriaTurno.EXTRA -> ShiftType.HORA_EXTRA
            CategoriaTurno.DESCANSO -> ShiftType.FOLGA
            CategoriaTurno.COMPENSACAO -> ShiftType.BH
            CategoriaTurno.AUSENCIA -> ShiftType.AFASTAMENTO
        }
    }
}
