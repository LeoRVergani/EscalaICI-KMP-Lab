package br.com.leorvergani.escalaici.kmp.lab.model

/**
 * Modelos puros "universais" da FASE 12b, extraidos da spec
 * `EscalaSOC/docs/spec/34-ESCALAICI-UNIVERSAL-SETORES-E-TIPOS-DE-ESCALA.md`.
 *
 * Estendem o modelo existente (`Team`, `Member`, `SchedulePeriod`,
 * `ScheduleAssignment`, `OnCallPeriod`, `OnCallAssignment`, `ShiftType`) para
 * suportar multiplos setores/siglas do ICI (nao so COSI/SOC) e tipos de
 * escala configuraveis por equipe (ex.: codigos M1-M4 do N1). Nao substituem
 * nada existente — nenhum campo/modelo antigo foi removido ou renomeado.
 *
 * Kotlin puro, sem dependencia de Android/JVM/Firebase. Ainda nao conectados
 * a UI nem a nenhum parser/Firestore real (isso fica para fases futuras,
 * FASE 12c em diante).
 */

enum class OrgUnitType {
    PRESIDENCY,
    DIRECTORATE,
    MANAGEMENT,
    COORDINATION,
    SECTOR,
    DEPARTMENT,
    TEAM_GROUP
}

data class Organization(
    val id: String,
    val name: String,
    val acronym: String,
    val active: Boolean = true
)

/**
 * No da arvore de diretorias/gerencias/coordenacoes/setores do ICI (ex.:
 * GEDSI -> COSI). `parentId` nulo marca a raiz de uma diretoria. `TEAM_GROUP`
 * permite representar um agrupamento de equipe (ex.: "Analistas de SOC")
 * dentro da arvore sem precisar alterar o modelo `Team` ja existente.
 */
data class OrgUnit(
    val id: String,
    val organizationId: String,
    val parentId: String? = null,
    val name: String,
    val acronym: String,
    val type: OrgUnitType,
    val active: Boolean = true
)

/**
 * Papel/funcao dentro de uma equipe ou setor (ex.: coordenador, analista,
 * tecnico). `roleShortName`/`roleDisplayName` sao rotulos pensados para
 * exibicao no app (card "Tecnico: Fulano", "Analista: Fulano") — se nulos,
 * caem para `acronym`/`name` (`effectiveShortName`/`effectiveDisplayName`),
 * entao um `Role` criado antes desta fase continua funcionando sem mudanca.
 */
data class Role(
    val id: String,
    val name: String,
    val acronym: String,
    val orgUnitId: String? = null,
    val active: Boolean = true,
    val roleShortName: String? = null,
    val roleDisplayName: String? = null
) {
    val effectiveShortName: String get() = roleShortName ?: acronym
    val effectiveDisplayName: String get() = roleDisplayName ?: name
}

/**
 * Formata "cargo: nome" para exibicao no app (ex.: "Tecnico: Fulano"),
 * respeitando `ScheduleUiConfig.showRoleLabel` — funcao pura, sem nenhuma
 * dependencia de UI, so para validar a regra antes de existir uma tela real
 * que a use (fica para uma fase futura, apos dashboard/Firebase definidos).
 */
fun formatMemberWithRole(memberDisplayName: String, role: Role?, showRoleLabel: Boolean = true): String {
    if (!showRoleLabel || role == null) return memberDisplayName
    return "${role.effectiveShortName}: $memberDisplayName"
}

/**
 * Vinculo entre um membro e uma equipe, com periodo de vigencia — permite
 * que uma pessoa pertenca a mais de uma equipe ao longo do tempo (ou, no
 * futuro, simultaneamente), em vez de ficar presa a um `teamId` fixo em
 * `Member`. `isPrimary` marca qual equipe usar por padrao quando ha mais de
 * um vinculo ativo.
 */
data class MemberTeamMembership(
    val id: String,
    val memberId: String,
    val teamId: String,
    val roleId: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val active: Boolean = true,
    val isPrimary: Boolean = false
)

enum class ScheduleProfileType {
    ROTATING_6X1,
    MATRIX_6X1,
    TWELVE_BY_THIRTY_SIX,
    BUSINESS_HOURS,
    ON_CALL_INTERVAL,
    CUSTOM
}

enum class SchedulePeriodMode {
    DAY_26_TO_25,
    MONTHLY,
    WEEKLY,
    FIXED_RANGE,
    CONTINUOUS
}

/**
 * Configuracao visual de um perfil de escala — controla se o card de
 * atividade/codigo aparece no app para aquela equipe (FASE 12b-2). Cada
 * equipe/perfil decide por conta propria; nao ha nenhum "card do N1"
 * hardcoded, so um card generico que so aparece quando
 * `showActivityCodeCard = true` no perfil daquela equipe.
 */
data class ScheduleUiConfig(
    val showActivityCodeCard: Boolean = false,
    val showRoleLabel: Boolean = true,
    val activityCardTitle: String = "Atividade do dia"
)

/** Tipo estrutural de escala que uma equipe usa (turno fixo, matriz por codigo, 12x36...). */
data class ScheduleProfile(
    val id: String,
    val teamId: String? = null,
    val name: String,
    val type: ScheduleProfileType,
    val periodMode: SchedulePeriodMode,
    val description: String? = null,
    val active: Boolean = true,
    val uiConfig: ScheduleUiConfig = ScheduleUiConfig()
)

enum class ActivityCodeType {
    WORK,
    REST,
    VACATION,
    ABSENCE,
    MONITORING,
    EMAIL,
    WARRANTY,
    MIXED,
    ON_CALL,
    CUSTOM
}

/**
 * Codigo de atividade configuravel por equipe (ex.: `F`, `X`, `AUS`, `M1`-
 * `M4`, `E`, `G`, `T` da equipe N1) — em vez de um enum fixo tipo `ShiftType`
 * para todas as equipes, cada equipe cadastra os proprios codigos.
 * `countsAsWork` e o que faz o app saber se um dia conta como trabalho sem
 * precisar entender o significado especifico do codigo. `visibleInApp`/
 * `cardTitle`/`sortOrder` (FASE 12b-2) sao so metadados de exibicao do card
 * configuravel — nao afetam `countsAsWork` nem nenhuma regra de negocio.
 */
data class ActivityCode(
    val id: String,
    val teamId: String? = null,
    val scheduleProfileId: String? = null,
    val code: String,
    val label: String,
    val type: ActivityCodeType,
    val countsAsWork: Boolean,
    val colorHex: String? = null,
    val active: Boolean = true,
    val visibleInApp: Boolean = true,
    val cardTitle: String = "Atividade do dia",
    val sortOrder: Int = 0
)

/** Regra de horario administrativo (ex.: segunda a sexta, 08:00-18:00 com 2h de almoco). */
data class BusinessHoursRule(
    val id: String,
    val scheduleProfileId: String,
    /** Dias uteis, 1=segunda .. 7=domingo (ISO-8601 DayOfWeek). */
    val workDays: List<Int>,
    val startTime: String,
    val endTime: String,
    val breakMinutes: Int
)

/** Regra de rodizio 12x36 (12h trabalhadas, 36h de folga, alternando). */
data class TwelveByThirtySixRule(
    val id: String,
    val scheduleProfileId: String,
    val startTime: String,
    val endTime: String,
    val workHours: Int,
    val restHours: Int
)
