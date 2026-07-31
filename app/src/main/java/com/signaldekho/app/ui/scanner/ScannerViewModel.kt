package com.signaldekho.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaldekho.app.data.BleReading
import com.signaldekho.app.data.BleRepo
import com.signaldekho.app.data.CellReading
import com.signaldekho.app.data.CellularRepo
import com.signaldekho.app.data.WifiReading
import com.signaldekho.app.data.WifiRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerState(
    val cells: List<CellReading> = emptyList(),
    val wifi: List<WifiReading> = emptyList(),
    val connectedSsid: String? = null,
    val ble: List<BleReading> = emptyList(),
    val secondsToNextScan: Int = 0,
)

class ScannerViewModel(
    private val cellularRepo: CellularRepo,
    private val wifiRepo: WifiRepo,
    private val bleRepo: BleRepo,
) : ViewModel() {
    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()
    private val bleSeen = LinkedHashMap<String, BleReading>() // keyed by address
    private var tickJob: Job? = null

    private fun currentSecondsToNextScan(): Int {
        val next = wifiRepo.scheduler.nextAllowedAtMillis()
        return (((next - System.currentTimeMillis()).coerceAtLeast(0) + 999) / 1000).toInt()
    }

    fun start() {
        refresh()
        bleRepo.startScan { reading ->
            bleSeen[reading.address] = reading
            _state.update { it.copy(ble = bleSeen.values.sortedByDescending { b -> b.rssi }) }
        }
        tickJob?.cancel()
        tickJob = viewModelScope.launch {           // 1s tick: cellular + countdown
            while (true) {
                _state.update {
                    it.copy(cells = cellularRepo.read(), secondsToNextScan = currentSecondsToNextScan())
                }
                delay(1000)
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        bleRepo.stopScan()
    }

    fun refresh() {
        wifiRepo.requestScan()
        _state.update {
            it.copy(
                wifi = wifiRepo.latestResults(),
                connectedSsid = wifiRepo.connectedSsidAndRssi()?.first,
                cells = cellularRepo.read(),
                secondsToNextScan = currentSecondsToNextScan(),
            )
        }
    }

    override fun onCleared() = stop()
}
