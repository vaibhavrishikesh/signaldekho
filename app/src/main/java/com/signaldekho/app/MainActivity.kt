package com.signaldekho.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.signaldekho.app.ui.AppNav
import com.signaldekho.app.ui.LocalAppContainer
import com.signaldekho.app.ui.PermissionGate
import com.signaldekho.app.ui.theme.SignalDekhoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SignalDekhoApp).container
        setContent {
            SignalDekhoTheme {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    CompositionLocalProvider(LocalAppContainer provides container) {
                        PermissionGate { AppNav() }
                    }
                }
            }
        }
    }
}
