package com.vibecoding.ui.placeholder.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibecoding.data.local.DiaryProcessingStatus
import com.vibecoding.data.local.SyncStatus
import com.vibecoding.data.repository.LocalDiaryRepository
import com.vibecoding.recording.Step9ProcessingUseCase
import com.vibecoding.ui.placeholder.theme.PlaceholderColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun ProcessingScreen(
    entryId: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val vm: ProcessingViewModel = viewModel(factory = ProcessingViewModelFactory(context, entryId))
    val state by vm.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(state.title, color = PlaceholderColors.PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(state.description, color = PlaceholderColors.SecondaryText, fontSize = 14.sp)
        }
        if (state.isActive) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = PlaceholderColors.Accent
                )
            }
        }
        item { StatusCard("处理状态", state.statusLabel) }
        if (state.errorCode != null || state.errorMessage != null) {
            item {
                StatusCard(
                    title = "错误信息",
                    content = listOfNotNull(state.errorCode, state.errorMessage).joinToString("\n")
                )
            }
        }
        item { StatusCard("转写文本", state.transcript.ifBlank { state.transcriptPlaceholder }) }
        item {
            StatusCard(
                title = "本地处理说明",
                content = "当前流程只在本地保存处理结果；真实 AI、backend sync 和 WorkManager 仍未接入。"
            )
        }
        if (state.canOpenDetail) {
            item {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("查看详情")
                }
            }
        }
        if (state.canRetry) {
            item {
                Button(
                    onClick = vm::retry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重试处理")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PlaceholderColors.Surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = PlaceholderColors.PrimaryText, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            SelectionContainer {
                Text(content, color = PlaceholderColors.SecondaryText)
            }
        }
    }
}

data class ProcessingUiState(
    val status: String = "",
    val transcript: String = "",
    val errorCode: String? = null,
    val errorMessage: String? = null
) {
    val title: String
        get() = when (status) {
            "loading" -> "正在读取日记"
            "entry_not_found" -> "日记不存在"
            DiaryProcessingStatus.RecordedPendingUpload -> "等待转写"
            DiaryProcessingStatus.Processing -> "正在转写"
            DiaryProcessingStatus.ProcessedSucceeded -> "处理完成"
            DiaryProcessingStatus.ProcessedFailed -> "处理失败"
            else -> "处理状态未知"
        }

    val description: String
        get() = when (status) {
            "loading" -> "正在读取本地日记状态。"
            "entry_not_found" -> "这条日记可能已被删除或尚未创建成功。"
            DiaryProcessingStatus.RecordedPendingUpload -> "录音已保存，准备开始本地 ASR 处理。"
            DiaryProcessingStatus.Processing -> "正在进行 ASR 转写，请保持应用打开。"
            DiaryProcessingStatus.ProcessedSucceeded -> "转写和本地日记保存已完成。"
            DiaryProcessingStatus.ProcessedFailed -> "本地处理没有完成，可以检查错误后重试。"
            else -> "当前状态不在本地 MVP 状态集合中。"
        }

    val statusLabel: String
        get() = when (status) {
            "loading" -> "加载中"
            "entry_not_found" -> "条目不存在"
            DiaryProcessingStatus.RecordedPendingUpload -> "等待处理"
            DiaryProcessingStatus.Processing -> "处理中"
            DiaryProcessingStatus.ProcessedSucceeded -> "已完成"
            DiaryProcessingStatus.ProcessedFailed -> "失败"
            else -> status.ifBlank { "unknown" }
        }

    val transcriptPlaceholder: String
        get() = when (status) {
            DiaryProcessingStatus.ProcessedFailed -> "本次处理未生成可保存的转写文本。"
            DiaryProcessingStatus.ProcessedSucceeded -> "转写为空，详情页会显示原始保存内容。"
            else -> "转写完成后会显示在这里。"
        }

    val isActive: Boolean
        get() = status == "loading" || DiaryProcessingStatus.needsProcessing(status)

    val canRetry: Boolean
        get() = DiaryProcessingStatus.canRetry(status)

    val canOpenDetail: Boolean
        get() = status == DiaryProcessingStatus.ProcessedSucceeded
}

class ProcessingViewModel(
    context: Context,
    private val entryId: String
) : ViewModel() {
    private val repository = LocalDiaryRepository(context.applicationContext)
    private val useCase = Step9ProcessingUseCase(context.applicationContext)

    val uiState = repository.observeEntry(entryId)
        .map { entry ->
            if (entry == null) {
                ProcessingUiState(status = "entry_not_found", errorMessage = "条目不存在")
            } else {
                val sync = repository.findSyncState(entry.syncStateId)
                val realError = sync
                    ?.takeIf {
                        entry.processingStatus == DiaryProcessingStatus.ProcessedFailed ||
                            it.syncStatus == SyncStatus.Failed
                    }
                val errorMessage = realError
                    ?.lastErrorMessage
                    ?.takeIf { it.isNotBlank() && !it.startsWith("[debug]") }
                val errorCode = realError
                    ?.lastErrorCode
                    ?.takeIf { it.isNotBlank() }
                ProcessingUiState(
                    status = entry.processingStatus,
                    transcript = entry.rawTranscript,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProcessingUiState(status = "loading"))

    init {
        viewModelScope.launch {
            val entry = repository.findEntry(entryId) ?: return@launch
            if (DiaryProcessingStatus.needsProcessing(entry.processingStatus)) {
                useCase.run(entryId)
            }
        }
    }

    fun retry() {
        viewModelScope.launch {
            useCase.run(entryId)
        }
    }
}

class ProcessingViewModelFactory(
    private val context: Context,
    private val entryId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProcessingViewModel(context, entryId) as T
    }
}
