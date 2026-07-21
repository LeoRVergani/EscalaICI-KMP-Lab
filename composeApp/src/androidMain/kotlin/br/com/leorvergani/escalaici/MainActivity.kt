package br.com.leorvergani.escalaici

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
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
import br.com.leorvergani.escalaici.identity.scheduleSummaryForMember
import br.com.leorvergani.escalaici.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.source.DemoPublicationResolver
import br.com.leorvergani.escalaici.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.source.createDemoPublicationGateway
import br.com.leorvergani.escalaici.source.createFirebaseRawCacheStore
import br.com.leorvergani.escalaici.source.createFirebaseScheduleGateway
import br.com.leorvergani.escalaici.source.initializeFirebasePlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebasePlatform(this)
        setContent {
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
                corporateAuthRepository = MsalCorporateAuthRepository(this),
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
                    demoDataSourceStateProvider = { demoPublicationRepository.state() }
                ),
                isDemoAuthorized = { identity ->
                    runCatching {
                        demoResolver.loadActivePointer().allowedDeveloperObjectIds.contains(identity.objectId)
                    }.getOrDefault(false)
                },
                loadPublishedScheduleSummary = { workspaceId, memberId ->
                    when (workspaceId) {
                        OrganizationWorkspace.CORPORATE_WORKSPACE_ID -> corporatePublicationRepository.scheduleSummaryForMember(memberId)
                        OrganizationWorkspace.DEMO_WORKSPACE_ID -> demoPublicationRepository.scheduleSummaryForMember(memberId)
                        else -> null
                    }
                },
                platformCapabilities = PlatformCapabilities(
                    supportsAppUpdate = true,
                    supportsCorporateAuth = true
                )
            )
        }
    }
}
