package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.Member
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class IdentityNormalizerTest {

    @Test
    fun normalizeIdentity_trimsAndLowercases() {
        assertEquals("ana.silva@example.invalid", normalizeIdentity("  Ana.Silva@Example.Invalid  "))
    }

    @Test
    fun normalizeIdentity_blankAndNullReturnNull() {
        assertNull(normalizeIdentity("   "))
        assertNull(normalizeIdentity(null))
    }

    @Test
    fun equivalentStringsMatchAfterNormalize() {
        assertEquals(normalizeIdentity(" USER@EXAMPLE.INVALID "), normalizeIdentity("user@example.invalid"))
    }

    @Test
    fun memberDirectory_neverMatchesPartialIdentity() = runTest {
        val repository = InMemoryMemberDirectoryRepository(
            members = listOf(
                Member(
                    email = "ana.silva@example.invalid",
                    scaleName = "ana.silva",
                    displayName = "Ana Silva",
                    id = "member-ana"
                )
            )
        )

        assertEquals(emptyList(), repository.findActiveMemberIds(normalizedEmail = null, normalizedLogin = "ana"))
    }
}
