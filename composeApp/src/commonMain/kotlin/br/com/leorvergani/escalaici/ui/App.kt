package br.com.leorvergani.escalaici.ui

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
import br.com.leorvergani.escalaici.auth.CorporateAuthConfigurationState
import br.com.leorvergani.escalaici.auth.CorporateAuthRepository
import br.com.leorvergani.escalaici.auth.CorporateAuthState
import br.com.leorvergani.escalaici.auth.CorporateIdentity
import br.com.leorvergani.escalaici.identity.DemoPersona
import br.com.leorvergani.escalaici.identity.DemoPersonaCatalog
import br.com.leorvergani.escalaici.identity.OrganizationIdentityResolver
import br.com.leorvergani.escalaici.identity.OrganizationResolutionResult
import br.com.leorvergani.escalaici.model.ImportedWorkbook
import br.com.leorvergani.escalaici.model.LabWorkbookParser
import br.com.leorvergani.escalaici.model.ScheduleImportPreview
import br.com.leorvergani.escalaici.model.ScheduleSummary
import br.com.leorvergani.escalaici.model.WorkbookImportResult
import br.com.leorvergani.escalaici.model.mockScheduleSummary
import br.com.leorvergani.escalaici.platform.rememberWorkbookImportLauncher
import br.com.leorvergani.escalaici.platform.SystemTodayProvider
import br.com.leorvergani.escalaici.platform.TodayProvider
import br.com.leorvergani.escalaici.platform.CurrentTimeProvider
import br.com.leorvergani.escalaici.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.platform.WebNotificationService
import br.com.leorvergani.escalaici.platform.UnsupportedWebNotificationService
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.repository.DropboxScaleRepository
import br.com.leorvergani.escalaici.repository.InMemoryAuthSessionRepository
import br.com.leorvergani.escalaici.repository.CacheRead
import br.com.leorvergani.escalaici.repository.CachedSchedule
import br.com.leorvergani.escalaici.repository.LocalDataCache
import br.com.leorvergani.escalaici.repository.UnavailableLocalDataCache
import br.com.leorvergani.escalaici.ui.components.LabPremiumBackground
import br.com.leorvergani.escalaici.ui.theme.LabColorScheme
import br.com.leorvergani.escalaici.ui.theme.LabColors
import br.com.leorvergani.escalaici.ui.theme.LabShapes
import br.com.leorvergani.escalaici.ui.theme.LabTypography
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import br.com.leorvergani.escalaici.source.DataLoadResult
import br.com.leorvergani.escalaici.source.ScheduleSyncCause
import br.com.leorvergani.escalaici.source.isEmptyState
import br.com.leorvergani.escalaici.source.FirebaseOnCallSource
import br.com.leorvergani.escalaici.source.FirebaseScheduleGateway
import br.com.leorvergani.escalaici.source.FirebaseScheduleSource
import br.com.leorvergani.escalaici.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.source.OnCallSourceData
import br.com.leorvergani.escalaici.source.ScheduleSourceData
import br.com.leorvergani.escalaici.source.SourceMetadata
import br.com.leorvergani.escalaici.source.SourceQuery

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

private enum class EntryContext {
    LOGIN,
    DEMO
}

@Composable
fun EscalaIciLabApp(
    todayProvider: TodayProvider = SystemTodayProvider,
    localDataCache: LocalDataCache = UnavailableLocalDataCache,
    currentTimeProvider: CurrentTimeProvider = CurrentTimeProvider { LabDateTime(todayProvider.today(), 0) },
    platformCapabilities: PlatformCapabilities = PlatformCapabilities(),
    notificationService: WebNotificationService = UnsupportedWebNotificationService,
    firebaseGateway: FirebaseScheduleGateway? = null,
    firebaseCache: FirebaseSourceCache? = null,
    corporateAuthRepository: CorporateAuthRepository? = null,
    organizationIdentityResolver: OrganizationIdentityResolver? = null,
    isDemoAuthorized: (suspend (CorporateIdentity) -> Boolean)? = null,
    loadPublishedScheduleSummary: (suspend (String, String) -> ScheduleSummary?)? = null
) {
    MaterialTheme(colorScheme = LabColorScheme, typography = LabTypography) {
        val authRepository = remember { InMemoryAuthSessionRepository() }
        val scope = rememberCoroutineScope()
        var sessionMemberId by remember { mutableStateOf<String?>(null) }
        val corporateAuthState = corporateAuthRepository?.state?.collectAsState()?.value
        var organizationResolutionResult by remember { mutableStateOf<OrganizationResolutionResult?>(null) }
        var selectedDemoPersona by remember { mutableStateOf<DemoPersona?>(null) }
        var demoPersonaResolutionResult by remember { mutableStateOf<OrganizationResolutionResult?>(null) }
        var requestedEntryContext by remember { mutableStateOf<EntryContext?>(null) }
        var gateErrorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            corporateAuthRepository
                ?.takeIf { it.configurationState == CorporateAuthConfigurationState.CONFIGURED }
                ?.restoreSession()
            sessionMemberId = authRepository.currentMemberId()
        }

        var activeTab by remember { mutableStateOf(LabTab.Hoje) }
        var stackedScreen by remember { mutableStateOf<StackedScreen?>(null) }
        var summary by remember { mutableStateOf(mockScheduleSummary()) }
        var cacheWarning by remember { mutableStateOf<String?>(null) }
        var firebaseMetadata by remember { mutableStateOf<SourceMetadata?>(null) }
        var firebaseError by remember { mutableStateOf<String?>(null) }
        var firebaseSyncCause by remember { mutableStateOf<ScheduleSyncCause?>(null) }
        var firebaseLoading by remember { mutableStateOf(false) }
        var firebaseOnCall by remember { mutableStateOf<OnCallSourceData?>(null) }
        var importPreview by remember { mutableStateOf<ScheduleImportPreview?>(null) }
        var importedWorkbook by remember { mutableStateOf<ImportedWorkbook?>(null) }
        var isFetchingFromCloud by remember { mutableStateOf(false) }
        val today = remember(todayProvider) { todayProvider.today() }
        var now by remember(currentTimeProvider) { mutableStateOf(currentTimeProvider.now()) }
        LaunchedEffect(currentTimeProvider) {
            while (true) {
                delay(30_000)
                now = currentTimeProvider.now()
            }
        }

        LaunchedEffect(corporateAuthState, requestedEntryContext) {
            val resolver = organizationIdentityResolver
            val state = corporateAuthState
            if (state is CorporateAuthState.Authenticated && resolver != null) {
                organizationResolutionResult = null
                organizationResolutionResult = resolver.resolveCorporateIdentity(state.identity)
                when (requestedEntryContext) {
                    EntryContext.LOGIN -> {
                        gateErrorMessage = null
                        when (val result = organizationResolutionResult) {
                            is OrganizationResolutionResult.Resolved -> {
                                sessionMemberId = result.context.memberId
                                summary = loadPublishedScheduleSummary?.invoke(
                                    result.context.workspaceId,
                                    result.context.memberId
                                ) ?: summary.copy(
                                    member = summary.member.copy(
                                        id = result.context.memberId,
                                        displayName = result.context.memberDisplayName,
                                        scaleName = result.context.memberDisplayName
                                    ),
                                    team = summary.team.copy(
                                        teamId = result.context.primaryTeamId ?: summary.team.teamId,
                                        name = result.context.primaryTeamName ?: summary.team.name,
                                        displayName = result.context.primaryTeamName ?: summary.team.displayName
                                    )
                                )
                            }
                            is OrganizationResolutionResult.MemberNotFound -> {
                                gateErrorMessage = "Publicação corporativa indisponível ou cadastro não localizado."
                            }
                            else -> {
                                gateErrorMessage = "Não foi possível confirmar seu vínculo organizacional no momento."
                            }
                        }
                    }
                    EntryContext.DEMO -> {
                        val authorized = isDemoAuthorized?.invoke(state.identity) == true
                        if (authorized) {
                            gateErrorMessage = null
                            selectedDemoPersona = DemoPersonaCatalog.personas[2]
                        } else {
                            selectedDemoPersona = null
                            demoPersonaResolutionResult = null
                            gateErrorMessage = "Esta conta não possui acesso ao modo Demo."
                        }
                    }
                    null -> Unit
                }
            } else {
                organizationResolutionResult = null
            }
            // Nao limpa selectedDemoPersona aqui: workspace Demo e ortogonal ao
            // CorporateAuthState (spec 56 secao 12), entao uma transicao generica
            // (ex.: restauracao silenciosa terminando depois da selecao) nunca deve
            // apagar a persona escolhida - so uma acao explicita do usuario faria
            // isso (nao implementada nesta fase; hoje a selecao so muda quando o
            // usuario toca em outro personagem).
        }

        LaunchedEffect(selectedDemoPersona) {
            val resolver = organizationIdentityResolver
            val persona = selectedDemoPersona
            if (resolver != null && persona != null) {
                demoPersonaResolutionResult = null
                demoPersonaResolutionResult = resolver.resolveDemoPersona(persona)
                val result = demoPersonaResolutionResult
                if (result is OrganizationResolutionResult.Resolved && requestedEntryContext == EntryContext.DEMO) {
                    sessionMemberId = result.context.memberId
                    summary = loadPublishedScheduleSummary?.invoke(
                        result.context.workspaceId,
                        result.context.memberId
                    ) ?: summary.copy(
                        member = summary.member.copy(
                            id = result.context.memberId,
                            displayName = result.context.memberDisplayName,
                            scaleName = result.context.memberDisplayName
                        ),
                        team = summary.team.copy(
                            teamId = result.context.primaryTeamId ?: summary.team.teamId,
                            name = result.context.primaryTeamName ?: summary.team.name,
                            displayName = result.context.primaryTeamName ?: summary.team.displayName
                        )
                    )
                }
            } else {
                demoPersonaResolutionResult = null
            }
        }

        LaunchedEffect(localDataCache) {
            when (val cached = localDataCache.loadSchedule()) {
                is CacheRead.Valid -> summary = cached.value.summary
                is CacheRead.Invalid -> cacheWarning = cached.safeMessage
                CacheRead.Missing -> Unit
            }
        }

        fun refreshFirebase() {
            // MVP Firebase dev: a leitura Firestore permitida e revisionada por
            // workspace. A sincronizacao legada abaixo consulta colecoes raiz e
            // fica desativada ate ser religada ao snapshot revisionado.
        }

        LaunchedEffect(sessionMemberId, firebaseGateway, firebaseCache) {
            Unit
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
                supportsCorporateAuth = platformCapabilities.supportsCorporateAuth,
                corporateAuthRepository = corporateAuthRepository,
                errorMessage = gateErrorMessage,
                onLogin = { requestedEntryContext = EntryContext.LOGIN },
                onDemo = { requestedEntryContext = EntryContext.DEMO }
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
                                    localDataCache = localDataCache,
                                    firebaseData = firebaseOnCall,
                                    onRetryFirebase = ::refreshFirebase
                                )
                                StackedScreen.SWAP -> ShiftSwapScreen(
                                    currentMemberId = summary.member.id,
                                    onBack = { stackedScreen = null }
                                )
                                null -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
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
                                        activeFileName = summary.sourceFileName,
                                        selectedCollaborator = summary.member.scaleName,
                                        onSelectXls = { importLauncher.launch() },
                                        onFetchFromDropbox = ::fetchFromDropbox,
                                        isFetchingFromCloud = isFetchingFromCloud,
                                        onUseImported = {
                                            importPreview?.summary?.let { imported ->
                                                summary = imported
                                                val resolution = importPreview?.yearResolution as? br.com.leorvergani.escalaici.model.YearResolution.Resolved
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
                                    LabTab.Alertas -> AlertsTab(summary = summary, onOpenPlantao = onOpenPlantao)
                                    LabTab.Perfil -> ProfileTab(
                                        summary = summary,
                                        now = now,
                                        supportsAppUpdate = platformCapabilities.supportsAppUpdate,
                                        supportsWebNotifications = platformCapabilities.supportsWebNotifications,
                                        supportsCorporateAuth = platformCapabilities.supportsCorporateAuth,
                                        corporateAuthRepository = corporateAuthRepository,
                                        organizationResolutionResult = organizationResolutionResult,
                                        demoPersonaResolutionResult = demoPersonaResolutionResult,
                                        selectedDemoPersona = selectedDemoPersona,
                                        notificationService = notificationService,
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
    }
}

@Composable
private fun FirebaseStatusBar(
    metadata: SourceMetadata?,
    error: String?,
    cause: ScheduleSyncCause?,
    loading: Boolean,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(LabColors.surfaceElevated.copy(alpha = 0.92f)).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val text = when {
            loading -> "Atualizando Firebase..."
            error != null -> error
            metadata?.fromCache == true -> "Fonte: Firebase · disponível offline · ${metadata.periodId.orEmpty()}"
            metadata != null -> "Fonte: Firebase · ${metadata.periodId.orEmpty()} · sincronizado em ${metadata.localSyncedAt.orEmpty()}"
            else -> "Fonte Firebase ainda não carregada"
        }
        // Estados "vazios" (equipe/período/turnos inexistentes) não são falhas — só o
        // que sobra depois de classificar por causa é exibido em vermelho como erro real.
        val isRealError = error != null && cause?.isEmptyState() != true
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = if (isRealError) LabColors.red else LabColors.onSurfaceMuted)
        if (!loading) TextButton(onClick = onRetry) { Text("Tentar novamente") }
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
