package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.NotificationSettings
import br.com.leorvergani.escalaici.model.ShiftOccurrence
import br.com.leorvergani.escalaici.model.pauseFor
import br.com.leorvergani.escalaici.platform.NotificationPermissionState

data class ProfilePauseNotificationDecision(
    val checked: Boolean,
    val enabled: Boolean,
    val disabledReason: String?
)

fun decideProfilePauseNotification(
    settings: NotificationSettings,
    relevantShift: ShiftOccurrence?,
    notificationPermission: NotificationPermissionState,
    requiresNotificationPermission: Boolean
): ProfilePauseNotificationDecision {
    val reason = when {
        relevantShift == null -> "Nenhum turno ativo para agendar pausa."
        pauseFor(relevantShift) == null -> "A pausa só pode ser marcada entre a janela permitida do turno."
        requiresNotificationPermission && notificationPermission == NotificationPermissionState.DENIED ->
            "Ative as notificações para agendar lembretes de pausa."
        requiresNotificationPermission && notificationPermission == NotificationPermissionState.UNSUPPORTED ->
            "Este navegador não oferece notificações compatíveis."
        else -> null
    }
    return ProfilePauseNotificationDecision(
        checked = settings.notifyPause,
        enabled = reason == null,
        disabledReason = reason
    )
}
