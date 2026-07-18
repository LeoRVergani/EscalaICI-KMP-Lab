package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.MemberRole
import br.com.leorvergani.escalaici.model.MemberTeamMembership
import br.com.leorvergani.escalaici.model.Team

object DemoOrganizationData {
    private const val WORKSPACE_ID = OrganizationWorkspace.DEMO_WORKSPACE_ID

    val members: List<Member> = DemoPersonaCatalog.personas.map { persona ->
        Member(
            email = persona.fictitiousEmail,
            scaleName = persona.displayName,
            displayName = persona.displayName,
            id = persona.memberId,
            role = MemberRole.ANALYST,
            active = true,
            workspaceId = WORKSPACE_ID
        )
    }

    val teams: List<Team> = listOf(
        Team(
            teamId = "team-demo-soc",
            name = "SOC Demonstração",
            displayName = "SOC Demonstração",
            members = members.filter { it.id == "member-demo-soc-01" || it.id == "member-demo-gestor-seguranca" },
            workspaceId = WORKSPACE_ID
        ),
        Team(
            teamId = "team-demo-seguranca",
            name = "Segurança da Informação Demonstração",
            displayName = "Segurança da Informação Demonstração",
            members = members.filter { it.id == "member-demo-seguranca-01" || it.id == "member-demo-gestor-seguranca" },
            workspaceId = WORKSPACE_ID
        )
    )

    val memberships: List<MemberTeamMembership> = listOf(
        MemberTeamMembership(
            id = "membership-demo-soc-01",
            memberId = "member-demo-soc-01",
            teamId = "team-demo-soc",
            roleId = null,
            startDate = "2020-01-01",
            active = true,
            isPrimary = true,
            workspaceId = WORKSPACE_ID
        ),
        MemberTeamMembership(
            id = "membership-demo-seguranca-01",
            memberId = "member-demo-seguranca-01",
            teamId = "team-demo-seguranca",
            roleId = null,
            startDate = "2020-01-01",
            active = true,
            isPrimary = true,
            workspaceId = WORKSPACE_ID
        ),
        // Sem primary de propósito: o gestor testa a saída MultipleActiveTeams.
        MemberTeamMembership(
            id = "membership-demo-gestor-soc",
            memberId = "member-demo-gestor-seguranca",
            teamId = "team-demo-soc",
            roleId = null,
            startDate = "2020-01-01",
            active = true,
            isPrimary = false,
            workspaceId = WORKSPACE_ID
        ),
        MemberTeamMembership(
            id = "membership-demo-gestor-seguranca",
            memberId = "member-demo-gestor-seguranca",
            teamId = "team-demo-seguranca",
            roleId = null,
            startDate = "2020-01-01",
            active = true,
            isPrimary = false,
            workspaceId = WORKSPACE_ID
        )
    )
}
