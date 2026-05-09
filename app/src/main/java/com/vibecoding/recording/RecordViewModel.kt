package com.vibecoding.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val MAX_DURATION_MS = 30 * 60 * 1000L

enum class RecordStatus {
    IDLE, RECORDING, PAUSED, STOPPED, ERROR
}

data class RecordUiState(
    val status: RecordStatus = RecordStatus.IDLE,
    val elapsedMs: Long = 0L,
    val draftEntryId: String? = null,
    val permissionDenied: Boolean = false,
    val message: String? = null
)

class RecordViewModel(
    context: Context
) : ViewModel() {
    private val repository = RecordingRepository(context)
    private val recorder = AudioRecorder(File(context.filesDir, "recordings"))
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private val segments = mutableListOf<RecordingSegment>()
    private var tickJob: Job? = null
    private var currentSegmentStartedAtMs: Long = 0L
    private var elapsedBeforeCurrentSegmentMs: Long = 0L

    fun onPermissionDenied() {
        cleanupDraftOnFailure("需要麦克风权限才能录音")
        _uiState.update {
            it.copy(
                permissionDenied = true,
                status = RecordStatus.IDLE,
                message = "麦克风权限被拒绝"
            )
        }
    }

    fun startRecording() {
        if (_uiState.value.status == RecordStatus.RECORDING) return
        viewModelScope.launch {
            try {
                val draftId = _uiState.value.draftEntryId ?: repository.createDraft(RecordingRepository.MVP_LOCAL_USER_ID)
                recorder.startNewSegment()
                currentSegmentStartedAtMs = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        draftEntryId = draftId,
                        status = RecordStatus.RECORDING,
                        permissionDenied = false,
                        message = null
                    )
                }
                startTicker()
            } catch (_: Exception) {
                cleanupDraftOnFailure("录音启动失败")
                _uiState.update { it.copy(status = RecordStatus.ERROR, message = "录音启动失败") }
            }
        }
    }

    fun pauseRecording() {
        if (_uiState.value.status != RecordStatus.RECORDING) return
        val segment = recorder.stopCurrentSegment()
        if (segment == null) {
            cleanupDraftOnFailure("录音暂停失败")
            _uiState.update { it.copy(status = RecordStatus.ERROR, message = "录音暂停失败") }
            return
        }
        segments += segment
        elapsedBeforeCurrentSegmentMs += segment.durationMs
        stopTicker()
        _uiState.update { it.copy(status = RecordStatus.PAUSED, elapsedMs = elapsedBeforeCurrentSegmentMs) }
    }

    fun resumeRecording() {
        if (_uiState.value.status != RecordStatus.PAUSED) return
        try {
            recorder.startNewSegment()
            currentSegmentStartedAtMs = System.currentTimeMillis()
            _uiState.update { it.copy(status = RecordStatus.RECORDING, message = null) }
            startTicker()
        } catch (_: Exception) {
            cleanupDraftOnFailure("录音继续失败")
            _uiState.update { it.copy(status = RecordStatus.ERROR, message = "录音继续失败") }
        }
    }

    fun stopRecording(onCompleted: (String) -> Unit) {
        val currentStatus = _uiState.value.status
        if (currentStatus != RecordStatus.RECORDING && currentStatus != RecordStatus.PAUSED) return
        viewModelScope.launch {
            try {
                if (currentStatus == RecordStatus.RECORDING) {
                    val lastSegment = recorder.stopCurrentSegment()
                    if (lastSegment == null) {
                        cleanupDraftOnFailure("录音停止失败")
                        _uiState.update { it.copy(status = RecordStatus.ERROR, message = "录音保存失败") }
                        return@launch
                    }
                    segments += lastSegment
                    elapsedBeforeCurrentSegmentMs += lastSegment.durationMs
                }
                stopTicker()
                val entryId = _uiState.value.draftEntryId ?: throw IllegalStateException("Draft is missing")
                if (segments.isEmpty()) {
                    cleanupDraftOnFailure("录音内容为空")
                    _uiState.update { it.copy(status = RecordStatus.ERROR, message = "录音内容为空") }
                    return@launch
                }
                repository.saveSegmentsAndFinalize(
                    userId = RecordingRepository.MVP_LOCAL_USER_ID,
                    entryId = entryId,
                    segments = segments.toList()
                )
                _uiState.update {
                    it.copy(
                        status = RecordStatus.STOPPED,
                        elapsedMs = elapsedBeforeCurrentSegmentMs,
                        message = "录音已保存，正在上传并处理"
                    )
                }
                onCompleted(entryId)
                resetRuntimeStateKeepDraft()
            } catch (_: Exception) {
                cleanupDraftOnFailure("录音停止失败")
                _uiState.update { it.copy(status = RecordStatus.ERROR, message = "录音保存失败") }
            }
        }
    }

    fun clearDeniedHint() {
        _uiState.update { it.copy(permissionDenied = false) }
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(200)
                val running = (System.currentTimeMillis() - currentSegmentStartedAtMs).coerceAtLeast(0L)
                val total = elapsedBeforeCurrentSegmentMs + running
                _uiState.update { it.copy(elapsedMs = total) }
                if (total >= MAX_DURATION_MS) {
                    stopRecording {}
                    break
                }
            }
        }
    }

    private fun stopTicker() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun cleanupDraftOnFailure(msg: String) {
        recorder.abortAndDeleteCurrentSegment()
        stopTicker()
        val draftId = _uiState.value.draftEntryId
        if (draftId != null) {
            viewModelScope.launch { repository.clearDraftAndFiles(draftId) }
        }
        segments.clear()
        elapsedBeforeCurrentSegmentMs = 0L
        currentSegmentStartedAtMs = 0L
        _uiState.update {
            it.copy(
                status = RecordStatus.IDLE,
                elapsedMs = 0L,
                draftEntryId = null,
                message = msg
            )
        }
    }

    private fun resetRuntimeStateKeepDraft() {
        segments.clear()
        elapsedBeforeCurrentSegmentMs = 0L
        currentSegmentStartedAtMs = 0L
        _uiState.update { it.copy(draftEntryId = null) }
    }
}

class RecordViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RecordViewModel(context.applicationContext) as T
    }
}
