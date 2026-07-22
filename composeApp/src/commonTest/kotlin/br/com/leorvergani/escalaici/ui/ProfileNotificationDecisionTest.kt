package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.NotificationSettings
import br.com.leorvergani.escalaici.model.ScheduleSummary
import br.com.leorvergani.escalaici.model.ShiftDay
import br.com.leorvergani.escalaici.model.ShiftType
import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.Team
import br.com.leorvergani.escalaici.model.relevantShift
import br.com.leorvergani.escalaici.platform.NotificationPermissionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProfileNotificationDecisionTest {
    @Test
    fun pauseToggleReflectsRealSettingsInsteadOfFixedTrue() {
        val shift = workSummary().relevantShift(LabDateTime(LabDate(2026, 7, 23), 8 * 60))

        val enabledDecision = decideProfilePauseNotification(
            settings = NotificationSettings(notifyPause = true),
            relevantShift = shift,
            notificationPermission = NotificationPermissionState.GRANTED,
            requiresNotificationPermission = true
        )
        val disabledDecision = decideProfilePauseNotification(
            settings = NotificationSettings(notifyPause = false),
            relevantShift = shift,
            notificationPermission = NotificationPermissionState.GRANTED,
            requiresNotificationPermission = true
        )

        assertTrue(enabledDecision.checked)
        assertFalse(disabledDecision.checked)
    }

    @Test
    fun disabledPauseToggleShowsExplicitReason() {
        val decision = decideProfilePauseNotification(
            settings = NotificationSettings(),
            relevantShift = null,
            notificationPermission = NotificationPermissionState.GRANTED,
            requiresNotificationPermission = true
        )

        assertFalse(decision.enabled)
        assertNotNull(decision.disabledReason)
        assertEquals("Nenhum turno ativo para agendar pausa.", decision.disabledReason)
    }

    private fun workSummary() = ScheduleSummary(
        member = Member("teste@example.invalid", "teste", "Pessoa Teste"),
        team = Team("teste", "Equipe Teste"),
        days = listOf(
            ShiftDay(
                dayLabel = "Qui",
                dateLabel = "23/07",
                fullDateLabel = "quinta-feira, 23/07/2026",
                type = ShiftType.MANHA,
                date = LabDate(2026, 7, 23)
            )
        ),
        periodLabel = "",
        pauseLabel = "",
        pauseOffsetLabel = ""
    )
}
