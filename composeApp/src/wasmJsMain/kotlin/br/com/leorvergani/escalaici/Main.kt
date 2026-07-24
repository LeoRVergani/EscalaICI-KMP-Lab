package br.com.leorvergani.escalaici

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeViewport
import br.com.leorvergani.escalaici.auth.CorporateAuthConfigurationState
import br.com.leorvergani.escalaici.auth.WasmMsalCorporateAuthRepository
import br.com.leorvergani.escalaici.identity.DefaultOrganizationIdentityResolver
import br.com.leorvergani.escalaici.identity.DemoPublicationRepository
import br.com.leorvergani.escalaici.identity.InMemoryMemberDirectoryRepository
import br.com.leorvergani.escalaici.identity.InMemoryMemberRepository
import br.com.leorvergani.escalaici.identity.InMemoryMembershipRepository
import br.com.leorvergani.escalaici.identity.InMemoryTeamRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoMemberDirectoryRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoMemberRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoMembershipRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoTeamRepository
import br.com.leorvergani.escalaici.identity.OrganizationWorkspace
import br.com.leorvergani.escalaici.identity.isDemoAuthorizedForIdentity
import br.com.leorvergani.escalaici.identity.scheduleSummaryForMember
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.repository.WebLocalDataCache
import br.com.leorvergani.escalaici.platform.WebCurrentTimeProvider
import br.com.leorvergani.escalaici.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.platform.BrowserNotificationScheduler
import br.com.leorvergani.escalaici.platform.BrowserNotificationService
import br.com.leorvergani.escalaici.platform.WebNotificationSettingsStore
import br.com.leorvergani.escalaici.source.DemoPublicationResolver
import br.com.leorvergani.escalaici.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.source.createDemoPublicationGateway
import br.com.leorvergani.escalaici.source.createFirebaseRawCacheStore
import br.com.leorvergani.escalaici.source.createFirebaseScheduleGateway

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "webApp") {
        val notifications = remember { BrowserNotificationService() }
        val notificationSettingsStore = remember { WebNotificationSettingsStore() }
        val notificationScheduler = remember { BrowserNotificationScheduler() }
        val notificationDateState = remember { mutableStateOf(notificationDateFromLocation()) }
        LaunchedEffect(Unit) {
            registerNotificationClickListener { dateIso ->
                notificationDateState.value = dateIso?.let(LabDate::parseIso)
            }
        }
        val corporateAuthRepository = remember { WasmMsalCorporateAuthRepository() }
        val demoResolver = remember { DemoPublicationResolver(createDemoPublicationGateway()) }
        val demoPublicationRepository = remember { DemoPublicationRepository(demoResolver) }
        val corporateResolver = remember {
            DemoPublicationResolver(
                gateway = createDemoPublicationGateway(),
                workspaceId = OrganizationWorkspace.CORPORATE_WORKSPACE_ID
            )
        }
        val corporatePublicationRepository = remember { DemoPublicationRepository(
            resolver = corporateResolver,
            fixtureProvider = null
        ) }
        EscalaIciLabApp(
            firebaseGateway = createFirebaseScheduleGateway(),
            firebaseCache = FirebaseSourceCache(createFirebaseRawCacheStore()),
            localDataCache = WebLocalDataCache(),
            currentTimeProvider = WebCurrentTimeProvider,
            platformCapabilities = PlatformCapabilities(
                supportsAppUpdate = false,
                supportsWebNotifications = notifications.capability().supportsSystemNotifications,
                supportsCorporateAuth = corporateAuthRepository.configurationState == CorporateAuthConfigurationState.CONFIGURED
            ),
            notificationService = notifications,
            notificationSettingsStore = notificationSettingsStore,
            localNotificationRuntime = notificationScheduler,
            initialNotificationDate = notificationDateState.value,
            corporateAuthRepository = corporateAuthRepository,
            organizationIdentityResolver = DefaultOrganizationIdentityResolver(
                corporateMemberDirectoryRepository = RemoteFirstDemoMemberDirectoryRepository(
                    corporatePublicationRepository,
                    workspaceId = OrganizationWorkspace.CORPORATE_WORKSPACE_ID
                ),
                corporateMembershipRepository = RemoteFirstDemoMembershipRepository(corporatePublicationRepository),
                corporateMemberRepository = RemoteFirstDemoMemberRepository(corporatePublicationRepository),
                corporateTeamRepository = RemoteFirstDemoTeamRepository(corporatePublicationRepository),
                demoMemberDirectoryRepository = RemoteFirstDemoMemberDirectoryRepository(demoPublicationRepository),
                demoMembershipRepository = RemoteFirstDemoMembershipRepository(demoPublicationRepository),
                demoMemberRepository = RemoteFirstDemoMemberRepository(demoPublicationRepository),
                demoTeamRepository = RemoteFirstDemoTeamRepository(demoPublicationRepository),
                corporateDataSourceStateProvider = { corporatePublicationRepository.state() },
                demoDataSourceStateProvider = { demoPublicationRepository.state() }
            ),
            corporateDataSourceStateProvider = { corporatePublicationRepository.state() },
            isDemoAuthorized = { identity ->
                isDemoAuthorizedForIdentity(
                    identity = identity,
                    // O target Wasm deste projeto nao expoe BuildConfig.DEBUG
                    // nem variante debug/release equivalente no codigo comum.
                    isDevelopmentBuild = false,
                    allowedDeveloperObjectIdsProvider = {
                        demoResolver.loadActivePointer().allowedDeveloperObjectIds
                    }
                )
            },
            loadDemoWorkspaceOverview = { demoPublicationRepository.workspaceOverview() },
            loadPublishedScheduleSummary = { workspaceId, memberId ->
                when (workspaceId) {
                    OrganizationWorkspace.CORPORATE_WORKSPACE_ID -> corporatePublicationRepository.scheduleSummaryForMember(memberId)
                    OrganizationWorkspace.DEMO_WORKSPACE_ID -> demoPublicationRepository.scheduleSummaryForMember(memberId)
                    else -> null
                }
            },
            loadScheduleChangeRequests = { workspaceId, _ ->
                when (workspaceId) {
                    OrganizationWorkspace.CORPORATE_WORKSPACE_ID -> corporatePublicationRepository.data().scheduleChangeRequests
                    OrganizationWorkspace.DEMO_WORKSPACE_ID -> demoPublicationRepository.data().scheduleChangeRequests
                    else -> emptyList()
                }
            }
        )
    }
}

private fun notificationDateFromLocation(): LabDate? =
    notificationDateParameter()?.let(LabDate::parseIso)

private fun notificationDateParameter(): String? =
    js("{ try { const value = new URLSearchParams(window.location.search).get('date'); return typeof value === 'string' ? value : null; } catch (_) { return null; } }")

private fun registerNotificationClickListener(callback: (String?) -> Unit) {
    js("if ('serviceWorker' in navigator && !window.__escalaIciNotificationClickListenerRegistered) { window.__escalaIciNotificationClickListenerRegistered = true; navigator.serviceWorker.addEventListener('message', function(event){ var data = event.data || {}; if (data.type === 'escalaici-notification-click') callback(typeof data.date === 'string' ? data.date : null); }); }")
}
