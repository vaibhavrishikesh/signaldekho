package com.signaldekho.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaldekho.app.data.survey.SurveyDao
import com.signaldekho.app.domain.Finding
import com.signaldekho.app.domain.RecommendationEngine
import com.signaldekho.app.domain.RoomResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReportRow(val room: String, val wifiRssi: Int?, val cellDbm: Int?)
data class ReportState(val rows: List<ReportRow> = emptyList(), val findings: List<Finding> = emptyList())

class ReportViewModel(dao: SurveyDao, surveyId: Long) : ViewModel() {
    val state: StateFlow<ReportState> = dao.readingsFor(surveyId)
        .map { readings ->
            val rows = readings.map { r ->
                // strongest SIM reading represents the room
                val cell = listOfNotNull(r.cellDbmSim1, r.cellDbmSim2).maxOrNull()
                ReportRow(r.roomName, r.wifiRssi, cell)
            }
            val findings = RecommendationEngine.analyze(
                rows.map { RoomResult(it.room, it.wifiRssi, it.cellDbm) }
            )
            ReportState(rows, findings)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReportState())
}
