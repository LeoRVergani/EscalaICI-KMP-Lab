package br.com.leorvergani.escalaici.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.auth.CorporateAuthConfigurationState
import br.com.leorvergani.escalaici.auth.CorporateAuthRepository
import br.com.leorvergani.escalaici.auth.CorporateAuthState
import br.com.leorvergani.escalaici.auth.defaultMessage
import br.com.leorvergani.escalaici.auth.rememberCorporateAuthHost
import br.com.leorvergani.escalaici.ui.components.LabPremiumBackground
import br.com.leorvergani.escalaici.ui.theme.LabColors
import br.com.leorvergani.escalaici.ui.theme.LabShapes
import kotlinx.coroutines.launch

/**
 * Tela desautenticada (FASE 14J, spec 67 seção 3): mostra somente um botão -
 * "Entrar com a conta corporativa". O acesso ao Ambiente Demo deixou de
 * aparecer aqui - vira uma ação secundária em Perfil, visível só depois de
 * autenticado e só para quem tiver autorização (ver `ProfileTab`/`App.kt`).
 */
@Composable
internal fun LoginGateScreen(
    supportsCorporateAuth: Boolean,
    corporateAuthRepository: CorporateAuthRepository?,
    errorMessage: String?,
    onLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val host = rememberCorporateAuthHost()
    val corporateAuthState = corporateAuthRepository?.state?.collectAsState()?.value
    val isAuthenticating = corporateAuthState == CorporateAuthState.Authenticating

    fun signInCorporate(afterAuthenticated: () -> Unit) {
        val repository = corporateAuthRepository ?: return
        if (corporateAuthState is CorporateAuthState.Authenticated) {
            afterAuthenticated()
        } else {
            scope.launch { repository.signInInteractive(host) }
            afterAuthenticated()
        }
    }

    LabPremiumBackground {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Escala ICI",
                color = LabColors.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))

            when {
                !supportsCorporateAuth -> Text(
                    text = "Login corporativo indisponível neste ambiente.",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                corporateAuthRepository == null ||
                    corporateAuthRepository.configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED -> {
                    Text(
                        text = "Autenticação corporativa ainda não configurada neste ambiente.",
                        color = LabColors.onSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    if (corporateAuthState is CorporateAuthState.Authenticated) {
                        Text("Conta corporativa autenticada", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        Text("Nome: ${corporateAuthState.identity.displayName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        Text("Login: ${corporateAuthState.identity.username}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    EntryButton(
                        text = "Entrar com a conta corporativa",
                        loading = isAuthenticating,
                        enabled = !isAuthenticating,
                        onClick = { signInCorporate(onLogin) }
                    )
                    (corporateAuthState as? CorporateAuthState.Failed)?.let { failed ->
                        Spacer(Modifier.height(12.dp))
                        Text(failed.error.defaultMessage(), color = LabColors.red, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = it,
                    color = LabColors.red,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
                }
    }
}

@Composable
private fun EntryButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    tertiary: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = LabShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (tertiary) LabColors.tertiary else LabColors.primary,
            disabledContainerColor = if (tertiary) LabColors.tertiary else LabColors.primary,
            disabledContentColor = LabColors.onSurface
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = LabColors.onSurface.copy(alpha = if (loading) 0f else 1f))
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = LabColors.onSurface, strokeWidth = 2.dp)
            }
        }
    }
}
