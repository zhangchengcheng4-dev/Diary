package com.vibecoding.ui.placeholder.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibecoding.data.local.db.AppDatabase
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
            Text("处理中", color = PlaceholderColors.PrimaryText)
            Spacer(modifier = Modifier.height(8.dp))
            Text("EntryId: $entryId", color = PlaceholderColors.SecondaryText)
        }
        item { StatusCard("状态", state.status.ifBlank { "unknown" }) }
        item { StatusCard("错误", state.errorMessage ?: "无") }
        item { StatusCard("Transcript", state.transcript.ifBlank { "等待转写结果..." }) }
        if (state.status == "processed_succeeded") {
            item {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("查看详情")
                }
            }
        }
        if (state.status == "processed_failed") {
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = PlaceholderColors.PrimaryText)
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
    val errorMessage: String? = null
)

class ProcessingViewModel(
    context: Context,
    private val entryId: String
) : ViewModel() {
    private val appDb = AppDatabase.getInstance(context.applicationContext)
    private val useCase = Step9ProcessingUseCase(context.applicationContext)

    val uiState = appDb.diaryEntryDao().observeById(entryId)
        .map { entry ->
            if (entry == null) {
                ProcessingUiState(status = "entry_not_found", errorMessage = "条目不存在")
            } else {
                val sync = appDb.syncStateDao().findById(entry.syncStateId)
                ProcessingUiState(
                    status = entry.processingStatus,
                    transcript = entry.rawTranscript,
                    errorMessage = sync?.lastErrorMessage
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProcessingUiState(status = "loading"))

    init {
        viewModelScope.launch {
            val entry = appDb.diaryEntryDao().findById(entryId) ?: return@launch
            if (entry.processingStatus == "recorded_pending_upload" || entry.processingStatus == "processing") {
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
