package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftDay
import br.com.leorvergani.escalaici.kmp.lab.ui.components.HeroCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumHeader
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.shiftColor

@Composable
internal fun TodayTab(summary: ScheduleSummary, onOpenPlantao: () -> Unit) {
    val next = summary.nextShift
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LabPremiumHeader(selectedCollaborator = summary.member.scaleName, onOpenPlantao = onOpenPlantao)
        }
        item {
            NextTurnHero(summary = summary)
        }
        item {
            WeekSummaryCard(summary = summary)
        }
        item {
            EventsCard(summary = summary)
        }
        item {
            PauseCard(summary = summary)
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
private fun NextTurnHero(summary: ScheduleSummary) {
    val day = summary.nextShift
    HeroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PRÓXIMO TURNO", color = Color(0xFF93C5FD), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                Text(day?.type?.label ?: "Importe uma escala", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(day?.type?.timeRange ?: "Selecione um arquivo XLS", color = day?.type?.shiftColor() ?: LabColors.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(day?.fullDateLabel ?: "Demonstração local", color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text("Analista: ${summary.member.scaleName}", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
            }
            WeatherChip()
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
        Text(
            text = if (day?.teamMembers?.isNotEmpty() == true) "Com: ${day.teamMembers.joinToString(", ")}" else "Equipe mock do laboratório",
            color = Color.White.copy(alpha = 0.74f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WeatherChip() {
    Column(
        modifier = Modifier.width(58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        Text("Clima", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
        Text("17°", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeekSummaryCard(summary: ScheduleSummary) {
    val selectedDate = summary.nextShift?.date ?: summary.days.firstOrNull()?.date
    LabCard(title = "Resumo da semana", badge = if (summary.isImported) "xls" else "demo", icon = Icons.Default.CalendarMonth) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            summary.days.take(7).forEachIndexed { index, day ->
                val selected = if (selectedDate != null) day.date == selectedDate else index == 0
                WeekDayPill(day = day, selected = selected, modifier = Modifier.weight(if (selected) 1.35f else 1f))
            }
        }
    }
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
private fun EventsCard(summary: ScheduleSummary) {
    LabCard(title = "Eventos da escala", icon = Icons.Default.Checklist) {
        EventLine(Icons.Default.CalendarMonth, "${summary.nextShift?.dateLabel ?: "--/--"} · ${summary.nextShift?.type?.label ?: "Sem turno"}", Color(0xFF22D3EE))
        EventLine(Icons.Default.CalendarMonth, "${summary.nextRest?.dateLabel ?: "--/--"} · ${summary.nextRest?.type?.label ?: "Sem folga"}", Color(0xFF22C55E))
        EventLine(Icons.Default.Bolt, "Dias seguidos: ${summary.workedDays.coerceAtMost(5)}", Color(0xFF22D3EE))
    }
}

@Composable
private fun EventLine(icon: ImageVector, text: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PauseCard(summary: ScheduleSummary) {
    LabCard(
        title = "Pausa",
        icon = Icons.Default.Schedule,
        iconTint = LabColors.tertiary,
        borderColor = LabColors.tertiary.copy(alpha = 0.46f),
        gradient = listOf(Color(0xFF0D2832), Color(0xFF092A28), Color(0xFF0D1730))
    ) {
        Text(summary.pauseLabel, color = Color(0xFF30F188), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(summary.pauseOffsetLabel, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PeriodSummary(summary: ScheduleSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text("RESUMO DO PERÍODO", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Text(summary.periodLabel, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Trabalho", "${summary.workedDays}d", Icons.Default.Work, LabColors.primary, Modifier.weight(1f))
            MetricCard("Folga", "${summary.restDays}d", Icons.Default.BeachAccess, LabColors.tertiary, Modifier.weight(1f))
            MetricCard("Horas", "${summary.totalHours}h", Icons.Default.Schedule, Color(0xFFF59E0B), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.13f), LabColors.surfaceElevated.copy(alpha = 0.92f))))
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        }
        Text(value, color = LabColors.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(label, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
    }
}
