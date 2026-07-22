package br.com.leorvergani.escalaici.platform

import android.content.Context
import br.com.leorvergani.escalaici.model.NotificationSettings

class AndroidNotificationSettingsStore(context: Context) : NotificationSettingsStore {
    private val preferences = context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

    override suspend fun load(): NotificationSettings =
        NotificationSettings(
            notifyDayBefore = preferences.getBoolean(KeyNotifyDayBefore, true),
            dayBeforeTime = preferences.getString(KeyDayBeforeTime, null) ?: "18:00",
            notifyShiftStart = preferences.getBoolean(KeyNotifyShiftStart, true),
            shiftStartOffsetMinutes = preferences.getInt(KeyShiftStartOffsetMinutes, 30),
            notifyShiftEnd = preferences.getBoolean(KeyNotifyShiftEnd, true),
            notifyPause = preferences.getBoolean(KeyNotifyPause, true),
            pauseCustomTime = preferences.getString(KeyPauseCustomTime, null),
            notifyScheduleChanged = preferences.getBoolean(KeyNotifyScheduleChanged, true),
            lastRescheduleAt = preferences.getString(KeyLastRescheduleAt, null)
        )

    override suspend fun save(settings: NotificationSettings) {
        preferences.edit()
            .putBoolean(KeyNotifyDayBefore, settings.notifyDayBefore)
            .putString(KeyDayBeforeTime, settings.dayBeforeTime)
            .putBoolean(KeyNotifyShiftStart, settings.notifyShiftStart)
            .putInt(KeyShiftStartOffsetMinutes, settings.shiftStartOffsetMinutes)
            .putBoolean(KeyNotifyShiftEnd, settings.notifyShiftEnd)
            .putBoolean(KeyNotifyPause, settings.notifyPause)
            .putString(KeyPauseCustomTime, settings.pauseCustomTime)
            .putBoolean(KeyNotifyScheduleChanged, settings.notifyScheduleChanged)
            .putString(KeyLastRescheduleAt, settings.lastRescheduleAt)
            .apply()
    }

    private companion object {
        const val PrefsName = "escalaici.notification.settings"
        const val KeyNotifyDayBefore = "notify_day_before"
        const val KeyDayBeforeTime = "day_before_time"
        const val KeyNotifyShiftStart = "notify_shift_start"
        const val KeyShiftStartOffsetMinutes = "shift_start_offset_minutes"
        const val KeyNotifyShiftEnd = "notify_shift_end"
        const val KeyNotifyPause = "notify_pause"
        const val KeyPauseCustomTime = "pause_custom_time"
        const val KeyNotifyScheduleChanged = "notify_schedule_changed"
        const val KeyLastRescheduleAt = "last_reschedule_at"
    }
}
