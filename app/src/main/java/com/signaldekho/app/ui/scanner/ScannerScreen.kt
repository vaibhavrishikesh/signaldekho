package com.signaldekho.app.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signaldekho.app.R
import com.signaldekho.app.data.CarrierName
import com.signaldekho.app.domain.SignalGrade
import com.signaldekho.app.ui.LocalAppContainer
import com.signaldekho.app.ui.components.SegmentBar
import com.signaldekho.app.ui.components.gradeColor
import com.signaldekho.app.ui.components.gradeLabel
import com.signaldekho.app.ui.theme.HeroSubtext
import com.signaldekho.app.ui.theme.HeroText
import com.signaldekho.app.ui.theme.HeroTint

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

        item {
            val ssid = state.connectedSsid
            val rssi = state.connectedRssi
            if (ssid != null && rssi != null) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(HeroTint, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.scanner_your_wifi, ssid),
                        style = MaterialTheme.typography.bodySmall, color = HeroSubtext)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(gradeLabel(SignalGrade.wifi(rssi)),
                            style = MaterialTheme.typography.headlineMedium, color = HeroText)
                        Text(stringResource(R.string.scanner_dbm, rssi),
                            style = MaterialTheme.typography.bodySmall, color = HeroSubtext)
                    }
                    SegmentBar(SignalGrade.wifiFraction(rssi), gradeColor(SignalGrade.wifi(rssi)),
                        modifier = Modifier.fillMaxWidth())
                }
            } else {
                Text(stringResource(R.string.scanner_no_wifi_connection), style = MaterialTheme.typography.bodyMedium)
            }
        }

        item { Text(stringResource(R.string.scanner_sim_header), style = MaterialTheme.typography.titleMedium) }
        if (state.cells.isEmpty()) {
            item { Text(stringResource(R.string.scanner_no_sim)) }
        } else {
            items(state.cells) { cell ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(CarrierName.clean(cell.operatorName))
                            Text(cell.networkType, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                        val dbm = cell.dbm
                        if (dbm != null) {
                            SegmentBar(SignalGrade.cellFraction(dbm), gradeColor(SignalGrade.cell(dbm)),
                                modifier = Modifier.width(90.dp))
                        }
                        if (cell.ageMillis > 60_000) {
                            Text(stringResource(R.string.scanner_stale_minutes, cell.ageMillis / 60_000),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val dbm = cell.dbm
                        if (dbm != null) {
                            Text(gradeLabel(SignalGrade.cell(dbm)), color = gradeColor(SignalGrade.cell(dbm)))
                            Text(stringResource(R.string.scanner_dbm, dbm), style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("—")
                        }
                    }
                }
            }
        }

        item { HorizontalDivider() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.scanner_nearby_header, state.nearby.size), style = MaterialTheme.typography.titleMedium)
                if (state.secondsToNextScan > 0) {
                    Text(stringResource(R.string.scanner_next_scan_in, state.secondsToNextScan),
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedButton(onClick = { vm.refresh() }) { Text(stringResource(R.string.scanner_refresh)) }
                }
            }
        }
        if (state.nearby.isEmpty()) {
            item { Text(stringResource(R.string.scanner_no_wifi)) }
        } else {
            items(state.nearby, key = { it.ssid }) { net ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SegmentBar(SignalGrade.wifiFraction(net.rssi), gradeColor(SignalGrade.wifi(net.rssi)),
                        segments = 3, modifier = Modifier.width(14.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(net.ssid, style = MaterialTheme.typography.bodyLarge)
                        if (net.count > 1) {
                            Text(stringResource(R.string.scanner_duplicate_count, net.count),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(stringResource(R.string.scanner_band_ghz, net.band), style = MaterialTheme.typography.bodySmall)
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
                    SegmentBar(SignalGrade.wifiFraction(dev.rssi), gradeColor(SignalGrade.wifi(dev.rssi)),
                        segments = 3, modifier = Modifier.width(14.dp))
                    Text(dev.name ?: stringResource(R.string.scanner_unknown_device))
                    Text(stringResource(R.string.scanner_dbm, dev.rssi),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
