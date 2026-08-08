package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.io.File
import java.time.LocalDate
import java.util.Properties
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Testes de integracao REAIS - exercitam o mesmo `HttpClient(CIO)` +
 * `IdentityToolkitAuthClient` + `FirestoreRestClient` que Android e Web
 * usam em produção, sem nenhum mock/fake. So rodam quando explicitamente
 * pedido via variavel de ambiente (nunca no `testDebugUnitTest` padrao,
 * que nao depende de rede/Emulator/staging estarem disponiveis):
 *
 *   ESCALAICI_FIREBASE_EMULATOR=true ./gradlew :composeApp:testDebugUnitTest --tests "*FirebaseIntegrationTest*"
 *   ESCALAICI_FIREBASE_STAGING=true  ./gradlew :composeApp:testDebugUnitTest --tests "*FirebaseIntegrationTest*"
 *
 * O caminho do Emulator espera os dados semeados por
 * `scripts/seed-firebase-emulator.sh` (mesma equipe/usuario/escala do
 * `firebase/test/firestore.rules.test.mjs`). O caminho de staging le
 * `local.firebase.properties`/`local.firebase.test.properties`
 * (gitignored) via a system property `escalaici.repoRoot`.
 */
class FirebaseIntegrationTest {

    private fun repoRoot(): File {
        val path = System.getProperty("escalaici.repoRoot")
            ?: error("system property 'escalaici.repoRoot' nao configurada (ver composeApp/build.gradle.kts).")
        return File(path)
    }

    private fun loadProperties(fileName: String): Properties? {
        val file = File(repoRoot(), fileName)
        if (!file.exists()) return null
        return Properties().apply { file.inputStream().use { load(it) } }
    }

    @Test
    fun emulator_fullStack_login_usuario_escala_catalogo_mapper() = runBlocking {
        assumeTrue(
            "Defina ESCALAICI_FIREBASE_EMULATOR=true (com o Firebase Emulator rodando e semeado) para rodar este teste.",
            System.getenv("ESCALAICI_FIREBASE_EMULATOR") == "true",
        )

        val config = EscalaIciFirebaseConfig(
            environment = FirebaseEnvironment.LOCAL_EMULATOR,
            projectId = "", apiKey = "", authDomain = "", appId = "",
            storageBucket = "", messagingSenderId = "",
            emulatorProjectId = "demo-escalaici-kmp",
            emulatorAuthHost = "127.0.0.1", emulatorAuthPort = 9099,
            emulatorFirestoreHost = "127.0.0.1", emulatorFirestorePort = 8080,
        )

        val resultado = exercitarStackCompleto(
            config = config,
            email = "ana.silva@empresa.com",
            senha = "TesteEmulator123!",
            equipeIdEsperado = "EQ_TESTE",
        )

        assertEquals("ana.silva", resultado.login)
        assertEquals("EQ_TESTE", resultado.equipeId)
        assertEquals(TurnosMesStatus.PUBLICADA, resultado.status)
        assertTrue(resultado.diasNaEscala > 0, "esperava pelo menos 1 dia na escala semeada")
        assertTrue(resultado.catalogoTemCodigo, "catalogo tiposTurno deveria conter o codigo 'M'")
    }

    @Test
    fun staging_fullStack_employee_caioMonteiro() = runBlocking {
        val stagingConfigProps = loadProperties("local.firebase.properties")
        val testProps = loadProperties("local.firebase.test.properties")
        assumeTrue(
            "local.firebase.properties e/ou local.firebase.test.properties nao encontrados na raiz do repo - " +
                "sem eles nao ha como testar contra staging real.",
            stagingConfigProps != null && testProps != null,
        )
        assumeTrue(
            "Defina ESCALAICI_FIREBASE_STAGING=true para testar contra o Firebase de staging real.",
            System.getenv("ESCALAICI_FIREBASE_STAGING") == "true",
        )

        val config = configFromProperties(stagingConfigProps!!)
        val email = testProps!!.getProperty("test.staging.employeeEmail")
            ?: fail("local.firebase.test.properties nao tem 'test.staging.employeeEmail'.")
        val senha = testProps.getProperty("test.staging.password")
            ?: fail("local.firebase.test.properties nao tem 'test.staging.password'.")

        val resultado = exercitarStackCompleto(config = config, email = email, senha = senha, equipeIdEsperado = null)

        assertEquals(email.substringBefore("@"), resultado.login)
        assertTrue(resultado.equipeId.isNotBlank(), "usuarios/${resultado.login} deveria ter equipeId")
    }

    @Test
    fun staging_fullStack_manager_marinaAzevedo_identidadeEquipe() = runBlocking {
        val stagingConfigProps = loadProperties("local.firebase.properties")
        val testProps = loadProperties("local.firebase.test.properties")
        assumeTrue(
            "local.firebase.properties e/ou local.firebase.test.properties nao encontrados na raiz do repo.",
            stagingConfigProps != null && testProps != null,
        )
        assumeTrue(
            "Defina ESCALAICI_FIREBASE_STAGING=true para testar contra o Firebase de staging real.",
            System.getenv("ESCALAICI_FIREBASE_STAGING") == "true",
        )

        val config = configFromProperties(stagingConfigProps!!)
        val email = testProps!!.getProperty("test.staging.managerEmail")
            ?: fail("local.firebase.test.properties nao tem 'test.staging.managerEmail'.")
        val senha = testProps.getProperty("test.staging.password")
            ?: fail("local.firebase.test.properties nao tem 'test.staging.password'.")

        // So identidade/equipe/Rules aqui - nao exige escala publicada para o gestor.
        val httpClient = HttpClient(CIO)
        val authClient = IdentityToolkitAuthClient(httpClient, config)
        val firestoreClient = FirestoreRestClient(httpClient, config)
        val login = loginFromEmail(email)

        val tokenResponse = authClient.signInWithPassword(email, senha)
        val usuario = UsuarioRepository(firestoreClient).resolveUsuario(tokenResponse.idToken, login)

        assertEquals(login, usuario.login)
        assertTrue(usuario.equipeId.isNotBlank(), "usuarios/$login deveria ter equipeId")
    }

    private fun configFromProperties(properties: Properties): EscalaIciFirebaseConfig = EscalaIciFirebaseConfig(
        environment = FirebaseEnvironment.STAGING,
        projectId = properties.getProperty("firebase.projectId", ""),
        apiKey = properties.getProperty("firebase.apiKey", ""),
        authDomain = properties.getProperty("firebase.authDomain", ""),
        appId = properties.getProperty("firebase.appId", ""),
        storageBucket = properties.getProperty("firebase.storageBucket", ""),
        messagingSenderId = properties.getProperty("firebase.messagingSenderId", ""),
        emulatorProjectId = "", emulatorAuthHost = "127.0.0.1", emulatorAuthPort = 9099,
        emulatorFirestoreHost = "127.0.0.1", emulatorFirestorePort = 8080,
    )

    private data class ResultadoStack(
        val login: String,
        val equipeId: String,
        val status: TurnosMesStatus,
        val diasNaEscala: Int,
        val catalogoTemCodigo: Boolean,
    )

    /**
     * Percorre o caminho real, sem mocks: IdentityToolkitAuthClient (login)
     * -> UsuarioRepository (usuarios/{login}) -> CurrentScheduleResolver
     * (turnosMes PUBLICADA vigente) -> TiposTurnoRepository (tiposTurno) ->
     * EscalaIciScheduleMapper (ScheduleSummary). Nao mascara nenhuma
     * falha - qualquer documento faltante propaga a mensagem exata do
     * `EscalaIciException` (ex.: "Seu login nao esta cadastrado...",
     * "Nenhuma escala publicada..."), nunca um fallback silencioso.
     */
    private suspend fun exercitarStackCompleto(
        config: EscalaIciFirebaseConfig,
        email: String,
        senha: String,
        equipeIdEsperado: String?,
    ): ResultadoStack {
        val httpClient = HttpClient(CIO)
        val authClient = IdentityToolkitAuthClient(httpClient, config)
        val firestoreClient = FirestoreRestClient(httpClient, config)
        val usuarioRepository = UsuarioRepository(firestoreClient)
        val tiposTurnoRepository = TiposTurnoRepository(firestoreClient)
        val scheduleResolver = FirestoreCurrentScheduleResolver(firestoreClient)

        val login = loginFromEmail(email)
        val tokenResponse = try {
            authClient.signInWithPassword(email, senha)
        } catch (e: IdentityToolkitException) {
            fail("Login falhou para $email (${e.errorCode}): usuario de teste nao existe no Auth Emulator/staging ou senha incorreta em local.firebase.test.properties.")
        }
        assertEquals(login, loginFromEmail(tokenResponse.email.ifBlank { email }))

        val usuario = try {
            usuarioRepository.resolveUsuario(tokenResponse.idToken, login)
        } catch (e: EscalaIciException) {
            fail("Documento faltando: usuarios/$login (${e.error}) - ${e.message}")
        }
        if (equipeIdEsperado != null) {
            assertEquals(equipeIdEsperado, usuario.equipeId)
        }

        val hojeIso = LocalDate.now().toString()
        val turnosMes = try {
            scheduleResolver.resolve(tokenResponse.idToken, login, usuario.equipeId, hojeIso)
        } catch (e: EscalaIciException) {
            fail("Nenhuma turnosMes PUBLICADA vigente para $login/${usuario.equipeId} em $hojeIso (${e.error}) - ${e.message}")
        }

        val catalogo = tiposTurnoRepository.catalogoPara(tokenResponse.idToken, usuario.equipeId)
        val summary = EscalaIciScheduleMapper.map(turnosMes, usuario, catalogo)

        return ResultadoStack(
            login = login,
            equipeId = usuario.equipeId,
            status = turnosMes.status,
            diasNaEscala = summary.days.size,
            catalogoTemCodigo = catalogo.containsKey(turnosMes.turnoPadrao) || catalogo.values.isNotEmpty(),
        )
    }
}
