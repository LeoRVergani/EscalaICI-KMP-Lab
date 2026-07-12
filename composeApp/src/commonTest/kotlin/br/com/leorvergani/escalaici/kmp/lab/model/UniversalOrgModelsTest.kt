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
}
