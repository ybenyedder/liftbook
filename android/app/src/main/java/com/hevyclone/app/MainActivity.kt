package com.hevyclone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hevyclone.app.data.Cloud
import com.hevyclone.app.data.Repo
import com.hevyclone.app.ui.App
import com.hevyclone.app.ui.AuthScreen
import com.hevyclone.app.ui.HevyTheme

class MainActivity : ComponentActivity() {
    private val bootKey = androidx.compose.runtime.mutableIntStateOf(0)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (com.hevyclone.app.data.Cloud.handleAuthRedirect(intent.data)) return
        if (intent.getBooleanExtra("start_empty_workout", false)) {
            com.hevyclone.app.ui.Nav.pendingStartEmpty = true
            bootKey.intValue += 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.hevyclone.app.ui.RestTimer.restore(applicationContext)
        // Ask for POST_NOTIFICATIONS once, not on every launch (system re-prompt was spammy).
        val notifPrefs = getSharedPreferences("boot", MODE_PRIVATE)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            !notifPrefs.getBoolean("notifAsked", false) &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifPrefs.edit().putBoolean("notifAsked", true).apply()
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        Repo.init(applicationContext)
        Cloud.init(applicationContext)
        enableEdgeToEdge()
        if (savedInstanceState == null && Cloud.handleAuthRedirect(intent?.data)) {
            // Google OAuth deep link on cold start — session applied, fall through to UI
        } else if (intent?.getBooleanExtra("start_empty_workout", false) == true && savedInstanceState == null) {
            com.hevyclone.app.ui.Nav.pendingStartEmpty = true
        }
        setContent {
            HevyTheme(accent = Repo.settings.accent) {
                if (Cloud.session == null && !Cloud.skipped) AuthScreen()
                else App(refreshKey = bootKey.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // back from the browser without completing Google sign-in → drop the pending spinner
        Cloud.clearGooglePendingIfStale()
    }

    override fun onStart() {
        super.onStart()
        // pull changes made on another device while the app was closed
        Cloud.requestSync(debounceMs = 400)
    }

    override fun onStop() {
        super.onStop()
        com.hevyclone.app.data.Repo.persistDraftNow()
    }
}
