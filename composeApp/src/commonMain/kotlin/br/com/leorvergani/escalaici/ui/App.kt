package br.com.leorvergani.escalaici.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.auth.CorporateAuthConfigurationState
import br.com.leorvergani.escalaici.auth.CorporateAuthRepository
import br.com.leorvergani.escalaici.auth.CorporateAuthState
import br.com.leorvergani.escalaici.auth.CorporateIdentity
import br.com.leorvergani.escalaici.identity.DemoDataOrigin
import br.com.leorvergani.escalaici.identity.DemoDataSourceState
import br.com.leorvergani.escalaici.identity.DemoPersona
import br.com.leorvergani.escalaici.identity.DemoWorkspaceOverview
import br.com.leorvergani.escalaici.identity.OrganizationIdentityResolver
import br.com.leorvergani.escalaici.identity.OrganizationResolutionResult
import br.com.leorvergani.escalaici.model.ImportedWorkbook
import br.com.leorvergani.escalaici.model.LabWorkbookParser
import br.com.leorvergani.escalaici.model.OnCallGroup
import br.com.leorvergani.escalaici.model.ScheduleImportPreview
import br.com.leorvergani.escalaici.model.ScheduleSummary
import br.com.leorvergani.escalaici.model.WorkbookImportResult
import br.com.leorvergani.escalaici.model.mockScheduleSummary
import br.com.leorvergani.escalaici.platform.rememberWorkbookImportLauncher
import br.com.leorvergani.escalaici.platform.SystemTodayProvider
import br.com.leorvergani.escalaici.platform.TodayProvider
import br.com.leorvergani.escalaici.platform.CurrentTimeProvider
import br.com.leorvergani.escalaici.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.platform.PlatformBackHandler
import br.com.leorvergani.escalaici.platform.WebNotificationService
import br.com.leorvergani.escalaici.platform.UnsupportedWebNotificationService
import br.com.leorvergani.escalaici.platform.InMemoryNotificationSettingsStore
import br.com.leorvergani.escalaici.platform.LocalNotificationRuntime
import br.com.leorvergani.escalaici.platform.NoOpLocalNotificationRuntime
import br.com.leorvergani.escalaici.platform.NotificationSettingsStore
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.NotificationSettings
import br.com.leorvergani.escalaici.model.YearResolutionSource
import br.com.leorvergani.escalaici.model.buildNotificationPlan
import br.com.leorvergani.escalaici.model.isoMinuteLabel
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

internal enum class EntryContext {
    LOGIN,
    DEMO
}

/**
 * Três estados explícitos de inicialização da sessão (FASE 14J, spec 67 seção 2.1) -
 * evita mostrar `LoginGateScreen` antes da restauração silenciosa do MSAL terminar
 * (o "flash" reportado pelo usuário). `RESTORING` só se aplica quando a sessão já
 * está `Authenticated` mas a resolução de identidade/escala (disparada pelo
 * `LaunchedEffect(corporateAuthState, requestedEntryContext)` já existente) ainda
 * não produziu um resultado definitivo (nem `sessionMemberId`, nem `gateErrorMessage`,
 * nem uma entrada explícita no Demo).
 */
internal enum class SessionBootstrapPhase { INITIALIZING, RESTORING, READY }

internal fun computeSessionBootstrapPhase(
    hasConfiguredCorporateAuth: Boolean,
    corporateAuthState: CorporateAuthState?,
    sessionMemberId: String?,
    gateErrorMessage: String?,
    requestedEntryContext: EntryContext?
): SessionBootstrapPhase {
    if (!hasConfiguredCorporateAuth) return SessionBootstrapPhase.READY
    return when (corporateAuthState) {
        null -> SessionBootstrapPhase.INITIALIZING
        is CorporateAuthState.Authenticated ->
            if (sessionMemberId != null || gateErrorMessage != null || requestedEntryContext == EntryContext.DEMO) {
                SessionBootstrapPhase.READY
            } else {
                SessionBootstrapPhase.RESTORING
            }
        CorporateAuthState.NotConfigured,
        CorporateAuthState.SignedOut,
        CorporateAuthState.Authenticating,
        CorporateAuthState.Demo,
        is CorporateAuthState.Failed -> SessionBootstrapPhase.READY
    }
}

@Composable
fun EscalaIciLabApp(
    todayProvider: TodayProvider = SystemTodayProvider,
    localDataCache: LocalDataCache = UnavailableLocalDataCache,
    currentTimeProvider: CurrentTimeProvider = CurrentTimeProvider { LabDateTime(todayProvider.today(), 0) },
    platformCapabilities: PlatformCapabilities = PlatformCapabilities(),
    notificationService: WebNotificationService = UnsupportedWebNotificationService,
    notificationSettingsStore: NotificationSettingsStore = InMemoryNotificationSettingsStore(),
    localNotificationRuntime: LocalNotificationRuntime = NoOpLocalNotificationRuntime,
    initialNotificationDate: LabDate? = null,
    firebaseGateway: FirebaseScheduleGateway? = null,
    firebaseCache: FirebaseSourceCache? = null,
    corporateAuthRepository: CorporateAuthRepository? = null,
    organizationIdentityResolver: OrganizationIdentityResolver? = null,
    corporateDataSourceStateProvider: (suspend () -> DemoDataSourceState?)? = null,
    isDemoAuthorized: (suspend (CorporateIdentity) -> Boolean)? = null,
    loadDemoWorkspaceOverview: (suspend () -> DemoWorkspaceOverview)? = null,
    loadPublishedScheduleSummary: (suspend (String, String) -> ScheduleSummary?)? = null
) {
    MaterialTheme(colorScheme = LabColorScheme, typography = LabTypography) {
        val authRepository = remember { InMemoryAuthSessionRepository() }
        val scope = rememberCoroutineScope()
        var sessionMemberId by remember { mutableStateOf<String?>(null) }
        val corporateAuthState = corporateAuthRepository?.state?.collectAsState()?.value
        var organizationResolutionResult by remember { mutableStateOf<OrganizationResolutionResult?>(null) }
        var corporateDataSourceState by remember { mutableStateOf<DemoDataSourceState?>(null) }
        var demoWorkspaceSession by remember { mutableStateOf<DemoWorkspaceOverviewSession?>(null) }
        var selectedDemoPersona by remember { mutableStateOf<DemoPersona?>(null) }
        var demoPersonaResolutionResult by remember { mutableStateOf<OrganizationResolutionResult?>(null) }
        var demoPersonaLoading by remember { mutableStateOf(false) }
        var requestedEntryContext by remember { mutableStateOf<EntryContext?>(null) }
        var gateErrorMessage by remember { mutableStateOf<String?>(null) }
        val sessionBootstrapPhase = computeSessionBootstrapPhase(
            hasConfiguredCorporateAuth = corporateAuthRepository?.configurationState == CorporateAuthConfigurationState.CONFIGURED,
            corporateAuthState = corporateAuthState,
            sessionMemberId = sessionMemberId,
            gateErrorMessage = gateErrorMessage,
            requestedEntryContext = requestedEntryContext
        )

        LaunchedEffect(Unit) {
            corporateAuthRepository
                ?.takeIf { it.configurationState == CorporateAuthConfigurationState.CONFIGURED }
                ?.restoreSession()
            sessionMemberId = authRepository.currentMemberId()
        }

        // Dispara a resolução de identidade automaticamente quando a sessão MSAL é
        // restaurada silenciosamente (sem toque do usuário) - complementa o
        // LaunchedEffect abaixo, que já resolve identidade quando requestedEntryContext
        // é setado por um toque manual em LoginGateScreen. Não sobrescreve uma entrada
        // já em andamento (ex.: Demo escolhido explicitamente).
        LaunchedEffect(corporateAuthState) {
            if (corporateAuthState is CorporateAuthState.Authenticated && requestedEntryContext == null) {
                requestedEntryContext = EntryContext.LOGIN
            }
        }

        var activeTab by remember(initialNotificationDate) {
            mutableStateOf(if (initialNotificationDate != null) LabTab.Escala else LabTab.Hoje)
        }
        var stackedScreen by remember { mutableStateOf<StackedScreen?>(null) }
        var summary by remember { mutableStateOf(mockScheduleSummary()) }
        var cacheWarning by remember { mutableStateOf<String?>(null) }
        var firebaseMetadata by remember { mutableStateOf<SourceMetadata?>(null) }
        var firebaseError by remember { mutableStateOf<String?>(null) }
        var firebaseSyncCause by remember { mutableStateOf<ScheduleSyncCause?>(null) }
        var firebaseLoading by remember { mutableStateOf(false) }
        var firebaseOnCall by remember { mutableStateOf<OnCallSourceData?>(null) }
        var firebaseOnCallGroups by remember { mutableStateOf<List<OnCallGroup>>(emptyList()) }
        var appNotificationSettings by remember(notificationSettingsStore) { mutableStateOf(NotificationSettings()) }
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
                corporateDataSourceState = null
                val resolvedIdentity = resolver.resolveCorporateIdentity(state.identity)
                organizationResolutionResult = resolvedIdentity
                corporateDataSourceState = corporateDataSourceStateProvider?.let { provider ->
                    runCatching { provider() }.getOrNull()
                }
                when (requestedEntryContext) {
                    EntryContext.LOGIN -> {
                        gateErrorMessage = null
                        demoWorkspaceSession = null
                        selectedDemoPersona = null
                        demoPersonaResolutionResult = null
                        when (val result = resolvedIdentity) {
                            is OrganizationResolutionResult.Resolved -> {
                                val publishedSummary = loadPublishedScheduleSummary?.invoke(
                                    result.context.workspaceId,
                                    result.context.memberId
                                )
                                val decision = decideResolvedScheduleSummary(
                                    currentSummary = summary,
                                    resolvedContext = result.context,
                                    loadPublishedScheduleSummaryAvailable = loadPublishedScheduleSummary != null,
                                    publishedSummary = publishedSummary
                                )
                                gateErrorMessage = decision.errorMessage
                                if (decision.summary != null) {
                                    sessionMemberId = result.context.memberId
                                    summary = decision.summary
                                } else {
                                    sessionMemberId = null
                                }
                            }
                            else -> {
                                gateErrorMessage = loginGateErrorMessage(result, corporateDataSourceState)
                            }
                        }
                    }
                    EntryContext.DEMO -> {
                        val authorized = isDemoAuthorized?.invoke(state.identity) == true
                        if (!authorized) {
                            demoWorkspaceSession = null
                            selectedDemoPersona = null
                            demoPersonaResolutionResult = null
                            gateErrorMessage = "Esta conta não possui acesso ao modo Demo."
                        } else if (demoWorkspaceSession == null && selectedDemoPersona == null && sessionMemberId == null) {
                            val overview = loadDemoWorkspaceOverview?.invoke()
                            if (overview == null) {
                                gateErrorMessage = "Ambiente Demo indisponível neste momento."
                            } else {
                                when (val decision = decideDemoWorkspaceEntry(state.identity, authorized = true, overview)) {
                                    is DemoWorkspaceEntryDecision.ShowOverview -> {
                                        gateErrorMessage = null
                                        demoWorkspaceSession = decision.session
                                        demoPersonaResolutionResult = null
                                    }
                                    is DemoWorkspaceEntryDecision.Denied -> {
                                        demoWorkspaceSession = null
                                        selectedDemoPersona = null
                                        demoPersonaResolutionResult = null
                                        gateErrorMessage = decision.message
                                    }
                                }
                            }
                        }
                    }
                    null -> Unit
                }
            } else {
                organizationResolutionResult = null
                corporateDataSourceState = null
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
                demoPersonaLoading = true
                demoPersonaResolutionResult = null
                demoPersonaResolutionResult = resolver.resolveDemoPersona(persona)
                val result = demoPersonaResolutionResult
                if (result is OrganizationResolutionResult.Resolved && requestedEntryContext == EntryContext.DEMO) {
                    val publishedSummary = loadPublishedScheduleSummary?.invoke(
                        result.context.workspaceId,
                        result.context.memberId
                    )
                    val decision = decideResolvedScheduleSummary(
                        currentSummary = summary,
                        resolvedContext = result.context,
                        loadPublishedScheduleSummaryAvailable = loadPublishedScheduleSummary != null,
                        publishedSummary = publishedSummary
                    )
                    gateErrorMessage = decision.errorMessage
                    if (decision.summary != null) {
                        sessionMemberId = result.context.memberId
                        summary = decision.summary
                    } else {
                        sessionMemberId = null
                    }
                } else if (result != null && requestedEntryContext == EntryContext.DEMO) {
                    gateErrorMessage = loginGateErrorMessage(result, null)
                }
                demoPersonaLoading = false
            } else {
                demoPersonaResolutionResult = null
                demoPersonaLoading = false
            }
        }

        LaunchedEffect(localDataCache) {
            when (val cached = localDataCache.loadSchedule()) {
                is CacheRead.Valid -> summary = cached.value.summary
                is CacheRead.Invalid -> cacheWarning = cached.safeMessage
                CacheRead.Missing -> Unit
            }
        }

        LaunchedEffect(notificationSettingsStore) {
            appNotificationSettings = notificationSettingsStore.load()
        }

        LaunchedEffect(summary.days, appNotificationSettings, localNotificationRuntime) {
            if (hasPublishedOrImportedSchedule(summary)) {
                localNotificationRuntime.reconcile(
                    buildNotificationPlan(summary.days, appNotificationSettings, now)
                )
            }
        }

        LaunchedEffect(summary, localDataCache) {
            if (!hasPublishedOrImportedSchedule(summary)) return@LaunchedEffect
            val start = summary.periodStart ?: return@LaunchedEffect
            localDataCache.saveSchedule(
                CachedSchedule(
                    originalFileName = summary.sourceFileName ?: summary.remoteSourceLabel ?: "publicacao-remota",
                    importedAt = now.isoMinuteLabel(),
                    resolvedYear = start.year,
                    yearResolutionSource = YearResolutionSource.USER_CONFIRMED,
                    summary = summary
                )
            )
        }

        fun refreshFirebaseOnCall(groupId: String? = null) {
            val gateway = firebaseGateway ?: return
            val cache = firebaseCache ?: return
            val teamId = summary.team.teamId
            if (teamId.isBlank()) return
            scope.launch {
                val source = FirebaseOnCallSource(gateway, cache) { now.firebaseTimestamp() }
                val groups = runCatching { source.loadOnCallGroups(teamId) }.getOrElse {
                    firebaseError = "Não foi possível carregar grupos de plantão."
                    emptyList()
                }
                firebaseOnCallGroups = groups
                val selectedGroupId = groupId ?: groups.singleOrNull()?.id
                if (selectedGroupId == null) {
                    firebaseOnCall = null
                    return@launch
                }
                when (val result = source.loadActive(SourceQuery(memberId = sessionMemberId, teamId = teamId, groupId = selectedGroupId))) {
                    is DataLoadResult.Success -> {
                        firebaseOnCall = result.data
                        firebaseError = null
                    }
                    is DataLoadResult.OfflineCache -> {
                        firebaseOnCall = result.data
                        firebaseError = null
                    }
                    is DataLoadResult.Empty -> {
                        firebaseOnCall = null
                        firebaseSyncCause = result.cause
                        firebaseError = result.metadata.userMessage
                    }
                    is DataLoadResult.RecoverableError -> {
                        firebaseError = result.message
                    }
                    is DataLoadResult.FatalError -> {
                        firebaseError = result.message
                    }
                }
            }
        }

        fun refreshFirebase() {
            refreshFirebaseOnCall(firebaseOnCall?.period?.groupId)
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

        val onOpenPlantao: () -> Unit = {
            stackedScreen = StackedScreen.PLANTAO
            refreshFirebaseOnCall()
        }

        val activeDemoWorkspaceSession = demoWorkspaceSession
        fun returnToEntryGateFromDemoWorkspace() {
            demoWorkspaceSession = null
            selectedDemoPersona = null
            demoPersonaResolutionResult = null
            gateErrorMessage = null
            requestedEntryContext = null
        }

        fun returnToDemoWorkspaceFromPersona() {
            sessionMemberId = null
            selectedDemoPersona = null
            demoPersonaResolutionResult = null
            gateErrorMessage = null
            activeTab = LabTab.Hoje
            stackedScreen = null
        }

        when (decideDemoBackNavigation(
            requestedEntryIsDemo = requestedEntryContext == EntryContext.DEMO,
            hasDemoWorkspaceSession = activeDemoWorkspaceSession != null,
            hasSelectedDemoPersona = selectedDemoPersona != null,
            hasSessionMemberId = sessionMemberId != null
        )) {
            DemoBackNavigationTarget.ENTRY_GATE -> PlatformBackHandler(
                onBack = ::returnToEntryGateFromDemoWorkspace
            )
            DemoBackNavigationTarget.DEMO_WORKSPACE_OVERVIEW -> PlatformBackHandler(
                enabled = stackedScreen == null,
                onBack = ::returnToDemoWorkspaceFromPersona
            )
            null -> Unit
        }
        PlatformBackHandler(enabled = sessionMemberId != null && stackedScreen != null) {
            stackedScreen = null
        }

        if (sessionBootstrapPhase != SessionBootstrapPhase.READY) {
            SessionBootstrapScreen()
        } else if (sessionMemberId == null && activeDemoWorkspaceSession == null) {
            LoginGateScreen(
                supportsCorporateAuth = platformCapabilities.supportsCorporateAuth,
                corporateAuthRepository = corporateAuthRepository,
                errorMessage = gateErrorMessage,
                onLogin = { requestedEntryContext = EntryContext.LOGIN },
                onDemo = { requestedEntryContext = EntryContext.DEMO }
            )
        } else if (sessionMemberId == null && activeDemoWorkspaceSession != null) {
            DemoWorkspaceOverviewScreen(
                session = activeDemoWorkspaceSession,
                selectedPersona = selectedDemoPersona,
                isResolvingPersona = demoPersonaLoading,
                errorMessage = gateErrorMessage,
                onViewAsPersona = { persona ->
                    gateErrorMessage = null
                    selectedDemoPersona = persona
                },
                onBack = ::returnToEntryGateFromDemoWorkspace
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
                                    onCallGroups = firebaseOnCallGroups,
                                    teamId = summary.team.teamId,
                                    onRetryFirebase = ::refreshFirebase,
                                    onGroupSelected = { group -> refreshFirebaseOnCall(group.id) }
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
                                        notificationSettings = appNotificationSettings,
                                        onOpenPlantao = onOpenPlantao,
                                        onImportClick = { activeTab = LabTab.Importar }
                                    )
                                    LabTab.Escala -> ScheduleTab(
                                        summary = summary,
                                        today = today,
                                        initialSelectedDate = initialNotificationDate,
                                        onOpenPlantao = onOpenPlantao
                                    )
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
                                        notificationSettingsStore = notificationSettingsStore,
                                        initialNotificationSettings = appNotificationSettings,
                                        onNotificationSettingsSaved = { settings ->
                                            appNotificationSettings = settings
                                        },
                                        onReconcileNotifications = { plan ->
                                            localNotificationRuntime.reconcile(plan)
                                        },
                                        onLogout = {
                                            if (requestedEntryContext == EntryContext.DEMO && selectedDemoPersona != null) {
                                                returnToDemoWorkspaceFromPersona()
                                            } else {
                                                scope.launch { authRepository.signOut() }
                                                sessionMemberId = null
                                            }
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

/**
 * Tela mínima exibida enquanto [SessionBootstrapPhase] ainda não é `READY` -
 * evita mostrar `LoginGateScreen` (e seu botão de login) antes da restauração
 * silenciosa da sessão MSAL terminar (FASE 14J, spec 67 seção 2.1).
 */
@Composable
private fun SessionBootstrapScreen() {
    LabPremiumBackground {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Escala ICI",
                color = LabColors.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = LabColors.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Verificando sessão...",
                color = LabColors.onSurfaceMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun loginGateErrorMessage(
    result: OrganizationResolutionResult,
    dataSourceState: DemoDataSourceState?
): String {
    // Granularidade real atual: ScheduleSyncCause nao separa ponteiro do
    // workspace ausente de revisao ausente, e MemberNotFound nao separa link
    // de usuario ausente de membro removido apos o lookup do diretorio.
    if (dataSourceState?.origin == DemoDataOrigin.REMOTE_UNAVAILABLE) {
        return corporatePublicationUnavailableMessage(dataSourceState)
    }

    return when (result) {
        is OrganizationResolutionResult.Resolved -> ""
        is OrganizationResolutionResult.MemberNotFound ->
            "Cadastro corporativo não localizado na publicação oficial."
        is OrganizationResolutionResult.MemberInactive ->
            "Seu cadastro corporativo está inativo no momento. Contate o administrador."
        is OrganizationResolutionResult.MemberIdentityAmbiguous ->
            "Foram encontrados cadastros duplicados para esta identidade. Contate o administrador."
        is OrganizationResolutionResult.MembershipNotFound ->
            "Seu cadastro foi localizado, mas ainda não possui vínculo de equipe ativo."
        is OrganizationResolutionResult.TeamNotFound ->
            "Seu vínculo de equipe foi localizado, mas a equipe correspondente não está disponível."
        is OrganizationResolutionResult.MemberFoundNoActiveTeam ->
            "Seu cadastro foi localizado, mas não há equipe ativa vinculada."
        is OrganizationResolutionResult.MultipleActiveTeams ->
            "Há mais de uma equipe ativa vinculada a esta conta. Contate o administrador."
        is OrganizationResolutionResult.WorkspaceMismatch ->
            "A publicação oficial retornou dados de outro ambiente. Contate o administrador."
        is OrganizationResolutionResult.DataSourceUnavailable ->
            result.message.ifBlank { "Não foi possível confirmar seu vínculo organizacional no momento." }
    }
}

private fun corporatePublicationUnavailableMessage(state: DemoDataSourceState): String =
    when (state.fallbackCause) {
        ScheduleSyncCause.AUTH_REQUIRED ->
            "Firebase não configurado para leitura da publicação oficial neste ambiente."
        ScheduleSyncCause.FIRESTORE_DATABASE_DISABLED ->
            "O banco de dados Firebase deste ambiente ainda não foi ativado."
        ScheduleSyncCause.PERMISSION_DENIED ->
            "Sem permissão para ler a publicação oficial neste ambiente."
        ScheduleSyncCause.NETWORK_ERROR ->
            "Não foi possível conectar ao Firebase. Verifique sua internet e tente novamente."
        ScheduleSyncCause.INVALID_REMOTE_DATA ->
            "A publicação oficial está incompleta ou inválida neste ambiente."
        ScheduleSyncCause.TEAM_NOT_FOUND,
        ScheduleSyncCause.NO_ACTIVE_PERIOD,
        ScheduleSyncCause.NO_ASSIGNMENTS ->
            "A escala oficial ainda não possui dados ativos para esta conta."
        ScheduleSyncCause.CACHE_AVAILABLE ->
            "Mostrando a última publicação oficial salva localmente."
        ScheduleSyncCause.WORKSPACE_NOT_PUBLISHED,
        ScheduleSyncCause.IDENTITY_NOT_LINKED,
        ScheduleSyncCause.UNKNOWN,
        null ->
            "A escala oficial ainda não foi publicada neste ambiente."
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

private fun LabDateTime.firebaseTimestamp(): String {
    val dateLabel = "${date.year.toString().padStart(4, '0')}-${date.month.toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
    val timeLabel = "${(minuteOfDay / 60).toString().padStart(2, '0')}:${(minuteOfDay % 60).toString().padStart(2, '0')}:00"
    return "${dateLabel}T$timeLabel"
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
