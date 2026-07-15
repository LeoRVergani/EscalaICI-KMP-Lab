package br.com.leorvergani.escalaici.kmp.lab.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleSyncCauseTest {
    @Test fun classifiesPermissionDeniedFromMessageText() {
        assertEquals(ScheduleSyncCause.PERMISSION_DENIED, classifySyncFailure(RuntimeException("PERMISSION_DENIED: Missing or insufficient permissions.")))
        assertEquals(ScheduleSyncCause.PERMISSION_DENIED, classifySyncFailure(RuntimeException("403 Forbidden")))
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

    @Test fun fallsBackToUnknownWhenNothingMatches() {
        assertEquals(ScheduleSyncCause.UNKNOWN, classifySyncFailure(RuntimeException("Assignment duplicado.")))
        assertEquals(ScheduleSyncCause.UNKNOWN, classifySyncFailure(IllegalStateException()))
    }

    @Test fun emptyStateCausesAreNotTreatedAsFailures() {
        assertTrue(ScheduleSyncCause.TEAM_NOT_FOUND.isEmptyState())
        assertTrue(ScheduleSyncCause.NO_ACTIVE_PERIOD.isEmptyState())
        assertTrue(ScheduleSyncCause.NO_ASSIGNMENTS.isEmptyState())
        assertFalse(ScheduleSyncCause.NETWORK_ERROR.isEmptyState())
        assertFalse(ScheduleSyncCause.PERMISSION_DENIED.isEmptyState())
        assertFalse(ScheduleSyncCause.UNKNOWN.isEmptyState())
    }

    @Test fun defaultMessageIncludesTeamIdForTeamNotFound() {
        assertTrue(ScheduleSyncCause.TEAM_NOT_FOUND.defaultMessage("soc").contains("soc"))
        assertTrue(ScheduleSyncCause.TEAM_NOT_FOUND.defaultMessage(null).isNotBlank())
    }
}
