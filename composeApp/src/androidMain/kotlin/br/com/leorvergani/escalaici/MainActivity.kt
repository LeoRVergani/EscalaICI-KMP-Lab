package br.com.leorvergani.escalaici

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import br.com.leorvergani.escalaici.BuildConfig
import br.com.leorvergani.escalaici.auth.MsalCorporateAuthRepository
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
import br.com.leorvergani.escalaici.platform.AndroidCurrentTimeProvider
import br.com.leorvergani.escalaici.platform.AndroidNotificationScheduler
import br.com.leorvergani.escalaici.platform.AndroidNotificationService
import br.com.leorvergani.escalaici.platform.AndroidNotificationSettingsStore
import br.com.leorvergani.escalaici.platform.ExtraNotificationDate
import br.com.leorvergani.escalaici.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.platform.ensureShiftReminderChannel
import br.com.leorvergani.escalaici.repository.AndroidLocalDataCache
import br.com.leorvergani.escalaici.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.source.DemoPublicationResolver
import br.com.leorvergani.escalaici.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.source.createDemoPublicationGateway
import br.com.leorvergani.escalaici.source.createFirebaseRawCacheStore
import br.com.leorvergani.escalaici.source.createFirebaseScheduleGateway
import br.com.leorvergani.escalaici.source.initializeFirebasePlatform

class MainActivity : ComponentActivity() {
    private lateinit var notificationService: AndroidNotificationService
    private val notificationDateState = mutableStateOf<LabDate?>(null)

    private val postNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationService.onPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebasePlatform(this)
        ensureShiftReminderChannel(this)
        notificationDateState.value = notificationDateFromIntent(intent)
        notificationService = AndroidNotificationService(this, postNotificationsLauncher)
        if (notificationService.shouldRequestPermissionOnStartup()) {
            notificationService.requestPermission { }
        }
        setContent {
            val notificationDate = notificationDateState.value
            val localDataCache = remember { AndroidLocalDataCache(this@MainActivity) }
            val notificationSettingsStore = remember { AndroidNotificationSettingsStore(this@MainActivity) }
            val notificationScheduler = remember { AndroidNotificationScheduler(this@MainActivity) }
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
                localDataCache = localDataCache,
                currentTimeProvider = AndroidCurrentTimeProvider,
                corporateAuthRepository = MsalCorporateAuthRepository(this),
                notificationService = notificationService,
                notificationSettingsStore = notificationSettingsStore,
                localNotificationRuntime = notificationScheduler,
                initialNotificationDate = notificationDate,
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
                        isDevelopmentBuild = BuildConfig.DEBUG,
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
                },
                // FASE 14J.1 (spec 68): reaproveita o mesmo MemberRepository.getMembersByTeam()
                // já usado pelo resolver de identidade acima - nunca uma segunda fonte paralela.
                loadTeamRoster = { workspaceId, teamId ->
                    when (workspaceId) {
                        OrganizationWorkspace.CORPORATE_WORKSPACE_ID -> RemoteFirstDemoMemberRepository(corporatePublicationRepository).getMembersByTeam(teamId)
                        OrganizationWorkspace.DEMO_WORKSPACE_ID -> RemoteFirstDemoMemberRepository(demoPublicationRepository).getMembersByTeam(teamId)
                        else -> emptyList()
                    }
                },
                platformCapabilities = PlatformCapabilities(
                    supportsAppUpdate = true,
                    supportsBackgroundScheduledNotifications = true,
                    supportsCorporateAuth = true
                )
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationDateState.value = notificationDateFromIntent(intent)
    }

    private fun notificationDateFromIntent(intent: Intent?): LabDate? =
        intent?.getStringExtra(ExtraNotificationDate)?.let(LabDate::parseIso)
}
