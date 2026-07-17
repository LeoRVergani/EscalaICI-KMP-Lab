package br.com.leorvergani.escalaici.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberCorporateAuthHost(): CorporateAuthHost? {
    val context = LocalContext.current
    return remember(context) {
        context.findActivity()?.let(::AndroidCorporateAuthHost)
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
