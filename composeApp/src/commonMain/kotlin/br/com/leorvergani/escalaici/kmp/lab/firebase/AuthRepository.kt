package br.com.leorvergani.escalaici.kmp.lab.firebase

/** Usuario autenticado (sessao Firebase Auth valida) - login ja derivado do e-mail, ver [loginFromEmail]. */
data class AuthenticatedUser(val login: String, val email: String)

/**
 * Abstracao comum de autenticacao (prompt FASE 15 secao 7/8). Hoje so
 * email+senha (mesmo mecanismo do Escala-ICI atual); estruturada para
 * trocar/adicionar Microsoft/Entra depois sem reescrever o resto do app -
 * nenhum outro componente (`UsuarioRepository`, `CurrentScheduleResolver`,
 * `LoggedScheduleSyncCoordinator`) depende de como a sessao foi obtida, so
 * do resultado ([AuthenticatedUser] + [currentIdToken]).
 */
interface AuthRepository {
    suspend fun restoreSession(): AuthenticatedUser?
    suspend fun login(email: String, password: String): AuthenticatedUser
    suspend fun refreshSession(): AuthenticatedUser
    suspend fun logout()

    /** ID token atual (pode estar perto de expirar - [ensureFreshIdToken] forca renovacao). Nulo se nao houver sessao. */
    suspend fun currentIdToken(): String?

    /** Forca a renovacao do ID token (usado apos um 401/`PERMISSION_DENIED` de autenticacao expirada) e retorna o novo token. */
    suspend fun ensureFreshIdToken(): String
}

/**
 * Implementacao real sobre [IdentityToolkitAuthClient] (Identity
 * Toolkit/Secure Token REST) + [SessionTokenStore]. Nunca abre Firestore
 * anonimamente, nunca embute service account/secret (prompt FASE 15 secao
 * 43) - so a mesma API publica de cliente que o SDK JS do Escala-ICI usa.
 */
class FirebaseAuthRepository(
    private val authClient: IdentityToolkitAuthClient,
    private val tokenStore: SessionTokenStore,
) : AuthRepository {

    override suspend fun restoreSession(): AuthenticatedUser? {
        val stored = tokenStore.read() ?: return null
        return AuthenticatedUser(stored.login, stored.email)
    }

    override suspend fun login(email: String, password: String): AuthenticatedUser {
        val response = try {
            authClient.signInWithPassword(email, password)
        } catch (failure: IdentityToolkitException) {
            throw AuthErrorMapper.fromIdentityToolkit(failure)
        }
        val login = loginFromEmail(response.email.ifBlank { email })
        tokenStore.write(StoredAuthSession(login, response.email.ifBlank { email }, response.idToken, response.refreshToken))
        return AuthenticatedUser(login, response.email.ifBlank { email })
    }

    override suspend fun refreshSession(): AuthenticatedUser {
        ensureFreshIdToken()
        val stored = tokenStore.read() ?: throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, "Sessao nao encontrada.")
        return AuthenticatedUser(stored.login, stored.email)
    }

    override suspend fun logout() {
        tokenStore.clear()
    }

    override suspend fun currentIdToken(): String? = tokenStore.read()?.idToken

    override suspend fun ensureFreshIdToken(): String {
        val stored = tokenStore.read() ?: throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, "Sessao nao encontrada.")
        val response = try {
            authClient.refreshIdToken(stored.refreshToken)
        } catch (failure: IdentityToolkitException) {
            tokenStore.clear()
            throw AuthErrorMapper.fromIdentityToolkit(failure)
        }
        tokenStore.write(stored.copy(idToken = response.idToken, refreshToken = response.refreshToken))
        return response.idToken
    }
}

/** Traduz codigos do Identity Toolkit (`EMAIL_NOT_FOUND`, `INVALID_PASSWORD`, ...) para os erros tipados da FASE 15 - nunca uma mensagem generica. */
object AuthErrorMapper {
    fun fromIdentityToolkit(failure: IdentityToolkitException): EscalaIciException {
        val error = when {
            failure.errorCode.startsWith("EMAIL_NOT_FOUND") ||
                failure.errorCode.startsWith("INVALID_PASSWORD") ||
                failure.errorCode.startsWith("INVALID_LOGIN_CREDENTIALS") -> EscalaIciError.INVALID_CREDENTIALS
            failure.errorCode.startsWith("USER_DISABLED") -> EscalaIciError.USER_INACTIVE
            failure.errorCode.startsWith("TOKEN_EXPIRED") ||
                failure.errorCode.startsWith("INVALID_REFRESH_TOKEN") ||
                failure.errorCode.startsWith("USER_NOT_FOUND") -> EscalaIciError.AUTH_REQUIRED
            else -> EscalaIciError.UNKNOWN_ERROR
        }
        val message = when (error) {
            EscalaIciError.INVALID_CREDENTIALS -> "E-mail ou senha invalidos."
            EscalaIciError.USER_INACTIVE -> "Esta conta foi desativada."
            EscalaIciError.AUTH_REQUIRED -> "Sessao expirada. Entre novamente."
            else -> "Nao foi possivel entrar (${failure.errorCode})."
        }
        return EscalaIciException(error, message)
    }
}
