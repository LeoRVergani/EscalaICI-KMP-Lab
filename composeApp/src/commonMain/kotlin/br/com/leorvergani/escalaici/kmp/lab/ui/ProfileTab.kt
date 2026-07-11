package br.com.leorvergani.escalaici.kmp.lab.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.AppVersion
import br.com.leorvergani.escalaici.kmp.lab.model.GenerateLabAlerts
import br.com.leorvergani.escalaici.kmp.lab.model.LabAlert
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCollaboratorAvatar
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumHeader
import br.com.leorvergani.escalaici.kmp.lab.ui.components.PageList
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabShapes
import br.com.leorvergani.escalaici.kmp.lab.ui.util.initials

@Composable
internal fun ProfileTab(
    summary: ScheduleSummary,
    onLogout: () -> Unit,
    onOpenPlantao: () -> Unit,
    onOpenSwap: () -> Unit
) {
    val criticalAlerts = remember(summary) { GenerateLabAlerts(summary).count { it.severity == LabAlert.Severity.CRITICO } }
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
                            if (summary.isImported) "Escala salva apenas neste dispositivo" else "Dados de exemplo",
                            color = LabColors.tertiary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Text("Período: ${summary.periodLabel}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Fonte: ${summary.sourceFileName ?: "dados de exemplo"}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(summary.member.email, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onLogout) {
                    Text("Sair (login de teste)", color = LabColors.primary)
                }
            }
        }
        item {
            LabCard(title = "Identidade da escala", icon = Icons.Default.Security, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Colaborador identificado: ${summary.member.scaleName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("A associação Microsoft -> member -> teamId já está representada visualmente aqui.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
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
            LabCard(title = "Administração da escala", icon = Icons.Default.AdminPanelSettings, borderColor = LabColors.primary.copy(alpha = 0.22f)) {
                StatusLine("Modo ADM", "ainda não implementado")
                StatusLine("OneDrive ADM", "não conectado")
                StatusLine("Dropbox ADM", "não conectado")
                StatusLine("Mês atual", if (summary.isImported) "arquivo carregado" else "aguardando importação")
                DisabledAction("Importação Firebase")
                DisabledAction("Buscar escala no OneDrive ADM")
                DisabledAction("Conectar Dropbox ADM")
                DisabledAction("Publicar escala no Dropbox")
            }
        }
        item {
            LabCard(title = "Conta corporativa", icon = Icons.Default.Security, borderColor = LabColors.tertiary.copy(alpha = 0.25f)) {
                StatusLine("Conta", "login Microsoft ainda não conectado")
                Text("Entre com sua conta Microsoft para identificar seu usuário. Login MSAL real está na FASE 11.3.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                DisabledAction("Entrar com conta corporativa")
            }
        }
        item {
            LabCard(title = "Login de teste", icon = Icons.Default.Person, borderColor = LabColors.outline.copy(alpha = 0.32f)) {
                Text("Colaboradores de teste, sem dados reais.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                DisabledAction("Entrar como Teste SOC A")
                DisabledAction("Entrar como Teste SOC B")
                DisabledAction("Entrar como Aprovador SOC")
                DisabledAction("Criar/atualizar time de teste")
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
                StatusLine("Status", "ativas visualmente")
                Text("Alertas são problemas detectados na escala. Notificações são lembretes enviados pelo celular.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                VisualToggle("Plantão amanhã", true)
                VisualToggle("Folga amanhã", true)
                VisualToggle("Saída do turno", false)
                HorizontalDivider(color = LabColors.outline.copy(alpha = 0.22f))
                Text("Entrada do turno", color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Receba um aviso antes do seu turno começar.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileChip("No horário", false, Modifier.weight(1f))
                    ProfileChip("5 min antes", false, Modifier.weight(1f))
                    ProfileChip("10 min antes", false, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileChip("15 min antes", true, Modifier.weight(1f))
                    ProfileChip("30 min antes", false, Modifier.weight(1f))
                    ProfileChip("1h antes", false, Modifier.weight(1f))
                }
                Text("Analista: ${summary.member.scaleName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                DisabledAction("Reprogramar notificações")
            }
        }
        item {
            LabCard(title = "Pausa de 15 minutos", icon = Icons.Default.Schedule, iconTint = LabColors.tertiary, borderColor = LabColors.tertiary.copy(alpha = 0.30f), gradient = listOf(Color(0xFF0D2832), Color(0xFF092A28), Color(0xFF0D1730))) {
                Text("Horário calculado a partir do próximo turno do analista selecionado.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                VisualToggle("Lembrete de pausa", true)
                Text("Permitido 1h após o início do turno", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileChip(summary.pauseLabel.substringBefore(" - "), true, Modifier.weight(1f))
                    ProfileChip("Outro horário", false, Modifier.weight(1f))
                }
                Text("Analista: ${summary.member.scaleName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            LabCard(title = "Resumo", icon = Icons.Default.Checklist, borderColor = LabColors.primary.copy(alpha = 0.22f)) {
                Text("Entrada do turno: 15 min antes", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Pausa: ${summary.pauseLabel}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Janela permitida: ${summary.pauseOffsetLabel}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            LabCard(title = "Armazenamento local", icon = Icons.Default.Storage, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Arquivo salvo: ${summary.sourceFileName ?: "dados de exemplo"}", color = LabColors.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Status: ${if (summary.isImported) "Escala lida na sessão Web/Android" else "Sem XLS aplicado"}", color = LabColors.tertiary, style = MaterialTheme.typography.labelMedium)
                DisabledAction("Remover escala local")
            }
        }
        item {
            LabCard(title = "Aplicativo", icon = Icons.Default.SystemUpdate, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Versão atual: ${AppVersion.LABEL}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Atualização APK/Dropbox fica no app Android real.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                DisabledAction("Atualizar aplicativo")
            }
        }
        item {
            LabCard(title = "Migração KMP", icon = Icons.Default.CloudDone, borderColor = LabColors.outline.copy(alpha = 0.35f)) {
                Text(
                    "Projeto oficial em Kotlin Multiplatform, rodando em paralelo ao app Android atual até substituí-lo. " +
                        "Dropbox real já funciona no Android (Web depende da autorização OAuth do Dropbox). " +
                        "MSAL, Firebase, notificações reais e parser oficial completo seguem em andamento.",
                    color = LabColors.onSurfaceMuted
                )
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
    TextButton(onClick = {}) {
        Text(label, color = LabColors.primary.copy(alpha = 0.82f))
    }
}

@Composable
private fun VisualToggle(label: String, checked: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = {})
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
