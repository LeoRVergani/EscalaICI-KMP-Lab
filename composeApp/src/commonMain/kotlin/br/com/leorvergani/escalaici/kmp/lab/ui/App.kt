package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.ImportedWorkbook
import br.com.leorvergani.escalaici.kmp.lab.model.LabWorkbookParser
import br.com.leorvergani.escalaici.kmp.lab.model.Member
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleImportPreview
import br.com.leorvergani.escalaici.kmp.lab.model.WorkbookImportResult
import br.com.leorvergani.escalaici.kmp.lab.model.mockScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.platform.rememberWorkbookImportLauncher
import br.com.leorvergani.escalaici.kmp.lab.platform.SystemTodayProvider
import br.com.leorvergani.escalaici.kmp.lab.platform.TodayProvider
import br.com.leorvergani.escalaici.kmp.lab.platform.CurrentTimeProvider
import br.com.leorvergani.escalaici.kmp.lab.model.LabDateTime
import br.com.leorvergani.escalaici.kmp.lab.repository.DropboxScaleRepository
import br.com.leorvergani.escalaici.kmp.lab.repository.InMemoryAuthSessionRepository
import br.com.leorvergani.escalaici.kmp.lab.repository.CacheRead
import br.com.leorvergani.escalaici.kmp.lab.repository.CachedSchedule
import br.com.leorvergani.escalaici.kmp.lab.repository.LocalDataCache
import br.com.leorvergani.escalaici.kmp.lab.repository.MockMemberRepository
import br.com.leorvergani.escalaici.kmp.lab.repository.UnavailableLocalDataCache
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
fun EscalaIciLabApp(
    todayProvider: TodayProvider = SystemTodayProvider,
    localDataCache: LocalDataCache = UnavailableLocalDataCache,
    currentTimeProvider: CurrentTimeProvider = CurrentTimeProvider { LabDateTime(todayProvider.today(), 0) }
) {
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
        var cacheWarning by remember { mutableStateOf<String?>(null) }
        var importPreview by remember { mutableStateOf<ScheduleImportPreview?>(null) }
        var importedWorkbook by remember { mutableStateOf<ImportedWorkbook?>(null) }
        var isFetchingFromCloud by remember { mutableStateOf(false) }
        val today = remember(todayProvider) { todayProvider.today() }
        val now = remember(currentTimeProvider) { currentTimeProvider.now() }

        LaunchedEffect(localDataCache) {
            when (val cached = localDataCache.loadSchedule()) {
                is CacheRead.Valid -> summary = cached.value.summary
                is CacheRead.Invalid -> cacheWarning = cached.safeMessage
                CacheRead.Missing -> Unit
            }
        }

        fun handleWorkbookImportResult(result: WorkbookImportResult) {
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

        val importLauncher = rememberWorkbookImportLauncher { result -> handleWorkbookImportResult(result) }

        fun fetchFromDropbox() {
            if (isFetchingFromCloud) return
            scope.launch {
                isFetchingFromCloud = true
                val result = DropboxScaleRepository().downloadCurrentScale()
                isFetchingFromCloud = false
                handleWorkbookImportResult(result)
            }
        }

        fun resetMock() {
            localDataCache.clearSchedule()
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
                            when (stackedScreen) {
                                StackedScreen.PLANTAO -> PlantaoScreen(
                                    onBack = { stackedScreen = null },
                                    today = today,
                                    now = now,
                                    localDataCache = localDataCache
                                )
                                StackedScreen.SWAP -> ShiftSwapScreen(
                                    currentMemberId = summary.member.id,
                                    onBack = { stackedScreen = null }
                                )
                                null -> {
                                when (activeTab) {
                                    LabTab.Hoje -> TodayTab(
                                        summary = summary,
                                        today = today,
                                        now = now,
                                        onOpenPlantao = onOpenPlantao,
                                        onImportClick = { activeTab = LabTab.Importar }
                                    )
                                    LabTab.Escala -> ScheduleTab(summary = summary, today = today, onOpenPlantao = onOpenPlantao)
                                    LabTab.Importar -> ImportTab(
                                        preview = importPreview,
                                        selectedCollaborator = summary.member.scaleName,
                                        onSelectXls = { importLauncher.launch() },
                                        onFetchFromDropbox = ::fetchFromDropbox,
                                        isFetchingFromCloud = isFetchingFromCloud,
                                        onUseImported = {
                                            importPreview?.summary?.let { imported ->
                                                summary = imported
                                                val resolution = importPreview?.yearResolution as? br.com.leorvergani.escalaici.kmp.lab.model.YearResolution.Resolved
                                                val start = imported.periodStart
                                                if (resolution != null && start != null) {
                                                    localDataCache.saveSchedule(CachedSchedule(
                                                        originalFileName = importPreview?.fileName ?: "arquivo",
                                                        importedAt = "${today.year.toString().padStart(4, '0')}-${today.month.toString().padStart(2, '0')}-${today.day.toString().padStart(2, '0')}",
                                                        resolvedYear = resolution.startYear,
                                                        yearResolutionSource = resolution.source,
                                                        summary = imported
                                                    ))
                                                }
                                                activeTab = LabTab.Hoje
                                            }
                                        },
                                        onSelectCollaborator = { collaborator ->
                                            importedWorkbook?.let { workbook ->
                                                importPreview = LabWorkbookParser.parse(workbook, collaborator)
                                            }
                                        },
                                        onConfirmYear = { startYear ->
                                            importedWorkbook?.let { workbook ->
                                                importPreview = LabWorkbookParser.parse(
                                                    workbook = workbook,
                                                    requestedCollaborator = importPreview?.selectedCollaborator,
                                                    confirmedStartYear = startYear
                                                )
                                            }
                                        },
                                        onCancelYearConfirmation = {
                                            importPreview = null
                                            importedWorkbook = null
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
