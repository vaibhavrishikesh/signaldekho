package com.signaldekho.app.ui.gharscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaldekho.app.data.CellularRepo
import com.signaldekho.app.data.WifiRepo
import com.signaldekho.app.data.survey.RoomReadingEntity
import com.signaldekho.app.data.survey.Survey
import com.signaldekho.app.data.survey.SurveyDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GharScanState(
    val measuredRooms: List<String> = emptyList(),
    val roomInput: String = "",
    val saving: Boolean = false,
)

class GharScanViewModel(
    private val wifiRepo: WifiRepo,
    private val cellularRepo: CellularRepo,
    private val dao: SurveyDao,
) : ViewModel() {
    private val _state = MutableStateFlow(GharScanState())
    val state: StateFlow<GharScanState> = _state.asStateFlow()
    private var surveyId: Long? = null

    fun setRoomInput(name: String) = _state.update { it.copy(roomInput = name) }

    fun measureCurrentRoom() {
        val room = _state.value.roomInput.trim()
        if (room.isEmpty() || _state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = surveyId ?: dao.insertSurvey(Survey(createdAt = System.currentTimeMillis()))
                .also { surveyId = it }
            val wifi = wifiRepo.connectedSsidAndRssi()
            val cells = cellularRepo.read()
            dao.insertReading(
                RoomReadingEntity(
                    surveyId = id,
                    roomName = room,
                    wifiSsid = wifi?.first,
                    wifiRssi = wifi?.second,
                    cellDbmSim1 = cells.getOrNull(0)?.dbm,
                    cellDbmSim2 = cells.getOrNull(1)?.dbm,
                    takenAt = System.currentTimeMillis(),
                )
            )
            _state.update {
                it.copy(measuredRooms = it.measuredRooms + room, roomInput = "", saving = false)
            }
        }
    }

    fun finish(onFinished: (Long) -> Unit) {
        surveyId?.let(onFinished)
    }
}
