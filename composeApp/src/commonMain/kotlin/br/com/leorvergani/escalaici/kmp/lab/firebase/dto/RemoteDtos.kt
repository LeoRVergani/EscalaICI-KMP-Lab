package br.com.leorvergani.escalaici.kmp.lab.firebase.dto

import kotlinx.serialization.Serializable

/**
 * DTOs do contrato remoto do Escala-ICI (`packages/contrato/src/tipos.ts`,
 * `lib/modelos.ts`, confirmados via `git show origin/main:...` - ver
 * docs/spec/FASE-15-FIREBASE-UNIFICADO.md). Nao acoplar a UI a estes tipos
 * diretamente - passam por `EscalaIciScheduleMapper` antes de chegar ao
 * `ScheduleSummary` usado pela UI. `@Serializable` porque tambem sao
 * persistidos no cache offline (`EscalaIciScheduleCache`).
 */

@Serializable
data class DiaRemoteDto(
    val c: String,
    val i: String? = null,
    val f: String? = null,
    val m: Int? = null,
    val vd: Boolean? = null,
    val seq: Int? = null,
)

@Serializable
data class TotaisRemoteDto(
    val min: Int = 0,
    val diasTrabalhados: Int = 0,
    val df: Int = 0,
    val du: Int = 0,
    val x: Int = 0,
    val he: Int = 0,
    val bh: Int = 0,
    val an: Int = 0,
    val folga: Int = 0,
    val afa: Int = 0,
)

/** Status real: so `RASCUNHO`/`PUBLICADA` existem no contrato - `RASCUNHO` nunca deve ser mostrado ao colaborador comum. */
@Serializable
enum class TurnosMesStatus { RASCUNHO, PUBLICADA }

@Serializable
data class TurnosMesRemoteDto(
    val schemaVersion: Int,
    /** Legado: hoje contem o mesmo valor de [login], nao um UID real (ver auditoria da FASE 15). Mantido so para fidelidade do documento. */
    val usuarioUid: String,
    val login: String,
    val equipeId: String,
    val competencia: String,
    val periodoInicio: String,
    val periodoFim: String,
    val turnoPadrao: String,
    val status: TurnosMesStatus,
    val dias: Map<String, DiaRemoteDto>,
    val totais: TotaisRemoteDto,
    val importacaoId: String? = null,
    val publicadoPor: String? = null,
    val publicadoEm: String? = null,
    val atualizadoEm: String? = null,
)

@Serializable
enum class CategoriaTurno { TRABALHO, PLANTAO, EXTRA, DESCANSO, COMPENSACAO, AUSENCIA }

@Serializable
data class TipoTurnoRemoteDto(
    val codigo: String,
    val descricao: String,
    val categoria: CategoriaTurno,
    val horaInicio: String? = null,
    val horaFim: String? = null,
    val duracaoMinutos: Int,
    val viraDia: Boolean,
    val contaComoPlantao: Boolean,
    val pesoPlantao: Int,
    val corHex: String,
    val aliasesXLS: List<String> = emptyList(),
)

@Serializable
data class UsuarioRemoteDto(
    /** Chave real - ID do documento `usuarios/{login}`, nao um UID Firebase (ver auditoria FASE 15). */
    val login: String,
    val uid: String? = null,
    val loginAliases: List<String> = emptyList(),
    val nome: String,
    val email: String,
    val cargo: String,
    val equipeId: String,
    val gestorUid: String? = null,
    val nivelHierarquico: Int,
    val turnoPadrao: String,
    val ativo: Boolean,
)
