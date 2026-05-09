package com.vibecoding.ui.placeholder.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibecoding.recording.RecordStatus
import com.vibecoding.recording.RecordViewModel
import com.vibecoding.recording.RecordViewModelFactory
import com.vibecoding.ui.placeholder.theme.PlaceholderColors

@Composable
fun RecordScreen(
    onFinish: (String) -> Unit
) {
    val context = LocalContext.current
    val vm: RecordViewModel = viewModel(factory = RecordViewModelFactory(context))
    val state by vm.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startRecording() else vm.onPermissionDenied()
    }

    LaunchedEffect(state.permissionDenied) {
        if (state.permissionDenied) vm.clearDeniedHint()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = formatDuration(state.elapsedMs), color = PlaceholderColors.PrimaryText)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = when (state.status) {
                RecordStatus.IDLE -> "准备录音"
                RecordStatus.RECORDING -> "录音中"
                RecordStatus.PAUSED -> "已暂停"
                RecordStatus.STOPPED -> "已停止"
                RecordStatus.ERROR -> "录音异常"
            },
            color = PlaceholderColors.SecondaryText
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (state.status == RecordStatus.IDLE || state.status == RecordStatus.ERROR) {
            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) vm.startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.size(width = 180.dp, height = 52.dp)
            ) {
                Text("开始")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state.status) {
                    RecordStatus.RECORDING -> {
                        TextButton(onClick = vm::pauseRecording) { Text("暂停") }
                        Button(onClick = { vm.stopRecording(onFinish) }) { Text("停止") }
                    }
                    RecordStatus.PAUSED -> {
                        TextButton(onClick = vm::resumeRecording) { Text("继续") }
                        Button(onClick = { vm.stopRecording(onFinish) }) { Text("停止") }
                    }
                    else -> Unit
                }
            }
        }

        state.message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = PlaceholderColors.SecondaryText)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    val mm = seconds / 60
    val ss = seconds % 60
    return "%02d:%02d".format(mm, ss)
}
