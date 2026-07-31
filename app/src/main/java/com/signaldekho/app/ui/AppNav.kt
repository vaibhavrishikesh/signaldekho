package com.signaldekho.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.signaldekho.app.AppContainer
import com.signaldekho.app.ui.gharscan.GharScanScreen
import com.signaldekho.app.ui.report.ReportScreen
import com.signaldekho.app.ui.scanner.ScannerScreen

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "scanner") {
        composable("scanner") {
            ScannerScreen(onStartGharScan = { nav.navigate("gharscan") })
        }
        composable("gharscan") {
            GharScanScreen(onFinished = { surveyId ->
                nav.navigate("report/$surveyId") { popUpTo("scanner") }
            })
        }
        composable(
            "report/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.LongType }),
        ) { entry ->
            ReportScreen(surveyId = entry.arguments!!.getLong("surveyId"))
        }
    }
}
