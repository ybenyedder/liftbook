package com.hevyclone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hevyclone.app.data.Repo
import com.hevyclone.app.ui.App
import com.hevyclone.app.ui.HevyTheme

class MainActivity : ComponentActivity() {
    private val bootKey = androidx.compose.runtime.mutableIntStateOf(0)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("start_empty_workout", false)) {
            com.hevyclone.app.ui.Nav.pendingStartEmpty = true
            bootKey.intValue += 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.hevyclone.app.ui.RestTimer.restore(applicationContext)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        Repo.init(applicationContext)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra("start_empty_workout", false) == true && savedInstanceState == null) {
            com.hevyclone.app.ui.Nav.pendingStartEmpty = true
        }
        setContent {
            HevyTheme(accent = Repo.settings.accent) {
                App(refreshKey = bootKey.intValue)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        com.hevyclone.app.data.Repo.persistDraftNow()
    }
}
