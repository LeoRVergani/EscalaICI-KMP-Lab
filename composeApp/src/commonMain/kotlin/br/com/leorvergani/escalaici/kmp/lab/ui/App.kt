package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.ImportedWorkbook
import br.com.leorvergani.escalaici.kmp.lab.model.LabWorkbookParser
import br.com.leorvergani.escalaici.kmp.lab.model.Member
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleImportPreview
import br.com.leorvergani.escalaici.kmp.lab.model.WorkbookImportResult
import br.com.leorvergani.escalaici.kmp.lab.model.mockScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.platform.rememberWorkbookImportLauncher
import br.com.leorvergani.escalaici.kmp.lab.repository.InMemoryAuthSessionRepository
import br.com.leorvergani.escalaici.kmp.lab.repository.MockMemberRepository
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumBackground
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColorScheme
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabShapes
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabTypography
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

/** Telas fora do bottom nav, empilhadas sobre a aba ativa (igual ao app real). */
private enum class StackedScreen(val title: String) {
    PLANTAO("Plantão"),
    SWAP("Trocas de escala")
}

@Composable
fun EscalaIciLabApp() {
    MaterialTheme(colorScheme = LabColorScheme, typography = LabTypography) {
        val authRepository = remember { InMemoryAuthSessionRepository() }
        val memberRepository = remember { MockMemberRepository() }
        val scope = rememberCoroutineScope()
        var sessionMemberId by remember { mutableStateOf<String?>(null) }
        var demoMembers by remember { mutableStateOf<List<Member>>(emptyList()) }

        LaunchedEffect(Unit) {
            sessionMemberId = authRepository.currentMemberId()
            demoMembers = memberRepository.getMembersByTeam("soc")
        }

        var activeTab by remember { mutableStateOf(LabTab.Hoje) }
        var stackedScreen by remember { mutableStateOf<StackedScreen?>(null) }
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
            summary = mockScheduleSummary()
            importPreview = null
            importedWorkbook = null
        }

        val onOpenPlantao: () -> Unit = { stackedScreen = StackedScreen.PLANTAO }

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
                        if (stackedScreen == null) {
                            BottomNav(activeTab = activeTab, onSelect = { activeTab = it })
                        }
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
                            val currentStackedScreen = stackedScreen
                            if (currentStackedScreen != null) {
                                StackedScreenPlaceholder(
                                    title = currentStackedScreen.title,
                                    onBack = { stackedScreen = null }
                                )
                            } else {
                                when (activeTab) {
                                    LabTab.Hoje -> TodayTab(
                                        summary = summary,
                                        onOpenPlantao = onOpenPlantao,
                                        onImportClick = { activeTab = LabTab.Importar }
                                    )
                                    LabTab.Escala -> ScheduleTab(summary = summary, onOpenPlantao = onOpenPlantao)
                                    LabTab.Importar -> ImportTab(
                                        preview = importPreview,
                                        selectedCollaborator = summary.member.scaleName,
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
                                        onResetMock = ::resetMock,
                                        onOpenPlantao = onOpenPlantao
                                    )
                                    LabTab.Alertas -> AlertsTab(summary = summary, onOpenPlantao = onOpenPlantao)
                                    LabTab.Perfil -> ProfileTab(
                                        summary = summary,
                                        onLogout = {
                                            scope.launch { authRepository.signOut() }
                                            sessionMemberId = null
                                        },
                                        onOpenPlantao = onOpenPlantao,
                                        onOpenSwap = { stackedScreen = StackedScreen.SWAP }
                                    )
                                }
                            }
                        }
                    }
                }
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

/**
 * Placeholder temporario para as telas empilhadas (Plantao/Trocas de
 * escala) enquanto elas nao sao implementadas de fato (FASES 10.10/10.11).
 */
@Composable
private fun StackedScreenPlaceholder(title: String, onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = LabColors.onSurface)
                }
                Text(title, color = LabColors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        item {
            LabCard(title = "Em construção") {
                Text(
                    "Esta tela ainda sera implementada no laboratorio (mock visual, sem Firebase real).",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
                                    .clip(LabShapes.navPill)
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
