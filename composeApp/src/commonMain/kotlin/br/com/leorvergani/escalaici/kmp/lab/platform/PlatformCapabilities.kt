package br.com.leorvergani.escalaici.kmp.lab.platform

data class PlatformCapabilities(
    val supportsAppUpdate: Boolean = true,
    val supportsWebNotifications: Boolean = false,
    val supportsLocalFilePicker: Boolean = true,
    val supportsCloudFilePicker: Boolean = false,
    val supportsPushNotifications: Boolean = false,
    val supportsBackgroundScheduledNotifications: Boolean = false
)
