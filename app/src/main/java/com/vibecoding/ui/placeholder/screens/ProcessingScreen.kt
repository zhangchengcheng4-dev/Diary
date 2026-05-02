package com.vibecoding.ui.placeholder.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibecoding.ui.placeholder.theme.PlaceholderColors
import kotlinx.coroutines.delay

@Composable
fun ProcessingScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI 正在处理中...", color = PlaceholderColors.PrimaryText)
        Spacer(modifier = Modifier.height(8.dp))
        Text("1. 上传音频", color = PlaceholderColors.SecondaryText)
        Text("2. 语音转文字", color = PlaceholderColors.SecondaryText)
        Text("3. 整理成文", color = PlaceholderColors.SecondaryText)
    }
}
