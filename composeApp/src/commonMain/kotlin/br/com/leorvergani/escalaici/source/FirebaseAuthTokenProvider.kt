package br.com.leorvergani.escalaici.source

interface FirebaseAuthTokenProvider {
    suspend fun idToken(): FirebaseAuthTokenResult
    fun invalidate()
}

sealed interface FirebaseAuthTokenResult {
    data class Success(val idToken: String) : FirebaseAuthTokenResult
    data class Failure(
        val cause: ScheduleSyncCause,
        val message: String,
        val throwable: Throwable? = null
    ) : FirebaseAuthTokenResult
}
