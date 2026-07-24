package br.com.leorvergani.escalaici.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.model.ScheduleAssignment
import br.com.leorvergani.escalaici.model.ScheduleChangeRequest
import br.com.leorvergani.escalaici.model.label
import br.com.leorvergani.escalaici.model.requestTypeTyped
import br.com.leorvergani.escalaici.model.statusTyped
import br.com.leorvergani.escalaici.ui.components.LabCard
import br.com.leorvergani.escalaici.ui.theme.LabColors

/**
 * Tela real de "Solicitações de troca" (FASE 14J, spec 67 seção 6.2) - lê
 * `ScheduleChangeRequest` (contrato real do Dashboard), não mais o mock
 * peer-to-peer `ShiftSwapRequest`/`SwapStatus` (FASE 10.11, removido). Somente
 * leitura: sem botões de aprovar/recusar/cancelar funcionais - esta fase não
 * autoriza escrita real no Firestore, mesma decisão honesta já tomada pelo
 * próprio Dashboard (`DemoChangeRequestsDialog`, "Aprovação será habilitada
 * em uma próxima etapa").
 */
@Composable
internal fun ShiftSwapScreen(
    currentMemberId: String,
    changeRequests: List<ScheduleChangeRequest>,
    scheduleAssignments: List<ScheduleAssignment> = emptyList(),
    onBack: () -> Unit
) {
    val relevant = changeRequestsRelevantTo(currentMemberId, changeRequests)
    val sent = relevant.filter { it.memberId == currentMemberId }
    val awaitingApproval = relevant.filter { it.assignedManagerMemberId == currentMemberId }
    val assignmentsById = scheduleAssignments.associateBy { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = LabColors.onSurface)
                }
                Spacer(Modifier.width(4.dp))
                Text("Solicitações de troca", style = MaterialTheme.typography.titleLarge, color = LabColors.onSurface, fontWeight = FontWeight.Bold)
            }
        }
        item { ChangeRequestSectionHeader("Enviadas por você") }
        if (sent.isEmpty()) {
            item { EmptyChangeRequestCard("Nenhuma solicitação enviada.") }
        } else {
            items(sent) { request -> ChangeRequestCard(request, assignmentsById) }
        }
        item { ChangeRequestSectionHeader("Aguardando sua ação") }
        if (awaitingApproval.isEmpty()) {
            item { EmptyChangeRequestCard("Nenhuma solicitação aguardando sua ação.") }
        } else {
            items(awaitingApproval) { request -> ChangeRequestCard(request, assignmentsById) }
        }
    }
}

@Composable
private fun ChangeRequestSectionHeader(title: String) {
    Text(title, color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyChangeRequestCard(message: String) {
    LabCard(borderColor = LabColors.outline.copy(alpha = 0.30f)) {
        Text(message, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChangeRequestCard(request: ScheduleChangeRequest, assignmentsById: Map<String, ScheduleAssignment>) {
    val assignment = request.assignmentId?.let { assignmentsById[it] }

    LabCard(borderColor = LabColors.primary.copy(alpha = 0.24f)) {
        Text(
            request.requestTypeTyped.label(),
            color = LabColors.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (assignment != null) {
            Text(
                "Turno referenciado: ${assignment.shiftType.label}, ${assignment.date}",
                color = LabColors.onSurfaceMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(request.reason, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(
            "Status: ${request.statusTyped.label()}",
            color = LabColors.tertiary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text("Criada em: ${request.createdAt}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        request.resolutionNote?.takeIf { it.isNotBlank() }?.let { note ->
            Text("Nota da resolução: $note", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
