package com.signaldekho.app.ui.report

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signaldekho.app.R
import com.signaldekho.app.domain.Finding
import com.signaldekho.app.domain.SignalGrade
import com.signaldekho.app.ui.LocalAppContainer
import com.signaldekho.app.ui.components.gradeColor

@Composable
fun findingText(f: Finding): String = when (f) {
    is Finding.BestWifiRoom -> stringResource(R.string.report_finding_best_wifi, f.room)
    is Finding.WeakestWifiRoom -> stringResource(R.string.report_finding_weakest_wifi, f.room)
    is Finding.WeakestCellRoom -> stringResource(R.string.report_finding_weakest_cell, f.room)
    is Finding.BestRoomForCalls -> stringResource(R.string.report_finding_best_wifi, f.room)
    Finding.WifiAllGood -> stringResource(R.string.report_finding_wifi_all_good)
    Finding.RouterReposition -> stringResource(R.string.report_finding_router)
    Finding.WifiNotMeasured -> stringResource(R.string.report_finding_wifi_all_good)
    Finding.AllRoomsSimilarWifi -> stringResource(R.string.report_finding_wifi_all_good)
    Finding.AllRoomsSimilarCell -> stringResource(R.string.report_finding_wifi_all_good)
}

@Composable
fun ReportScreen(surveyId: Long) {
    val container = LocalAppContainer.current
    val vm: ReportViewModel = viewModel { ReportViewModel(container.surveyDao, surveyId) }
    val state by vm.state.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(stringResource(R.string.report_title), style = MaterialTheme.typography.headlineMedium) }

        items(state.rows) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.room, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.report_row_wifi) + " " +
                            (row.wifiRssi?.let { stringResource(R.string.scanner_dbm, it) } ?: stringResource(R.string.report_wifi_not_measured)),
                        color = row.wifiRssi?.let { gradeColor(SignalGrade.wifi(it)) }
                            ?: MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.report_row_sim) + " " +
                            (row.cellDbm?.let { stringResource(R.string.scanner_dbm, it) } ?: stringResource(R.string.report_wifi_not_measured)),
                        color = row.cellDbm?.let { gradeColor(SignalGrade.cell(it)) }
                            ?: MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        item { HorizontalDivider() }
        items(state.findings) { f ->
            Text("• " + findingText(f), style = MaterialTheme.typography.bodyLarge)
        }

        item {
            val context = LocalContext.current
            val findingStrings = state.findings.map { findingText(it) }
            Button(onClick = {
                val file = ReportImageRenderer.render(context, state.rows, findingStrings)
                val uri = FileProvider.getUriForFile(context, "com.signaldekho.app.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, null))
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.report_share))
            }
        }
    }
}
