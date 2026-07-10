package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftSwapRequest
import br.com.leorvergani.escalaici.kmp.lab.model.SwapStatus
import br.com.leorvergani.escalaici.kmp.lab.model.mockShiftSwapRequests
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors

private enum class ShiftSwapSection { RECEIVED, SENT }

/**
 * Porte de `ui/swap/ShiftSwapScreen.kt` (app real): sem Firestore — usa
 * `ShiftSwapRequest`/`SwapStatus` (estendidos na FASE 10.11) e
 * `mockShiftSwapRequests()`, com aceitar/recusar/cancelar mudando o status
 * em memória (sem persistência real).
 */
@Composable
internal fun ShiftSwapScreen(currentMemberId: String, onBack: () -> Unit) {
    var requests by remember { mutableStateOf(mockShiftSwapRequests()) }
    val received = requests.filter { it.targetMemberId == currentMemberId }
    val sent = requests.filter { it.requesterMemberId == currentMemberId }

    fun updateStatus(id: String, status: SwapStatus) {
        requests = requests.map { if (it.id == id) it.copy(status = status) else it }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = LabColors.onSurface)
                }
                Spacer(Modifier.width(4.dp))
                Text("Trocas de escala", style = MaterialTheme.typography.titleLarge, color = LabColors.onSurface, fontWeight = FontWeight.Bold)
            }
        }
        item {
            ShiftSwapSectionHeader("Recebidos")
        }
        if (received.isEmpty()) {
            item { EmptySwapCard("Nenhuma solicitação recebida.") }
        } else {
            items(received) { request ->
                ShiftSwapRequestCard(
                    request = request,
                    section = ShiftSwapSection.RECEIVED,
                    onAccept = { updateStatus(request.id, SwapStatus.APROVADA) },
                    onRefuse = { updateStatus(request.id, SwapStatus.RECUSADA_TECNICO_DESTINO) },
                    onCancel = {}
                )
            }
        }
        item {
            ShiftSwapSectionHeader("Enviados")
        }
        if (sent.isEmpty()) {
            item { EmptySwapCard("Nenhuma solicitação enviada.") }
        } else {
            items(sent) { request ->
                ShiftSwapRequestCard(
                    request = request,
                    section = ShiftSwapSection.SENT,
                    onAccept = {},
                    onRefuse = {},
                    onCancel = { updateStatus(request.id, SwapStatus.CANCELADA) }
                )
            }
        }
    }
}

@Composable
private fun ShiftSwapSectionHeader(title: String) {
    Text(title, color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptySwapCard(message: String) {
    LabCard(borderColor = LabColors.outline.copy(alpha = 0.30f)) {
        Text(message, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ShiftSwapRequestCard(
    request: ShiftSwapRequest,
    section: ShiftSwapSection,
    onAccept: () -> Unit,
    onRefuse: () -> Unit,
    onCancel: () -> Unit
) {
    val otherName = when (section) {
        ShiftSwapSection.RECEIVED -> request.requesterName
        ShiftSwapSection.SENT -> request.targetName
    }
    val firstShiftText = when (section) {
        ShiftSwapSection.RECEIVED -> "Seu turno: ${request.targetShiftType?.label ?: "-"}, ${request.requestedDate}"
        ShiftSwapSection.SENT -> "Seu turno: ${request.requesterShiftType?.label ?: "-"}, ${request.originalDate}"
    }
    val secondShiftText = when (section) {
        ShiftSwapSection.RECEIVED -> "Turno de $otherName: ${request.requesterShiftType?.label ?: "-"}, ${request.originalDate}"
        ShiftSwapSection.SENT -> "Turno de $otherName: ${request.targetShiftType?.label ?: "-"}, ${request.requestedDate}"
    }

    LabCard(borderColor = LabColors.primary.copy(alpha = 0.24f)) {
        Text(otherName, color = LabColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(firstShiftText, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        Text(secondShiftText, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("Time: ${request.teamName ?: "Não identificado"}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        Text("Status: ${request.status.toDisplayText()}", color = LabColors.tertiary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text("Criada em: ${request.createdAt}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)

        if (request.status == SwapStatus.PENDENTE_TECNICO_DESTINO) {
            when (section) {
                ShiftSwapSection.RECEIVED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = LabColors.primary)) {
                            Text("Aceitar")
                        }
                        TextButton(onClick = onRefuse) {
                            Text("Recusar", color = LabColors.primary)
                        }
                    }
                }
                ShiftSwapSection.SENT -> {
                    TextButton(onClick = onCancel) {
                        Text("Cancelar", color = LabColors.primary)
                    }
                }
            }
        }
    }
}

private fun SwapStatus.toDisplayText(): String = when (this) {
    SwapStatus.PENDENTE_TECNICO_DESTINO -> "Aguardando colega"
    SwapStatus.AGUARDANDO_COORDENADOR -> "Aguardando coordenador"
    SwapStatus.APROVADA -> "Aprovada"
    SwapStatus.RECUSADA_TECNICO_DESTINO, SwapStatus.RECUSADA_COORDENADOR -> "Recusada"
    SwapStatus.CANCELADA -> "Cancelada"
}
