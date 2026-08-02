package com.signaldekho.app.ui.report

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signaldekho.app.R
import com.signaldekho.app.domain.Finding
import com.signaldekho.app.domain.Grade
import com.signaldekho.app.domain.SignalGrade
import com.signaldekho.app.ui.LocalAppContainer
import com.signaldekho.app.ui.components.FillBar
import com.signaldekho.app.ui.components.gradeColor
import com.signaldekho.app.ui.components.gradeLabel
import com.signaldekho.app.ui.theme.GradeWeak

@Composable
fun findingText(f: Finding): String = when (f) {
    is Finding.BestWifiRoom -> stringResource(R.string.report_finding_best_wifi, f.room)
    is Finding.WeakestWifiRoom -> stringResource(R.string.report_finding_weakest_wifi, f.room)
    is Finding.WeakestCellRoom -> stringResource(R.string.report_finding_weakest_cell, f.room)
    is Finding.BestRoomForCalls -> stringResource(R.string.report_finding_best_calls, f.room)
    Finding.WifiAllGood -> stringResource(R.string.report_finding_wifi_all_good)
    Finding.RouterReposition -> stringResource(R.string.report_finding_router)
    Finding.WifiNotMeasured -> stringResource(R.string.report_wifi_not_measured)
    Finding.AllRoomsSimilarWifi -> stringResource(R.string.report_finding_similar_wifi)
    Finding.AllRoomsSimilarCell -> stringResource(R.string.report_finding_similar_cell)
}

/** Best signal first; rooms missing the sort signal go last. */
private fun sortedRows(rows: List<ReportRow>): List<ReportRow> {
    val byWifi = rows.any { it.wifiRssi != null }
    return rows.sortedWith(
        compareByDescending { row -> (if (byWifi) row.wifiRssi else row.cellDbm) ?: Int.MIN_VALUE }
    )
}

@Composable
fun ReportScreen(surveyId: Long) {
    val container = LocalAppContainer.current
    val vm: ReportViewModel = viewModel { ReportViewModel(container.surveyDao, surveyId) }
    val state by vm.state.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(stringResource(R.string.report_title), style = MaterialTheme.typography.headlineMedium) }
        item {
            Text(
                stringResource(R.string.report_subtitle, state.rows.size),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (state.findings.contains(Finding.WifiNotMeasured)) {
            item {
                Text(
                    stringResource(R.string.report_wifi_not_measured),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GradeWeak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                )
            }
        }

        items(sortedRows(state.rows)) { row ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.room, style = MaterialTheme.typography.bodyLarge)
                row.wifiRssi?.let { rssi ->
                    val grade = SignalGrade.wifi(rssi)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.report_row_wifi), style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(44.dp))
                        FillBar(SignalGrade.wifiFraction(rssi), gradeColor(grade), Modifier.weight(1f))
                        Text(gradeLabel(grade), style = MaterialTheme.typography.bodySmall, color = gradeColor(grade),
                            modifier = Modifier.width(84.dp), textAlign = TextAlign.End)
                    }
                }
                row.cellDbm?.let { dbm ->
                    val grade = SignalGrade.cell(dbm)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.report_row_sim), style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(44.dp))
                        FillBar(SignalGrade.cellFraction(dbm), gradeColor(grade), Modifier.weight(1f))
                        Text(gradeLabel(grade), style = MaterialTheme.typography.bodySmall, color = gradeColor(grade),
                            modifier = Modifier.width(84.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }

        item { HorizontalDivider() }
        items(state.findings.filterNot { it == Finding.WifiNotMeasured }) { f ->
            Text("• " + findingText(f), style = MaterialTheme.typography.bodyLarge)
        }

        item {
            val context = LocalContext.current
            val rows = sortedRows(state.rows)
            val findingStrings = state.findings
                .filterNot { it == Finding.WifiNotMeasured }
                .map { findingText(it) }
            val header = RenderHeader(
                title = stringResource(R.string.report_image_title),
                subtitle = stringResource(R.string.report_subtitle, state.rows.size),
                wifiLabel = stringResource(R.string.report_row_wifi),
                simLabel = stringResource(R.string.report_row_sim),
                notMeasured = stringResource(R.string.report_wifi_not_measured)
                    .takeIf { state.findings.contains(Finding.WifiNotMeasured) },
                watermark = stringResource(R.string.report_watermark),
                gradeLabels = Grade.entries.associateWith { gradeLabel(it) },
            )
            Button(onClick = {
                val file = ReportImageRenderer.render(context, header, rows, findingStrings)
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
