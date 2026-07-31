package com.signaldekho.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
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
                CompositionLocalProvider(LocalAppContainer provides container) {
                    PermissionGate { AppNav() }
                }
            }
        }
    }
}
