package br.com.leorvergani.escalaici.platform

data class PlatformCapabilities(
    val supportsAppUpdate: Boolean = false,
    val supportsWebNotifications: Boolean = false,
    val supportsLocalFilePicker: Boolean = true,
    val supportsCloudFilePicker: Boolean = false,
    val supportsPushNotifications: Boolean = false,
    val supportsBackgroundScheduledNotifications: Boolean = false,
    val supportsCorporateAuth: Boolean = false
)
