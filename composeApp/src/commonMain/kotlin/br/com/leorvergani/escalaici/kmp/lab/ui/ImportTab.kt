package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleImportPreview
import br.com.leorvergani.escalaici.kmp.lab.ui.components.HeroCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.PageList
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumHeader
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors

@Composable
internal fun ImportTab(
    preview: ScheduleImportPreview?,
    selectedCollaborator: String,
    onSelectXls: () -> Unit,
    onUseImported: () -> Unit,
    onSelectCollaborator: (String) -> Unit,
    onResetMock: () -> Unit,
    onOpenPlantao: () -> Unit
) {
    PageList {
        item {
            LabPremiumHeader(selectedCollaborator = selectedCollaborator, onOpenPlantao = onOpenPlantao)
        }
        item {
            HeroCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Importar escala", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("Leitura XLS do laboratório KMP", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(
                    onClick = onSelectXls,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Text("Selecionar XLS")
                }
            }
        }
        item {
            LabCard(title = "Pré-visualização", badge = if (preview?.canUseImportedData == true) "lido" else "lab", icon = Icons.Default.Checklist) {
                ImportStatusLine("Arquivo", preview?.fileName ?: "Nenhum arquivo selecionado")
                ImportStatusLine("Status", preview?.statusLabel ?: "Aguardando XLS")
                ImportStatusLine("Dias", preview?.daysRead?.let { "$it dias encontrados" } ?: "--")
                ImportStatusLine("Colaboradores", preview?.collaborators?.size?.let { "$it escalistas" } ?: "--")
                ImportStatusLine("Selecionado", preview?.selectedCollaborator ?: "lvergani")
                preview?.sheetNames?.takeIf { it.isNotEmpty() }?.let { sheets ->
                    ImportStatusLine("Abas", sheets.joinToString(", "))
                }
                preview?.collaborators?.takeIf { it.isNotEmpty() }?.let { collaborators ->
                    Text("Escalista da leitura", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    CollaboratorSelector(
                        collaborators = collaborators,
                        selected = preview.selectedCollaborator,
                        onSelect = onSelectCollaborator
                    )
                }
                preview?.warnings.orEmpty().forEach { warning ->
                    ImportWarning(warning)
                }
                preview?.errors.orEmpty().forEach { error ->
                    ImportError(error)
                }
                if (preview == null) {
                    ImportWarning("A leitura usa o parser experimental do KMP Lab, sem alterar o parser oficial Android.")
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onUseImported,
                        enabled = preview?.canUseImportedData == true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LabColors.primary, contentColor = Color.White)
                    ) {
                        Text("Usar dados importados")
                    }
                    OutlinedButton(
                        onClick = onResetMock,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LabColors.onSurface),
                        border = BorderStroke(1.dp, LabColors.outline.copy(alpha = 0.70f))
                    ) {
                        Text("Voltar para mock")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportStatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(106.dp), color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(value, modifier = Modifier.weight(1f), color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CollaboratorSelector(
    collaborators: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    val orderedCollaborators = if (selected.isNullOrBlank()) {
        collaborators
    } else {
        collaborators.filter { it == selected } + collaborators.filterNot { it == selected }
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 2.dp)
    ) {
        items(orderedCollaborators) { collaborator ->
            val active = collaborator == selected
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(collaborator) },
                shape = RoundedCornerShape(14.dp),
                color = if (active) LabColors.primary.copy(alpha = 0.20f) else LabColors.surfaceElevated.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, if (active) LabColors.primary.copy(alpha = 0.62f) else LabColors.outline.copy(alpha = 0.55f))
            ) {
                Text(
                    collaborator,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (active) Color.White else LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ImportWarning(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF59E0B).copy(alpha = 0.11f))
            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(17.dp))
        Text(text, color = Color(0xFFFDE68A), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ImportError(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEF4444).copy(alpha = 0.11f))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
        Text(text, color = Color(0xFFFEE2E2), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
