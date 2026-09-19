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
        Repo.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            HevyTheme(theme = Repo.settings.theme) {
                App()
            }
        }
    }
}
