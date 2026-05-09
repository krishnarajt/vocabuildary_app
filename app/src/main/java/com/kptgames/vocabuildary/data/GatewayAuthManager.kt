package com.kptgames.vocabuildary.data

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.kptgames.vocabuildary.BuildConfig

class GatewayAuthManager(
    private val preferences: AppPreferences
) {
    @Volatile private var token: String? = null
    private val authRequired: Boolean
        get() = !BuildConfig.VOCABUILDARY_AUTH_MODE.equals("local", ignoreCase = true)

    fun isAuthRequired(): Boolean = authRequired

    suspend fun load() {
        token = preferences.getGatewayAuthToken()
    }

    fun isAuthorized(): Boolean = !authRequired || !token.isNullOrBlank()

    suspend fun startLogin(activity: Activity) {
        if (!authRequired) return
        val loginUri = mobileAuthUri(preferences.getOrCreateDeviceId())
        activity.startActivity(Intent(Intent.ACTION_VIEW, loginUri))
    }

    suspend fun handleAuthorizationIntent(intent: Intent?): Result<Unit>? {
        if (!authRequired) return Result.success(Unit)
        val data = intent?.data ?: return null
        if (!isCallbackUri(data)) return null

        val returnedToken = data.getQueryParameter("token")
        if (returnedToken.isNullOrBlank()) {
            val error = data.getQueryParameter("error") ?: "Missing mobile auth token"
            return Result.failure(IllegalStateException(error))
        }

        token = returnedToken
        preferences.saveGatewayAuthToken(returnedToken)
        return Result.success(Unit)
    }

    suspend fun accessToken(): String? {
        if (!authRequired) return null
        if (token.isNullOrBlank()) load()
        return token
    }

    suspend fun logout() {
        token = null
        preferences.clearGatewayAuthToken()
    }

    private fun mobileAuthUri(deviceId: String): Uri {
        val base = Uri.parse(BuildConfig.VOCABUILDARY_API_BASE_URL.ensureTrailingSlash())
        return base.buildUpon()
            .appendEncodedPath(BuildConfig.VOCABUILDARY_MOBILE_AUTH_PATH.trim('/'))
            .appendQueryParameter("redirect_uri", BuildConfig.VOCABUILDARY_MOBILE_REDIRECT_URI)
            .appendQueryParameter("device_id", deviceId)
            .appendQueryParameter("label", android.os.Build.MODEL.ifBlank { "Android" })
            .build()
    }

    private fun isCallbackUri(uri: Uri): Boolean {
        val expected = Uri.parse(BuildConfig.VOCABUILDARY_MOBILE_REDIRECT_URI)
        if (uri.scheme != expected.scheme) return false
        val expectedHost = expected.host
        return expectedHost.isNullOrBlank() || uri.host == expectedHost
    }
}

private fun String.ensureTrailingSlash(): String {
    return if (endsWith("/")) this else "$this/"
}
