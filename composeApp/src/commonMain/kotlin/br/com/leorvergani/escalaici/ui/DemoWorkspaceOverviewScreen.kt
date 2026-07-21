package br.com.leorvergani.escalaici.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.identity.DemoDataOrigin
import br.com.leorvergani.escalaici.identity.DemoPersona
import br.com.leorvergani.escalaici.ui.components.LabCard
import br.com.leorvergani.escalaici.ui.components.LabPremiumBackground
import br.com.leorvergani.escalaici.ui.components.PageList
import br.com.leorvergani.escalaici.ui.theme.LabColors

@Composable
internal fun DemoWorkspaceOverviewScreen(
    session: DemoWorkspaceOverviewSession,
    selectedPersona: DemoPersona?,
    isResolvingPersona: Boolean,
    errorMessage: String?,
    onViewAsPersona: (DemoPersona) -> Unit,
    onBack: () -> Unit
) {
    LabPremiumBackground {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
        ) {
            PageList {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Ambiente Demo",
                                color = LabColors.onSurface,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Workspace ${session.overview.workspaceId}",
                                color = LabColors.onSurfaceMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = onBack) {
                            Text("Voltar", color = LabColors.primary)
                        }
                    }
                }
                item {
                    LabCard(
                        title = "Sessão administrativa",
                        icon = Icons.Default.AdminPanelSettings,
                        borderColor = LabColors.tertiary.copy(alpha = 0.34f)
                    ) {
                        OverviewLine("Papel", session.role)
                        OverviewLine("Nome", session.authenticatedDisplayName)
                        OverviewLine("Login", session.authenticatedLogin)
                        OverviewLine("E-mail", session.authenticatedEmail)
                        Text(
                            "A identidade autenticada não foi convertida em persona operacional.",
                            color = LabColors.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                item {
                    LabCard(
                        title = "Publicação",
                        icon = Icons.Default.CloudDone,
                        borderColor = LabColors.primary.copy(alpha = 0.28f)
                    ) {
                        OverviewLine("Origem", session.overview.state.origin.displayLabel())
                        OverviewLine("Revisão", session.overview.state.publicationRevision?.toString() ?: "indisponível")
                        session.overview.state.fallbackCause?.let { cause ->
                            OverviewLine("Fallback", cause.name)
                        }
                        session.overview.state.message?.let { message ->
                            Text(message, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    LabCard(
                        title = "Workspace",
                        icon = Icons.Default.Groups,
                        borderColor = LabColors.primary.copy(alpha = 0.24f)
                    ) {
                        OverviewLine("Membros", "${session.overview.activeMemberCount} ativos de ${session.overview.memberCount}")
                        OverviewLine("Período ativo", session.overview.activePeriodLabel ?: "não publicado")
                        if (session.overview.teamNames.isNotEmpty()) {
                            HorizontalDivider(color = LabColors.outline.copy(alpha = 0.30f))
                            session.overview.teamNames.forEach { teamName ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = LabColors.primary, modifier = Modifier.size(16.dp))
                                    Text(teamName, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                item {
                    LabCard(
                        title = "Visualizar escala",
                        icon = Icons.Default.CalendarMonth,
                        borderColor = LabColors.tertiary.copy(alpha = 0.30f)
                    ) {
                        if (session.overview.operationalPersonas.isEmpty()) {
                            Text(
                                "Nenhuma persona operacional com escala publicada neste workspace.",
                                color = LabColors.onSurfaceMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            session.overview.operationalPersonas.forEach { persona ->
                                PersonaRow(
                                    persona = persona,
                                    loading = selectedPersona == persona && isResolvingPersona,
                                    onClick = { onViewAsPersona(persona) }
                                )
                            }
                        }
                        errorMessage?.let {
                            Text(it, color = LabColors.red, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(
            "$label:",
            color = LabColors.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = LabColors.onSurfaceMuted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PersonaRow(
    persona: DemoPersona,
    loading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(persona.displayName, color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(persona.memberId, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
        }
        Button(onClick = onClick, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LabColors.onSurface, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text("Ver como", modifier = Modifier.padding(start = 6.dp))
        }
    }
}

private fun DemoDataOrigin.displayLabel(): String = when (this) {
    DemoDataOrigin.REMOTE_PUBLICATION -> "publicação remota"
    DemoDataOrigin.LOCAL_FIXTURE -> "fixture local"
    DemoDataOrigin.REMOTE_UNAVAILABLE -> "remoto indisponível"
}
