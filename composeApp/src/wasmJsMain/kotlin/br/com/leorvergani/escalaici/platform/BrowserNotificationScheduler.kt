package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.ScheduledNotification
import br.com.leorvergani.escalaici.model.isoLabel

class BrowserNotificationScheduler(
    private val horizonMinutes: Int = BrowserScheduleHorizonMinutes,
    reevaluationIntervalMinutes: Int = BrowserScheduleReevaluationIntervalMinutes
) : LocalNotificationRuntime {
    private val activeTimeouts = mutableMapOf<String, Int>()
    private var latestPlan: List<ScheduledNotification> = emptyList()

    init {
        setIntervalHandle(
            callback = { reevaluateLatestPlan() },
            intervalMs = reevaluationIntervalMinutes * 60 * 1000
        )
    }

    override suspend fun reconcile(plan: List<ScheduledNotification>): LocalNotificationResult {
        latestPlan = plan
        val previousIds = activeTimeouts.keys.toSet()
        val nextById = plan.associateBy { it.id }
        val reconciliation = reconcileNotificationIds(previousIds, nextById.keys)

        reconciliation.idsToCancel.forEach(::cancel)
        val armedCount = armCurrentHorizon(replaceExisting = true)

        return LocalNotificationResult.Applied(
            scheduledCount = armedCount,
            cancelledCount = reconciliation.idsToCancel.size
        )
    }

    private fun reevaluateLatestPlan() {
        val now = WebCurrentTimeProvider.now()
        latestPlan = latestPlan.filter { it.triggerAt >= now }
        armCurrentHorizon(replaceExisting = false, now = now)
    }

    private fun armCurrentHorizon(
        replaceExisting: Boolean,
        now: LabDateTime = WebCurrentTimeProvider.now()
    ): Int {
        val schedulable = schedulableWithinHorizon(latestPlan, now, horizonMinutes)
        val schedulableById = schedulable.associateBy { it.id }

        (activeTimeouts.keys.toSet() - schedulableById.keys).forEach(::cancel)
        schedulableById.values.forEach { notification ->
            if (replaceExisting) cancel(notification.id)
            if (!activeTimeouts.containsKey(notification.id)) schedule(notification)
        }
        return schedulableById.size
    }

    private fun schedule(notification: ScheduledNotification) {
        val delayMs = (notification.triggerAt.toEpochMillis() - currentEpochMillis())
            .coerceAtLeast(0.0)
            .coerceAtMost(MaxTimeoutDelayMs.toDouble())
            .toInt()
        val handle = setTimeoutHandle(
            callback = {
                activeTimeouts.remove(notification.id)
                showScheduledNotification(notification)
            },
            delayMs = delayMs
        )
        activeTimeouts[notification.id] = handle
    }

    private fun cancel(id: String) {
        val handle = activeTimeouts.remove(id) ?: return
        clearTimeoutHandle(handle)
    }
}

private const val BrowserScheduleHorizonMinutes = 12 * 60
private const val BrowserScheduleReevaluationIntervalMinutes = 5
private const val MaxTimeoutDelayMs = 2_147_483_647

private fun showScheduledNotification(notification: ScheduledNotification) {
    showViaServiceWorker(
        title = notification.title,
        body = notification.body,
        tag = notification.id,
        date = notification.triggerAt.date.isoLabel()
    )
}

private fun showViaServiceWorker(title: String, body: String, tag: String, date: String) {
    js("{ if (typeof Notification !== 'undefined' && Notification.permission === 'granted' && 'serviceWorker' in navigator) { navigator.serviceWorker.ready.then(function(reg){ return reg.showNotification(title, { body: body, tag: tag, icon: './icons/icon-192.png', badge: './icons/icon-192.png', data: { date: date } }); }).catch(function(){ }); } }")
}

private fun LabDateTime.toEpochMillis(): Double =
    localEpochMillis(date.year, date.month, date.day, minuteOfDay / 60, minuteOfDay % 60)

private fun localEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Double =
    js("new Date(year, month - 1, day, hour, minute, 0, 0).getTime()")

private fun currentEpochMillis(): Double = js("Date.now()")
private fun setTimeoutHandle(callback: () -> Unit, delayMs: Int): Int = js("setTimeout(callback, delayMs)")
private fun clearTimeoutHandle(handle: Int): Unit = js("clearTimeout(handle)")
private fun setIntervalHandle(callback: () -> Unit, intervalMs: Int): Int = js("setInterval(callback, intervalMs)")
