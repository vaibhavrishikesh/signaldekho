package com.signaldekho.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.signaldekho.app.R
import com.signaldekho.app.ui.theme.SignalDekhoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SignalDekhoTheme { Text(stringResource(R.string.app_name)) } }
    }
}
