package com.vibecoding.recording

import android.media.MediaRecorder
import android.os.SystemClock
import java.io.File
import java.util.UUID

class AudioRecorder(
    private val recordingsDir: File
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startedAtElapsedMs: Long = 0L

    fun startNewSegment() {
        recordingsDir.mkdirs()
        val file = File(recordingsDir, "rec_${System.currentTimeMillis()}_${UUID.randomUUID()}.m4a")
        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        currentFile = file
        mediaRecorder = recorder
        startedAtElapsedMs = SystemClock.elapsedRealtime()
    }

    fun stopCurrentSegment(): RecordingSegment? {
        val recorder = mediaRecorder ?: return null
        val file = currentFile ?: return null
        return runCatching {
            recorder.stop()
            val durationMs = (SystemClock.elapsedRealtime() - startedAtElapsedMs).coerceAtLeast(0L)
            RecordingSegment(
                audioAssetId = UUID.randomUUID().toString(),
                localPath = file.absolutePath,
                durationMs = durationMs,
                fileSizeBytes = file.length(),
                mimeType = "audio/mp4"
            )
        }.getOrNull().also {
            recorder.release()
            mediaRecorder = null
            currentFile = null
            startedAtElapsedMs = 0L
        }
    }

    fun abortAndDeleteCurrentSegment() {
        val recorder = mediaRecorder
        val file = currentFile
        runCatching {
            recorder?.stop()
        }
        runCatching { recorder?.release() }
        runCatching { file?.delete() }
        mediaRecorder = null
        currentFile = null
        startedAtElapsedMs = 0L
    }
}
