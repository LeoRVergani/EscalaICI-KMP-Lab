package br.com.leorvergani.escalaici.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.TemporalState
import br.com.leorvergani.escalaici.model.pauseFor
import br.com.leorvergani.escalaici.model.relevantShift
import br.com.leorvergani.escalaici.model.ScheduleSummary
import br.com.leorvergani.escalaici.model.ShiftDay
import br.com.leorvergani.escalaici.ui.components.HeroCard
import br.com.leorvergani.escalaici.ui.components.LabCard
import br.com.leorvergani.escalaici.ui.components.LabPremiumHeader
import br.com.leorvergani.escalaici.ui.theme.LabColors
import br.com.leorvergani.escalaici.ui.theme.shiftColor

@Composable
internal fun TodayTab(summary: ScheduleSummary, today: LabDate, now: LabDateTime, onOpenPlantao: () -> Unit, onImportClick: () -> Unit) {
    val next = summary.nextShift(today)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LabPremiumHeader(selectedCollaborator = summary.member.scaleName, onOpenPlantao = onOpenPlantao)
        }
        item {
            NextTurnHero(summary = summary, now = now, onImportClick = onImportClick)
        }
        item {
            WeekSummaryCard(summary = summary, today = today)
        }
        item {
            EventsCard(summary = summary, today = today)
        }
        item {
            PauseCard(summary = summary, now = now)
        }
        item {
            PeriodSummary(summary = summary)
        }
        if (!next?.note.isNullOrBlank()) {
            item {
                LabCard(title = "Observações da escala") {
                    Text(next?.note.orEmpty(), color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun NextTurnHero(summary: ScheduleSummary, now: LabDateTime, onImportClick: () -> Unit) {
    val occurrence = summary.relevantShift(now)
    val day = occurrence?.day
    HeroCard {
        if (day == null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NENHUM PRÓXIMO TURNO", color = Color(0xFF93C5FD), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                Text(if (summary.isImported) "Sem turnos futuros neste período" else "Importe uma escala", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    "A análise local mantém os dados salvos no dispositivo.",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.bodySmall
                )
                ImportVisualButton(onClick = onImportClick)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(if (occurrence?.state == TemporalState.CURRENT) "TURNO ATUAL" else "PRÓXIMO TURNO", color = Color(0xFF93C5FD), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Text(day.type.label, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(day.type.timeRange, color = day.type.shiftColor(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(day.fullDateLabel, color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = heroMetaText("Analista:", summary.member.scaleName),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            Text(
                text = if (day.teamMembers.isNotEmpty()) heroMetaText("Com:", day.teamMembers.joinToString(", ")) else buildAnnotatedString { append("Equipe não localizada na escala") },
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun heroMetaText(label: String, value: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = Color(0xFF60A5FA), fontWeight = FontWeight.Black)) {
        append(label)
    }
    append(" ")
    append(value)
}

@Composable
private fun ImportVisualButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Importar escala", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun WeekSummaryCard(summary: ScheduleSummary, today: LabDate) {
    val selectedDate = summary.nextShift(today)?.date
    val week = currentWeekWindow(summary.days, today)
    LabCard(
        title = "Resumo da semana",
        badge = if (!summary.isImported) "não importada" else null,
        icon = Icons.Default.CalendarMonth,
        gradient = listOf(Color(0xFF0B1B2C), Color(0xFF0D1A2D)),
        borderColor = LabColors.primary.copy(alpha = 0.42f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            week.forEachIndexed { index, day ->
                val selected = if (selectedDate != null) day.date == selectedDate else index == 0
                WeekDayPill(day = day, selected = selected, modifier = Modifier.weight(if (selected) 1.35f else 1f))
            }
        }
    }
}

/**
 * Janela de 7 dias ancorada em "hoje" real (`todayLabDate()`), não os
 * primeiros 7 registros da lista importada — a escala real começa no dia
 * 26 de um mês, então `days.take(7)` mostrava sempre o início do ciclo
 * importado em vez da semana que contém a data atual do dispositivo.
 */
private fun currentWeekWindow(days: List<ShiftDay>, today: LabDate): List<ShiftDay> {
    val sorted = days.filter { it.date != null }.sortedBy { it.date }
    if (sorted.isEmpty()) return days.take(7)
    val maxStart = (sorted.size - 7).coerceAtLeast(0)
    val todayIndex = sorted.indexOfFirst { it.date!! >= today }
    val startIndex = if (todayIndex < 0) maxStart else todayIndex.coerceAtMost(maxStart)
    return sorted.drop(startIndex).take(7)
}

@Composable
private fun WeekDayPill(day: ShiftDay, selected: Boolean, modifier: Modifier = Modifier) {
    val border = if (selected) LabColors.primary.copy(alpha = 0.50f) else Color.Transparent
    Column(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) LabColors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, border, RoundedCornerShape(15.dp))
            .padding(vertical = 10.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.dayLabel, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(day.dateLabel, color = LabColors.onSurface, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .clip(CircleShape)
                    .background(day.type.shiftColor())
                    .border(1.4.dp, Color.White.copy(alpha = 0.86f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    day.type.shortLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.92f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 3.2f
                        )
                    ),
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun EventsCard(summary: ScheduleSummary, today: LabDate) {
    LabCard(
        title = "Eventos da escala",
        icon = Icons.Default.Checklist,
        gradient = listOf(Color(0xFF0B1B2C), Color(0xFF0D1A2D)),
        borderColor = LabColors.primary.copy(alpha = 0.42f)
    ) {
        EventLine(Icons.Default.CalendarMonth, summary.nextShift(today).eventLabel(), Color(0xFF22D3EE))
        EventLine(Icons.Default.CalendarMonth, summary.nextRest(today).eventLabel(), Color(0xFF22C55E))
        EventLine(Icons.Default.Bolt, "Dias seguidos: ${summary.workedDays.coerceAtMost(5)}", Color(0xFF22D3EE))
    }
}

private fun ShiftDay?.eventLabel(): String = this?.let { "${it.dateLabel} · ${it.label}" } ?: "Não encontrado"

@Composable
private fun EventLine(icon: ImageVector, text: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PauseCard(summary: ScheduleSummary, now: LabDateTime) {
    val pause = pauseFor(summary.relevantShift(now))
    LabCard(
        title = "Pausa",
        icon = Icons.Default.Schedule,
        iconTint = LabColors.tertiary,
        borderColor = Color(0xFF14B8A6).copy(alpha = 0.46f),
        gradient = listOf(Color(0xFF0D2832), Color(0xFF092A28), Color(0xFF0D1730))
    ) {
        Text(pause?.displayValue ?: "Pausa não configurada", color = Color(0xFF30F188), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        pause?.let { Text(it.displayTitle, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun PeriodSummary(summary: ScheduleSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    if (summary.isImported) "RESUMO DO PERÍODO" else "RESUMO DA SEMANA",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (summary.isImported) summary.periodLabel else "Nenhuma escala importada",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (!summary.isImported) DemoBadge()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Trabalho", "${summary.workedDays}d", Icons.Default.Work, LabColors.primary, Modifier.weight(1f))
            MetricCard("Folga", "${summary.restDays}d", Icons.Default.BeachAccess, LabColors.tertiary, Modifier.weight(1f))
            MetricCard("Horas", "${summary.totalHours}h", Icons.Default.Schedule, Color(0xFFF59E0B), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DemoBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LabColors.primary.copy(alpha = 0.16f))
            .border(1.dp, LabColors.primary.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text("não importada", color = LabColors.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.12f), LabColors.surfaceElevated.copy(alpha = 0.92f))))
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(value, color = LabColors.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(label, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
    }
}
