package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.GenerateLabAlerts
import br.com.leorvergani.escalaici.kmp.lab.model.LabAlert
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumHeader
import br.com.leorvergani.escalaici.kmp.lab.ui.components.PageList
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabShapes

@Composable
internal fun AlertsTab(summary: ScheduleSummary, onOpenPlantao: () -> Unit) {
    val alerts = remember(summary) { GenerateLabAlerts(summary) }
    var filter by remember { mutableStateOf(AlertFilter.TODOS) }
    val filteredAlerts = alerts.filter { filter.matches(it) }

    PageList {
        item {
            LabPremiumHeader(selectedCollaborator = summary.member.scaleName, onOpenPlantao = onOpenPlantao)
        }
        item {
            AlertsHero(summary = summary, alerts = alerts)
        }
        item {
            AlertSummaryRow(alerts = alerts)
        }
        item {
            AlertFilterRow(selected = filter, onSelect = { filter = it })
        }
        if (filteredAlerts.isEmpty()) {
            item {
                LabCard(
                    title = "Nenhum alerta neste filtro",
                    icon = Icons.Default.CheckCircle,
                    iconTint = LabColors.tertiary,
                    borderColor = LabColors.tertiary.copy(alpha = 0.42f)
                ) {
                    Text("A escala atual não possui alertas com esta severidade.", color = LabColors.onSurfaceMuted)
                }
            }
        } else {
            items(filteredAlerts) { alert ->
                PremiumAlertCard(alert = alert, summary = summary)
            }
        }
    }
}

/**
 * Porte literal de `AlertsHero` (`ui/alerts/AlertsScreen.kt` real): shape
 * `cardLarge`, borda `primary@0.30`, gradiente `#0B274F,#111A31,#24104D` —
 * sem a textura `NocHeroTexture` do `HeroCard` compartilhado (o real não
 * tem textura aqui), por isso não reaproveita o `HeroCard`.
 */
@Composable
private fun AlertsHero(summary: ScheduleSummary, alerts: List<LabAlert>) {
    Surface(
        shape = LabShapes.cardLarge,
        color = Color.Transparent,
        border = BorderStroke(1.dp, LabColors.primary.copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0B274F), Color(0xFF111A31), Color(0xFF24104D))))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alertas da escala", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("${alerts.size} itens gerados pela escala ${if (summary.isImported) "real" else "de exemplo"}", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                    }
                    CountBadge(alerts.size)
                }
                Text("Analista: ${summary.member.scaleName}", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Período: ${summary.periodLabel}", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(LabShapes.cardSmall)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), LabShapes.cardSmall),
        contentAlignment = Alignment.Center
    ) {
        Text("$count", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AlertSummaryRow(alerts: List<LabAlert>) {
    val items = listOf(
        AlertSummarySpec("Críticos", alerts.count { it.severity == LabAlert.Severity.CRITICO }, AlertCriticalColor),
        AlertSummarySpec("Atenção", alerts.count { it.severity == LabAlert.Severity.ATENCAO }, AlertWarningColor),
        AlertSummarySpec("Info", alerts.count { it.severity == LabAlert.Severity.INFO }, AlertInfoColor)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items.forEach { item ->
            AlertSummaryCard(item = item, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AlertSummaryCard(item: AlertSummarySpec, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(LabShapes.cardMedium)
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(item.color.copy(alpha = 0.20f), LabColors.surface.copy(alpha = 0.94f))))
            .border(1.dp, item.color.copy(alpha = 0.46f), LabShapes.cardMedium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(31.dp).clip(CircleShape).background(item.color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text(item.count.toString(), color = item.color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        }
        Text(item.label, color = LabColors.onSurface, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AlertFilterRow(selected: AlertFilter, onSelect: (AlertFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AlertFilter.entries) { filter ->
            val active = filter == selected
            val color = filter.color
            Row(
                modifier = Modifier
                    .clip(LabShapes.chip)
                    .background(color.copy(alpha = if (active) 0.20f else 0.08f))
                    .border(1.dp, color.copy(alpha = if (active) 0.48f else 0.22f), LabShapes.chip)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filter == AlertFilter.TODOS) {
                    Icon(Icons.Default.FilterAlt, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                }
                Text(filter.label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PremiumAlertCard(alert: LabAlert, summary: ScheduleSummary) {
    val color = alert.severity.alertColor()
    Surface(
        shape = LabShapes.cardMedium,
        color = LabColors.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(color))
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.26f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(alert.severity.alertIcon(), contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Text(alert.title, color = LabColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Spacer(Modifier.size(8.dp))
                        SeverityBadge(alert.severity)
                    }
                    Text(alert.message, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    AlertContextLine(alert = alert, summary = summary)
                    alert.date?.let { date ->
                        Text(date.fullDateLabel(), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertContextLine(alert: LabAlert, summary: ScheduleSummary) {
    val text = if (alert.title.contains("Fonte", ignoreCase = true)) {
        "${summary.periodLabel} • Analista: ${summary.member.scaleName}"
    } else {
        "Fonte: ${summary.sourceFileName ?: "dados de exemplo"}"
    }
    Text(
        text,
        color = LabColors.onSurfaceMuted.copy(alpha = 0.72f),
        style = MaterialTheme.typography.labelSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SeverityBadge(severity: LabAlert.Severity) {
    val color = severity.alertColor()
    Box(modifier = Modifier.clip(LabShapes.chip).background(color.copy(alpha = 0.15f)).border(1.dp, color.copy(alpha = 0.34f), LabShapes.chip).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(severity.label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

private data class AlertSummarySpec(val label: String, val count: Int, val color: Color)

private enum class AlertFilter(val label: String, val color: Color) {
    TODOS("Todos", LabColors.primary),
    CRITICO("Crítico", AlertCriticalColor),
    ATENCAO("Atenção", AlertWarningColor),
    INFO("Info", AlertInfoColor);

    fun matches(alert: LabAlert): Boolean = when (this) {
        TODOS -> true
        CRITICO -> alert.severity == LabAlert.Severity.CRITICO
        ATENCAO -> alert.severity == LabAlert.Severity.ATENCAO
        INFO -> alert.severity == LabAlert.Severity.INFO
    }
}

private fun LabAlert.Severity.alertColor(): Color = when (this) {
    LabAlert.Severity.CRITICO -> AlertCriticalColor
    LabAlert.Severity.ATENCAO -> AlertWarningColor
    LabAlert.Severity.INFO -> AlertInfoColor
}

private fun LabAlert.Severity.alertIcon(): ImageVector = when (this) {
    LabAlert.Severity.CRITICO -> Icons.Default.Warning
    LabAlert.Severity.ATENCAO -> Icons.Default.Warning
    LabAlert.Severity.INFO -> Icons.Default.Analytics
}

private val AlertCriticalColor = Color(0xFFEF4444)
private val AlertWarningColor = Color(0xFFF59E0B)
private val AlertInfoColor = Color(0xFF3B82F6)
