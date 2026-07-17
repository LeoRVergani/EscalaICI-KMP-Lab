package br.com.leorvergani.escalaici.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.auth.CorporateAuthConfigurationState
import br.com.leorvergani.escalaici.auth.CorporateAuthRepository
import br.com.leorvergani.escalaici.auth.CorporateAuthState
import br.com.leorvergani.escalaici.auth.defaultMessage
import br.com.leorvergani.escalaici.model.AppUpdateResult
import br.com.leorvergani.escalaici.model.AppVersion
import br.com.leorvergani.escalaici.model.GenerateLabAlerts
import br.com.leorvergani.escalaici.model.LabAlert
import br.com.leorvergani.escalaici.model.ScheduleSummary
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.pauseFor
import br.com.leorvergani.escalaici.model.relevantShift
import br.com.leorvergani.escalaici.platform.WebNotificationService
import br.com.leorvergani.escalaici.platform.NotificationPermissionState
import br.com.leorvergani.escalaici.platform.rememberAppUpdateChecker
import br.com.leorvergani.escalaici.ui.components.LabCard
import br.com.leorvergani.escalaici.ui.components.LabCollaboratorAvatar
import br.com.leorvergani.escalaici.ui.components.LabPremiumHeader
import br.com.leorvergani.escalaici.ui.components.PageList
import br.com.leorvergani.escalaici.ui.theme.LabColors
import br.com.leorvergani.escalaici.ui.theme.LabShapes
import br.com.leorvergani.escalaici.ui.util.initials
import kotlinx.coroutines.launch

@Composable
internal fun ProfileTab(
    summary: ScheduleSummary,
    now: LabDateTime,
    supportsAppUpdate: Boolean,
    supportsWebNotifications: Boolean,
    supportsCorporateAuth: Boolean,
    corporateAuthRepository: CorporateAuthRepository?,
    notificationService: WebNotificationService,
    onLogout: () -> Unit,
    onOpenPlantao: () -> Unit,
    onOpenSwap: () -> Unit
) {
    val corporateAuthState = corporateAuthRepository?.state?.collectAsState()?.value
    val criticalAlerts = remember(summary) { GenerateLabAlerts(summary).count { it.severity == LabAlert.Severity.CRITICO } }
    var notificationPermission by remember(notificationService) { mutableStateOf(notificationService.capability().permissionState) }
    var requestingNotification by remember { mutableStateOf(false) }
    var notificationFeedback by remember { mutableStateOf<String?>(null) }
    PageList {
        item {
            LabPremiumHeader(selectedCollaborator = summary.member.scaleName, onOpenPlantao = onOpenPlantao)
        }
        item {
            Text("Perfil", color = LabColors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            LabCard(borderColor = LabColors.primary.copy(alpha = 0.34f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabCollaboratorAvatar(initials = summary.member.displayName.initials(), modifier = Modifier.size(52.dp))
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text("Perfil selecionado", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                        Text(summary.member.displayName, color = LabColors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (summary.isImported) "Escala salva apenas neste dispositivo" else "Nenhuma escala importada",
                            color = LabColors.tertiary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Text("Período: ${summary.periodLabel}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (summary.isImported) "Arquivo importado: ${summary.sourceFileName}" else "Fonte: dados de demonstração",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(summary.member.email, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onLogout) {
                    Text("Sair (login de teste)", color = LabColors.primary)
                }
            }
        }
        item {
            LabCard(title = "Identidade da escala", icon = Icons.Default.Security, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Colaborador identificado: ${summary.member.scaleName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                when {
                    !supportsCorporateAuth -> Text("Login corporativo Web ainda não configurado.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    corporateAuthRepository == null || corporateAuthRepository.configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED ->
                        Text("Autenticação corporativa ainda não configurada neste ambiente.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    corporateAuthState == CorporateAuthState.SignedOut -> Text("Nenhuma conta corporativa conectada.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    corporateAuthState == CorporateAuthState.Authenticating -> Text("Autenticando conta corporativa...", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    corporateAuthState == CorporateAuthState.Demo -> Text("Modo demonstração corporativo ativo (nenhuma conta Microsoft real conectada).", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    corporateAuthState is CorporateAuthState.Failed -> Text(corporateAuthState.error.defaultMessage(), color = LabColors.red, style = MaterialTheme.typography.bodySmall)
                    corporateAuthState is CorporateAuthState.Authenticated -> {
                        Text("Nome: ${corporateAuthState.identity.displayName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        Text("Login: ${corporateAuthState.identity.username}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        Text("Organização corporativa identificada.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        Text("Conta corporativa autenticada", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        Text("O vínculo com o membro/time da escala ainda será configurado em uma próxima fase.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    else -> Text("Nenhuma conta corporativa conectada.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            LabCard(title = "Resumo do perfil", icon = Icons.Default.Analytics, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileMetric("Trabalho", "${summary.workedDays}d", Modifier.weight(1f))
                    ProfileMetric("Folgas", "${summary.restDays}d", Modifier.weight(1f))
                    ProfileMetric("Horas", "${summary.totalHours}h", Modifier.weight(1f))
                }
                Text("Alertas críticos: $criticalAlerts", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            LabCard(title = "Trocas de escala", icon = Icons.Default.SwapHoriz, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Veja e responda pedidos de troca de turno.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onOpenSwap) {
                    Text("Ver minhas solicitações", color = LabColors.primary)
                }
            }
        }
        if (supportsWebNotifications) item {
            LabCard(title = "Notificações", icon = Icons.Default.Notifications, borderColor = LabColors.primary.copy(alpha = 0.30f), gradient = listOf(LabColors.surfaceElevated.copy(alpha = 0.88f), LabColors.surface.copy(alpha = 0.96f))) {
                val status = when {
                    requestingNotification -> "Solicitando permissão..."
                    notificationPermission == NotificationPermissionState.GRANTED -> "Notificações Web ativadas"
                    notificationPermission == NotificationPermissionState.DENIED -> "Notificações bloqueadas no navegador"
                    notificationPermission == NotificationPermissionState.UNSUPPORTED -> "Este navegador não oferece notificações compatíveis"
                    else -> "Notificações Web desativadas"
                }
                StatusLine("Status", status)
                when (notificationPermission) {
                    NotificationPermissionState.DEFAULT -> TextButton(enabled = !requestingNotification, onClick = {
                        requestingNotification = true
                        notificationService.requestPermission { result ->
                            notificationPermission = result
                            requestingNotification = false
                        }
                    }) { Text("Ativar notificações Web") }
                    NotificationPermissionState.GRANTED -> TextButton(onClick = {
                        notificationService.showNotification(
                            title = "Pausa do turno",
                            body = "Notificações do Escala ICI estão funcionando.",
                            tag = "escala-ici-notification-test"
                        ) { ok -> notificationFeedback = if (ok) "Notificação de teste enviada." else "Não foi possível exibir a notificação." }
                    }) { Text("Testar notificação Web") }
                    NotificationPermissionState.DENIED -> Text("Altere a permissão nas configurações do site.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    NotificationPermissionState.UNSUPPORTED -> Unit
                }
                notificationFeedback?.let { Text(it, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall) }
                Text("As notificações Web funcionam enquanto o site ou PWA estiver ativo. Avisos com o aplicativo totalmente fechado exigirão uma integração futura de Push.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Selecione uma pausa válida antes de programar um lembrete.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            val pause = pauseFor(summary.relevantShift(now))
            LabCard(title = "Pausa de 15 minutos", icon = Icons.Default.Schedule, iconTint = LabColors.tertiary, borderColor = LabColors.tertiary.copy(alpha = 0.30f), gradient = listOf(Color(0xFF0D2832), Color(0xFF092A28), Color(0xFF0D1730))) {
                Text(if (pause != null) "${pause.displayTitle}: ${pause.displayValue}" else "Pausa não configurada", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                VisualToggle("Lembrete de pausa", true)
                pause?.let { Text("Permitido entre ${it.windowStart} e ${it.windowEnd}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall) }
                Text("Analista: ${summary.member.scaleName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            val pause = pauseFor(summary.relevantShift(now))
            LabCard(title = "Resumo", icon = Icons.Default.Checklist, borderColor = LabColors.primary.copy(alpha = 0.22f)) {
                Text("Entrada do turno: 15 min antes", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Pausa: ${pause?.displayValue ?: "não configurada"}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                pause?.let { Text("Janela permitida: ${it.windowStart}–${it.windowEnd}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item {
            LabCard(title = "Armazenamento local", icon = Icons.Default.Storage, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Arquivo salvo: ${summary.sourceFileName ?: "nenhuma escala importada"}", color = LabColors.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Status: ${if (summary.isImported) "Escala lida na sessão Web/Android" else "Sem XLS aplicado"}", color = LabColors.tertiary, style = MaterialTheme.typography.labelMedium)
                DisabledAction("Remover escala local")
            }
        }
        if (supportsAppUpdate) item {
            val updateChecker = rememberAppUpdateChecker()
            val scope = rememberCoroutineScope()
            var updateMessage by remember { mutableStateOf("") }

            LabCard(title = "Aplicativo", icon = Icons.Default.SystemUpdate, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Versão atual: ${AppVersion.LABEL}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text(
                    updateMessage.ifBlank { "Atualização real via Dropbox — mesmo mecanismo do app Android oficial." },
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = {
                    updateMessage = "Verificando atualização..."
                    scope.launch {
                        updateMessage = when (val result = updateChecker.checkAndInstall()) {
                            AppUpdateResult.UpToDate -> "Você já está usando a versão mais recente."
                            is AppUpdateResult.InstallStarted -> buildString {
                                append("Nova versão disponível: v${result.versionName}")
                                result.changelog?.let { append(". $it") }
                            }
                            AppUpdateResult.PermissionRequired -> "Permita instalar atualizações deste app e tente novamente."
                            AppUpdateResult.NotSupported -> "Atualização automática disponível só no Android."
                            is AppUpdateResult.Failure -> result.message
                        }
                    }
                }) {
                    Text("Atualizar aplicativo", color = LabColors.primary)
                }
            }
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(LabShapes.cardSmall)
            .background(LabColors.surfaceElevated.copy(alpha = 0.62f))
            .border(1.dp, LabColors.primary.copy(alpha = 0.18f), LabShapes.cardSmall)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(112.dp), color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(value, modifier = Modifier.weight(1f), color = LabColors.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DisabledAction(label: String) {
    TextButton(onClick = ::unavailableAction, enabled = false) {
        Text(label)
    }
    Text("Disponível em uma próxima etapa.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
}

private fun unavailableAction() = Unit

@Composable
private fun VisualToggle(label: String, checked: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = null, enabled = false)
    }
}

@Composable
private fun ProfileChip(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(LabShapes.chip)
            .background(if (selected) LabColors.primary.copy(alpha = 0.20f) else LabColors.surfaceElevated.copy(alpha = 0.78f))
            .border(1.dp, if (selected) LabColors.primary.copy(alpha = 0.62f) else LabColors.outline.copy(alpha = 0.55f), LabShapes.chip)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
