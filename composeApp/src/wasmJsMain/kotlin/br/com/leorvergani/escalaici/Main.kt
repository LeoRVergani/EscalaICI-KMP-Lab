package br.com.leorvergani.escalaici

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.com.leorvergani.escalaici.identity.DefaultOrganizationIdentityResolver
import br.com.leorvergani.escalaici.identity.InMemoryMemberDirectoryRepository
import br.com.leorvergani.escalaici.identity.InMemoryMemberRepository
import br.com.leorvergani.escalaici.identity.InMemoryMembershipRepository
import br.com.leorvergani.escalaici.identity.InMemoryTeamRepository
import br.com.leorvergani.escalaici.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.repository.WebLocalDataCache
import br.com.leorvergani.escalaici.platform.WebCurrentTimeProvider
import br.com.leorvergani.escalaici.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.platform.BrowserNotificationService
import br.com.leorvergani.escalaici.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.source.createFirebaseRawCacheStore
import br.com.leorvergani.escalaici.source.createFirebaseScheduleGateway

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "webApp") {
        val notifications = BrowserNotificationService()
        EscalaIciLabApp(
            firebaseGateway = createFirebaseScheduleGateway(),
            firebaseCache = FirebaseSourceCache(createFirebaseRawCacheStore()),
            localDataCache = WebLocalDataCache(),
            currentTimeProvider = WebCurrentTimeProvider,
            platformCapabilities = PlatformCapabilities(
                supportsAppUpdate = false,
                supportsWebNotifications = notifications.capability().supportsSystemNotifications
            ),
            notificationService = notifications,
            // O seletor "Testar como" (workspace demo-v1) e UI comum, sempre visivel
            // independente de supportsCorporateAuth (spec 56 secao 12) - precisa de um
            // resolver aqui tambem, senao a selecao Demo nunca resolve no alvo Web
            // (achado da revisao independente desta fase). Diretorio corporativo
            // vazio de proposito, mesmo padrao honesto do MainActivity.kt Android -
            // nao ha fonte real de member_team_memberships ainda nesta fase.
            organizationIdentityResolver = DefaultOrganizationIdentityResolver(
                corporateMemberDirectoryRepository = InMemoryMemberDirectoryRepository(members = emptyList()),
                corporateMembershipRepository = InMemoryMembershipRepository(),
                corporateMemberRepository = InMemoryMemberRepository(emptyList()),
                corporateTeamRepository = InMemoryTeamRepository(emptyList())
            )
        )
    }
}
