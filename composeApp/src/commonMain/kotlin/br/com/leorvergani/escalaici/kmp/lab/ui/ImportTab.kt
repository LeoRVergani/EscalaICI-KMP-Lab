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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleImportPreview
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumHeader
import br.com.leorvergani.escalaici.kmp.lab.ui.components.PageList
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabShapes

/**
 * Porte de `ui/settings/ImportScaleScreen.kt` (app real): título/subtítulo
 * simples (sem hero), `LocalFileCard` clicável, `ScaleSummaryCard` com
 * diagnóstico OK/Atenção por aba, `IdentifiedCollaboratorCard` e
 * `CloudFileCard`. Adição própria do laboratório (documentada): o seletor
 * de colaborador da pré-visualização — o app real não precisa disso porque
 * a identidade vem do login, não de uma escolha manual na tela de import.
 */
@Composable
internal fun ImportTab(
    preview: ScheduleImportPreview?,
    selectedCollaborator: String,
    onSelectXls: () -> Unit,
    onFetchFromDropbox: () -> Unit,
    isFetchingFromCloud: Boolean,
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Importar escala", color = LabColors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Escolha a origem do arquivo da escala que deseja analisar.",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            LocalFileCard(preview = preview, onClick = onSelectXls)
        }
        preview?.let { p ->
            item { ScaleSummaryCard(preview = p) }
            if (p.collaborators.isNotEmpty()) {
                item { CollaboratorPreviewCard(preview = p, onSelectCollaborator = onSelectCollaborator) }
            }
            item { IdentifiedCollaboratorCard(preview = p) }
        }
        item {
            CloudFileCard(
                onChooseFile = onSelectXls,
                onFetchFromDropbox = onFetchFromDropbox,
                isFetchingFromCloud = isFetchingFromCloud
            )
        }
        item {
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
                    Text("Remover escala importada")
                }
            }
        }
    }
}

@Composable
private fun LocalFileCard(preview: ScheduleImportPreview?, onClick: () -> Unit) {
    val hasError = preview != null && preview.errors.isNotEmpty()
    val accent = when {
        hasError -> LabColors.red
        preview != null -> LabColors.tertiary
        else -> LabColors.primary
    }

    Surface(
        onClick = onClick,
        shape = LabShapes.cardMedium,
        color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = if (preview == null) 0.35f else 0.50f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(LabColors.surface.copy(alpha = 0.96f), LabColors.surfaceElevated.copy(alpha = 0.90f))))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasError) Icons.Default.Error else Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = accent
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Arquivo local", color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    when {
                        preview == null -> {
                            Text("Selecionar arquivo XLS/XLSX", color = LabColors.onSurface, style = MaterialTheme.typography.titleSmall)
                            Text("O arquivo será validado antes de importar.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        hasError -> {
                            Text(preview.errors.first(), color = LabColors.red, style = MaterialTheme.typography.titleSmall)
                            Text("Toque para tentar novamente.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {
                            SuccessStatusPill(text = "Escala analisada e salva com sucesso", color = LabColors.tertiary)
                            Spacer(Modifier.height(8.dp))
                            Text(preview.fileName, color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Dias processados: ${preview.daysRead}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            Text("Status: salva apenas neste dispositivo (sem sincronização)", color = LabColors.primary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LabColors.onSurfaceMuted)
            }
        }
    }
}

@Composable
private fun SuccessStatusPill(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ScaleSummaryCard(preview: ScheduleImportPreview) {
    val hasEscalistas = preview.sheetNames.any { it.equals("Escalistas", ignoreCase = true) }
    val hasEscala = preview.sheetNames.any { it.equals("Escala", ignoreCase = true) }
    LabCard(
        title = "Escala analisada e salva",
        borderColor = if (preview.errors.isEmpty()) LabColors.tertiary.copy(alpha = 0.40f) else LabColors.red.copy(alpha = 0.45f)
    ) {
        Text(preview.fileName, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)

        DiagnosticLine(label = "Aba Escalistas", ok = hasEscalistas)
        DiagnosticLine(label = "Aba Escala", ok = hasEscala)

        HorizontalDivider(color = LabColors.onSurfaceMuted.copy(alpha = 0.20f))
        SectionLine("Abas encontradas", preview.sheetNames.joinToString(", ").ifBlank { "Nenhuma" })
        SectionLine("Colaborador selecionado", preview.selectedCollaborator ?: "Nenhum", valueColor = LabColors.primary)
        SectionLine("Dias processados", "${preview.daysRead} dias")
        SectionLine("Status", "Salva apenas neste dispositivo (sem sincronização)", valueColor = LabColors.tertiary)

        if (preview.collaborators.isNotEmpty()) {
            SectionLine("Colaboradores encontrados", preview.collaborators.joinToString(", "))
        }

        val messages = preview.errors + preview.warnings
        if (messages.isNotEmpty()) {
            val color = if (preview.errors.isEmpty()) Color(0xFFF59E0B) else LabColors.red
            Text("Avisos", color = color, style = MaterialTheme.typography.labelLarge)
            messages.forEach { message ->
                Text(message, color = color, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SectionLine(label: String, value: String, valueColor: Color = LabColors.onSurfaceMuted) {
    Column {
        Text(label, color = LabColors.onSurface, style = MaterialTheme.typography.labelLarge)
        Text(value, color = valueColor, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DiagnosticLine(label: String, ok: Boolean) {
    val color = if (ok) LabColors.tertiary else Color(0xFFF59E0B)
    Text(
        text = "${if (ok) "OK" else "Atenção"} - $label ${if (ok) "encontrada" else "não encontrada"}",
        color = color,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun IdentifiedCollaboratorCard(preview: ScheduleImportPreview) {
    val collaborator = preview.selectedCollaborator?.takeIf { it.isNotBlank() }
    LabCard(title = "Identidade da escala", borderColor = LabColors.primary.copy(alpha = 0.25f)) {
        if (collaborator == null) {
            Text(
                "Não encontramos um colaborador identificado nesta leitura.",
                color = LabColors.red,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Text(
                "Colaborador identificado: $collaborator",
                color = LabColors.onSurfaceMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Adição própria do laboratório (não existe no app real): permite trocar o
 * colaborador da pré-visualização, já que aqui a identidade não vem de um
 * login de verdade.
 */
@Composable
private fun CollaboratorPreviewCard(preview: ScheduleImportPreview, onSelectCollaborator: (String) -> Unit) {
    LabCard(title = "Trocar colaborador da leitura", borderColor = LabColors.outline.copy(alpha = 0.36f)) {
        val ordered = preview.selectedCollaborator?.let { selected ->
            preview.collaborators.filter { it == selected } + preview.collaborators.filterNot { it == selected }
        } ?: preview.collaborators
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 2.dp)
        ) {
            items(ordered) { collaborator ->
                val active = collaborator == preview.selectedCollaborator
                Surface(
                    modifier = Modifier.clip(LabShapes.chip).clickable { onSelectCollaborator(collaborator) },
                    shape = LabShapes.chip,
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
}

@Composable
private fun CloudFileCard(
    onChooseFile: () -> Unit,
    onFetchFromDropbox: () -> Unit,
    isFetchingFromCloud: Boolean
) {
    LabCard(borderColor = LabColors.primary.copy(alpha = 0.32f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(LabShapes.cardSmall).background(LabColors.primary.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = LabColors.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Arquivo em nuvem", color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Importe pelo seletor do sistema ou baixe a escala publicada no Dropbox (mesmo link do app Android).",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onChooseFile, enabled = !isFetchingFromCloud) {
                Text("Escolher arquivo", color = LabColors.primary, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onFetchFromDropbox, enabled = !isFetchingFromCloud) {
                if (isFetchingFromCloud) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = LabColors.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Buscando no Dropbox…", color = LabColors.onSurfaceMuted)
                } else {
                    Text("Procurar escalas (Dropbox)", color = LabColors.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
