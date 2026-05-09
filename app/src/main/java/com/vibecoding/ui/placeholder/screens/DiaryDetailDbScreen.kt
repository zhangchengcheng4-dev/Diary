package com.vibecoding.ui.placeholder.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibecoding.data.local.db.AppDatabase
import com.vibecoding.ui.placeholder.theme.PlaceholderColors
import kotlinx.coroutines.flow.map

@Composable
fun DiaryDetailDbScreen(entryId: String) {
    val context = LocalContext.current.applicationContext
    val vm: DiaryDetailDbViewModel = viewModel(factory = DiaryDetailDbViewModelFactory(context, entryId))
    val state by vm.uiState.collectAsState(initial = DiaryDetailDbUiState())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("真实详情", color = PlaceholderColors.PrimaryText)
                Spacer(modifier = Modifier.height(8.dp))
                Text("状态: ${state.processingStatus}", color = PlaceholderColors.SecondaryText)
                Text("分类: ${state.category}", color = PlaceholderColors.SecondaryText)
                Text("标签: ${state.tags}", color = PlaceholderColors.SecondaryText)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Transcript", color = PlaceholderColors.PrimaryText)
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Text(state.transcript.ifBlank { "暂无转写结果" }, color = PlaceholderColors.SecondaryText)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Polished Article", color = PlaceholderColors.PrimaryText)
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Text(state.polishedArticle.ifBlank { "暂无正文" }, color = PlaceholderColors.SecondaryText)
                }
            }
        }
    }
}

data class DiaryDetailDbUiState(
    val processingStatus: String = "loading",
    val category: String = "",
    val tags: String = "",
    val transcript: String = "",
    val polishedArticle: String = ""
)

class DiaryDetailDbViewModel(
    context: Context,
    entryId: String
) : ViewModel() {
    private val db = AppDatabase.getInstance(context.applicationContext)
    val uiState = db.diaryEntryDao().observeById(entryId).map { entry ->
        if (entry == null) {
            DiaryDetailDbUiState(processingStatus = "entry_not_found")
        } else {
            DiaryDetailDbUiState(
                processingStatus = entry.processingStatus,
                category = entry.primaryCategoryId,
                tags = entry.dynamicTags,
                transcript = entry.rawTranscript,
                polishedArticle = entry.polishedArticle
            )
        }
    }
}

class DiaryDetailDbViewModelFactory(
    private val context: Context,
    private val entryId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiaryDetailDbViewModel(context, entryId) as T
    }
}
