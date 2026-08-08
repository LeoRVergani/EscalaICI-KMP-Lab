package br.com.leorvergani.escalaici.kmp.lab.firebase

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/** `LoggedScheduleSyncCoordinator` (Hoje/Escala/Perfil) + `TrocasSession` (FASE 16) - montados juntos porque compartilham o mesmo `HttpClient`/`FirestoreRestClient`/repositorios de usuario e catalogo. */
data class EscalaIciSession(
    val syncCoordinator: LoggedScheduleSyncCoordinator,
    val trocasSession: TrocasSession,
)

/**
 * Monta a sessao completa (mesmo `HttpClient(CIO)` ja usado pelo resto do
 * app - engine unico, sem expect/actual, valido em Android e Wasm) -
 * chamado uma vez por `App.kt`. So o `SessionTokenStore`/`RawKeyValueStore`
 * (via `createSessionTokenStore()`/`createRawKeyValueStore()`) sao
 * expect/actual; tudo o resto e comum.
 */
fun createEscalaIciSession(todayIsoProvider: () -> String): EscalaIciSession {
    val config = currentFirebaseConfig()
    val httpClient = HttpClient(CIO)
    val authClient = IdentityToolkitAuthClient(httpClient, config)
    val firestoreClient = FirestoreRestClient(httpClient, config)
    val authRepository = FirebaseAuthRepository(authClient, createSessionTokenStore())
    val usuarioRepository = UsuarioRepository(firestoreClient)
    val tiposTurnoRepository = TiposTurnoRepository(firestoreClient)

    val syncCoordinator = LoggedScheduleSyncCoordinator(
        authRepository = authRepository,
        usuarioRepository = usuarioRepository,
        tiposTurnoRepository = tiposTurnoRepository,
        scheduleResolver = FirestoreCurrentScheduleResolver(firestoreClient),
        cache = EscalaIciScheduleCache(createRawKeyValueStore()),
        todayIsoProvider = todayIsoProvider,
    )
    val trocasSession = TrocasSession(
        syncCoordinator = syncCoordinator,
        trocasRepository = TrocasEscalaRepository(firestoreClient),
        teamScheduleRepository = TeamScheduleRepository(firestoreClient, usuarioRepository, tiposTurnoRepository),
    )
    return EscalaIciSession(syncCoordinator, trocasSession)
}
