package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.ScheduledNotification

data class NotificationReconciliation(
    val idsToCancel: Set<String>,
    val idsToSchedule: Set<String>,
    val idsKept: Set<String>
)

fun notificationRequestCodeFor(id: String): Int = id.hashCode()

fun reconcileNotificationIds(
    previousIds: Set<String>,
    nextIds: Set<String>
): NotificationReconciliation =
    NotificationReconciliation(
        idsToCancel = previousIds - nextIds,
        idsToSchedule = nextIds,
        idsKept = previousIds intersect nextIds
    )

fun shouldRequestPostNotifications(
    sdkInt: Int,
    permissionGranted: Boolean,
    alreadyRequested: Boolean
): Boolean = sdkInt >= 33 && !permissionGranted && !alreadyRequested

interface LocalNotificationRuntime {
    suspend fun reconcile(plan: List<ScheduledNotification>): LocalNotificationResult
}

sealed interface LocalNotificationResult {
    data class Applied(val scheduledCount: Int, val cancelledCount: Int) : LocalNotificationResult
    data class Skipped(val reason: String) : LocalNotificationResult
}

object NoOpLocalNotificationRuntime : LocalNotificationRuntime {
    override suspend fun reconcile(plan: List<ScheduledNotification>): LocalNotificationResult =
        LocalNotificationResult.Skipped("Agendamento local indisponível nesta plataforma.")
}
