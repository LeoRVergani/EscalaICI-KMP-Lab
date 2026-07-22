package br.com.leorvergani.escalaici.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import br.com.leorvergani.escalaici.identity.DemoPersona
import br.com.leorvergani.escalaici.identity.OrganizationResolutionResult
import br.com.leorvergani.escalaici.model.AppUpdateResult
import br.com.leorvergani.escalaici.model.AppVersion
import br.com.leorvergani.escalaici.model.GenerateLabAlerts
import br.com.leorvergani.escalaici.model.LabAlert
import br.com.leorvergani.escalaici.model.ScheduleSummary
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.NotificationSettings
import br.com.leorvergani.escalaici.model.ScheduledNotification
import br.com.leorvergani.escalaici.model.NotificationType
import br.com.leorvergani.escalaici.model.ShiftStartOffsetOptions
import br.com.leorvergani.escalaici.model.buildNotificationPlan
import br.com.leorvergani.escalaici.model.isoMinuteLabel
import br.com.leorvergani.escalaici.model.pauseFor
import br.com.leorvergani.escalaici.model.pauseSuggestionTimes
import br.com.leorvergani.escalaici.model.relevantShift
import br.com.leorvergani.escalaici.model.timeLabel
import br.com.leorvergani.escalaici.platform.WebNotificationService
import br.com.leorvergani.escalaici.platform.NotificationPermissionState
import br.com.leorvergani.escalaici.platform.NotificationSettingsStore
import br.com.leorvergani.escalaici.platform.LocalNotificationResult
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
    organizationResolutionResult: OrganizationResolutionResult?,
    demoPersonaResolutionResult: OrganizationResolutionResult?,
    selectedDemoPersona: DemoPersona?,
    notificationService: WebNotificationService,
    notificationSettingsStore: NotificationSettingsStore,
    initialNotificationSettings: NotificationSettings,
    onNotificationSettingsSaved: (NotificationSettings) -> Unit,
    onReconcileNotifications: suspend (List<ScheduledNotification>) -> LocalNotificationResult,
    onLogout: () -> Unit,
    onOpenPlantao: () -> Unit,
    onOpenSwap: () -> Unit
) {
    val notificationScope = rememberCoroutineScope()
    val corporateAuthState = corporateAuthRepository?.state?.collectAsState()?.value
    val criticalAlerts = remember(summary) { GenerateLabAlerts(summary).count { it.severity == LabAlert.Severity.CRITICO } }
    val identityDecision = remember(selectedDemoPersona, organizationResolutionResult) {
        decideProfileIdentityPresentation(selectedDemoPersona, organizationResolutionResult)
    }
    val notificationCapability = notificationService.capability()
    val supportsSystemNotifications = notificationCapability.supportsSystemNotifications
    val supportsReliableBackgroundScheduling = notificationCapability.supportsReliableBackgroundScheduling
    var notificationPermission by remember(notificationService) { mutableStateOf(notificationCapability.permissionState) }
    var requestingNotification by remember { mutableStateOf(false) }
    var notificationFeedback by remember { mutableStateOf<String?>(null) }
    var notificationSettings by remember(notificationSettingsStore, initialNotificationSettings) {
        mutableStateOf(initialNotificationSettings)
    }
    var rescheduleFeedback by remember { mutableStateOf<String?>(null) }
    val relevantShift = remember(summary, now) { summary.relevantShift(now) }
    val pause = remember(relevantShift) { pauseFor(relevantShift) }
    val pauseDecision = remember(notificationSettings, relevantShift, notificationPermission, supportsSystemNotifications) {
        decideProfilePauseNotification(
            settings = notificationSettings,
            relevantShift = relevantShift,
            notificationPermission = notificationPermission,
            requiresNotificationPermission = supportsSystemNotifications
        )
    }
    val notificationPlan = remember(summary.days, notificationSettings, now) {
        buildNotificationPlan(summary.days, notificationSettings, now)
    }
    val updateNotificationSettings: (NotificationSettings) -> Unit = { next ->
        notificationSettings = next
        notificationScope.launch {
            notificationSettingsStore.save(next)
            onNotificationSettingsSaved(next)
            val result = onReconcileNotifications(buildNotificationPlan(summary.days, next, now))
            rescheduleFeedback = result.feedbackMessage()
        }
    }

    LaunchedEffect(notificationSettingsStore) {
        val stored = notificationSettingsStore.load()
        notificationSettings = stored
        onNotificationSettingsSaved(stored)
    }

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
                            profileScheduleStatusLine(summary),
                            color = LabColors.tertiary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Text("Período: ${summary.periodLabel}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text(
                    profileScheduleSourceLine(summary),
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
                    else -> {
                        when (corporateAuthState) {
                            null,
                            CorporateAuthState.NotConfigured,
                            CorporateAuthState.SignedOut -> Text("Nenhuma conta corporativa conectada.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            CorporateAuthState.Authenticating -> Text("Autenticando conta corporativa...", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            CorporateAuthState.Demo ->
                                Text("Modo demonstração corporativo ativo (nenhuma conta Microsoft real conectada).", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            is CorporateAuthState.Failed -> Text(corporateAuthState.error.defaultMessage(), color = LabColors.red, style = MaterialTheme.typography.bodySmall)
                            is CorporateAuthState.Authenticated -> {
                                Text("Nome: ${corporateAuthState.identity.displayName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                                Text("Login: ${corporateAuthState.identity.username}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                                if (identityDecision.showOrganizationResolutionBody) {
                                    Text("Organização corporativa identificada.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                                    Text("Conta corporativa autenticada", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                                    OrganizationResolutionBody(organizationResolutionResult)
                                } else {
                                    identityDecision.demoSessionLabel?.let { label ->
                                        Text(label, color = LabColors.tertiary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                // Sempre visível quando há personagem Demo selecionado, independente do
                // corporateAuthState (workspace demo-v1 é ortogonal à identidade
                // corporativa, spec 56 seção 12 — mesma correção de alcançabilidade
                // aplicada em LoginGateScreen).
                if (selectedDemoPersona != null && identityDecision.showDemoPersonaResolutionSection) {
                    DemoPersonaResolutionSection(
                        selectedDemoPersona = selectedDemoPersona,
                        demoPersonaResolutionResult = demoPersonaResolutionResult
                    )
                    identityDecision.demoModeExplanation?.let { message ->
                        Text(message, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    }
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
        item {
            LabCard(title = "Notificações", icon = Icons.Default.Notifications, borderColor = LabColors.primary.copy(alpha = 0.30f), gradient = listOf(LabColors.surfaceElevated.copy(alpha = 0.88f), LabColors.surface.copy(alpha = 0.96f))) {
                val status = when {
                    requestingNotification -> "Solicitando permissão..."
                    supportsReliableBackgroundScheduling && notificationPermission == NotificationPermissionState.GRANTED -> "Notificações Android ativadas"
                    supportsReliableBackgroundScheduling && notificationPermission == NotificationPermissionState.DENIED -> "Notificações bloqueadas no sistema"
                    supportsWebNotifications && notificationPermission == NotificationPermissionState.GRANTED -> "Notificações Web ativadas"
                    supportsWebNotifications && notificationPermission == NotificationPermissionState.DENIED -> "Notificações bloqueadas no navegador"
                    supportsSystemNotifications && notificationPermission == NotificationPermissionState.UNSUPPORTED -> "Este ambiente não oferece notificações compatíveis"
                    !supportsSystemNotifications -> "Preferências e plano calculados no app; disparo real indisponível nesta plataforma"
                    else -> "Notificações desativadas"
                }
                StatusLine("Status", status)
                when {
                    !supportsSystemNotifications -> Text("Esta tela salva as preferências pela interface comum.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    notificationPermission == NotificationPermissionState.DEFAULT -> TextButton(enabled = !requestingNotification, onClick = {
                        requestingNotification = true
                        notificationService.requestPermission { result ->
                            notificationPermission = result
                            requestingNotification = false
                        }
                    }) { Text("Ativar notificações") }
                    notificationPermission == NotificationPermissionState.GRANTED -> TextButton(onClick = {
                        notificationService.showNotification(
                            title = "Pausa do turno",
                            body = "Notificações do Escala ICI estão funcionando.",
                            tag = "escala-ici-notification-test"
                        ) { ok -> notificationFeedback = if (ok) "Notificação de teste enviada." else "Não foi possível exibir a notificação." }
                    }) { Text(if (supportsReliableBackgroundScheduling) "Testar notificação Android" else "Testar notificação Web") }
                    notificationPermission == NotificationPermissionState.DENIED -> Text(
                        if (supportsReliableBackgroundScheduling) "Ative as notificações nas configurações do sistema." else "Altere a permissão nas configurações do site.",
                        color = LabColors.onSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                notificationFeedback?.let { Text(it, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall) }
                if (supportsWebNotifications) {
                    Text("As notificações Web funcionam enquanto o site ou PWA estiver ativo. Avisos com o aplicativo totalmente fechado exigirão uma integração futura de Push.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(color = LabColors.outline.copy(alpha = 0.28f))

                PreferenceToggle(
                    label = "Você trabalha amanhã / Você está de folga amanhã",
                    checked = notificationSettings.notifyDayBefore,
                    enabled = true,
                    disabledReason = null,
                    onCheckedChange = { updateNotificationSettings(notificationSettings.copy(notifyDayBefore = it)) }
                )
                OutlinedTextField(
                    value = notificationSettings.dayBeforeTime,
                    onValueChange = { updateNotificationSettings(notificationSettings.copy(dayBeforeTime = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Horário do aviso da véspera") }
                )

                PreferenceToggle(
                    label = "Aviso de entrada do turno",
                    checked = notificationSettings.notifyShiftStart,
                    enabled = true,
                    disabledReason = null,
                    onCheckedChange = { updateNotificationSettings(notificationSettings.copy(notifyShiftStart = it)) }
                )
                ChoiceChips(
                    values = ShiftStartOffsetOptions.map { if (it == 0) "Na hora" else "${it}min" },
                    selected = if (notificationSettings.shiftStartOffsetMinutes == 0) "Na hora" else "${notificationSettings.shiftStartOffsetMinutes}min",
                    enabled = notificationSettings.notifyShiftStart,
                    disabledReason = if (!notificationSettings.notifyShiftStart) "Ative o aviso de entrada para escolher o tempo de antecedência." else null,
                    onSelect = { label ->
                        val offset = if (label == "Na hora") 0 else label.removeSuffix("min").toIntOrNull() ?: notificationSettings.shiftStartOffsetMinutes
                        updateNotificationSettings(notificationSettings.copy(shiftStartOffsetMinutes = offset))
                    }
                )

                PreferenceToggle(
                    label = "Lembrete de pausa",
                    checked = pauseDecision.checked,
                    enabled = pauseDecision.enabled,
                    disabledReason = pauseDecision.disabledReason,
                    onCheckedChange = { updateNotificationSettings(notificationSettings.copy(notifyPause = it)) }
                )
                Text(if (pause != null) "Janela permitida: ${pause.windowStart}–${pause.windowEnd}" else "Pausa não configurada", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                val pauseOptions = remember(relevantShift) { pauseSuggestionTimes(relevantShift) }
                if (pauseOptions.isNotEmpty()) {
                    ChoiceChips(
                        values = listOf("Sugestão") + pauseOptions,
                        selected = notificationSettings.pauseCustomTime ?: "Sugestão",
                        enabled = pauseDecision.enabled && notificationSettings.notifyPause,
                        disabledReason = if (pauseDecision.enabled && !notificationSettings.notifyPause) "Ative o lembrete de pausa para escolher um horário." else null,
                        onSelect = { label ->
                            updateNotificationSettings(notificationSettings.copy(pauseCustomTime = label.takeUnless { it == "Sugestão" }))
                        }
                    )
                }

                PreferenceToggle(
                    label = "Aviso de término do turno",
                    checked = notificationSettings.notifyShiftEnd,
                    enabled = true,
                    disabledReason = null,
                    onCheckedChange = { updateNotificationSettings(notificationSettings.copy(notifyShiftEnd = it)) }
                )
                PreferenceToggle(
                    label = "Nova escala publicada / meu dia mudou",
                    checked = notificationSettings.notifyScheduleChanged,
                    enabled = true,
                    disabledReason = null,
                    onCheckedChange = { updateNotificationSettings(notificationSettings.copy(notifyScheduleChanged = it)) }
                )

                HorizontalDivider(color = LabColors.outline.copy(alpha = 0.28f))
                Text("Próximos alarmes", color = LabColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (notificationPlan.isEmpty()) {
                    Text("Nenhum alarme futuro calculado com a escala atual.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                } else {
                    notificationPlan.take(5).forEach { item ->
                        StatusLine(notificationTypeLabel(item.type), "${item.triggerAt.date.dateLabel()} ${item.triggerAt.timeLabel()}")
                    }
                }
                notificationSettings.lastRescheduleAt?.let { StatusLine("Última reprogramação", it) }
                rescheduleFeedback?.let { Text(it, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = {
                    val next = notificationSettings.copy(lastRescheduleAt = now.isoMinuteLabel())
                    updateNotificationSettings(next)
                }) {
                    Text("Reprogramar", color = LabColors.primary)
                }
            }
        }
        item {
            LabCard(title = "Pausa de 15 minutos", icon = Icons.Default.Schedule, iconTint = LabColors.tertiary, borderColor = LabColors.tertiary.copy(alpha = 0.30f), gradient = listOf(Color(0xFF0D2832), Color(0xFF092A28), Color(0xFF0D1730))) {
                Text(if (pause != null) "${pause.displayTitle}: ${pause.displayValue}" else "Pausa não configurada", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                PreferenceToggle(
                    label = "Lembrete de pausa",
                    checked = pauseDecision.checked,
                    enabled = pauseDecision.enabled,
                    disabledReason = pauseDecision.disabledReason,
                    onCheckedChange = { updateNotificationSettings(notificationSettings.copy(notifyPause = it)) }
                )
                pause?.let { Text("Permitido entre ${it.windowStart} e ${it.windowEnd}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall) }
                Text("Analista: ${summary.member.scaleName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            LabCard(title = "Resumo", icon = Icons.Default.Checklist, borderColor = LabColors.primary.copy(alpha = 0.22f)) {
                Text("Entrada do turno: ${if (notificationSettings.shiftStartOffsetMinutes == 0) "na hora" else "${notificationSettings.shiftStartOffsetMinutes} min antes"}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Pausa: ${if (notificationSettings.notifyPause) pause?.displayValue ?: "não configurada" else "desativada"}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                pause?.let { Text("Janela permitida: ${it.windowStart}–${it.windowEnd}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item {
            LabCard(title = "Armazenamento local", icon = Icons.Default.Storage, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Arquivo salvo: ${summary.sourceFileName ?: "nenhum arquivo XLS importado"}", color = LabColors.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Status: ${if (summary.isImported) "Escala lida na sessão Web/Android" else "Sem XLS local aplicado"}", color = LabColors.tertiary, style = MaterialTheme.typography.labelMedium)
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
private fun OrganizationResolutionBody(result: OrganizationResolutionResult?) {
    when (result) {
        null -> Text("Buscando vínculo com a organização...", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        is OrganizationResolutionResult.Resolved -> {
            Text(
                "Equipe: ${result.context.primaryTeamName ?: "equipe principal não informada"}",
                color = LabColors.onSurfaceMuted,
                style = MaterialTheme.typography.bodySmall
            )
            result.context.roleDisplayName?.let { role ->
                Text("Função: $role", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
            result.context.dataSourceMessage?.let { message ->
                Text(message, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        is OrganizationResolutionResult.MemberFoundNoActiveTeam ->
            Text("Seu cadastro foi encontrado, mas ainda não possui uma equipe ativa vinculada.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        is OrganizationResolutionResult.MemberNotFound ->
            Text("Conta corporativa autenticada, mas seu cadastro ainda não foi localizado na organização.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        is OrganizationResolutionResult.MemberInactive ->
            Text("Seu cadastro na organização está inativo no momento. Contate o administrador.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        is OrganizationResolutionResult.MemberIdentityAmbiguous ->
            Text("Foram encontrados cadastros duplicados para esta identidade. O responsável pelo cadastro precisa revisar os dados.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        is OrganizationResolutionResult.MembershipNotFound ->
            Text("Seu cadastro foi encontrado, mas ainda não possui nenhum vínculo de equipe registrado.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        is OrganizationResolutionResult.TeamNotFound ->
            Text("Seu vínculo de equipe foi encontrado, mas a equipe correspondente não está mais disponível. Contate o administrador.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        is OrganizationResolutionResult.MultipleActiveTeams ->
            Text("Foram encontrados múltiplos vínculos de equipe ativos. A seleção de equipe estará disponível em uma próxima fase.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        is OrganizationResolutionResult.WorkspaceMismatch,
        is OrganizationResolutionResult.DataSourceUnavailable ->
            Text("Não foi possível confirmar seu vínculo organizacional no momento. Tente novamente mais tarde.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DemoPersonaResolutionSection(
    selectedDemoPersona: DemoPersona,
    demoPersonaResolutionResult: OrganizationResolutionResult?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LabShapes.cardSmall)
            .background(LabColors.tertiary.copy(alpha = 0.10f))
            .border(1.dp, LabColors.tertiary.copy(alpha = 0.45f), LabShapes.cardSmall)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "AMBIENTE DE DEMONSTRAÇÃO",
            color = LabColors.tertiary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text("Personagem: ${selectedDemoPersona.displayName}", color = LabColors.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(demoPersonaResolutionLine(demoPersonaResolutionResult), color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun demoPersonaResolutionLine(result: OrganizationResolutionResult?): String = when (result) {
    null -> "Equipe: buscando vínculo de demonstração..."
    is OrganizationResolutionResult.Resolved -> buildString {
        append("Equipe: ${result.context.primaryTeamName ?: "equipe principal não informada"}")
        result.context.dataSourceMessage?.let { append(" · ").append(it) }
    }
    is OrganizationResolutionResult.MemberFoundNoActiveTeam -> "Seu cadastro foi encontrado, mas ainda não possui uma equipe ativa vinculada."
    is OrganizationResolutionResult.MemberNotFound -> "Conta corporativa autenticada, mas seu cadastro ainda não foi localizado na organização."
    is OrganizationResolutionResult.MemberInactive -> "Seu cadastro na organização está inativo no momento. Contate o administrador."
    is OrganizationResolutionResult.MemberIdentityAmbiguous -> "Foram encontrados cadastros duplicados para esta identidade. O responsável pelo cadastro precisa revisar os dados."
    is OrganizationResolutionResult.MembershipNotFound -> "Seu cadastro foi encontrado, mas ainda não possui nenhum vínculo de equipe registrado."
    is OrganizationResolutionResult.TeamNotFound -> "Seu vínculo de equipe foi encontrado, mas a equipe correspondente não está mais disponível. Contate o administrador."
    is OrganizationResolutionResult.MultipleActiveTeams -> "Múltiplos vínculos de equipe (ver Dashboard)."
    is OrganizationResolutionResult.WorkspaceMismatch,
    is OrganizationResolutionResult.DataSourceUnavailable -> "Não foi possível confirmar seu vínculo organizacional no momento. Tente novamente mais tarde."
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
private fun PreferenceToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    disabledReason: String?,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
    disabledReason?.let {
        Text(it, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
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

@Composable
private fun ChoiceChips(
    values: List<String>,
    selected: String,
    enabled: Boolean,
    disabledReason: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(3).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowValues.forEach { value ->
                    ProfileChip(
                        text = value,
                        selected = value == selected,
                        modifier = Modifier
                            .weight(1f)
                            .then(if (enabled) Modifier.clickable { onSelect(value) } else Modifier)
                    )
                }
                repeat(3 - rowValues.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    disabledReason?.let {
        Text(it, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
    }
}

private fun notificationTypeLabel(type: NotificationType): String = when (type) {
    NotificationType.DAY_BEFORE_WORK -> "Trabalho amanhã"
    NotificationType.DAY_BEFORE_REST -> "Folga amanhã"
    NotificationType.SHIFT_START -> "Entrada"
    NotificationType.SHIFT_END -> "Término"
    NotificationType.PAUSE_START -> "Início da pausa"
    NotificationType.PAUSE_END -> "Fim da pausa"
}

private fun LocalNotificationResult.feedbackMessage(): String = when (this) {
    is LocalNotificationResult.Applied ->
        "$scheduledCount alarmes reconciliados; $cancelledCount cancelados."
    is LocalNotificationResult.Skipped -> reason
}
