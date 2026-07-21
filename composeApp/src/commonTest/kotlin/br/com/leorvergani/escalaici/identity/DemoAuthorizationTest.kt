package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.auth.CorporateIdentity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoAuthorizationTest {
    @Test
    fun temporaryDeveloperEmailFallbackAuthorizesExactNormalizedEmailInDevelopmentBuild() = runTest {
        val authorized = isDemoAuthorizedForIdentity(
            identity = identity(email = " LVERGANI@ICI.TEC.BR "),
            isDevelopmentBuild = true,
            allowedDeveloperObjectIdsProvider = { error("Firestore pointer unavailable") }
        )

        assertTrue(authorized)
    }

    @Test
    fun temporaryDeveloperEmailFallbackRejectsOtherEmailsInDevelopmentBuild() = runTest {
        val authorized = isDemoAuthorizedForIdentity(
            identity = identity(email = "outra.pessoa@ici.tec.br"),
            isDevelopmentBuild = true,
            allowedDeveloperObjectIdsProvider = { emptyList() }
        )

        assertFalse(authorized)
    }

    @Test
    fun allowedDeveloperObjectIdAuthorizesOutsideDevelopmentBuildWithoutTemporaryEmail() = runTest {
        val authorized = isDemoAuthorizedForIdentity(
            identity = identity(email = "outra.pessoa@ici.tec.br", objectId = "object-allowed"),
            isDevelopmentBuild = false,
            allowedDeveloperObjectIdsProvider = { listOf("other-object", "object-allowed") }
        )

        assertTrue(authorized)
    }

    @Test
    fun temporaryDeveloperEmailFallbackDoesNotApplyOutsideDevelopmentBuild() = runTest {
        val authorized = isDemoAuthorizedForIdentity(
            identity = identity(email = "lvergani@ici.tec.br"),
            isDevelopmentBuild = false,
            allowedDeveloperObjectIdsProvider = { error("Firestore pointer unavailable") }
        )

        assertFalse(authorized)
    }

    private fun identity(
        email: String?,
        objectId: String = "object-not-allowed"
    ) = CorporateIdentity(
        tenantId = "tenant",
        objectId = objectId,
        username = "user",
        displayName = "User",
        email = email,
        accountId = "account"
    )
}
