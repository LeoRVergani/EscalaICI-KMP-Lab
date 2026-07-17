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
import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.ui.components.LabPremiumBackground
import br.com.leorvergani.escalaici.ui.theme.LabColors
import br.com.leorvergani.escalaici.ui.theme.LabShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Porte fiel de `ui/auth/LoginScreen.kt` (app Android real): mesmo layout e
 * textos. Diferenca atual: o login Microsoft (MSAL) ainda nao foi portado
 * (ver FASE 11.3 do spec 33) — "Login" sempre termina em mensagem de
 * indisponibilidade e o "Login de teste" seleciona um dos 3 colaboradores
 * de teste em vez de carregar um usuario Microsoft real. Fica como unico
 * caminho de entrada ate o MSAL real estar pronto.
 */
@Composable
internal fun LoginGateScreen(
    members: List<Member>,
    onSelectMember: (Member) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoggingIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDemoOptions by remember { mutableStateOf(false) }

    fun onLoginClick() {
        errorMessage = null
        isLoggingIn = true
        scope.launch {
            delay(600)
            isLoggingIn = false
            errorMessage = "Login corporativo Microsoft ainda não disponível. Use o login de teste abaixo."
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

            Button(
                onClick = ::onLoginClick,
                enabled = !isLoggingIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = LabShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LabColors.primary,
                    disabledContainerColor = LabColors.primary,
                    disabledContentColor = LabColors.onSurface
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Login", color = LabColors.onSurface.copy(alpha = if (isLoggingIn) 0f else 1f))
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = LabColors.onSurface,
                            strokeWidth = 2.dp
                        )
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
            errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    color = LabColors.red,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { showDemoOptions = true }) {
                Text("Login de teste", color = LabColors.primary)
            }
        }
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
