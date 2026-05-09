package com.kptgames.vocabuildary.data

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kptgames.vocabuildary.BuildConfig
import com.kptgames.vocabuildary.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import kotlin.coroutines.resume

class OidcAuthManager(
    private val context: Context,
    private val preferences: AppPreferences
) {
    private val authService = AuthorizationService(context)
    @Volatile private var authState: AuthState = AuthState()

    suspend fun load() {
        val json = preferences.getAuthStateJson()
        authState = if (json.isNullOrBlank()) AuthState() else AuthState.jsonDeserialize(json)
    }

    fun isAuthorized(): Boolean = authState.isAuthorized

    suspend fun startLogin(activity: Activity) {
        val serviceConfig = fetchServiceConfig()
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            BuildConfig.OIDC_CLIENT_ID,
            "code",
            Uri.parse(BuildConfig.OIDC_REDIRECT_URI)
        )
            .setScopes(BuildConfig.OIDC_SCOPES.split(" ").filter { it.isNotBlank() })
            .build()

        val completionIntent = Intent(activity, MainActivity::class.java).apply {
            action = MainActivity.ACTION_AUTH_COMPLETE
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val cancelIntent = Intent(activity, MainActivity::class.java).apply {
            action = MainActivity.ACTION_AUTH_CANCELLED
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        authService.performAuthorizationRequest(
            request,
            PendingIntent.getActivity(activity, 0, completionIntent, flags),
            PendingIntent.getActivity(activity, 1, cancelIntent, flags)
        )
    }

    suspend fun handleAuthorizationIntent(intent: Intent?): Result<Unit> {
        val response = AuthorizationResponse.fromIntent(intent ?: return Result.failure(
            IllegalArgumentException("Missing authorization intent")
        ))
        val exception = AuthorizationException.fromIntent(intent)
        if (response == null) {
            return Result.failure(exception ?: IllegalStateException("Authorization was cancelled"))
        }

        val nextState = AuthState(response, exception)
        authState = nextState
        val result = suspendCancellableCoroutine { continuation ->
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
                nextState.update(tokenResponse, tokenException)
                authState = nextState
                if (tokenResponse != null) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(
                        Result.failure(tokenException ?: IllegalStateException("Token exchange failed"))
                    )
                }
            }
        }
        if (result.isSuccess) save()
        return result
    }

    suspend fun freshAccessToken(): String? {
        if (!authState.isAuthorized) load()
        if (!authState.isAuthorized) return null

        val token = suspendCancellableCoroutine<String?> { continuation ->
            authState.performActionWithFreshTokens(authService) { accessToken, _, exception ->
                if (exception != null) {
                    continuation.resume(null)
                } else {
                    continuation.resume(accessToken)
                }
            }
        }
        save()
        return token
    }

    suspend fun logout() {
        authState = AuthState()
        preferences.clearAuthState()
    }

    private suspend fun save() {
        preferences.saveAuthStateJson(authState.jsonSerializeString())
    }

    private suspend fun fetchServiceConfig(): AuthorizationServiceConfiguration {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(BuildConfig.OIDC_ISSUER)) { config, ex ->
                    if (config != null) {
                        continuation.resume(config)
                    } else {
                        continuation.cancel(ex ?: IllegalStateException("OIDC discovery failed"))
                    }
                }
            }
        }
    }
}
