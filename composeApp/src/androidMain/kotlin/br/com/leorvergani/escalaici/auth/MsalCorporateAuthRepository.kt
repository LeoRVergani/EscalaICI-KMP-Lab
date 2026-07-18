package br.com.leorvergani.escalaici.auth

import android.content.Context
import android.util.Log
import br.com.leorvergani.escalaici.BuildConfig
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalArgumentException
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalDeclinedScopeException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class MsalCorporateAuthRepository(context: Context) : CorporateAuthRepository {
    private val appContext = context.applicationContext
    private var msalApplication: ISingleAccountPublicClientApplication? = null

    override val configurationState: CorporateAuthConfigurationState =
        if (BuildConfig.MSAL_CONFIGURED) {
            CorporateAuthConfigurationState.CONFIGURED
        } else {
            CorporateAuthConfigurationState.NOT_CONFIGURED
        }

    private val mutableState = MutableStateFlow(
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) {
            CorporateAuthState.NotConfigured
        } else {
            CorporateAuthState.SignedOut
        },
    )

    override val state: StateFlow<CorporateAuthState> = mutableState

    override suspend fun restoreSession() {
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) return

        val app = getOrCreateMsalApplication() ?: return
        val result = suspendCancellableCoroutine<CorporateAuthState> { continuation ->
            app.getCurrentAccountAsync(
                object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                    override fun onAccountLoaded(activeAccount: IAccount?) {
                        continuation.resumeIfActive(
                            activeAccount?.let {
                                CorporateAuthState.Authenticated(it.toCorporateIdentity())
                            } ?: CorporateAuthState.SignedOut,
                        )
                    }

                    override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                        continuation.resumeIfActive(
                            currentAccount?.let {
                                CorporateAuthState.Authenticated(it.toCorporateIdentity())
                            } ?: CorporateAuthState.SignedOut,
                        )
                    }

                    override fun onError(exception: MsalException) {
                        continuation.resumeIfActive(CorporateAuthState.Failed(exception.toCorporateAuthError()))
                    }
                },
            )
        }

        mutableState.value = result
    }

    override suspend fun signInInteractive(host: CorporateAuthHost?) {
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) return

        if (host !is AndroidCorporateAuthHost) {
            mutableState.value = CorporateAuthState.Failed(CorporateAuthError.InvalidConfiguration)
            return
        }

        mutableState.value = CorporateAuthState.Authenticating
        val app = getOrCreateMsalApplication() ?: return

        val result = suspendCancellableCoroutine<CorporateAuthState> { continuation ->
            val callback = object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    continuation.resumeIfActive(
                        CorporateAuthState.Authenticated(authenticationResult.account.toCorporateIdentity()),
                    )
                }

                override fun onError(exception: MsalException) {
                    continuation.resumeIfActive(CorporateAuthState.Failed(exception.toCorporateAuthError()))
                }

                override fun onCancel() {
                    continuation.resumeIfActive(CorporateAuthState.Failed(CorporateAuthError.Cancelled))
                }
            }

            val parameters = SignInParameters.builder()
                .withActivity(host.activity)
                .withScopes(listOf(USER_READ_SCOPE))
                .withCallback(callback)
                .build()

            app.signIn(parameters)
        }

        mutableState.value = result
    }

    override suspend fun signOut() {
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) return

        val app = getOrCreateMsalApplication() ?: run {
            mutableState.value = CorporateAuthState.SignedOut
            return
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            app.signOut(
                object : ISingleAccountPublicClientApplication.SignOutCallback {
                    override fun onSignOut() {
                        continuation.resumeIfActive(Unit)
                    }

                    override fun onError(exception: MsalException) {
                        continuation.resumeIfActive(Unit)
                    }
                },
            )
        }

        mutableState.value = CorporateAuthState.SignedOut
    }

    override fun enterDemoMode() {
        mutableState.value = CorporateAuthState.Demo
    }

    private suspend fun getOrCreateMsalApplication(): ISingleAccountPublicClientApplication? {
        msalApplication?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            val configFile = writeRuntimeConfigFile()
            PublicClientApplication.createSingleAccountPublicClientApplication(
                appContext,
                configFile,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        msalApplication = application
                        continuation.resumeIfActive(application)
                    }

                    override fun onError(exception: MsalException) {
                        if (BuildConfig.DEBUG) {
                            Log.w(
                                AUTH_LOG_TAG,
                                "MSAL init failed: ${exception.toDiagnosticCode()} " +
                                    "(exceptionClass=${exception::class.simpleName}, errorCode=${exception.errorCode})",
                            )
                        }
                        mutableState.value = CorporateAuthState.Failed(exception.toCorporateAuthError())
                        continuation.resumeIfActive(null)
                    }
                },
            )
        }
    }

    private fun writeRuntimeConfigFile(): File {
        val redirectUri = if (BuildConfig.DEBUG) {
            BuildConfig.MSAL_REDIRECT_URI_DEBUG
        } else {
            BuildConfig.MSAL_REDIRECT_URI_RELEASE
        }
        val config = """
            {
              "client_id": "${BuildConfig.MSAL_CLIENT_ID.jsonEscaped()}",
              "redirect_uri": "${redirectUri.jsonEscaped()}",
              "account_mode": "SINGLE",
              "broker_redirect_uri_registered": true,
              "authorities": [
                {
                  "type": "AAD",
                  "audience": {
                    "type": "AzureADMyOrg",
                    "tenant_id": "${BuildConfig.MSAL_TENANT_ID.jsonEscaped()}"
                  }
                }
              ]
            }
        """.trimIndent()

        return File(appContext.filesDir, MSAL_RUNTIME_CONFIG_FILE).apply {
            writeText(config)
        }
    }

    private companion object {
        const val AUTH_LOG_TAG = "EscalaICI-Auth"
        const val MSAL_RUNTIME_CONFIG_FILE = "msal_runtime_config.json"
        const val USER_READ_SCOPE = "User.Read"
    }
}

private fun MsalException.toCorporateAuthError(): CorporateAuthError =
    when (this) {
        is MsalUiRequiredException -> CorporateAuthError.InteractionRequired
        is MsalServiceException -> CorporateAuthError.TenantNotAllowed
        is MsalClientException -> {
            if (errorCode.contains("NO_NETWORK", ignoreCase = true) || cause is IOException) {
                CorporateAuthError.NetworkError
            } else {
                CorporateAuthError.InvalidConfiguration
            }
        }
        is MsalArgumentException -> CorporateAuthError.InvalidConfiguration
        is MsalDeclinedScopeException -> {
            CorporateAuthError.Unknown(detail = "Escopo recusado pelo usuario ou pela organizacao.")
        }
        else -> CorporateAuthError.Unknown(detail = message)
    }

private fun IAccount.toCorporateIdentity(): CorporateIdentity =
    CorporateIdentity(
        tenantId = tenantId ?: "",
        objectId = (claims?.get("oid") as? String) ?: id ?: "",
        username = username ?: "",
        displayName = (claims?.get("name") as? String) ?: username ?: "",
        email = (claims?.get("preferred_username") as? String) ?: username,
        accountId = id ?: "",
    )

private fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) {
        resume(value)
    }
}

private fun String.jsonEscaped(): String = buildString(length) {
    this@jsonEscaped.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (char < ' ') {
                    append("\\u")
                    append(char.code.toString(16).padStart(4, '0'))
                } else {
                    append(char)
                }
            }
        }
    }
}
