package com.signaldekho.app.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signaldekho.app.R
import com.signaldekho.app.domain.Grade
import com.signaldekho.app.domain.SignalGrade
import com.signaldekho.app.ui.LocalAppContainer
import com.signaldekho.app.ui.theme.GradeGood
import com.signaldekho.app.ui.theme.GradeWeak

fun gradeColor(g: Grade): Color = when (g) {
    Grade.EXCELLENT -> Color(0xFF1B5E20)
    Grade.GOOD -> GradeGood
    Grade.WEAK -> GradeWeak
    Grade.VERY_WEAK -> Color(0xFF8B0000)
}

@Composable
private fun GradeDot(g: Grade) {
    Box(Modifier.size(12.dp).background(gradeColor(g), CircleShape))
}

@Composable
fun ScannerScreen(onStartGharScan: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: ScannerViewModel = viewModel {
        ScannerViewModel(container.cellularRepo, container.wifiRepo, container.bleRepo)
    }
    val state by vm.state.collectAsState()
    LifecycleStartEffect(Unit) {
        vm.start()
        onStopOrDispose { vm.stop() }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Button(onClick = onStartGharScan) { Text(stringResource(R.string.scanner_home_scan_cta)) }
            }
        }

        item { Text(stringResource(R.string.scanner_sim_header), style = MaterialTheme.typography.titleMedium) }
        if (state.cells.isEmpty()) {
            item { Text(stringResource(R.string.scanner_no_sim)) }
        } else {
            items(state.cells) { cell ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cell.dbm?.let { GradeDot(SignalGrade.cell(it)) }
                    Text("${cell.operatorName} ${cell.networkType}")
                    Text(cell.dbm?.let { stringResource(R.string.scanner_dbm, it) } ?: "—",
                        style = MaterialTheme.typography.bodyMedium)
                    if (cell.ageMillis > 10_000) {
                        Text(stringResource(R.string.scanner_stale_minutes, cell.ageMillis / 60_000),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { HorizontalDivider() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.scanner_nearby_header, state.wifi.size), style = MaterialTheme.typography.titleMedium)
                if (state.secondsToNextScan > 0) {
                    Text(stringResource(R.string.scanner_next_scan_in, state.secondsToNextScan),
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedButton(onClick = { vm.refresh() }) { Text(stringResource(R.string.scanner_refresh)) }
                }
            }
        }
        if (state.wifi.isEmpty()) {
            item { Text(stringResource(R.string.scanner_no_wifi)) }
        } else {
            items(state.wifi, key = { it.bssid }) { net ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradeDot(SignalGrade.wifi(net.rssi))
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(net.ssid, style = MaterialTheme.typography.bodyLarge)
                            if (net.ssid == state.connectedSsid) {
                                Text(stringResource(R.string.scanner_connected),
                                    style = MaterialTheme.typography.labelSmall, color = GradeGood)
                            }
                        }
                        Text("Ch${net.channel} ${stringResource(R.string.scanner_band_ghz, net.band.name.removePrefix("GHZ_").replace('_', '.'))}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { HorizontalDivider() }
        item { Text(stringResource(R.string.scanner_ble_header), style = MaterialTheme.typography.titleMedium) }
        if (state.ble.isEmpty()) {
            item { Text(stringResource(R.string.scanner_no_ble)) }
        } else {
            items(state.ble, key = { it.address }) { dev ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradeDot(SignalGrade.wifi(dev.rssi))
                    Text(dev.name ?: stringResource(R.string.scanner_unknown_device))
                    Text(stringResource(R.string.scanner_dbm, dev.rssi),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
