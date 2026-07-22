package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.NotificationSettings
import kotlinx.browser.window

class WebNotificationSettingsStore : NotificationSettingsStore {
    override suspend fun load(): NotificationSettings {
        val raw = read(SettingsKey) ?: return NotificationSettings()
        return try {
            if (!isJsonObject(raw)) {
                remove(SettingsKey)
                return NotificationSettings()
            }
            NotificationSettings(
                notifyDayBefore = jsonBoolean(raw, "notifyDayBefore") ?: true,
                dayBeforeTime = jsonString(raw, "dayBeforeTime") ?: "18:00",
                notifyShiftStart = jsonBoolean(raw, "notifyShiftStart") ?: true,
                shiftStartOffsetMinutes = jsonInt(raw, "shiftStartOffsetMinutes") ?: 30,
                notifyShiftEnd = jsonBoolean(raw, "notifyShiftEnd") ?: true,
                notifyPause = jsonBoolean(raw, "notifyPause") ?: true,
                pauseCustomTime = jsonString(raw, "pauseCustomTime"),
                notifyScheduleChanged = jsonBoolean(raw, "notifyScheduleChanged") ?: true,
                lastRescheduleAt = jsonString(raw, "lastRescheduleAt")
            )
        } catch (_: Throwable) {
            remove(SettingsKey)
            NotificationSettings()
        }
    }

    override suspend fun save(settings: NotificationSettings) {
        write(
            SettingsKey,
            buildString {
                append('{')
                field("notifyDayBefore", settings.notifyDayBefore)
                field("dayBeforeTime", settings.dayBeforeTime)
                field("notifyShiftStart", settings.notifyShiftStart)
                field("shiftStartOffsetMinutes", settings.shiftStartOffsetMinutes)
                field("notifyShiftEnd", settings.notifyShiftEnd)
                field("notifyPause", settings.notifyPause)
                field("pauseCustomTime", settings.pauseCustomTime)
                field("notifyScheduleChanged", settings.notifyScheduleChanged)
                field("lastRescheduleAt", settings.lastRescheduleAt, trailingComma = false)
                append('}')
            }
        )
    }

    private fun read(key: String): String? = try { window.localStorage.getItem(key) } catch (_: Throwable) { null }
    private fun write(key: String, value: String): Boolean = try { window.localStorage.setItem(key, value); true } catch (_: Throwable) { false }
    private fun remove(key: String) { try { window.localStorage.removeItem(key) } catch (_: Throwable) { } }

    private companion object {
        const val SettingsKey = "escalaici.notification.settings"
    }
}

private fun StringBuilder.field(name: String, value: String?, trailingComma: Boolean = true) {
    append(jsonQuote(name)).append(':').append(value?.let(::jsonQuote) ?: "null")
    if (trailingComma) append(',')
}

private fun StringBuilder.field(name: String, value: Boolean, trailingComma: Boolean = true) {
    append(jsonQuote(name)).append(':').append(value)
    if (trailingComma) append(',')
}

private fun StringBuilder.field(name: String, value: Int, trailingComma: Boolean = true) {
    append(jsonQuote(name)).append(':').append(value)
    if (trailingComma) append(',')
}

private fun jsonQuote(value: String): String = buildString {
    append('"')
    value.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
    append('"')
}

private fun isJsonObject(raw: String): Boolean = js("{ try { const value = JSON.parse(raw); return value !== null && typeof value === 'object' && !Array.isArray(value); } catch (_) { return false; } }")
private fun jsonString(raw: String, key: String): String? = js("{ const value = JSON.parse(raw)[key]; return typeof value === 'string' ? value : null; }")
private fun jsonBoolean(raw: String, key: String): Boolean? = js("{ const value = JSON.parse(raw)[key]; return typeof value === 'boolean' ? value : null; }")
private fun jsonInt(raw: String, key: String): Int? = js("{ const value = JSON.parse(raw)[key]; return Number.isInteger(value) ? value : null; }")
