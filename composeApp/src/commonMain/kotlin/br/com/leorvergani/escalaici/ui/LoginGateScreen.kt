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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.ui.components.LabPremiumBackground
import br.com.leorvergani.escalaici.ui.theme.LabColors
import br.com.leorvergani.escalaici.ui.theme.LabShapes
import kotlinx.coroutines.launch

/**
 * Porte fiel de `ui/auth/LoginScreen.kt` (app Android real), preservando o
 * seletor de colaboradores de demonstração como fluxo independente.
 */
@Composable
internal fun LoginGateScreen(
    members: List<Member>,
    supportsCorporateAuth: Boolean,
    corporateAuthRepository: CorporateAuthRepository?,
    onSelectMember: (Member) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDemoOptions by remember { mutableStateOf(false) }
    var showConfigurationInstructions by remember { mutableStateOf(false) }
    val host = rememberCorporateAuthHost()
    val corporateAuthState = corporateAuthRepository?.state?.collectAsState()?.value
    val isAuthenticating = corporateAuthState == CorporateAuthState.Authenticating

    fun signInCorporate() {
        val repository = corporateAuthRepository ?: return
        scope.launch { repository.signInInteractive(host) }
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
                    text = "Login corporativo Web ainda não configurado.",
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
                    TextButton(onClick = { showConfigurationInstructions = true }) {
                        Text("Ver instruções de configuração", color = LabColors.primary)
                    }
                    TextButton(onClick = {
                        corporateAuthRepository?.enterDemoMode()
                        showDemoOptions = true
                    }) {
                        Text("Entrar no modo demonstração", color = LabColors.primary)
                    }
                }
                else -> {
                    if (corporateAuthState is CorporateAuthState.Authenticated) {
                        Text("Conta corporativa autenticada", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        Text("Nome: ${corporateAuthState.identity.displayName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        Text("Login: ${corporateAuthState.identity.username}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "O vínculo com membro e time será feito na próxima fase.",
                            color = LabColors.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { scope.launch { corporateAuthRepository.signOut() } }) {
                            Text("Sair da conta corporativa", color = LabColors.primary)
                        }
                        TextButton(onClick = { showDemoOptions = true }) {
                            Text("Continuar em modo demonstração", color = LabColors.primary)
                        }
                    } else {
                        if (corporateAuthState == CorporateAuthState.Demo) {
                            Text("Modo demonstração corporativo ativo", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = ::signInCorporate,
                            enabled = !isAuthenticating,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = LabShapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LabColors.primary,
                                disabledContainerColor = LabColors.primary,
                                disabledContentColor = LabColors.onSurface
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Entrar com conta corporativa", color = LabColors.onSurface.copy(alpha = if (isAuthenticating) 0f else 1f))
                                if (isAuthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = LabColors.onSurface, strokeWidth = 2.dp)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Será aberta a autenticação Microsoft corporativa",
                            color = LabColors.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        (corporateAuthState as? CorporateAuthState.Failed)?.let { failed ->
                            Spacer(Modifier.height(12.dp))
                            Text(failed.error.defaultMessage(), color = LabColors.red, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            TextButton(onClick = ::signInCorporate) { Text("Tentar novamente", color = LabColors.primary) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { showDemoOptions = true }) {
                Text("Login de teste (demonstração)", color = LabColors.primary)
            }
        }
    }

    if (showConfigurationInstructions) {
        AlertDialog(
            onDismissRequest = { showConfigurationInstructions = false },
            containerColor = LabColors.background,
            titleContentColor = LabColors.onSurface,
            textContentColor = LabColors.onSurfaceMuted,
            title = { Text("Configuração da autenticação") },
            text = { Text("Peça ao administrador do Entra o tenant_id, client_id, redirect URIs e o signature hash do app. Preencha esses valores em auth-config.json (raiz do projeto, fora do Git) — veja auth-config.example.json e docs/setup/00-CHECKLIST-CONFIGURACAO-AMANHA.md para o passo a passo completo.") },
            confirmButton = {
                TextButton(onClick = { showConfigurationInstructions = false }) { Text("Fechar", color = LabColors.primary) }
            },
            shape = LabShapes.cardMedium,
            tonalElevation = 6.dp
        )
    }

    if (showDemoOptions) {
        AlertDialog(
            onDismissRequest = { showDemoOptions = false },
            containerColor = LabColors.background,
            titleContentColor = LabColors.onSurface,
            textContentColor = LabColors.onSurfaceMuted,
            title = { Text("Login de teste") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    members.getOrNull(0)?.let { member ->
                        DemoOptionButton("Teste SOC A") {
                            onSelectMember(member)
                            showDemoOptions = false
                        }
                    }
                    members.getOrNull(1)?.let { member ->
                        DemoOptionButton("Teste SOC B") {
                            onSelectMember(member)
                            showDemoOptions = false
                        }
                    }
                    members.getOrNull(2)?.let { member ->
                        DemoOptionButton("Aprovador SOC") {
                            onSelectMember(member)
                            showDemoOptions = false
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDemoOptions = false }) {
                    Text("Cancelar", color = LabColors.primary)
                }
            },
            shape = LabShapes.cardMedium,
            tonalElevation = 6.dp
        )
    }
}

@Composable
private fun DemoOptionButton(text: String, onClick: () -> Unit) {
    TextButton(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(text, color = LabColors.primary)
    }
}
