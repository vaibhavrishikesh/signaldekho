package com.signaldekho.app.ui.gharscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signaldekho.app.R
import com.signaldekho.app.ui.LocalAppContainer

@Composable
fun GharScanScreen(onFinished: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm: GharScanViewModel = viewModel {
        GharScanViewModel(container.wifiRepo, container.cellularRepo, container.surveyDao)
    }
    val state by vm.state.collectAsState()
    val chips = listOf(
        stringResource(R.string.ghar_chip_bedroom), stringResource(R.string.ghar_chip_kitchen),
        stringResource(R.string.ghar_chip_hall), stringResource(R.string.ghar_chip_bathroom),
        stringResource(R.string.ghar_chip_balcony), stringResource(R.string.ghar_chip_chhat),
    )

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.ghar_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.ghar_instructions), style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = state.roomInput,
            onValueChange = vm::setRoomInput,
            label = { Text(stringResource(R.string.ghar_room_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips) { chip ->
                AssistChip(onClick = { vm.setRoomInput(chip) }, label = { Text(chip) })
            }
        }
        Button(
            onClick = vm::measureCurrentRoom,
            enabled = state.roomInput.isNotBlank() && !state.saving,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.ghar_measure)) }

        state.measuredRooms.forEach { room ->
            Text(stringResource(R.string.ghar_measured, room))
        }

        if (state.measuredRooms.size >= 2) {
            OutlinedButton(
                onClick = { vm.finish(onFinished) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.ghar_done)) }
        }
    }
}
