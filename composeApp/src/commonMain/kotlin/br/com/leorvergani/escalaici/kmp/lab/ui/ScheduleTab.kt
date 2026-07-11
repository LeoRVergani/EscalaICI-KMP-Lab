package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.leorvergani.escalaici.kmp.lab.model.LabDate
import br.com.leorvergani.escalaici.kmp.lab.model.LabYearMonth
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftDay
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftType
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumHeader
import br.com.leorvergani.escalaici.kmp.lab.ui.components.PageList
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabShapes
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.shiftColor

@Composable
internal fun ScheduleTab(summary: ScheduleSummary, onOpenPlantao: () -> Unit) {
    val sortedDays = summary.days.sortedBy { it.date }
    val initialDay = remember(summary) {
        sortedDays.firstOrNull { it.type.isWorkShift } ?: sortedDays.firstOrNull()
    }
    var selectedDate by remember(summary) { mutableStateOf(initialDay?.date) }
    var visibleMonth by remember(summary) { mutableStateOf((selectedDate ?: sortedDays.firstNotNullOfOrNull { it.date } ?: LabDate(2026, 7, 1)).yearMonth()) }
    var showLegend by remember { mutableStateOf(false) }
    val daysByDate = remember(summary) {
        sortedDays.mapNotNull { day -> day.date?.let { it to day } }.toMap()
    }
    val firstMonth = sortedDays.firstNotNullOfOrNull { it.date }?.yearMonth() ?: visibleMonth
    val lastMonth = sortedDays.mapNotNull { it.date }.lastOrNull()?.yearMonth() ?: visibleMonth
    val selectedDay = selectedDate?.let { daysByDate[it] }
    val monthDays = sortedDays.filter { it.date?.yearMonth() == visibleMonth }

    fun moveMonth(offset: Int) {
        val target = visibleMonth.plusMonths(offset)
        visibleMonth = target
        selectedDate = sortedDays.firstOrNull { it.date?.yearMonth() == target && it.type.isWorkShift }?.date
            ?: sortedDays.firstOrNull { it.date?.yearMonth() == target }?.date
            ?: target.atDay(1)
    }

    PageList {
        item {
            LabPremiumHeader(selectedCollaborator = summary.member.scaleName, onOpenPlantao = onOpenPlantao)
        }
        if (!summary.isImported) {
            item {
                DemoCalendarCard()
            }
        }
        item {
            CalendarMonthHeader(
                visibleMonth = visibleMonth,
                period = summary.periodLabel,
                canGoPrevious = visibleMonth.isAfter(firstMonth),
                canGoNext = visibleMonth.isBefore(lastMonth),
                onPrevious = { moveMonth(-1) },
                onNext = { moveMonth(1) }
            )
        }
        item {
            PeriodCalendarView(
                yearMonth = visibleMonth,
                daysByDate = daysByDate,
                selectedDate = selectedDate,
                onDateClick = { date ->
                    selectedDate = date
                }
            )
        }
        item {
            LegendChip(expanded = showLegend, onClick = { showLegend = !showLegend })
        }
        if (showLegend) {
            item {
                InlineLegendCard()
            }
        }
        selectedDay?.let { day ->
            item {
                CalendarDayDetailCard(day = day)
            }
            item {
                TeamOnDutyCard(day = day, selectedCollaborator = summary.member.scaleName)
            }
        }
        if (selectedDay == null) {
            item {
                LabCard(title = "Dia sem escala", icon = Icons.Default.CalendarMonth, borderColor = LabColors.outline.copy(alpha = 0.42f)) {
                    Text("Selecione um dia com marcador para ver os detalhes da escala.", color = LabColors.onSurfaceMuted)
                }
            }
        }
        item {
            LabCard(title = "Lista do mês", badge = visibleMonth.periodMonthLabel(), icon = Icons.Default.Checklist) {
                Text("${monthDays.size} dia(s) da escala neste mês.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        items(monthDays) { day ->
            ShiftDayRow(day = day)
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    visibleMonth: LabYearMonth,
    period: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MonthNavButton(enabled = canGoPrevious, onClick = onPrevious, previous = true)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                visibleMonth.monthTitle(),
                color = LabColors.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                period,
                color = LabColors.onSurfaceMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MonthNavButton(enabled = canGoNext, onClick = onNext, previous = false)
    }
}

@Composable
private fun MonthNavButton(enabled: Boolean, onClick: () -> Unit, previous: Boolean) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = LabColors.surface.copy(alpha = if (enabled) 0.70f else 0.24f),
        border = BorderStroke(1.dp, LabColors.outline.copy(alpha = if (enabled) 0.42f else 0.18f)),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (previous) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = LabColors.onSurface.copy(alpha = if (enabled) 0.88f else 0.34f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PeriodCalendarView(
    yearMonth: LabYearMonth,
    daysByDate: Map<LabDate, ShiftDay>,
    selectedDate: LabDate?,
    onDateClick: (LabDate) -> Unit
) {
    val weekHeaders = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")
    val startOffset = yearMonth.firstDayOffsetSunday()
    val daysInMonth = yearMonth.lengthOfMonth()
    val rows = (startOffset + daysInMonth + 6) / 7

    LabCard(title = "Calendário", badge = yearMonth.periodMonthLabel(), icon = Icons.Default.CalendarMonth) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            weekHeaders.forEach { header ->
                Text(
                    header,
                    modifier = Modifier.weight(1f),
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - startOffset + 1
                    val date = if (dayNumber in 1..daysInMonth) yearMonth.atDay(dayNumber) else null
                    PeriodDayCell(
                        date = date,
                        day = date?.let { daysByDate[it] },
                        selected = selectedDate == date,
                        modifier = Modifier.weight(1f),
                        onClick = { if (date != null) onDateClick(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodDayCell(
    date: LabDate?,
    day: ShiftDay?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (date == null) {
        Spacer(modifier = modifier.height(56.dp))
        return
    }
    val markerColor = day?.type?.shiftColor() ?: LabColors.onSurfaceMuted.copy(alpha = 0.42f)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(LabColors.primary)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                date.day.toString(),
                color = if (selected) Color.White else LabColors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color.White else markerColor)
            )
        }
    }
}

@Composable
private fun DemoCalendarCard() {
    LabCard(borderColor = LabColors.primary.copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Exemplo", color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Importe e analise uma escala para preencher este calendário com dados reais. Os dias abaixo são apenas ilustrativos.",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box(
                modifier = Modifier
                    .clip(LabShapes.chip)
                    .background(LabColors.primary.copy(alpha = 0.14f))
                    .border(1.dp, LabColors.primary.copy(alpha = 0.30f), LabShapes.chip)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Exemplo", color = LabColors.primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LegendChip(expanded: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = LabColors.primary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, LabColors.primary.copy(alpha = 0.28f))
    ) {
        Text(
            text = if (expanded) "Ocultar legenda" else "Ver legenda",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = LabColors.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InlineLegendCard() {
    LabCard(title = "Legenda", icon = Icons.Default.Checklist, borderColor = LabColors.outline.copy(alpha = 0.36f)) {
        val items = listOf(
            ShiftType.MADRUGADA,
            ShiftType.MANHA,
            ShiftType.TARDE,
            ShiftType.NOITE,
            ShiftType.FOLGA,
            ShiftType.FERIAS,
            ShiftType.BH,
            ShiftType.AFASTAMENTO,
            ShiftType.INCONSISTENCIA
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth >= 420.dp -> 4
                maxWidth >= 300.dp -> 3
                else -> 2
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.chunked(columns).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { type ->
                            ShiftLegendItem(type = type, modifier = Modifier.weight(1f))
                        }
                        repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftLegendItem(type: ShiftType, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(type.shiftColor()))
        Text(type.label, color = LabColors.onSurface, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CalendarDayDetailCard(day: ShiftDay) {
    val color = day.type.shiftColor()
    LabCard(
        title = "Detalhe do dia",
        icon = Icons.Default.CalendarMonth,
        iconTint = color,
        borderColor = color.copy(alpha = 0.42f),
        gradient = listOf(LabColors.surface.copy(alpha = 0.96f), LabColors.surfaceElevated.copy(alpha = 0.92f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(46.dp).clip(LabShapes.cardSmall).background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                ShiftMarker(day.type, size = 30)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(day.fullDateLabel, color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = LabColors.outline.copy(alpha = 0.22f))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(day.type.label, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(day.type.timeRange, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            if (day.teamMembers.isNotEmpty()) "Com: ${day.teamMembers.joinToString(", ")}" else "Equipe não localizada na escala",
            color = LabColors.onSurfaceMuted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        day.sourceStatus?.let { status ->
            Text(
                if (status.matches(Regex("[1-6]"))) "Sequência 6x1: $status" else "Status origem: $status",
                color = LabColors.onSurfaceMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        day.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text("Observação: $note", color = Color(0xFFFDE68A), style = MaterialTheme.typography.bodySmall)
        }
        WeatherMiniCard()
        if (day.type.isWorkShift) {
            TextButton(onClick = {}) {
                Text("Solicitar troca", color = LabColors.primary)
            }
        }
    }
}

@Composable
private fun WeatherMiniCard() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LabColors.surfaceElevated.copy(alpha = 0.70f))
            .border(1.dp, LabColors.outline.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text("Clima previsto: 17° · valor ilustrativo", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TeamOnDutyCard(day: ShiftDay, selectedCollaborator: String) {
    val shifts = listOf(ShiftType.MADRUGADA, ShiftType.MANHA, ShiftType.TARDE, ShiftType.NOITE)
    val membersByShift = if (day.membersByShift.isNotEmpty()) {
        day.membersByShift
    } else if (day.type.isWorkShift) {
        mapOf(day.type to (listOf(selectedCollaborator) + day.teamMembers))
    } else {
        emptyMap()
    }

    LabCard(title = "Quem trabalha nesse dia", icon = Icons.Default.Groups, borderColor = LabColors.outline.copy(alpha = 0.34f)) {
        Text(day.dateLabel, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            shifts.forEach { type ->
                ShiftTurnoTab(type = type, active = type == day.type, modifier = Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            shifts.forEach { type ->
                TurnoNamesColumn(type = type, names = membersByShift[type].orEmpty(), selfName = selectedCollaborator, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShiftTurnoTab(type: ShiftType, active: Boolean, modifier: Modifier = Modifier) {
    val color = type.shiftColor()
    Column(
        modifier = modifier
            .clip(LabShapes.chip)
            .background(color.copy(alpha = if (active) 0.24f else 0.12f))
            .border(1.dp, color.copy(alpha = if (active) 0.65f else 0.30f), LabShapes.chip)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(type.shortLabel, color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        Text(type.label, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TurnoNamesColumn(type: ShiftType, names: List<String>, selfName: String, modifier: Modifier = Modifier) {
    val color = type.shiftColor()
    Column(modifier = modifier.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.height(4.dp))
        if (names.isEmpty()) {
            Text("—", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        } else {
            names.forEach { name ->
                Text(
                    text = if (name.equals(selfName, ignoreCase = true)) "$name (Você)" else name,
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ShiftMarker(type: ShiftType, size: Int = 34) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(type.shiftColor())
            .border(1.4.dp, Color.White.copy(alpha = 0.86f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            type.shortLabel,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = if (size >= 40) 14.sp else 13.sp,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.92f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1.5f),
                    blurRadius = 3.2f
                )
            ),
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun ShiftDayRow(day: ShiftDay) {
    val statusText = day.sourceStatus?.takeIf { it.isNotBlank() } ?: if (day.type.isWorkShift) "ativo" else "folga"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LabColors.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(day.type.shiftColor()),
                contentAlignment = Alignment.Center
            ) {
                Text(day.type.shortLabel, color = Color.White, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${day.dayLabel} · ${day.dateLabel}", color = LabColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${day.type.label} · ${day.type.timeRange}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                if (day.teamMembers.isNotEmpty()) {
                    Text("Com: ${day.teamMembers.joinToString(", ")}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!day.note.isNullOrBlank()) {
                    Text(day.note, color = Color(0xFFFDE68A), style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(statusText, color = if (day.type.isWorkShift) LabColors.primary else LabColors.tertiary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
