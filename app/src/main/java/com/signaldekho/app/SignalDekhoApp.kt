package com.signaldekho.app

import android.app.Application
import android.content.Context
import com.signaldekho.app.data.BleRepo
import com.signaldekho.app.data.CellularRepo
import com.signaldekho.app.data.WifiRepo
import com.signaldekho.app.data.survey.SurveyDb
import com.signaldekho.app.domain.WifiThrottleScheduler

class AppContainer(context: Context) {
    val cellularRepo = CellularRepo(context)
    val wifiRepo = WifiRepo(context, WifiThrottleScheduler { System.currentTimeMillis() })
    val bleRepo = BleRepo(context)
    private val db = SurveyDb.build(context)
    val surveyDao = db.surveyDao()
}

class SignalDekhoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
