package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.Member

/**
 * FASE 14J.1 (spec 68) - "Visualizar como colaborador": um administrador autorizado troca o
 * CONTEXTO DE APRESENTACAO do app para o de outro membro da mesma equipe/publicacao já
 * carregada, sem nunca trocar a conta Microsoft autenticada, sem criar segunda autenticacao e
 * sem executar nenhuma escrita em nome do membro visualizado.
 *
 * Modelo de identidade: `authenticatedMemberId` é sempre o membro resolvido pelo login MSAL real
 * - autorização e qualquer ação sensível (hoje nenhuma escrita real existe no app, mas a regra
 * vale para o futuro) SEMPRE usam esse valor, nunca `viewedMemberId`. `effectiveMemberId` é
 * exclusivamente para APRESENTAÇÃO (qual `ScheduleSummary` mostrar nas abas).
 */
data class ViewAsIdentityContext(
    val authenticatedMemberId: String,
    val viewedMemberId: String?
) {
    val effectiveMemberId: String get() = viewedMemberId ?: authenticatedMemberId
    val isViewingOther: Boolean get() = viewedMemberId != null
}

/** Um colaborador selecionável na lista "Visualizar como colaborador". */
data class ViewableCollaborator(
    val memberId: String,
    val displayName: String,
    val corporateLogin: String?,
    val teamId: String,
    val teamName: String
)

/**
 * Converte o roster de uma equipe (já filtrado por membro ativo e escopado à revisão/workspace
 * atual por quem chama - `RemoteFirstDemoMemberRepository.getMembersByTeam`) na lista
 * apresentável, ordenada alfabeticamente. Nunca inclui o próprio administrador autenticado -
 * "visualizar como" a própria conta não faz sentido (já é a visualização real).
 */
fun collaboratorOptionsForViewAs(
    members: List<Member>,
    teamId: String,
    teamName: String,
    excludeMemberId: String
): List<ViewableCollaborator> =
    members
        .asSequence()
        .filter { it.active }
        .filter { it.id != excludeMemberId }
        .map { ViewableCollaborator(it.id, it.displayName, it.corporateLogin, teamId, teamName) }
        .sortedBy { it.displayName.lowercase() }
        .toList()

/** Busca por nome, login corporativo ou equipe - nunca expõe e-mail completo. */
fun filterViewAsCollaborators(collaborators: List<ViewableCollaborator>, query: String): List<ViewableCollaborator> {
    val normalized = query.trim().lowercase()
    if (normalized.isEmpty()) return collaborators
    return collaborators.filter { collaborator ->
        collaborator.displayName.lowercase().contains(normalized) ||
            collaborator.corporateLogin?.lowercase()?.contains(normalized) == true ||
            collaborator.teamName.lowercase().contains(normalized)
    }
}

/**
 * Decisão de domínio explícita para quem pode ver a função - reaproveita a MESMA autorização
 * administrativa já usada para o Ambiente Demo (`demoAccessGranted`, calculada via
 * `isDemoAuthorizedForIdentity`/claims do backend), nunca uma comparação de e-mail nova.
 */
fun isViewAsMemberAuthorized(administrativeAccessGranted: Boolean): Boolean = administrativeAccessGranted

/** Retrato do contexto de publicação no instante em que uma persona foi selecionada. */
data class ViewAsPublicationContext(
    val workspaceId: String?,
    val teamId: String?,
    val publicationRevision: Int?
)

/**
 * Verificação rápida (sem round-trip de rede) de que o contexto mudou o suficiente para encerrar
 * a visualização administrativa - troca de workspace, equipe ou revisão da publicação. A checagem
 * de "membro ainda presente" é feita à parte (rebuscando o resumo do membro visualizado; um
 * resultado nulo também encerra a visualização - ver `EscalaIciLabApp`).
 */
fun shouldClearViewAsOnPublicationChange(
    viewedMemberId: String?,
    previous: ViewAsPublicationContext,
    current: ViewAsPublicationContext
): Boolean {
    if (viewedMemberId == null) return false
    return previous.workspaceId != current.workspaceId ||
        previous.teamId != current.teamId ||
        previous.publicationRevision != current.publicationRevision
}
