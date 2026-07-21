package br.com.leorvergani.escalaici.platform

import androidx.compose.runtime.Composable

@Composable
internal expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)
