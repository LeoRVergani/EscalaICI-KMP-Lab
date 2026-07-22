package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.ScheduledNotification
import br.com.leorvergani.escalaici.model.plusMinutes

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

fun schedulableWithinHorizon(
    plan: List<ScheduledNotification>,
    now: LabDateTime,
    horizonMinutes: Int
): List<ScheduledNotification> {
    val horizonEnd = now.plusMinutes(horizonMinutes)
    return plan.filter { it.triggerAt >= now && it.triggerAt <= horizonEnd }
}

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
