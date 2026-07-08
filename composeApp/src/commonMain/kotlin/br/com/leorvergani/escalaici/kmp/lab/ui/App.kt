package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftDay
import br.com.leorvergani.escalaici.kmp.lab.model.mockScheduleSummary

@Composable
fun EscalaIciLabApp() {
    MaterialTheme {
        var refreshCount by remember { mutableIntStateOf(0) }
        val summary = remember(refreshCount) { mockScheduleSummary() }

        Surface(modifier = Modifier.fillMaxSize(), color = LabColors.background) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Header(refreshCount = refreshCount)
                }
                item {
                    Button(
                        onClick = { refreshCount += 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Atualizar mock")
                    }
                }
                item {
                    TodayCard(summary = summary)
                }
                item {
                    ScheduleCard(summary = summary)
                }
                items(summary.days) { day ->
                    ShiftDayRow(day = day)
                }
            }
        }
    }
}

@Composable
private fun Header(refreshCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Escala ICI KMP Lab",
            color = LabColors.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "POC Compose Multiplatform: Android + Web/Wasm",
            color = LabColors.onSurfaceMuted,
            style = MaterialTheme.typography.bodyMedium
        )
        if (refreshCount > 0) {
            Text(
                text = "Mock atualizado $refreshCount vez(es)",
                color = LabColors.accent,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun TodayCard(summary: ScheduleSummary) {
    val today = summary.nextShift
    LabCard(title = "Hoje") {
        Text(
            text = summary.member.scaleName,
            color = LabColors.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = today?.type?.label ?: "Sem turno",
            color = LabColors.accent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = today?.type?.timeRange ?: "Semana mock sem turno ativo",
            color = LabColors.onSurfaceMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ScheduleCard(summary: ScheduleSummary) {
    LabCard(title = "Escala") {
        Text(
            text = "Time ${summary.team.name} - ${summary.workedDays} dias trabalhados na semana mock",
            color = LabColors.onSurfaceMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ShiftDayRow(day: ShiftDay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LabColors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(LabColors.accent.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.type.shortLabel,
                    color = LabColors.accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${day.dayLabel} - ${day.dateLabel}",
                    color = LabColors.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${day.type.label} | ${day.type.timeRange}",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LabCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = LabColors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = LabColors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

private object LabColors {
    val background = Color(0xFF101418)
    val surface = Color(0xFF1B2329)
    val onSurface = Color(0xFFF0F4F4)
    val onSurfaceMuted = Color(0xFFB4C1C5)
    val accent = Color(0xFF55D6BE)
}
