package br.com.leorvergani.escalaici.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.leorvergani.escalaici.MainActivity
import br.com.leorvergani.escalaici.R
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.ScheduledNotification
import br.com.leorvergani.escalaici.model.buildNotificationPlan
import br.com.leorvergani.escalaici.model.isoLabel
import br.com.leorvergani.escalaici.repository.AndroidLocalDataCache
import br.com.leorvergani.escalaici.repository.CacheRead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

const val ShiftReminderChannelId = "shift_reminders"
const val ShiftReminderAction = "br.com.leorvergani.escalaici.action.SHIFT_REMINDER"
const val ExtraNotificationId = "br.com.leorvergani.escalaici.extra.NOTIFICATION_ID"
const val ExtraNotificationType = "br.com.leorvergani.escalaici.extra.NOTIFICATION_TYPE"
const val ExtraNotificationTitle = "br.com.leorvergani.escalaici.extra.NOTIFICATION_TITLE"
const val ExtraNotificationBody = "br.com.leorvergani.escalaici.extra.NOTIFICATION_BODY"
const val ExtraNotificationDate = "br.com.leorvergani.escalaici.extra.NOTIFICATION_DATE"

fun ensureShiftReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        ShiftReminderChannelId,
        "Lembretes de escala",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Avisos de véspera, entrada, término de turno e pausa."
    }
    manager.createNotificationChannel(channel)
}

fun hasPostNotificationsPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

class AndroidNotificationService(
    private val activity: ComponentActivity,
    private val permissionLauncher: ActivityResultLauncher<String>
) : WebNotificationService {
    private val appContext = activity.applicationContext
    private val preferences = appContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
    private var pendingPermissionResult: ((NotificationPermissionState) -> Unit)? = null

    override fun capability(): NotificationCapability =
        NotificationCapability(
            permissionState = permissionState(),
            supportsSystemNotifications = true,
            supportsReliableBackgroundScheduling = true
        )

    override fun requestPermission(onResult: (NotificationPermissionState) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasPostNotificationsPermission(appContext)) {
            onResult(NotificationPermissionState.GRANTED)
            return
        }
        pendingPermissionResult = onResult
        preferences.edit().putBoolean(KeyPostNotificationsRequested, true).apply()
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun onPermissionResult(granted: Boolean) {
        val result = if (granted) NotificationPermissionState.GRANTED else NotificationPermissionState.DENIED
        pendingPermissionResult?.invoke(result)
        pendingPermissionResult = null
    }

    fun shouldRequestPermissionOnStartup(): Boolean =
        shouldRequestPostNotifications(
            sdkInt = Build.VERSION.SDK_INT,
            permissionGranted = hasPostNotificationsPermission(appContext),
            alreadyRequested = preferences.getBoolean(KeyPostNotificationsRequested, false)
        )

    @SuppressLint("MissingPermission")
    override fun showNotification(title: String, body: String, tag: String, onResult: (Boolean) -> Unit) {
        if (!hasPostNotificationsPermission(appContext)) {
            onResult(false)
            return
        }
        ensureShiftReminderChannel(appContext)
        val notification = NotificationCompat.Builder(appContext, ShiftReminderChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent(appContext, requestCode = tag.hashCode(), dateIso = null))
            .build()
        NotificationManagerCompat.from(appContext).notify(tag, TestNotificationId, notification)
        onResult(true)
    }

    override fun cancelNotification(tag: String) {
        NotificationManagerCompat.from(appContext).cancel(tag, TestNotificationId)
    }

    private fun permissionState(): NotificationPermissionState =
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> NotificationPermissionState.GRANTED
            hasPostNotificationsPermission(appContext) -> NotificationPermissionState.GRANTED
            preferences.getBoolean(KeyPostNotificationsRequested, false) -> NotificationPermissionState.DENIED
            else -> NotificationPermissionState.DEFAULT
        }

    private companion object {
        const val PrefsName = "escalaici.notification.permission"
        const val KeyPostNotificationsRequested = "post_notifications_requested"
        const val TestNotificationId = 5001
    }
}

class AndroidNotificationScheduler(context: Context) : LocalNotificationRuntime {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = appContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

    override suspend fun reconcile(plan: List<ScheduledNotification>): LocalNotificationResult {
        val previousIds = preferences.getStringSet(KeyScheduledIds, emptySet()).orEmpty()
        val nextById = plan.associateBy { it.id }
        val reconciliation = reconcileNotificationIds(previousIds, nextById.keys)

        reconciliation.idsToCancel.forEach(::cancel)
        reconciliation.idsToSchedule.mapNotNull(nextById::get).forEach(::schedule)
        preferences.edit().putStringSet(KeyScheduledIds, nextById.keys).apply()

        return LocalNotificationResult.Applied(
            scheduledCount = reconciliation.idsToSchedule.size,
            cancelledCount = reconciliation.idsToCancel.size
        )
    }

    private fun schedule(notification: ScheduledNotification) {
        val pendingIntent = notificationPendingIntent(
            context = appContext,
            notification = notification,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            notification.triggerAt.toEpochMillis(),
            pendingIntent
        )
    }

    private fun cancel(id: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            notificationRequestCodeFor(id),
            Intent(appContext, ShiftNotificationReceiver::class.java).setAction(ShiftReminderAction),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private companion object {
        const val PrefsName = "escalaici.notification.scheduler"
        const val KeyScheduledIds = "scheduled_ids"
    }
}

class ShiftNotificationReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ShiftReminderAction) return
        if (!hasPostNotificationsPermission(context)) return
        ensureShiftReminderChannel(context)

        val id = intent.getStringExtra(ExtraNotificationId) ?: return
        val title = intent.getStringExtra(ExtraNotificationTitle) ?: return
        val body = intent.getStringExtra(ExtraNotificationBody) ?: return
        val dateIso = intent.getStringExtra(ExtraNotificationDate)
        val requestCode = notificationRequestCodeFor(id)
        val notification = NotificationCompat.Builder(context, ShiftReminderChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent(context, requestCode, dateIso))
            .build()

        NotificationManagerCompat.from(context).notify(requestCode, notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = AndroidNotificationSettingsStore(context).load()
                val cached = AndroidLocalDataCache(context).loadSchedule()
                val plan = when (cached) {
                    is CacheRead.Valid -> buildNotificationPlan(
                        days = cached.value.summary.days,
                        settings = settings,
                        now = AndroidCurrentTimeProvider.now()
                    )
                    CacheRead.Missing,
                    is CacheRead.Invalid -> emptyList()
                }
                AndroidNotificationScheduler(context).reconcile(plan)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private fun notificationPendingIntent(
    context: Context,
    notification: ScheduledNotification,
    flags: Int
): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        notificationRequestCodeFor(notification.id),
        Intent(context, ShiftNotificationReceiver::class.java)
            .setAction(ShiftReminderAction)
            .putExtra(ExtraNotificationId, notification.id)
            .putExtra(ExtraNotificationType, notification.type.name)
            .putExtra(ExtraNotificationTitle, notification.title)
            .putExtra(ExtraNotificationBody, notification.body)
            .putExtra(ExtraNotificationDate, notification.triggerAt.date.isoLabel()),
        flags
    )

private fun contentPendingIntent(context: Context, requestCode: Int, dateIso: String?): PendingIntent =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply {
                dateIso?.let { putExtra(ExtraNotificationDate, it) }
            },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

private fun LabDateTime.toEpochMillis(): Long {
    val localDate = LocalDate.of(date.year, date.month, date.day)
    return localDate
        .atStartOfDay()
        .plusMinutes(minuteOfDay.toLong())
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
