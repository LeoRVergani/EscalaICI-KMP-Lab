package br.com.leorvergani.escalaici.kmp.lab.firebase

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/**
 * Monta o `LoggedScheduleSyncCoordinator` completo (mesmo `HttpClient(CIO)`
 * ja usado pelo resto do app - engine unico, sem expect/actual, valido em
 * Android e Wasm) - chamado igual pelos dois targets em `MainActivity.kt`/
 * `Main.kt`. So o `SessionTokenStore`/`RawKeyValueStore` (via
 * `createSessionTokenStore()`/`createRawKeyValueStore()`) sao expect/actual;
 * tudo o resto e comum.
 */
fun createLoggedScheduleSyncCoordinator(todayIsoProvider: () -> String): LoggedScheduleSyncCoordinator {
    val config = currentFirebaseConfig()
    val httpClient = HttpClient(CIO)
    val authClient = IdentityToolkitAuthClient(httpClient, config)
    val firestoreClient = FirestoreRestClient(httpClient, config)
    val authRepository = FirebaseAuthRepository(authClient, createSessionTokenStore())
    return LoggedScheduleSyncCoordinator(
        authRepository = authRepository,
        usuarioRepository = UsuarioRepository(firestoreClient),
        tiposTurnoRepository = TiposTurnoRepository(firestoreClient),
        scheduleResolver = FirestoreCurrentScheduleResolver(firestoreClient),
        cache = EscalaIciScheduleCache(createRawKeyValueStore()),
        todayIsoProvider = todayIsoProvider,
    )
}
