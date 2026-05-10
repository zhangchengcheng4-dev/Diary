package com.vibecoding.ui.placeholder.screens

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibecoding.app.BuildConfig
import com.vibecoding.data.local.DiaryProcessingStatus
import com.vibecoding.data.repository.LocalDiaryRepository
import com.vibecoding.ui.placeholder.theme.PlaceholderColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DiaryDetailDbScreen(
    entryId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit = {},
    onRetryProcessing: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val vm: DiaryDetailDbViewModel = viewModel(factory = DiaryDetailDbViewModelFactory(context, entryId))
    val state by vm.uiState.collectAsState(initial = DiaryDetailDbUiState())
    val audioController = remember { DetailAudioPlayerController() }
    val currentAudio by rememberUpdatedState(state.audio)
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editArticle by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.audio?.playlistKey) {
        audioController.setAudio(state.audio)
    }

    LaunchedEffect(audioController.isPlaying, audioController.currentSegmentIndex, state.audio?.playlistKey) {
        while (audioController.isPlaying) {
            currentAudio?.let { audioController.updateProgress(it) }
            delay(300)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioController.release()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除日记") },
            text = { Text("本阶段只会软删除日记记录，不删除本地音频文件。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            vm.softDelete()
                            showDeleteConfirm = false
                            onDeleted()
                        }
                    }
                ) {
                    Text("删除", color = PlaceholderColors.Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = PlaceholderColors.SecondaryText)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
    ) {
        DetailTopBar(onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SummaryCard(state = state) }
            item {
                DetailActionCard(
                    isEditing = isEditing,
                    onEdit = {
                        editTitle = state.storedTitle.ifBlank { state.title }
                        editArticle = state.polishedArticle
                        editCategory = state.category.ifBlank { "life" }
                        editTags = state.tags.joinToString(", ")
                        editDate = state.entryDateLocal
                        editError = null
                        isEditing = true
                    },
                    onDelete = { showDeleteConfirm = true }
                )
            }
            if (isEditing) {
                item {
                    EditDiaryCard(
                        title = editTitle,
                        onTitleChange = { editTitle = it },
                        polishedArticle = editArticle,
                        onPolishedArticleChange = { editArticle = it },
                        category = editCategory,
                        onCategoryChange = { editCategory = it },
                        tags = editTags,
                        onTagsChange = { editTags = it },
                        entryDate = editDate,
                        onEntryDateChange = { editDate = it },
                        error = editError,
                        onCancel = {
                            editError = null
                            isEditing = false
                        },
                        onSave = {
                            scope.launch {
                                val error = vm.saveEdits(
                                    title = editTitle,
                                    polishedArticle = editArticle,
                                    category = editCategory,
                                    tagsText = editTags,
                                    entryDateLocal = editDate
                                )
                                editError = error
                                if (error == null) {
                                    isEditing = false
                                }
                            }
                        }
                    )
                }
            }
            if (DiaryProcessingStatus.canRetry(state.processingStatus) || DiaryProcessingStatus.needsProcessing(state.processingStatus)) {
                item { ProcessingActionCard(state.processingStatus, onRetryProcessing) }
            }
            item { AudioPlayerCard(audio = state.audio, controller = audioController) }
            item { TranscriptCard(transcript = state.transcript) }
            item { PolishedArticleCard(transcript = state.transcript, polishedArticle = state.polishedArticle) }
            item { TagsCard(tags = state.tags) }
            item { NoteCard() }
            if (BuildConfig.DEBUG) {
                item { DebugInfoCard(state = state) }
            }
        }
    }
}

@Composable
private fun ProcessingActionCard(status: String, onRetryProcessing: () -> Unit) {
    SoftCard {
        Text(
            text = if (DiaryProcessingStatus.canRetry(status)) "转写失败" else "转写未完成",
            color = PlaceholderColors.PrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetryProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (DiaryProcessingStatus.canRetry(status)) "重试处理" else "查看处理进度")
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlaceholderColors.Background)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text("返回", color = PlaceholderColors.Accent)
        }
        Text(
            text = "日记详情",
            color = PlaceholderColors.PrimaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SummaryCard(state: DiaryDetailDbUiState) {
    SoftCard {
        Text(
            text = state.title,
            color = PlaceholderColors.PrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(state.displayTime, color = PlaceholderColors.SecondaryText, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Chip(text = state.category.ifBlank { "life" })
    }
}

@Composable
private fun DetailActionCard(
    isEditing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SoftCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onEdit,
                enabled = !isEditing,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isEditing) "编辑中" else "编辑")
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            ) {
                Text("删除", color = PlaceholderColors.Accent)
            }
        }
    }
}

@Composable
private fun EditDiaryCard(
    title: String,
    onTitleChange: (String) -> Unit,
    polishedArticle: String,
    onPolishedArticleChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    entryDate: String,
    onEntryDateChange: (String) -> Unit,
    error: String?,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    SoftCard {
        Text("编辑日记", color = PlaceholderColors.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("标题") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = polishedArticle,
            onValueChange = onPolishedArticleChange,
            label = { Text("正文") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            label = { Text("分类") },
            supportingText = { Text("work / study / life / emotion / health") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = tags,
            onValueChange = onTagsChange,
            label = { Text("标签") },
            supportingText = { Text("用逗号分隔，最多 5 个") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = entryDate,
            onValueChange = onEntryDateChange,
            label = { Text("日期") },
            supportingText = { Text("yyyy-MM-dd") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = PlaceholderColors.Accent, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("取消", color = PlaceholderColors.SecondaryText)
            }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Text("保存")
            }
        }
    }
}

@Composable
private fun AudioPlayerCard(
    audio: AudioUiState?,
    controller: DetailAudioPlayerController
) {
    SoftCard {
        Text("录音", color = PlaceholderColors.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))
        if (audio == null || audio.playableSegments.isEmpty()) {
            Text("暂无可播放的录音文件", color = PlaceholderColors.SecondaryText, fontSize = 13.sp)
            return@SoftCard
        }

        val isPlaying = controller.isPlaying
        val progress = controller.progress
        val currentMs = controller.currentMs

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { controller.togglePlayback(audio) },
                modifier = Modifier.size(width = 82.dp, height = 42.dp)
            ) {
                Text(if (isPlaying) "暂停" else "播放")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = PlaceholderColors.Accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${formatDuration(currentMs)} / ${formatDuration(audio.totalDurationMs)}",
                    color = PlaceholderColors.SecondaryText,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Chip(text = "1.0x")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "共 ${audio.playableSegments.size} 段录音，将按录制顺序连续播放。波形展示 TODO",
            color = PlaceholderColors.SecondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun TranscriptCard(transcript: String) {
    val clipboard = LocalClipboardManager.current
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("转写文本", color = PlaceholderColors.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { clipboard.setText(AnnotatedString(transcript)) }) {
                Text("复制", color = PlaceholderColors.Accent)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SelectionContainer {
            Text(
                text = transcript.ifBlank { "暂无转写文本" },
                color = PlaceholderColors.PrimaryText,
                fontSize = 15.sp,
                lineHeight = 23.sp
            )
        }
    }
}

@Composable
private fun PolishedArticleCard(transcript: String, polishedArticle: String) {
    val content = polishedArticle.ifBlank { transcript }
    SoftCard {
        Text("润色文章", color = PlaceholderColors.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Text("AI 润色功能开发中，当前先展示转写内容。", color = PlaceholderColors.SecondaryText, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))
        SelectionContainer {
            Text(
                text = content.ifBlank { "暂无内容" },
                color = PlaceholderColors.PrimaryText,
                fontSize = 15.sp,
                lineHeight = 23.sp
            )
        }
    }
}

@Composable
private fun TagsCard(tags: List<String>) {
    SoftCard {
        Text("标签", color = PlaceholderColors.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (tags.isEmpty()) {
                Text("暂无标签", color = PlaceholderColors.SecondaryText, fontSize = 13.sp)
            } else {
                tags.forEach { Chip(text = it) }
            }
            TextButton(onClick = { /* TODO: add tag editing */ }) {
                Text("+", color = PlaceholderColors.Accent, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun NoteCard() {
    SoftCard {
        Text("备注", color = PlaceholderColors.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("添加备注...", color = PlaceholderColors.SecondaryText, fontSize = 14.sp)
        Text("TODO: 后续接入编辑保存。", color = PlaceholderColors.SecondaryText, fontSize = 12.sp)
    }
}

@Composable
private fun DebugInfoCard(state: DiaryDetailDbUiState) {
    var expanded by remember { mutableStateOf(false) }
    SoftCard {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "隐藏技术信息" else "显示技术信息", color = PlaceholderColors.Accent)
        }
        if (expanded) {
            Text("状态: ${state.processingStatus}", color = PlaceholderColors.SecondaryText, fontSize = 12.sp)
            Text("entryId: ${state.entryId}", color = PlaceholderColors.SecondaryText, fontSize = 12.sp)
            Text("audioAssetIds: ${state.audio?.segments.orEmpty().joinToString { it.audioAssetId }}", color = PlaceholderColors.SecondaryText, fontSize = 12.sp)
            Text("processing status: ${state.processingStatus}", color = PlaceholderColors.SecondaryText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SoftCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PlaceholderColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text = text,
        color = PlaceholderColors.SecondaryText,
        fontSize = 12.sp,
        modifier = Modifier
            .background(PlaceholderColors.TagBackground, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

data class AudioSegmentUiState(
    val audioAssetId: String,
    val path: String,
    val durationMs: Long,
    val exists: Boolean
)

data class AudioUiState(
    val segments: List<AudioSegmentUiState>
) {
    val playableSegments: List<AudioSegmentUiState> = segments.filter { it.exists }
    val totalDurationMs: Long = playableSegments.sumOf { it.durationMs }
    val playlistKey: String = playableSegments.joinToString("|") { "${it.audioAssetId}:${it.path}" }
}

private class DetailAudioPlayerController {
    private var player: MediaPlayer? = null
    private var playlistKey: String? = null

    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var currentMs by mutableStateOf(0L)
        private set
    var currentSegmentIndex by mutableIntStateOf(0)
        private set

    fun setAudio(audio: AudioUiState?) {
        val nextKey = audio?.playlistKey
        if (nextKey != playlistKey) {
            releasePlayerOnly()
            resetPlayback()
            playlistKey = nextKey
        }
    }

    fun togglePlayback(audio: AudioUiState) {
        if (audio.playableSegments.isEmpty()) return
        setAudio(audio)
        val current = player
        if (current?.isPlaying == true) {
            current.pause()
            isPlaying = false
        } else if (current != null) {
            current.start()
            isPlaying = true
        } else {
            startSegment(audio, currentSegmentIndex)
        }
    }

    fun updateProgress(audio: AudioUiState) {
        val elapsedBeforeCurrent = audio.playableSegments
            .take(currentSegmentIndex)
            .sumOf { it.durationMs }
        currentMs = elapsedBeforeCurrent + (player?.currentPosition?.toLong() ?: 0L)
        progress = (currentMs.toFloat() / audio.totalDurationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
    }

    fun release() {
        releasePlayerOnly()
        resetPlayback()
        playlistKey = null
    }

    private fun startSegment(audio: AudioUiState, index: Int) {
        releasePlayerOnly()
        val segment = audio.playableSegments.getOrNull(index)
        if (segment == null) {
            resetPlayback()
            return
        }

        currentSegmentIndex = index
        player = MediaPlayer().apply {
            setDataSource(segment.path)
            prepare()
            setOnCompletionListener {
                val nextIndex = index + 1
                if (nextIndex < audio.playableSegments.size) {
                    startSegment(audio, nextIndex)
                } else {
                    releasePlayerOnly()
                    resetPlayback()
                }
            }
            start()
        }
        isPlaying = true
    }

    private fun releasePlayerOnly() {
        player?.release()
        player = null
    }

    private fun resetPlayback() {
        isPlaying = false
        progress = 0f
        currentMs = 0L
        currentSegmentIndex = 0
    }
}

data class DiaryDetailDbUiState(
    val entryId: String = "",
    val title: String = "语音日记",
    val storedTitle: String = "",
    val displayTime: String = "",
    val entryOccurredAt: String = "",
    val entryDateLocal: String = "",
    val processingStatus: String = "loading",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val transcript: String = "",
    val polishedArticle: String = "",
    val audio: AudioUiState? = null
)

class DiaryDetailDbViewModel(
    context: Context,
    private val entryId: String
) : ViewModel() {
    private val repository = LocalDiaryRepository(context.applicationContext)
    val uiState = repository.observeEntry(entryId).map { entry ->
        if (entry == null) {
            DiaryDetailDbUiState(processingStatus = "entry_not_found")
        } else {
            val audioAssets = repository.findAudioAssets(entry.entryId)
            val transcript = entry.rawTranscript
            val article = entry.polishedArticle.ifBlank { transcript }
            DiaryDetailDbUiState(
                entryId = entry.entryId,
                title = buildTitle(transcript, article, entry.title),
                storedTitle = entry.title,
                displayTime = formatDateTime(entry.entryOccurredAt.ifBlank { entry.createdAt }),
                entryOccurredAt = entry.entryOccurredAt,
                entryDateLocal = entry.entryDateLocal,
                processingStatus = entry.processingStatus,
                category = entry.primaryCategoryId,
                tags = entry.dynamicTags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                transcript = transcript,
                polishedArticle = article,
                audio = AudioUiState(
                    segments = audioAssets.map {
                        AudioSegmentUiState(
                            audioAssetId = it.audioAssetId,
                            path = it.localPath,
                            durationMs = it.durationMs,
                            exists = File(it.localPath).exists()
                        )
                    }
                )
            )
        }
    }

    suspend fun saveEdits(
        title: String,
        polishedArticle: String,
        category: String,
        tagsText: String,
        entryDateLocal: String
    ): String? {
        return repository.saveDetailEdits(
            entryId = entryId,
            title = title,
            polishedArticle = polishedArticle,
            category = category,
            tagsText = tagsText,
            entryDateLocal = entryDateLocal
        )
    }

    suspend fun softDelete() {
        repository.softDeleteEntry(entryId)
    }

    private fun buildTitle(transcript: String, article: String, storedTitle: String): String {
        val source = transcript.ifBlank { article }
        val firstSentence = source
            .split("。", "！", "？", ".", "!", "?")
            .firstOrNull()
            .orEmpty()
            .trim()
        return firstSentence.take(20).ifBlank {
            storedTitle.takeIf { it.isNotBlank() && it != "Voice Draft" } ?: "语音日记"
        }
    }

    private fun formatDateTime(value: String): String {
        val zone = ZoneId.systemDefault()
        return runCatching {
            Instant.parse(value)
                .atZone(zone)
                .format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA))
        }.getOrElse {
            runCatching {
                LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))
            }.getOrElse { value }
        }
    }

}

class DiaryDetailDbViewModelFactory(
    private val context: Context,
    private val entryId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiaryDetailDbViewModel(context.applicationContext, entryId) as T
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
