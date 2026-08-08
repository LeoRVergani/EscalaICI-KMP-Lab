package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import br.com.leorvergani.escalaici.kmp.lab.firebase.EscalaIciError
import br.com.leorvergani.escalaici.kmp.lab.firebase.LoggedScheduleSyncCoordinator
import br.com.leorvergani.escalaici.kmp.lab.firebase.ScheduleSyncState
import br.com.leorvergani.escalaici.kmp.lab.firebase.TeamScheduleSnapshot
import br.com.leorvergani.escalaici.kmp.lab.firebase.TrocasBadge
import br.com.leorvergani.escalaici.kmp.lab.firebase.createEscalaIciSession
import br.com.leorvergani.escalaici.kmp.lab.platform.ObserveAppForeground
import br.com.leorvergani.escalaici.kmp.lab.model.ImportedWorkbook
import br.com.leorvergani.escalaici.kmp.lab.model.LabWorkbookParser
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleImportPreview
import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.model.WorkbookImportResult
import br.com.leorvergani.escalaici.kmp.lab.model.mockScheduleSummary
import br.com.leorvergani.escalaici.kmp.lab.platform.rememberWorkbookImportLauncher
import br.com.leorvergani.escalaici.kmp.lab.platform.SystemTodayProvider
import br.com.leorvergani.escalaici.kmp.lab.platform.TodayProvider
import br.com.leorvergani.escalaici.kmp.lab.platform.CurrentTimeProvider
import br.com.leorvergani.escalaici.kmp.lab.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.kmp.lab.platform.WebNotificationService
import br.com.leorvergani.escalaici.kmp.lab.platform.UnsupportedWebNotificationService
import br.com.leorvergani.escalaici.kmp.lab.model.LabDateTime
import br.com.leorvergani.escalaici.kmp.lab.repository.DropboxScaleRepository
import br.com.leorvergani.escalaici.kmp.lab.repository.CacheRead
import br.com.leorvergani.escalaici.kmp.lab.repository.CachedSchedule
import br.com.leorvergani.escalaici.kmp.lab.repository.LocalDataCache
import br.com.leorvergani.escalaici.kmp.lab.repository.UnavailableLocalDataCache
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumBackground
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColorScheme
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabShapes
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabTypography
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private enum class LabTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
) {
    Hoje("Hoje", Icons.Outlined.Home, Icons.Filled.Home),
    Escala("Escala", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    /** Só na bottom nav em Modo Demo (spec FASE 16 seção 7) - modo Firebase usa [Trocas] nessa posição. */
    Importar("Importar", Icons.Outlined.CloudUpload, Icons.Filled.CloudUpload),
    /** Só na bottom nav em modo Firebase - substitui [Importar] (spec FASE 16 seção 6). */
    Trocas("Trocas", Icons.Outlined.SwapHoriz, Icons.Filled.SwapHoriz),
    Alertas("Alertas", Icons.Outlined.Warning, Icons.Filled.Warning),
    Perfil("Perfil", Icons.Outlined.Person, Icons.Filled.Person)
}

/** Telas fora do bottom nav, empilhadas sobre a aba ativa (igual ao app real). */
private enum class StackedScreen(val title: String) {
    PLANTAO("Plantão"),
}

@Composable
fun EscalaIciLabApp(
    todayProvider: TodayProvider = SystemTodayProvider,
    localDataCache: LocalDataCache = UnavailableLocalDataCache,
    currentTimeProvider: CurrentTimeProvider = CurrentTimeProvider { LabDateTime(todayProvider.today(), 0) },
    platformCapabilities: PlatformCapabilities = PlatformCapabilities(),
    notificationService: WebNotificationService = UnsupportedWebNotificationService,
) {
    MaterialTheme(colorScheme = LabColorScheme, typography = LabTypography) {
        val scope = rememberCoroutineScope()
        val today = remember(todayProvider) { todayProvider.today() }
        fun todayIso(): String = "${today.year.toString().padStart(4, '0')}-${today.month.toString().padStart(2, '0')}-${today.day.toString().padStart(2, '0')}"

        val session = remember { createEscalaIciSession(::todayIso) }
        val syncCoordinator = session.syncCoordinator
        val trocasSession = session.trocasSession
        val syncState by syncCoordinator.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        var demoModeActive by remember { mutableStateOf(false) }
        var activeTab by remember { mutableStateOf(LabTab.Hoje) }
        var stackedScreen by remember { mutableStateOf<StackedScreen?>(null) }
        var importPreview by remember { mutableStateOf<ScheduleImportPreview?>(null) }
        var importedWorkbook by remember { mutableStateOf<ImportedWorkbook?>(null) }
        var isFetchingFromCloud by remember { mutableStateOf(false) }
        var isRefreshing by remember { mutableStateOf(false) }
        var trocasBadge by remember { mutableStateOf(TrocasBadge(0, 0)) }
        var teamSnapshot by remember { mutableStateOf<TeamScheduleSnapshot?>(null) }
        var now by remember(currentTimeProvider) { mutableStateOf(currentTimeProvider.now()) }

        suspend fun refreshTrocasBadgeQuietly() {
            if (demoModeActive) return
            runCatching { trocasBadge = trocasSession.badge() }
        }

        suspend fun refreshTeamSnapshotQuietly(forceRefresh: Boolean = false) {
            if (demoModeActive) return
            runCatching { teamSnapshot = trocasSession.teamSnapshot(forceRefresh) }
        }

        suspend fun performRefresh() {
            if (isRefreshing) return
            isRefreshing = true
            syncCoordinator.refresh()
            trocasSession.invalidateTeamSnapshot()
            refreshTrocasBadgeQuietly()
            refreshTeamSnapshotQuietly(forceRefresh = true)
            isRefreshing = false
            when (val state = syncCoordinator.state.value) {
                is ScheduleSyncState.Ready -> snackbarHostState.showSnackbar("Escala atualizada")
                is ScheduleSyncState.Error -> snackbarHostState.showSnackbar(errorMessageFor(state.error, state.message))
                else -> Unit
            }
        }

        LaunchedEffect(Unit) {
            syncCoordinator.start()
            refreshTrocasBadgeQuietly()
            refreshTeamSnapshotQuietly()
        }
        LaunchedEffect(currentTimeProvider) {
            while (true) {
                delay(30_000)
                now = currentTimeProvider.now()
            }
        }
        ObserveAppForeground { scope.launch { refreshTrocasBadgeQuietly() } }

        val remoteSummary: ScheduleSummary? = when (val state = syncState) {
            is ScheduleSyncState.Cached -> state.summary
            is ScheduleSyncState.Ready -> state.summary
            is ScheduleSyncState.Error -> state.cachedSummary
            else -> null
        }
        val isSyncing = syncState is ScheduleSyncState.RestoringSession ||
            syncState is ScheduleSyncState.Authenticating ||
            syncState is ScheduleSyncState.ResolvingUser ||
            syncState is ScheduleSyncState.LoadingSchedule
        val syncErrorState = syncState as? ScheduleSyncState.Error
        val needsLogin = !demoModeActive && (syncState is ScheduleSyncState.Idle ||
            (syncErrorState != null && syncErrorState.error == EscalaIciError.AUTH_REQUIRED && remoteSummary == null))

        var summary by remember { mutableStateOf(mockScheduleSummary()) }
        var cacheWarning by remember { mutableStateOf<String?>(null) }
        val effectiveSummary = if (demoModeActive) summary else (remoteSummary ?: summary)

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
                    val preferredCollaborator = importPreview?.selectedCollaborator ?: effectiveSummary.member.scaleName
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
        val onImportClick: () -> Unit = { if (demoModeActive) activeTab = LabTab.Importar }
        val onRefresh: (() -> Unit)? = if (demoModeActive) null else { { scope.launch { performRefresh() } } }
        val visibleTabs = if (demoModeActive) {
            listOf(LabTab.Hoje, LabTab.Escala, LabTab.Importar, LabTab.Alertas, LabTab.Perfil)
        } else {
            listOf(LabTab.Hoje, LabTab.Escala, LabTab.Trocas, LabTab.Alertas, LabTab.Perfil)
        }
        val syncedAtLabel = (syncState as? ScheduleSyncState.Ready)?.syncedAtLabel ?: (syncState as? ScheduleSyncState.Cached)?.syncedAtLabel
        val offlineAvailable = syncState is ScheduleSyncState.Ready || syncState is ScheduleSyncState.Cached

        when {
            needsLogin -> LoginGateScreen(
                isLoading = isSyncing,
                errorMessage = syncErrorState?.takeIf { remoteSummary == null }?.message,
                onLogin = { email, password -> scope.launch { syncCoordinator.login(email, password) } },
                onDemoMode = { demoModeActive = true }
            )
            !demoModeActive && effectiveSummary === summary && remoteSummary == null -> {
                // Sessao existe (ou esta sendo restaurada) mas nenhum dado (cache ou remoto) chegou ainda.
                SyncSplashScreen(state = syncState, onLogout = { scope.launch { syncCoordinator.logout() } })
            }
            else -> {
                LabPremiumBackground {
                    Scaffold(
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (stackedScreen == null) {
                                BottomNav(tabs = visibleTabs, activeTab = activeTab, onSelect = { activeTab = it }, trocasBadgeCount = trocasBadge.total)
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
                                    null -> {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                    when (activeTab) {
                                        LabTab.Hoje -> TodayTab(
                                            summary = effectiveSummary,
                                            today = today,
                                            now = now,
                                            onOpenPlantao = onOpenPlantao,
                                            onImportClick = onImportClick,
                                            onRefresh = onRefresh,
                                            isRefreshing = isRefreshing,
                                            teamSnapshot = teamSnapshot
                                        )
                                        LabTab.Escala -> ScheduleTab(summary = effectiveSummary, today = today, onOpenPlantao = onOpenPlantao, onRefresh = onRefresh, isRefreshing = isRefreshing, teamSnapshot = teamSnapshot)
                                        LabTab.Trocas -> TrocasScreen(
                                            trocasSession = trocasSession,
                                            selectedCollaborator = effectiveSummary.member.scaleName,
                                            loginAtual = effectiveSummary.member.id,
                                            today = today,
                                            onOpenPlantao = onOpenPlantao,
                                            onRefresh = onRefresh,
                                            isRefreshing = isRefreshing,
                                            onBadgeChanged = { trocasBadge = it }
                                        )
                                        LabTab.Importar -> ImportTab(
                                            preview = importPreview,
                                            activeFileName = effectiveSummary.sourceFileName,
                                            selectedCollaborator = effectiveSummary.member.scaleName,
                                            onSelectXls = { importLauncher.launch() },
                                            onFetchFromDropbox = ::fetchFromDropbox,
                                            isFetchingFromCloud = isFetchingFromCloud,
                                            onUseImported = {
                                                importPreview?.summary?.let { imported ->
                                                    summary = imported
                                                    demoModeActive = true
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
                                                    importPreview = null
                                                    importedWorkbook = null
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
                                        LabTab.Alertas -> AlertsTab(summary = effectiveSummary, onOpenPlantao = onOpenPlantao)
                                        LabTab.Perfil -> ProfileTab(
                                            summary = effectiveSummary,
                                            now = now,
                                            supportsAppUpdate = platformCapabilities.supportsAppUpdate,
                                            supportsWebNotifications = platformCapabilities.supportsWebNotifications,
                                            notificationService = notificationService,
                                            onLogout = {
                                                if (demoModeActive) {
                                                    demoModeActive = false
                                                } else {
                                                    scope.launch { syncCoordinator.logout() }
                                                }
                                            },
                                            onOpenPlantao = onOpenPlantao,
                                            onRefresh = onRefresh,
                                            isRefreshing = isRefreshing,
                                            syncedAtLabel = syncedAtLabel,
                                            offlineAvailable = offlineAvailable
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
    }
}

@Composable
private fun SyncSplashScreen(state: ScheduleSyncState, onLogout: () -> Unit) {
    LabPremiumBackground {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state is ScheduleSyncState.Error) {
                Text(errorMessageFor(state.error, state.message), color = LabColors.red, style = MaterialTheme.typography.bodyMedium)
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    TextButton(onClick = onLogout) { Text("Voltar para o login", color = LabColors.primary) }
                }
            } else {
                CircularProgressIndicator(color = LabColors.primary)
                Text(
                    labelFor(state),
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

private fun labelFor(state: ScheduleSyncState): String = when (state) {
    ScheduleSyncState.RestoringSession -> "Restaurando sessão..."
    ScheduleSyncState.Authenticating -> "Entrando..."
    ScheduleSyncState.ResolvingUser -> "Identificando usuário..."
    ScheduleSyncState.LoadingSchedule -> "Carregando escala..."
    else -> "Carregando..."
}

private fun errorMessageFor(error: EscalaIciError, message: String): String = when (error) {
    EscalaIciError.USER_NOT_FOUND -> "Seu login não está cadastrado na escala. Procure o gestor."
    EscalaIciError.USER_INACTIVE -> "Seu cadastro está inativo. Procure o gestor."
    EscalaIciError.TEAM_NOT_FOUND -> "Seu usuário não tem equipe associada."
    EscalaIciError.NO_PUBLISHED_SCHEDULE -> "Nenhuma escala publicada foi encontrada para você ainda."
    EscalaIciError.NO_CURRENT_PERIOD -> "Nenhuma escala publicada cobre o período atual."
    EscalaIciError.PERMISSION_DENIED -> "Sem permissão para ler sua escala."
    EscalaIciError.NETWORK_ERROR -> "Sem conexão e sem dados salvos localmente ainda."
    else -> message
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
private fun BottomNav(tabs: List<LabTab>, activeTab: LabTab, onSelect: (LabTab) -> Unit, trocasBadgeCount: Int) {
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
                tabs.forEach { tab ->
                    val active = tab == activeTab
                    val badgeCount = if (tab == LabTab.Trocas) trocasBadgeCount else 0
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
                                if (badgeCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                                        Icon(
                                            imageVector = if (active) tab.selectedIcon else tab.icon,
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (active) tab.selectedIcon else tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
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
