package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.MemberTeamMembership
import br.com.leorvergani.escalaici.model.Team
import br.com.leorvergani.escalaici.repository.MemberRepository
import br.com.leorvergani.escalaici.repository.TeamRepository

interface MemberDirectoryRepository {
    suspend fun findActiveMemberIds(normalizedEmail: String?, normalizedLogin: String?): List<String>
}

interface MembershipRepository {
    suspend fun getMemberships(memberId: String): List<MemberTeamMembership>
}

class InMemoryMemberDirectoryRepository(
    private val members: List<Member>,
    private val workspaceId: String = OrganizationWorkspace.CORPORATE_WORKSPACE_ID,
    private val loginByMemberId: Map<String, String> = emptyMap()
) : MemberDirectoryRepository {
    // Nao filtra por `active` aqui de proposito: um membro inativo que bate
    // exatamente na identidade ainda precisa ser retornado como candidato,
    // para que o resolver (nao este diretorio) classifique corretamente como
    // MemberInactive em vez de MemberNotFound (achado da revisao independente
    // desta fase - filtrar aqui tornava MemberInactive inalcancavel).
    override suspend fun findActiveMemberIds(normalizedEmail: String?, normalizedLogin: String?): List<String> {
        if (normalizedEmail == null && normalizedLogin == null) return emptyList()
        return members
            .asSequence()
            .filter { it.workspaceId == null || it.workspaceId == workspaceId }
            .filter { member ->
                val memberEmail = normalizeIdentity(member.email)
                val memberLogin = normalizeIdentity(loginByMemberId[member.id] ?: member.scaleName)
                (normalizedEmail != null && memberEmail == normalizedEmail) ||
                    (normalizedLogin != null && memberLogin == normalizedLogin)
            }
            .map { it.id }
            .distinct()
            .toList()
    }
}

class InMemoryMembershipRepository(
    private val memberships: List<MemberTeamMembership> = emptyList()
) : MembershipRepository {
    override suspend fun getMemberships(memberId: String): List<MemberTeamMembership> =
        memberships.filter { it.memberId == memberId }
}

class InMemoryMemberRepository(
    private val members: List<Member>
) : MemberRepository {
    override suspend fun getMember(memberId: String): Member? =
        members.firstOrNull { it.id == memberId }

    override suspend fun getMembersByTeam(teamId: String): List<Member> =
        members.filter { it.teamId == teamId }
}

class InMemoryTeamRepository(
    private val teams: List<Team>
) : TeamRepository {
    override suspend fun getTeam(teamId: String): Team? =
        teams.firstOrNull { it.teamId == teamId }

    override suspend fun getTeams(): List<Team> = teams
}

class DemoMemberDirectoryRepository : MemberDirectoryRepository {
    override suspend fun findActiveMemberIds(normalizedEmail: String?, normalizedLogin: String?): List<String> {
        val pkg = DemoFixtureCache.get()
        return InMemoryMemberDirectoryRepository(
            members = pkg.toMembers(),
            workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
            loginByMemberId = pkg.loginByMemberId()
        ).findActiveMemberIds(normalizedEmail, normalizedLogin)
    }
}

class DemoMembershipRepository : MembershipRepository {
    override suspend fun getMemberships(memberId: String): List<MemberTeamMembership> {
        val pkg = DemoFixtureCache.get()
        return InMemoryMembershipRepository(pkg.toMemberships()).getMemberships(memberId)
    }
}

class DemoMemberRepository : MemberRepository {
    override suspend fun getMember(memberId: String): Member? {
        val pkg = DemoFixtureCache.get()
        return InMemoryMemberRepository(pkg.toMembers()).getMember(memberId)
    }

    override suspend fun getMembersByTeam(teamId: String): List<Member> {
        val pkg = DemoFixtureCache.get()
        return InMemoryMemberRepository(pkg.toMembers()).getMembersByTeam(teamId)
    }
}

class DemoTeamRepository : TeamRepository {
    override suspend fun getTeam(teamId: String): Team? {
        val pkg = DemoFixtureCache.get()
        return InMemoryTeamRepository(pkg.toTeams()).getTeam(teamId)
    }

    override suspend fun getTeams(): List<Team> {
        val pkg = DemoFixtureCache.get()
        return InMemoryTeamRepository(pkg.toTeams()).getTeams()
    }
}
