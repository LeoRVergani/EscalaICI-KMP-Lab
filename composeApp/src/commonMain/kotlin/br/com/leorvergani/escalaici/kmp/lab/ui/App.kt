package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.leorvergani.escalaici.kmp.lab.model.GenerateLabAlerts
import br.com.leorvergani.escalaici.kmp.lab.model.ImportedWorkbook
import br.com.leorvergani.escalaici.kmp.lab.model.LabAlert
import br.com.leorvergani.escalaici.kmp.lab.model.LabDate
import br.com.leorvergani.escalaici.kmp.lab.model.LabWorkbookParser
import br.com.leorvergani.escalaici.kmp.lab.model.LabYearMonth
import br.com.leorvergani.escalaici.kmp.lab.model.Member
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleImportPreview
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftDay
import br.com.leorvergani.escalaici.kmp.lab.model.ShiftType
import br.com.leorvergani.escalaici.kmp.lab.model.WorkbookImportResult
import br.com.leorvergani.escalaici.kmp.lab.model.mockScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.platform.rememberWorkbookImportLauncher
import br.com.leorvergani.escalaici.kmp.lab.repository.InMemoryAuthSessionRepository
import br.com.leorvergani.escalaici.kmp.lab.repository.MockMemberRepository
import kotlinx.coroutines.launch

private enum class LabTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
) {
    Hoje("Hoje", Icons.Outlined.Home, Icons.Filled.Home),
    Escala("Escala", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Importar("Importar", Icons.Outlined.CloudUpload, Icons.Filled.CloudUpload),
    Alertas("Alertas", Icons.Outlined.Warning, Icons.Filled.Warning),
    Perfil("Perfil", Icons.Outlined.Person, Icons.Filled.Person)
}

@Composable
fun EscalaIciLabApp() {
    MaterialTheme(colorScheme = LabColorScheme) {
        val authRepository = remember { InMemoryAuthSessionRepository() }
        val memberRepository = remember { MockMemberRepository() }
        val scope = rememberCoroutineScope()
        var sessionMemberId by remember { mutableStateOf<String?>(null) }
        var demoMembers by remember { mutableStateOf<List<Member>>(emptyList()) }

        LaunchedEffect(Unit) {
            sessionMemberId = authRepository.currentMemberId()
            demoMembers = memberRepository.getMembersByTeam("soc")
        }

        var refreshCount by remember { mutableIntStateOf(0) }
        var activeTab by remember { mutableStateOf(LabTab.Hoje) }
        var summary by remember { mutableStateOf(mockScheduleSummary()) }
        var importPreview by remember { mutableStateOf<ScheduleImportPreview?>(null) }
        var importedWorkbook by remember { mutableStateOf<ImportedWorkbook?>(null) }
        val importLauncher = rememberWorkbookImportLauncher { result ->
            when (result) {
                is WorkbookImportResult.Success -> {
                    importedWorkbook = result.workbook
                    val preferredCollaborator = importPreview?.selectedCollaborator ?: summary.member.scaleName
                    importPreview = LabWorkbookParser.parse(result.workbook, preferredCollaborator)
                }
                is WorkbookImportResult.Failure -> {
                    importedWorkbook = null
                    importPreview = result.toImportPreview()
                }
            }
            activeTab = LabTab.Importar
        }

        fun resetMock() {
            refreshCount += 1
            summary = mockScheduleSummary()
            importPreview = null
            importedWorkbook = null
        }

        if (sessionMemberId == null) {
            LoginGateScreen(
                members = demoMembers,
                onSelectMember = { member ->
                    scope.launch {
                        authRepository.signIn(member.id)
                        sessionMemberId = member.id
                    }
                    summary = summary.copy(member = member)
                }
            )
        } else {
            LabPremiumBackground {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        BottomNav(activeTab = activeTab, onSelect = { activeTab = it })
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 760.dp)
                                .fillMaxWidth()
                        ) {
                            when (activeTab) {
                                LabTab.Hoje -> TodayTab(summary = summary, refreshCount = refreshCount)
                                LabTab.Escala -> ScheduleTab(summary = summary)
                                LabTab.Importar -> ImportTab(
                                    preview = importPreview,
                                    onSelectXls = { importLauncher.launch() },
                                    onUseImported = {
                                        importPreview?.summary?.let { imported ->
                                            summary = imported
                                            activeTab = LabTab.Hoje
                                        }
                                    },
                                    onSelectCollaborator = { collaborator ->
                                        importedWorkbook?.let { workbook ->
                                            importPreview = LabWorkbookParser.parse(workbook, collaborator)
                                        }
                                    },
                                    onResetMock = ::resetMock
                                )
                                LabTab.Alertas -> AlertsTab(summary = summary)
                                LabTab.Perfil -> ProfileTab(
                                    summary = summary,
                                    onLogout = {
                                        scope.launch { authRepository.signOut() }
                                        sessionMemberId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginGateScreen(
    members: List<Member>,
    onSelectMember: (Member) -> Unit
) {
    LabPremiumBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(LabColors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
                }
                Text(
                    "Escala ICI",
                    color = LabColors.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Login fake do laboratório KMP (FASE 9f). Sem MSAL ou Firebase reais — selecione um colaborador demonstrativo para continuar.",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                LabCard(title = "Selecionar colaborador demo", icon = Icons.Default.Person) {
                    members.forEach { member ->
                        OutlinedButton(
                            onClick = { onSelectMember(member) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Entrar como ${member.displayName}")
                        }
                    }
                }
                Text(
                    "Esta tela representa o LoginGate do app real, sem autenticação verdadeira.",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun WorkbookImportResult.toImportPreview(): ScheduleImportPreview {
    return when (this) {
        is WorkbookImportResult.Success -> LabWorkbookParser.parse(workbook)
        is WorkbookImportResult.Failure -> ScheduleImportPreview(
            fileName = fileName ?: "Arquivo não lido",
            sheetNames = emptyList(),
            collaborators = emptyList(),
            selectedCollaborator = null,
            daysRead = 0,
            warnings = emptyList(),
            errors = listOf(message),
            summary = null
        )
    }
}

@Composable
private fun LabPremiumBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050910))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2563EB).copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(130f, 0f),
                    radius = 780f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF581C87).copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(900f, 420f),
                    radius = 860f
                )
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0B1221).copy(alpha = 0.76f), Color(0xFF050910)),
                    start = Offset.Zero,
                    end = Offset(0f, 1600f)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color(0xFF93C5FD).copy(alpha = 0.050f)
            val lineColor = Color(0xFF60A5FA).copy(alpha = 0.060f)

            for (x in 24 until size.width.toInt() step 54) {
                for (y in 32 until size.height.toInt() step 74) {
                    val offset = ((x + y) % 19).toFloat()
                    drawCircle(dotColor, radius = 1.05f, center = Offset(x + offset, y.toFloat()))
                }
            }

            drawLine(lineColor, Offset(size.width * 0.10f, size.height * 0.18f), Offset(size.width * 0.86f, size.height * 0.08f), 1f)
            drawLine(lineColor, Offset(size.width * 0.20f, size.height * 0.72f), Offset(size.width * 0.92f, size.height * 0.58f), 1f)
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.08f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.90f, size.height * 0.08f),
                style = Stroke(width = 1.3f)
            )
        }
        content()
    }
}

@Composable
private fun TodayTab(summary: ScheduleSummary, refreshCount: Int) {
    val next = summary.nextShift
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Header(summary = summary, refreshCount = refreshCount)
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
private fun Header(summary: ScheduleSummary, refreshCount: Int) {
    val initials = summary.member.displayName.initials()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(LabColors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color.White, fontWeight = FontWeight.Black)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Escala", color = LabColors.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("ICI", color = LabColors.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(summary.team.name, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(summary.member.scaleName, color = LabColors.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            if (refreshCount > 0) {
                Text("mock $refreshCount", color = LabColors.tertiary, style = MaterialTheme.typography.labelSmall)
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
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.92f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 3.2f
                )
            ),
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
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

@Composable
private fun ScheduleTab(summary: ScheduleSummary) {
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

    PageList(title = "Escala", subtitle = "${summary.team.name} · ${summary.periodLabel}") {
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
        items.chunked(3).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { type ->
                    ShiftLegendItem(type = type, modifier = Modifier.weight(1f))
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
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
            ShiftMarker(day.type, size = 44)
            Column(modifier = Modifier.weight(1f)) {
                Text(day.fullDateLabel, color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${day.type.label} · ${day.type.timeRange}", color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = LabColors.outline.copy(alpha = 0.30f))
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
        Text("Clima previsto: 17° · laboratório", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
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
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = if (active) 0.24f else 0.12f))
            .border(1.dp, color.copy(alpha = if (active) 0.65f else 0.30f), RoundedCornerShape(14.dp))
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
            Text("-", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
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
private fun ImportTab(
    preview: ScheduleImportPreview?,
    onSelectXls: () -> Unit,
    onUseImported: () -> Unit,
    onSelectCollaborator: (String) -> Unit,
    onResetMock: () -> Unit
) {
    PageList(title = "Importar", subtitle = preview?.fileName ?: "Selecione a planilha oficial .xls") {
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
private fun AlertsTab(summary: ScheduleSummary) {
    val alerts = remember(summary) { GenerateLabAlerts(summary) }
    var filter by remember { mutableStateOf(AlertFilter.TODOS) }
    val filteredAlerts = alerts.filter { filter.matches(it) }

    PageList(title = "Alertas", subtitle = if (summary.isImported) "Alertas gerados da planilha importada" else "Validações locais da POC") {
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

@Composable
private fun AlertsHero(summary: ScheduleSummary, alerts: List<LabAlert>) {
    val critical = alerts.count { it.severity == LabAlert.Severity.CRITICO }
    HeroCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Alertas da escala", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${alerts.size} itens gerados pela escala ${if (summary.isImported) "real" else "demo"}", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                Text("Analista: ${summary.member.scaleName}", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Período: ${summary.periodLabel}", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(critical.toString(), color = if (critical > 0) Color(0xFFFCA5A5) else Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
        }
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
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(item.color.copy(alpha = 0.20f), LabColors.surface.copy(alpha = 0.94f))))
            .border(1.dp, item.color.copy(alpha = 0.46f), RoundedCornerShape(12.dp))
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
                    .clip(RoundedCornerShape(18.dp))
                    .background(color.copy(alpha = if (active) 0.20f else 0.08f))
                    .border(1.dp, color.copy(alpha = if (active) 0.48f else 0.22f), RoundedCornerShape(18.dp))
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
        shape = RoundedCornerShape(12.dp),
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
                    Text(
                        "Fonte: ${summary.sourceFileName ?: "mock do laboratório"}",
                        color = LabColors.onSurfaceMuted.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    alert.date?.let { date ->
                        Text(date.fullDateLabel(), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: LabAlert.Severity) {
    val color = severity.alertColor()
    Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.15f)).border(1.dp, color.copy(alpha = 0.34f), RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
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

@Composable
private fun ProfileTab(summary: ScheduleSummary, onLogout: () -> Unit) {
    val criticalAlerts = remember(summary) { GenerateLabAlerts(summary).count { it.severity == LabAlert.Severity.CRITICO } }
    PageList(title = "Perfil", subtitle = if (summary.isImported) "Perfil importado do XLS" else "Identidade demonstrativa") {
        item {
            LabCard(title = "Perfil selecionado", icon = Icons.Default.Person) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(LabColors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(summary.member.displayName.initials(), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Perfil selecionado", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                        Text(summary.member.displayName, color = LabColors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (summary.isImported) "Escala lida do XLS" else "Demonstração local", color = LabColors.tertiary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(summary.member.email, color = LabColors.onSurfaceMuted)
                Text("Time ${summary.team.name} · ${summary.team.teamId}", color = LabColors.onSurfaceMuted)
                summary.sourceFileName?.let { fileName ->
                    Text("Fonte: $fileName", color = LabColors.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onLogout) {
                    Text("Sair (login fake)")
                }
            }
        }
        item {
            LabCard(title = "Identidade da escala", icon = Icons.Default.Security, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Colaborador identificado: ${summary.member.scaleName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("A associação Microsoft -> member -> teamId está representada visualmente no lab.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            LabCard(title = "Resumo do perfil", icon = Icons.Default.Analytics, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileMetric("Trabalho", "${summary.workedDays}d", Modifier.weight(1f))
                    ProfileMetric("Folgas", "${summary.restDays}d", Modifier.weight(1f))
                    ProfileMetric("Horas", "${summary.totalHours}h", Modifier.weight(1f))
                }
                Text("Alertas críticos: $criticalAlerts", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            LabCard(title = "Administração da escala", icon = Icons.Default.AdminPanelSettings, borderColor = LabColors.primary.copy(alpha = 0.22f)) {
                StatusLine("Modo ADM", "visual no lab")
                StatusLine("OneDrive ADM", "não conectado")
                StatusLine("Dropbox ADM", "não conectado")
                StatusLine("Mês atual", if (summary.isImported) "arquivo carregado" else "aguardando importação")
                DisabledAction("Importação Firebase")
                DisabledAction("Buscar escala no OneDrive ADM")
                DisabledAction("Conectar Dropbox ADM")
                DisabledAction("Publicar escala no Dropbox")
            }
        }
        item {
            LabCard(title = "Conta corporativa", icon = Icons.Default.Security, borderColor = LabColors.tertiary.copy(alpha = 0.25f)) {
                StatusLine("Conta", "não conectada no lab")
                Text("Entre para identificar seu usuário no app real. MSAL fica fora desta POC.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                DisabledAction("Entrar com conta corporativa")
            }
        }
        item {
            LabCard(title = "Modo demo", icon = Icons.Default.Person, borderColor = LabColors.outline.copy(alpha = 0.32f)) {
                Text("Dados fake, somente laboratório.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                DisabledAction("Entrar como Teste SOC A")
                DisabledAction("Entrar como Teste SOC B")
                DisabledAction("Entrar como Aprovador SOC")
                DisabledAction("Criar/atualizar time demo")
            }
        }
        item {
            LabCard(title = "Trocas de escala", icon = Icons.Default.SwapHoriz, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Veja e responda pedidos de troca de turno.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                DisabledAction("Ver minhas solicitações")
            }
        }
        item {
            LabCard(title = "Notificações", icon = Icons.Default.Notifications, borderColor = LabColors.primary.copy(alpha = 0.30f), gradient = listOf(LabColors.surfaceElevated.copy(alpha = 0.88f), LabColors.surface.copy(alpha = 0.96f))) {
                StatusLine("Status", "ativas visualmente")
                Text("Alertas são problemas detectados na escala. Notificações são lembretes enviados pelo celular.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                VisualToggle("Plantão amanhã", true)
                VisualToggle("Folga amanhã", true)
                VisualToggle("Saída do turno", false)
                HorizontalDivider(color = LabColors.outline.copy(alpha = 0.22f))
                Text("Entrada do turno", color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Receba um aviso antes do seu turno começar.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileChip("No horário", false, Modifier.weight(1f))
                    ProfileChip("15 min antes", true, Modifier.weight(1f))
                    ProfileChip("30 min antes", false, Modifier.weight(1f))
                }
                Text("Analista: ${summary.member.scaleName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                DisabledAction("Reprogramar notificações")
            }
        }
        item {
            LabCard(title = "Pausa de 15 minutos", icon = Icons.Default.Schedule, iconTint = LabColors.tertiary, borderColor = LabColors.tertiary.copy(alpha = 0.30f), gradient = listOf(Color(0xFF0D2832), Color(0xFF092A28), Color(0xFF0D1730))) {
                Text("Horário calculado a partir do próximo turno do analista selecionado.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                VisualToggle("Lembrete de pausa", true)
                Text("Permitido 1h após o início do turno", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileChip(summary.pauseLabel.substringBefore(" - "), true, Modifier.weight(1f))
                    ProfileChip("Outro horário", false, Modifier.weight(1f))
                }
                Text("Analista: ${summary.member.scaleName}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            LabCard(title = "Resumo", icon = Icons.Default.Checklist, borderColor = LabColors.primary.copy(alpha = 0.22f)) {
                Text("Entrada do turno: 15 min antes", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Pausa: ${summary.pauseLabel}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Janela permitida: ${summary.pauseOffsetLabel}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            LabCard(title = "Armazenamento local", icon = Icons.Default.Storage, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Arquivo salvo: ${summary.sourceFileName ?: "mock interno do laboratório"}", color = LabColors.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Status: ${if (summary.isImported) "Escala lida na sessão Web/Android Lab" else "Sem XLS aplicado"}", color = LabColors.tertiary, style = MaterialTheme.typography.labelMedium)
                DisabledAction("Remover escala local")
            }
        }
        item {
            LabCard(title = "Aplicativo", icon = Icons.Default.SystemUpdate, borderColor = LabColors.primary.copy(alpha = 0.25f)) {
                Text("Versão atual: 0.1.0-lab", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text("Atualização APK/Dropbox fica no app Android real.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                DisabledAction("Atualizar aplicativo")
            }
        }
        item {
            LabCard(title = "POC KMP", icon = Icons.Default.CloudDone, borderColor = LabColors.outline.copy(alpha = 0.35f)) {
                Text("Visual completo em laboratório. MSAL, Firebase, Dropbox, notificações reais e parser oficial continuam fora desta etapa.", color = LabColors.onSurfaceMuted)
            }
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LabColors.surfaceElevated.copy(alpha = 0.62f))
            .border(1.dp, LabColors.primary.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(112.dp), color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(value, modifier = Modifier.weight(1f), color = LabColors.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DisabledAction(label: String) {
    TextButton(onClick = {}) {
        Text(label, color = LabColors.primary.copy(alpha = 0.82f))
    }
}

@Composable
private fun VisualToggle(label: String, checked: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = {})
    }
}

@Composable
private fun ProfileChip(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) LabColors.primary.copy(alpha = 0.20f) else LabColors.surfaceElevated.copy(alpha = 0.78f))
            .border(1.dp, if (selected) LabColors.primary.copy(alpha = 0.62f) else LabColors.outline.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
private fun PageList(
    title: String,
    subtitle: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = LabColors.onSurface, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(subtitle, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        content()
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

@Composable
private fun HeroCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0xFF4B6FA8).copy(alpha = 0.72f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF102A4E), Color(0xFF121D39), Color(0xFF3A0D56))))
        ) {
            NocHeroTexture()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun NocHeroTexture() {
    Canvas(modifier = Modifier.fillMaxWidth().height(172.dp)) {
        val grid = Color.White.copy(alpha = 0.06f)
        for (x in 0..size.width.toInt() step 42) {
            drawLine(grid, Offset(x.toFloat(), size.height * 0.12f), Offset(x.toFloat() + 72f, size.height), 0.8f)
        }
        for (i in 0..7) {
            val x = size.width * (0.12f + i * 0.11f)
            val y = size.height * (0.30f + (i % 3) * 0.16f)
            drawCircle(Color(0xFF60A5FA).copy(alpha = 0.14f), radius = 4f, center = Offset(x, y))
            drawCircle(Color.White.copy(alpha = 0.07f), radius = 16f, center = Offset(x, y), style = Stroke(width = 1f))
        }
    }
}

@Composable
private fun LabCard(
    title: String,
    badge: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = LabColors.primary,
    borderColor: Color = LabColors.primary.copy(alpha = 0.38f),
    gradient: List<Color> = listOf(Color(0xFF0C1B2F), Color(0xFF0B1729)),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconTint.copy(alpha = 0.16f))
                            .border(1.dp, iconTint.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
                    }
                }
                Text(title, modifier = Modifier.weight(1f), color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                if (badge != null) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LabColors.primary.copy(alpha = 0.16f)).border(1.dp, LabColors.primary.copy(alpha = 0.32f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(badge, color = LabColors.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun BottomNav(activeTab: LabTab, onSelect: (LabTab) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LabColors.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, LabColors.outline.copy(alpha = 0.35f)),
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            NavigationBar(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
                containerColor = LabColors.surface.copy(alpha = 0.94f),
                tonalElevation = 0.dp
            ) {
                LabTab.entries.forEach { tab ->
                    val active = tab == activeTab
                    NavigationBarItem(
                        selected = active,
                        onClick = { onSelect(tab) },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(if (active) 36.dp else 24.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (active) LabColors.primary.copy(alpha = 0.14f) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (active) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LabColors.primary,
                            selectedTextColor = LabColors.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = LabColors.onSurfaceMuted,
                            unselectedTextColor = LabColors.onSurfaceMuted
                        )
                    )
                }
            }
        }
    }
}

private fun ShiftType.shiftColor(): Color = when (this) {
    ShiftType.MADRUGADA -> Color(0xFF6366F1)
    ShiftType.MANHA -> Color(0xFFFACC15)
    ShiftType.TARDE -> Color(0xFFF97316)
    ShiftType.NOITE -> Color(0xFF1D4ED8)
    ShiftType.FOLGA -> Color(0xFF16A34A)
    ShiftType.FERIAS -> Color(0xFF14B8A6)
    ShiftType.BH -> Color(0xFFF59E0B)
    ShiftType.ANIVERSARIO -> Color(0xFFEC4899)
    ShiftType.HORA_EXTRA -> Color(0xFF22C55E)
    ShiftType.AFASTAMENTO -> Color(0xFF94A3B8)
    ShiftType.INCONSISTENCIA -> Color(0xFFEF4444)
    ShiftType.INDEFINIDO -> Color(0xFF6B7280)
}

private val AlertCriticalColor = Color(0xFFEF4444)
private val AlertWarningColor = Color(0xFFF59E0B)
private val AlertInfoColor = Color(0xFF3B82F6)

private object LabColors {
    val background = Color(0xFF070B12)
    val surface = Color(0xFF0B1827)
    val surfaceElevated = Color(0xFF111E31)
    val outline = Color(0xFF2F4668)
    val primary = Color(0xFF3B82F6)
    val tertiary = Color(0xFF18A874)
    val onSurface = Color(0xFFF3F4F6)
    val onSurfaceMuted = Color(0xFFAEB8C9)
}

private val LabColorScheme = darkColorScheme(
    primary = LabColors.primary,
    onPrimary = Color.White,
    secondary = Color(0xFF60A5FA),
    tertiary = LabColors.tertiary,
    background = LabColors.background,
    onBackground = LabColors.onSurface,
    surface = LabColors.surface,
    onSurface = LabColors.onSurface,
    surfaceVariant = LabColors.surfaceElevated,
    onSurfaceVariant = LabColors.onSurfaceMuted,
    outline = LabColors.outline,
    error = Color(0xFFEF4444)
)

private fun String.initials(): String {
    val parts = trim()
        .split(Regex("""[\s._-]+"""))
        .filter { it.isNotBlank() }
    return parts
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "IC" }
}
