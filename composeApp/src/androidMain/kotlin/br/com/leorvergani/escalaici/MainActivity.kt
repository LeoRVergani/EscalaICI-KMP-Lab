package br.com.leorvergani.escalaici

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.leorvergani.escalaici.auth.MsalCorporateAuthRepository
import br.com.leorvergani.escalaici.identity.DefaultOrganizationIdentityResolver
import br.com.leorvergani.escalaici.identity.InMemoryMemberDirectoryRepository
import br.com.leorvergani.escalaici.identity.InMemoryMemberRepository
import br.com.leorvergani.escalaici.identity.InMemoryMembershipRepository
import br.com.leorvergani.escalaici.identity.InMemoryTeamRepository
import br.com.leorvergani.escalaici.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.source.createFirebaseRawCacheStore
import br.com.leorvergani.escalaici.source.createFirebaseScheduleGateway
import br.com.leorvergani.escalaici.source.initializeFirebasePlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebasePlatform(this)
        setContent {
            EscalaIciLabApp(
                firebaseGateway = createFirebaseScheduleGateway(),
                firebaseCache = FirebaseSourceCache(createFirebaseRawCacheStore()),
                corporateAuthRepository = MsalCorporateAuthRepository(this),
                // Ainda nao existe uma fonte real (Firestore) de member_team_memberships
                // (FASE 14c-1, spec 59) -- o diretorio corporativo comeca vazio de proposito,
                // entao uma conta MSAL real hoje resolve para MEMBER_NOT_FOUND (estado valido
                // desta fase) ate a FASE 14c-2+ conectar dados reais.
                organizationIdentityResolver = DefaultOrganizationIdentityResolver(
                    corporateMemberDirectoryRepository = InMemoryMemberDirectoryRepository(members = emptyList()),
                    corporateMembershipRepository = InMemoryMembershipRepository(),
                    corporateMemberRepository = InMemoryMemberRepository(emptyList()),
                    corporateTeamRepository = InMemoryTeamRepository(emptyList())
                ),
                platformCapabilities = PlatformCapabilities(
                    supportsAppUpdate = true,
                    supportsCorporateAuth = true
                )
            )
        }
    }
}
