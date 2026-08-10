package br.com.leorvergani.escalaici.kmp.lab.firebase

/** `LoggedScheduleSyncCoordinator` (Hoje/Escala/Perfil) + `TrocasSession` (FASE 16) - montados juntos porque compartilham o mesmo `HttpClient`/`FirestoreRestClient`/repositorios de usuario e catalogo. */
data class EscalaIciSession(
    val syncCoordinator: LoggedScheduleSyncCoordinator,
    val trocasSession: TrocasSession,
)

/**
 * Monta a sessao completa - chamado uma vez por `App.kt`. `SessionTokenStore`/
 * `RawKeyValueStore` (via `createSessionTokenStore()`/`createRawKeyValueStore()`)
 * e o engine HTTP (via `createPlatformHttpClient()`, FASE 17B.1 - CIO no
 * Android, `Js`/fetch no Wasm) sao expect/actual; tudo o resto
 * (`IdentityToolkitAuthClient`, `FirestoreRestClient`, repositorios) e comum.
 */
fun createEscalaIciSession(todayIsoProvider: () -> String): EscalaIciSession {
    val config = currentFirebaseConfig()
    val httpClient = createPlatformHttpClient()
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
