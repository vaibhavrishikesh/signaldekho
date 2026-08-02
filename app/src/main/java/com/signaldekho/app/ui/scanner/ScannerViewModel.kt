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

data class NearbyNetwork(val ssid: String, val rssi: Int, val band: String, val count: Int)

data class ScannerState(
    val cells: List<CellReading> = emptyList(),
    val connectedSsid: String? = null,
    val connectedRssi: Int? = null,
    val nearby: List<NearbyNetwork> = emptyList(),
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

    private fun dedupe(readings: List<WifiReading>, connectedSsid: String?): List<NearbyNetwork> =
        readings
            .filter { it.ssid != connectedSsid }
            .groupBy { it.ssid }
            .map { (ssid, group) ->
                val strongest = group.maxBy { it.rssi }
                NearbyNetwork(
                    ssid = ssid,
                    rssi = strongest.rssi,
                    band = strongest.band.name.removePrefix("GHZ_").replace('_', '.'),
                    count = group.size,
                )
            }
            .sortedByDescending { it.rssi }

    fun start() {
        refresh()
        bleRepo.startScan { reading ->
            bleSeen[reading.address] = reading
            _state.update { it.copy(ble = bleSeen.values.sortedByDescending { b -> b.rssi }) }
        }
        tickJob?.cancel()
        tickJob = viewModelScope.launch {           // 1s tick: cellular + wifi + countdown
            while (true) {
                val wifi = wifiRepo.latestResults()
                val connected = wifiRepo.connectedSsidAndRssi()
                _state.update {
                    it.copy(
                        cells = cellularRepo.read(),
                        connectedSsid = connected?.first,
                        connectedRssi = connected?.second,
                        nearby = dedupe(wifi, connected?.first),
                        secondsToNextScan = currentSecondsToNextScan(),
                    )
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
        val wifi = wifiRepo.latestResults()
        val connected = wifiRepo.connectedSsidAndRssi()
        _state.update {
            it.copy(
                connectedSsid = connected?.first,
                connectedRssi = connected?.second,
                nearby = dedupe(wifi, connected?.first),
                cells = cellularRepo.read(),
                secondsToNextScan = currentSecondsToNextScan(),
            )
        }
    }

    override fun onCleared() = stop()
}
