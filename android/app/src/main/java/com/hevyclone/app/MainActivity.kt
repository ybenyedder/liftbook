package com.hevyclone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hevyclone.app.data.Repo
import com.hevyclone.app.ui.App
import com.hevyclone.app.ui.HevyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.hevyclone.app.ui.RestTimer.appContext = applicationContext
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        Repo.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            HevyTheme {
                App()
            }
        }
    }
}
