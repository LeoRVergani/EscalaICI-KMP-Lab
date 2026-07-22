package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.NotificationSettings

interface NotificationSettingsStore {
    suspend fun load(): NotificationSettings
    suspend fun save(settings: NotificationSettings)
}

class InMemoryNotificationSettingsStore(
    initialSettings: NotificationSettings = NotificationSettings()
) : NotificationSettingsStore {
    private var current = initialSettings

    override suspend fun load(): NotificationSettings = current

    override suspend fun save(settings: NotificationSettings) {
        current = settings
    }
}
