package br.com.leorvergani.escalaici.platform

import androidx.compose.runtime.Composable

@Composable
@Suppress("UNUSED_PARAMETER")
internal actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) = Unit
