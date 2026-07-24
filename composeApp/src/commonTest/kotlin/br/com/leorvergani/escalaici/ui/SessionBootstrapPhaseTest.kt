package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.auth.CorporateAuthError
import br.com.leorvergani.escalaici.auth.CorporateAuthState
import br.com.leorvergani.escalaici.auth.CorporateIdentity
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionBootstrapPhaseTest {
    private val sampleIdentity = CorporateIdentity(
        tenantId = "tenant-1",
        objectId = "object-1",
        username = "ana.paula@ici.tec.br",
        displayName = "Ana Paula",
        email = "ana.paula@ici.tec.br",
        accountId = "account-1",
    )

    @Test
    fun notConfiguredIsAlwaysReady() {
        assertEquals(
            SessionBootstrapPhase.READY,
            computeSessionBootstrapPhase(
                hasConfiguredCorporateAuth = false,
                corporateAuthState = null,
                sessionMemberId = null,
                gateErrorMessage = null,
                requestedEntryContext = null,
            ),
        )
    }

    @Test
    fun configuredWithoutStateYetIsInitializing() {
        assertEquals(
            SessionBootstrapPhase.INITIALIZING,
            computeSessionBootstrapPhase(
                hasConfiguredCorporateAuth = true,
                corporateAuthState = null,
                sessionMemberId = null,
                gateErrorMessage = null,
                requestedEntryContext = null,
            ),
        )
    }

    @Test
    fun signedOutFailedAuthenticatingAndDemoAreReady() {
        val states = listOf(
            CorporateAuthState.SignedOut,
            CorporateAuthState.Failed(CorporateAuthError.NetworkError),
            CorporateAuthState.Authenticating,
            CorporateAuthState.Demo,
            CorporateAuthState.NotConfigured,
        )
        states.forEach { state ->
            assertEquals(
                SessionBootstrapPhase.READY,
                computeSessionBootstrapPhase(
                    hasConfiguredCorporateAuth = true,
                    corporateAuthState = state,
                    sessionMemberId = null,
                    gateErrorMessage = null,
                    requestedEntryContext = null,
                ),
            )
        }
    }

    @Test
    fun authenticatedWithoutResolutionYetIsRestoring() {
        assertEquals(
            SessionBootstrapPhase.RESTORING,
            computeSessionBootstrapPhase(
                hasConfiguredCorporateAuth = true,
                corporateAuthState = CorporateAuthState.Authenticated(sampleIdentity),
                sessionMemberId = null,
                gateErrorMessage = null,
                requestedEntryContext = null,
            ),
        )
    }

    @Test
    fun authenticatedWithSessionMemberIdIsReady() {
        assertEquals(
            SessionBootstrapPhase.READY,
            computeSessionBootstrapPhase(
                hasConfiguredCorporateAuth = true,
                corporateAuthState = CorporateAuthState.Authenticated(sampleIdentity),
                sessionMemberId = "member-1",
                gateErrorMessage = null,
                requestedEntryContext = EntryContext.LOGIN,
            ),
        )
    }

    @Test
    fun authenticatedWithGateErrorMessageIsReady() {
        assertEquals(
            SessionBootstrapPhase.READY,
            computeSessionBootstrapPhase(
                hasConfiguredCorporateAuth = true,
                corporateAuthState = CorporateAuthState.Authenticated(sampleIdentity),
                sessionMemberId = null,
                gateErrorMessage = "Cadastro corporativo não localizado na publicação oficial.",
                requestedEntryContext = EntryContext.LOGIN,
            ),
        )
    }

    @Test
    fun authenticatedEnteringDemoIsReadyEvenWithoutSessionMemberId() {
        assertEquals(
            SessionBootstrapPhase.READY,
            computeSessionBootstrapPhase(
                hasConfiguredCorporateAuth = true,
                corporateAuthState = CorporateAuthState.Authenticated(sampleIdentity),
                sessionMemberId = null,
                gateErrorMessage = null,
                requestedEntryContext = EntryContext.DEMO,
            ),
        )
    }
}
