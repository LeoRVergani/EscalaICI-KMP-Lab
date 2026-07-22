package br.com.leorvergani.escalaici.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleSyncCauseTest {
    @Test fun classifiesPermissionDeniedFromMessageText() {
        assertEquals(ScheduleSyncCause.PERMISSION_DENIED, classifySyncFailure(RuntimeException("PERMISSION_DENIED: Missing or insufficient permissions.")))
        assertEquals(ScheduleSyncCause.PERMISSION_DENIED, classifySyncFailure(RuntimeException("403 Forbidden")))
    }

    @Test fun classifiesDisabledFirestoreApiBeforeGeneric403() {
        assertEquals(
            ScheduleSyncCause.FIRESTORE_DATABASE_DISABLED,
            classifySyncFailure(RuntimeException("Firestore indisponível (403). Cloud Firestore API has not been used in project escala-ici-dev before or it is disabled. reason: SERVICE_DISABLED"))
        )
    }

    @Test fun classifiesMissingConfigAsAuthRequiredWithoutFirestoreDisabledMessage() {
        assertEquals(ScheduleSyncCause.AUTH_REQUIRED, classifySyncFailure(RuntimeException("Firebase not configured.")))
    }

    @Test fun classifiesNetworkErrorFromMessageText() {
        assertEquals(ScheduleSyncCause.NETWORK_ERROR, classifySyncFailure(RuntimeException("UNAVAILABLE: The service is currently unavailable.")))
        assertEquals(ScheduleSyncCause.NETWORK_ERROR, classifySyncFailure(RuntimeException("Unable to resolve host (UnknownHostException)")))
        assertEquals(ScheduleSyncCause.NETWORK_ERROR, classifySyncFailure(RuntimeException("Connection timed out")))
    }

    @Test fun classifiesInvalidRemoteDataFromMessageText() {
        assertEquals(ScheduleSyncCause.INVALID_REMOTE_DATA, classifySyncFailure(RuntimeException("Invalid document format")))
    }

    @Test fun classifiesRealPortugueseGatewayMessages() {
        // Mensagens reais lançadas por FirestoreRestGateway.kt — não são só exemplos em inglês.
        assertEquals(ScheduleSyncCause.NETWORK_ERROR, classifySyncFailure(RuntimeException("Firestore indisponível (503).")))
    }

    @Test fun classifies404OnWorkspacePointerAsNotPublishedInsteadOfNetworkError() {
        // Bug real observado em producao (Web, apos MSAL real): workspaces/ici-dev ainda nao
        // publicado retorna 404, e a mensagem generica "Firestore indisponivel (404) em
        // workspaces/ici-dev. ..." contem a palavra "indisponivel", que por coincidencia e
        // um marcador de NETWORK_ERROR - levando a exibir "verifique sua internet" para um
        // caso que na verdade e "nenhuma publicacao existe ainda".
        assertEquals(
            ScheduleSyncCause.WORKSPACE_NOT_PUBLISHED,
            classifySyncFailure(RuntimeException("Firestore indisponível (404) em workspaces/ici-dev. {\"error\":{\"code\":404}}"))
        )
        assertEquals(
            ScheduleSyncCause.WORKSPACE_NOT_PUBLISHED,
            classifySyncFailure(RuntimeException("Firestore indisponível (404) em workspaces/demo-v1. {\"error\":{\"code\":404}}"))
        )
        // Um 404 fora do caminho workspaces/ (ex.: um subcaminho qualquer sem essa palavra)
        // nao deve ser reclassificado por engano.
        assertEquals(ScheduleSyncCause.NETWORK_ERROR, classifySyncFailure(RuntimeException("Firestore indisponível (404) em teams/soc.")))
    }

    @Test fun fallsBackToUnknownWhenNothingMatches() {
        assertEquals(ScheduleSyncCause.UNKNOWN, classifySyncFailure(RuntimeException("Assignment duplicado.")))
        assertEquals(ScheduleSyncCause.UNKNOWN, classifySyncFailure(IllegalStateException()))
    }

    @Test fun emptyStateCausesAreNotTreatedAsFailures() {
        assertTrue(ScheduleSyncCause.TEAM_NOT_FOUND.isEmptyState())
        assertTrue(ScheduleSyncCause.NO_ACTIVE_PERIOD.isEmptyState())
        assertTrue(ScheduleSyncCause.NO_ASSIGNMENTS.isEmptyState())
        assertTrue(ScheduleSyncCause.WORKSPACE_NOT_PUBLISHED.isEmptyState())
        assertFalse(ScheduleSyncCause.NETWORK_ERROR.isEmptyState())
        assertFalse(ScheduleSyncCause.PERMISSION_DENIED.isEmptyState())
        assertFalse(ScheduleSyncCause.FIRESTORE_DATABASE_DISABLED.isEmptyState())
        assertFalse(ScheduleSyncCause.UNKNOWN.isEmptyState())
    }

    @Test fun defaultMessageIncludesTeamIdForTeamNotFound() {
        assertTrue(ScheduleSyncCause.TEAM_NOT_FOUND.defaultMessage("soc").contains("soc"))
        assertTrue(ScheduleSyncCause.TEAM_NOT_FOUND.defaultMessage(null).isNotBlank())
    }
}
