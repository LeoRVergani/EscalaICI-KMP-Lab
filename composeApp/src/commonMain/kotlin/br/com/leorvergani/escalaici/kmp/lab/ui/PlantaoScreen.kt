package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.LabDate
import br.com.leorvergani.escalaici.kmp.lab.model.LabYearMonth
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallAssignment
import br.com.leorvergani.escalaici.kmp.lab.model.OnCallStatus
import br.com.leorvergani.escalaici.kmp.lab.model.mockOnCallAssignments
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabShapes

/**
 * Porte de `ui/plantao/PlantaoScreen.kt` (app real), mock: sem importação
 * de relatório real (não há `PlantaoWorkbookParser`/file picker de plantão
 * no laboratório) — usa `OnCallAssignment`/`OnCallStatus` (FASE 9c) e os
 * mocks de `MockSchedule.kt`. Sem relógio disponível em `commonMain`, o
 * "agora" é decidido pelo campo `status` já mockado (`ACTIVE`), não por
 * comparação de data real.
 */
@Composable
internal fun PlantaoScreen(onBack: () -> Unit) {
    val assignments = remember { mockOnCallAssignments() }
    val sortedAssignments = remember(assignments) { assignments.sortedBy { it.date } }
    val activeAssignments = remember(assignments) { assignments.filter { it.status == OnCallStatus.ACTIVE } }
    val nextAssignment = remember(sortedAssignments) { sortedAssignments.firstOrNull { it.status == OnCallStatus.SCHEDULED } }
    val heroShifts = if (activeAssignments.isNotEmpty()) activeAssignments else listOfNotNull(nextAssignment)
    val heroTitle = when {
        activeAssignments.isNotEmpty() -> "Plantão agora"
        nextAssignment != null -> "Próximo plantão"
        else -> "Nenhum plantão encontrado"
    }
    val referenceDate = remember(sortedAssignments) {
        (activeAssignments.firstOrNull() ?: nextAssignment ?: sortedAssignments.firstOrNull())
            ?.date?.let { LabDate.parseIso(it) }
    }

    var selectedDate by remember { mutableStateOf(referenceDate ?: LabDate(2026, 7, 1)) }
    var visibleMonth by remember { mutableStateOf(selectedDate.yearMonth()) }

    val selectedDayAssignments = remember(selectedDate, assignments) {
        assignments.filter { LabDate.parseIso(it.date) == selectedDate }
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
                Text("Plantão", style = MaterialTheme.typography.titleLarge, color = LabColors.onSurface, fontWeight = FontWeight.Bold)
            }
        }
        item {
            PlantaoHeroCard(title = heroTitle, active = activeAssignments.isNotEmpty(), heroShifts = heroShifts)
        }
        item {
            PlantaoMonthHeader(
                visibleMonth = visibleMonth,
                onPrevious = { visibleMonth = visibleMonth.plusMonths(-1) },
                onNext = { visibleMonth = visibleMonth.plusMonths(1) }
            )
        }
        item {
            PlantaoCalendarGrid(
                yearMonth = visibleMonth,
                assignments = assignments,
                selectedDate = selectedDate,
                referenceDate = referenceDate,
                onDateClick = { selectedDate = it }
            )
        }
        item {
            PlantaoDayDetailCard(selectedDate = selectedDate, assignments = selectedDayAssignments)
        }
    }
}

@Composable
private fun PlantaoHeroCard(title: String, active: Boolean, heroShifts: List<OnCallAssignment>) {
    val accent = when {
        active -> LabColors.primary
        heroShifts.isNotEmpty() -> LabColors.purple
        else -> LabColors.onSurfaceMuted
    }
    LabCard(borderColor = accent.copy(alpha = 0.40f)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (active) "PLANTÃO" else "PLANTÃO",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Text(title, style = MaterialTheme.typography.titleLarge, color = LabColors.onSurface, fontWeight = FontWeight.Black)
                Text(
                    if (heroShifts.isNotEmpty()) "${heroShifts.size} plantão(ões) no período" else "Nenhum plantão carregado para o período atual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LabColors.onSurfaceMuted
                )
            }
            Box(
                modifier = Modifier.size(46.dp).clip(LabShapes.cardSmall).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            }
        }
        if (heroShifts.isEmpty()) {
            Text("Nenhum plantão disponível no momento.", style = MaterialTheme.typography.bodySmall, color = LabColors.onSurfaceMuted)
        } else {
            heroShifts.forEach { shift ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LabShapes.cardSmall)
                        .background(LabColors.surfaceElevated.copy(alpha = 0.70f))
                        .padding(12.dp)
                ) {
                    Text(shift.memberName, style = MaterialTheme.typography.titleSmall, color = LabColors.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${shift.startTime} → ${shift.endTime}", style = MaterialTheme.typography.bodySmall, color = accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PlantaoMonthHeader(visibleMonth: LabYearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior", tint = LabColors.onSurface)
        }
        Text(
            text = visibleMonth.monthTitle(),
            style = MaterialTheme.typography.titleMedium,
            color = LabColors.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês", tint = LabColors.onSurface)
        }
    }
}

@Composable
private fun PlantaoCalendarGrid(
    yearMonth: LabYearMonth,
    assignments: List<OnCallAssignment>,
    selectedDate: LabDate,
    referenceDate: LabDate?,
    onDateClick: (LabDate) -> Unit
) {
    val weekHeaders = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")
    val startOffset = yearMonth.firstDayOffsetSunday()
    val daysInMonth = yearMonth.lengthOfMonth()
    val rows = (startOffset + daysInMonth + 6) / 7

    LabCard(borderColor = LabColors.outline.copy(alpha = 0.30f)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekHeaders.forEach { header ->
                Text(
                    header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = LabColors.onSurfaceMuted
                )
            }
        }
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - startOffset + 1
                    val date = if (dayNumber in 1..daysInMonth) yearMonth.atDay(dayNumber) else null
                    if (date == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val hasPlantao = assignments.any { LabDate.parseIso(it.date) == date }
                        PlantaoDayCell(
                            date = date,
                            selected = date == selectedDate,
                            isReference = date == referenceDate,
                            hasPlantao = hasPlantao,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateClick(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantaoDayCell(
    date: LabDate,
    selected: Boolean,
    isReference: Boolean,
    hasPlantao: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = when {
        selected -> LabColors.primary
        isReference -> LabColors.primary.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val textColor = if (selected) Color.White else LabColors.onSurface

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (!selected && isReference) Modifier.border(1.dp, LabColors.primary.copy(alpha = 0.5f), CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.day.toString(), style = MaterialTheme.typography.bodyMedium, color = textColor, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (hasPlantao) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else LabColors.purple)
                )
            }
        }
    }
}

@Composable
private fun PlantaoDayDetailCard(selectedDate: LabDate, assignments: List<OnCallAssignment>) {
    LabCard(borderColor = LabColors.primary.copy(alpha = 0.25f)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = LabColors.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(selectedDate.dayOfWeekShort(), style = MaterialTheme.typography.labelSmall, color = LabColors.onSurfaceMuted)
                Text(selectedDate.fullDateLabel(), style = MaterialTheme.typography.titleMedium, color = LabColors.onSurface, fontWeight = FontWeight.Bold)
            }
        }
        if (assignments.isEmpty()) {
            Text("Nenhum plantão registrado neste dia.", style = MaterialTheme.typography.bodySmall, color = LabColors.onSurfaceMuted)
        } else {
            assignments.forEach { shift ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LabShapes.cardSmall)
                        .background(LabColors.surfaceElevated.copy(alpha = 0.60f))
                        .padding(10.dp)
                ) {
                    Text(shift.memberName, style = MaterialTheme.typography.titleSmall, color = LabColors.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${shift.startTime} → ${shift.endTime}", style = MaterialTheme.typography.bodySmall, color = LabColors.onSurfaceMuted)
                    Text("${shift.durationLabel()} de plantão", style = MaterialTheme.typography.labelSmall, color = LabColors.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Duração do plantão a partir de "HH:mm" → "HH:mm", cruzando meia-noite se preciso. */
private fun OnCallAssignment.durationLabel(): String {
    fun minutesOf(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        return hours * 60 + minutes
    }

    val start = minutesOf(startTime) ?: return "-"
    val end = minutesOf(endTime) ?: return "-"
    val totalMinutes = if (end > start) end - start else (24 * 60 - start) + end
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0) "${hours}h" else "${hours}h${minutes.toString().padStart(2, '0')}"
}
