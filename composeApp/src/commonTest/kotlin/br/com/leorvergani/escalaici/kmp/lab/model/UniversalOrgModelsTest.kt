package br.com.leorvergani.escalaici.kmp.lab.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UniversalOrgModelsTest {

    @Test
    fun activityCode_m1ToM4CountAsWork() {
        val codes = mockActivityCodesN1().associateBy { it.code }

        assertTrue(codes.getValue("M1").countsAsWork)
        assertTrue(codes.getValue("M2").countsAsWork)
        assertTrue(codes.getValue("M3").countsAsWork)
        assertTrue(codes.getValue("M4").countsAsWork)
    }

    @Test
    fun activityCode_folgaAndFeriasDoNotCountAsWork() {
        val codes = mockActivityCodesN1().associateBy { it.code }

        assertFalse(codes.getValue("F").countsAsWork)
        assertFalse(codes.getValue("X").countsAsWork)
    }

    @Test
    fun orgHierarchy_representsIciGedsiCosiSoc() {
        val ici = mockOrganizationIci()
        val gedsi = mockOrgUnitGedsi()
        val cosi = mockOrgUnitCosi()
        val soc = mockTeamSoc()

        assertEquals(ici.id, gedsi.organizationId)
        assertEquals(ici.id, cosi.organizationId)
        assertEquals(gedsi.id, cosi.parentId)
        assertEquals(OrgUnitType.MANAGEMENT, gedsi.type)
        assertEquals(OrgUnitType.COORDINATION, cosi.type)
        assertEquals("soc", soc.teamId)
    }

    @Test
    fun orgUnitN1_hasNoHierarchicalRelationToCosi() {
        val cosi = mockOrgUnitCosi()
        val n1 = mockOrgUnitN1()

        assertFalse(n1.parentId == cosi.id)
        assertEquals(OrgUnitType.SECTOR, n1.type)
    }

    @Test
    fun memberTeamMembership_linksMemberToTeamRatherThanFixedField() {
        val memberships = mockMemberTeamMemberships()
        val lverganiMembership = memberships.first { it.memberId == "lvergani@ici.tec.br" }

        assertEquals("soc", lverganiMembership.teamId)
        assertTrue(lverganiMembership.isPrimary)
    }

    @Test
    fun scheduleUiConfig_n1EnablesActivityCodeCard() {
        val n1Profile = mockScheduleProfiles().first { it.id == "profile-n1-matrix" }

        assertTrue(n1Profile.uiConfig.showActivityCodeCard)
        assertEquals("Atividade do dia", n1Profile.uiConfig.activityCardTitle)
    }

    @Test
    fun scheduleUiConfig_socDoesNotEnableActivityCodeCard() {
        val socProfile = mockScheduleProfiles().first { it.id == "profile-soc-6x1" }
        val adminProfile = mockScheduleProfiles().first { it.id == "profile-administrativo" }

        assertFalse(socProfile.uiConfig.showActivityCodeCard)
        assertFalse(adminProfile.uiConfig.showActivityCodeCard)
    }

    @Test
    fun activityCode_m1ToM4BelongOnlyToN1Profile() {
        val codes = mockActivityCodesN1().associateBy { it.code }

        assertEquals("profile-n1-matrix", codes.getValue("M1").scheduleProfileId)
        assertEquals("profile-n1-matrix", codes.getValue("M2").scheduleProfileId)
        assertEquals("profile-n1-matrix", codes.getValue("M3").scheduleProfileId)
        assertEquals("profile-n1-matrix", codes.getValue("M4").scheduleProfileId)
        codes.values.forEach { code ->
            assertEquals("n1", code.teamId)
        }
    }

    @Test
    fun activityCode_m1ToM4DoNotBelongToSocProfile() {
        val socProfileId = mockScheduleProfiles().first { it.id == "profile-soc-6x1" }.id
        val codes = mockActivityCodesN1().associateBy { it.code }

        listOf("M1", "M2", "M3", "M4", "E", "G", "T", "F", "X", "AUS").forEach { code ->
            assertFalse(codes.getValue(code).scheduleProfileId == socProfileId)
            assertFalse(codes.getValue(code).teamId == "soc")
        }
    }

    @Test
    fun formatMemberWithRole_showsShortRoleBeforeName() {
        val tecnico = mockRoles().first { it.id == "role-n1-tecnico" }
        val analista = mockRoles().first { it.id == "role-analyst" }

        assertEquals("Técnico: Douglas", formatMemberWithRole("Douglas", tecnico, showRoleLabel = true))
        assertEquals("Analista: Leonardo", formatMemberWithRole("Leonardo", analista, showRoleLabel = true))
    }

    @Test
    fun formatMemberWithRole_hidesRoleWhenShowRoleLabelIsFalse() {
        val tecnico = mockRoles().first { it.id == "role-n1-tecnico" }

        assertEquals("Douglas", formatMemberWithRole("Douglas", tecnico, showRoleLabel = false))
        assertEquals("Douglas", formatMemberWithRole("Douglas", role = null, showRoleLabel = true))
    }
}
