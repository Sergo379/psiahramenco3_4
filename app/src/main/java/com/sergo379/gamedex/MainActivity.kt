package com.sergo379.gamedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sergo379.gamedex.ui.GameDexApp
import com.sergo379.gamedex.ui.theme.GameDexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val graph = (application as GameDexApplication).graph
        setContent { GameDexTheme { GameDexApp(graph) } }
    }
}

