package com.kptgames.vocabuildary

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kptgames.vocabuildary.data.ApiFactory
import com.kptgames.vocabuildary.data.AppPreferences
import com.kptgames.vocabuildary.data.OidcAuthManager
import com.kptgames.vocabuildary.data.VocabuildaryRepository
import com.kptgames.vocabuildary.notifications.NativeNotificationHelper
import com.kptgames.vocabuildary.ui.VocabuildaryRoot
import com.kptgames.vocabuildary.ui.VocabuildaryViewModel
import com.kptgames.vocabuildary.ui.VocabuildaryViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var authManager: OidcAuthManager
    private var appViewModel: VocabuildaryViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NativeNotificationHelper.ensureChannel(this)

        val preferences = AppPreferences(applicationContext)
        authManager = OidcAuthManager(applicationContext, preferences)
        val (api, client) = ApiFactory.create(authManager)
        val repository = VocabuildaryRepository(api, client, preferences)

        setContent {
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}
            val viewModel: VocabuildaryViewModel = viewModel(
                factory = VocabuildaryViewModelFactory(
                    application,
                    authManager,
                    repository
                )
            )
            appViewModel = viewModel

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                handleAuthIntent(intent)
            }

            VocabuildaryRoot(
                viewModel = viewModel,
                onLogin = {
                    lifecycleScope.launch {
                        authManager.startLogin(this@MainActivity)
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lifecycleScope.launch {
            handleAuthIntent(intent)
        }
    }

    private suspend fun handleAuthIntent(intent: Intent?) {
        if (intent?.action != ACTION_AUTH_COMPLETE) return
        val result = authManager.handleAuthorizationIntent(intent)
        appViewModel?.handleAuthResult(result)
        setIntent(Intent(this, MainActivity::class.java))
    }

    companion object {
        const val ACTION_AUTH_COMPLETE = "com.kptgames.vocabuildary.AUTH_COMPLETE"
        const val ACTION_AUTH_CANCELLED = "com.kptgames.vocabuildary.AUTH_CANCELLED"
    }
}
