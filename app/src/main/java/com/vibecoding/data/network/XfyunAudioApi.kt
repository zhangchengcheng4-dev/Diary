package com.vibecoding.data.network

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Third-party ASR implementation for Step 9.
 *
 * This calls Xfyun WebSocket ASR directly from Android to validate stable speech-to-text.
 * Category, tags, and article polishing are temporary placeholders until the later AI step.
 */
class XfyunAudioApi(
    private val appId: String,
    private val apiKey: String,
    private val apiSecret: String
) : AudioApi {
    companion object {
        private const val WS_HOST = "iat-api.xfyun.cn"
        private const val WS_PATH = "/v2/iat"
        private const val WS_URL = "wss://$WS_HOST$WS_PATH"
        private const val TARGET_SAMPLE_RATE = 16_000
        private const val MAX_DURATION_SECONDS = 60
        private const val FRAME_BYTES = 1280
        private const val FRAME_INTERVAL_MS = 40L
        private const val WAIT_RESULT_TIMEOUT_MS = 15_000L
    }

    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val wsResults = ConcurrentHashMap<String, ApiEnvelope<AudioJobResult>>()

    override suspend fun uploadAudio(request: UploadAudioRequest): ApiEnvelope<UploadAudioResponse> {
        val file = File(request.fileName)
        if (!file.exists() || file.length() <= 0L) return envelopeError("XFYUN_FILE_NOT_FOUND", "Audio file not found: ${request.fileName}")
        if (appId.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) return envelopeError("XFYUN_CONFIG_MISSING", "WebSocket appId/apiKey/apiSecret missing")
        if (request.durationSeconds > MAX_DURATION_SECONDS) return envelopeError("XFYUN_DURATION_EXCEEDED", "当前版本建议录音控制在 60 秒以内")

        val pcmBytes = runCatching { decodeToPcm16kMono(file) }.getOrElse {
            Log.e("XfyunAudioApi", "audio decode failed", it)
            return envelopeError("XFYUN_PCM_DECODE_FAILED", "音频转PCM失败: ${it.message}")
        }
        Log.i("XfyunAudioApi", "pcm ready bytes=${pcmBytes.size} from=${file.name}")

        val session = runWsAsr(pcmBytes) ?: return envelopeError("XFYUN_WS_FAILED", "WebSocket listen failed")
        if (session.errorCode != 0) return envelopeError("XFYUN_WS_${session.errorCode}", session.errorMessage.ifBlank { "WebSocket ASR failed" })

        val transcript = session.transcriptBuilder.toString().trim()
        if (transcript.isBlank()) return envelopeError("XFYUN_EMPTY_TRANSCRIPT", "WebSocket returned success but transcript is empty; msgCount=${session.receivedMessageCount}, wordCount=${session.parsedWordCount}")

        val jobId = "xfyun-ws-${UUID.randomUUID()}"
        wsResults[jobId] = ApiEnvelope(
            requestId = jobId,
            serverTime = (System.currentTimeMillis() / 1000L).toString(),
            data = AudioJobResult(
                jobId = jobId,
                status = "succeeded",
                transcript = transcript,
                category = "life", // Temporary AI placeholder.
                tags = emptyList(), // Temporary AI placeholder.
                polishedArticle = transcript // Temporary AI placeholder.
            ),
            error = null
        )
        return ApiEnvelope(jobId, (System.currentTimeMillis() / 1000L).toString(), UploadAudioResponse(jobId, jobId), null)
    }

    override suspend fun getAudioJob(jobId: String): ApiEnvelope<AudioJobResult> {
        return wsResults.remove(jobId) ?: envelopeError("XFYUN_JOB_NOT_FOUND", "WebSocket result not found for jobId=$jobId")
    }

    private data class WsSessionResult(
        val transcriptBuilder: StringBuilder = StringBuilder(),
        val segmentBySn: MutableMap<Int, String> = linkedMapOf(),
        var receivedMessageCount: Int = 0,
        var parsedWordCount: Int = 0,
        var errorCode: Int = 0,
        var errorMessage: String = ""
    )

    private suspend fun runWsAsr(pcmBytes: ByteArray): WsSessionResult? = suspendCancellableCoroutine { cont ->
        val wsResult = WsSessionResult()
        val sendCompleted = AtomicBoolean(false)
        val url = buildWsUrl()
        val request = Request.Builder().url(url).build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("XfyunAudioApi", "ws onOpen code=${response.code} url=${maskUrl(url)}")
                Thread {
                    try {
                        sendAudioFrames(webSocket, pcmBytes)
                        sendCompleted.set(true)
                        Log.i("XfyunAudioApi", "ws send complete")
                        Thread.sleep(WAIT_RESULT_TIMEOUT_MS)
                        if (cont.isActive) {
                            wsResult.errorCode = -3
                            wsResult.errorMessage = "wait final result timeout after send complete"
                            Log.e("XfyunAudioApi", "ws wait timeout msgCount=${wsResult.receivedMessageCount}")
                            webSocket.cancel()
                            cont.resume(wsResult)
                        }
                    } catch (t: Throwable) {
                        wsResult.errorCode = -1
                        wsResult.errorMessage = t.message ?: "send frame failed"
                        Log.e("XfyunAudioApi", "ws send failed", t)
                        webSocket.cancel()
                        if (cont.isActive) cont.resume(wsResult)
                    }
                }.start()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                wsResult.receivedMessageCount += 1
                Log.i("XfyunAudioApi", "ws onMessage length=${text.length}")
                val json = JSONObject(text)
                val code = json.optInt("code", -1)
                if (code != 0) {
                    wsResult.errorCode = code
                    wsResult.errorMessage = json.optString("message", "xfyun ws error")
                    Log.e("XfyunAudioApi", "ws error json=$text")
                    return
                }

                val data = json.optJSONObject("data")
                val result = data?.optJSONObject("result")
                val ws = result?.optJSONArray("ws")
                if (ws == null) {
                    Log.w("XfyunAudioApi", "ws result missing data=${data?.toString()}")
                } else {
                    val sn = result.optInt("sn", -1)
                    val pgs = result.optString("pgs", "")
                    val rg = result.optJSONArray("rg")
                    val chunkBuilder = StringBuilder()
                    for (i in 0 until ws.length()) {
                        val wsItem = ws.optJSONObject(i) ?: continue
                        val cw = wsItem.optJSONArray("cw") ?: continue
                        for (j in 0 until cw.length()) {
                            val w = cw.optJSONObject(j)?.optString("w", "").orEmpty()
                            if (w.isNotBlank()) {
                                wsResult.parsedWordCount += 1
                                chunkBuilder.append(w)
                            }
                        }
                    }
                    val chunk = chunkBuilder.toString()
                    if (chunk.isNotBlank() && sn >= 0) wsResult.segmentBySn[sn] = chunk
                    if (pgs == "rpl" && rg != null && rg.length() >= 2) {
                        val start = rg.optInt(0, -1)
                        val end = rg.optInt(1, -1)
                        if (start >= 0 && end >= start) for (k in start..end) if (k != sn) wsResult.segmentBySn.remove(k)
                    }
                    wsResult.transcriptBuilder.clear()
                    wsResult.segmentBySn.toSortedMap().values.forEach { wsResult.transcriptBuilder.append(it) }
                    Log.i("XfyunAudioApi", "ws transcriptBufferLength=${wsResult.transcriptBuilder.length}")
                }

                if ((data?.optInt("status", 1) ?: 1) == 2 && cont.isActive) {
                    Log.i("XfyunAudioApi", "ws final status=2 transcriptLength=${wsResult.transcriptBuilder.length}")
                    webSocket.close(1000, "done")
                    cont.resume(wsResult)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("XfyunAudioApi", "ws onClosing code=$code reason=$reason sendCompleted=${sendCompleted.get()}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("XfyunAudioApi", "ws onClosed code=$code reason=$reason sendCompleted=${sendCompleted.get()}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (wsResult.errorCode == 0) {
                    wsResult.errorCode = -2
                    wsResult.errorMessage = t.message ?: "websocket failure"
                }
                Log.e("XfyunAudioApi", "ws onFailure httpCode=${response?.code} message=${wsResult.errorMessage}", t)
                if (cont.isActive) cont.resume(wsResult)
            }
        }

        val ws = wsClient.newWebSocket(request, listener)
        cont.invokeOnCancellation { ws.cancel() }
    }

    private fun sendAudioFrames(webSocket: WebSocket, pcm: ByteArray) {
        var offset = 0
        var first = true
        var frameNo = 0
        var hasStatus0 = false
        while (offset < pcm.size) {
            val nextOffset = minOf(offset + FRAME_BYTES, pcm.size)
            val chunk = pcm.copyOfRange(offset, nextOffset)
            val status = when {
                first && nextOffset >= pcm.size -> 2
                first -> 0
                nextOffset >= pcm.size -> 2
                else -> 1
            }
            val payload = JSONObject().apply {
                if (status == 0) {
                    put("common", JSONObject().put("app_id", appId))
                    put("business", JSONObject().put("domain", "iat").put("language", "zh_cn").put("accent", "mandarin").put("vad_eos", 2000))
                }
                put("data", JSONObject().put("status", status).put("format", "audio/L16;rate=16000").put("encoding", "raw").put("audio", Base64.encodeToString(chunk, Base64.NO_WRAP)))
            }
            webSocket.send(payload.toString())
            frameNo += 1
            if (status == 0) hasStatus0 = true
            Log.i("XfyunAudioApi", "ws frame sent no=$frameNo status=$status bytes=${chunk.size}")
            first = false
            offset = nextOffset
            Thread.sleep(FRAME_INTERVAL_MS)
        }
        Log.i("XfyunAudioApi", "ws frame summary total=$frameNo hasStatus0=$hasStatus0")
    }

    private fun decodeToPcm16kMono(file: File): ByteArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        var audioTrack = -1
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) { audioTrack = i; break }
        }
        require(audioTrack >= 0) { "No audio track" }
        extractor.selectTrack(audioTrack)
        val format = extractor.getTrackFormat(audioTrack)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: error("mime missing")
        val srcRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else TARGET_SAMPLE_RATE
        val srcChannels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1
        Log.i("XfyunAudioApi", "decode source mime=$mime sampleRate=$srcRate channels=$srcChannels")

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val out = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        val pts = extractor.sampleTime
                        codec.queueInputBuffer(inIndex, 0, sampleSize, pts, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            if (outIndex >= 0) {
                val outBuffer = codec.getOutputBuffer(outIndex)
                if (outBuffer != null && info.size > 0) {
                    val bytes = ByteArray(info.size)
                    outBuffer.position(info.offset)
                    outBuffer.limit(info.offset + info.size)
                    outBuffer.get(bytes)
                    out.write(bytes)
                }
                codec.releaseOutputBuffer(outIndex, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
            }
        }
        runCatching { codec.stop() }
        runCatching { codec.release() }
        runCatching { extractor.release() }

        val decoded = out.toByteArray()
        val srcSamples = shortsFromLittleEndian(decoded)
        val mono = if (srcChannels <= 1) srcSamples else downMixToMono(srcSamples, srcChannels)
        val resampled = if (srcRate == TARGET_SAMPLE_RATE) mono else resampleTo16k(mono, srcRate)
        return littleEndianFromShorts(resampled)
    }

    private fun shortsFromLittleEndian(bytes: ByteArray): ShortArray {
        val count = bytes.size / 2
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val out = ShortArray(count)
        for (i in 0 until count) out[i] = bb.short
        return out
    }

    private fun littleEndianFromShorts(samples: ShortArray): ByteArray {
        val bb = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { bb.putShort(it) }
        return bb.array()
    }

    private fun downMixToMono(interleaved: ShortArray, channels: Int): ShortArray {
        val frames = interleaved.size / channels
        val out = ShortArray(frames)
        var i = 0
        for (f in 0 until frames) {
            var sum = 0
            for (c in 0 until channels) { sum += interleaved[i + c].toInt() }
            out[f] = (sum / channels).toShort()
            i += channels
        }
        return out
    }

    private fun resampleTo16k(src: ShortArray, srcRate: Int): ShortArray {
        if (src.isEmpty() || srcRate <= 0) return src
        val outSize = (src.size.toLong() * TARGET_SAMPLE_RATE / srcRate).toInt().coerceAtLeast(1)
        val out = ShortArray(outSize)
        val ratio = srcRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
        for (i in 0 until outSize) {
            val pos = i * ratio
            val left = pos.toInt().coerceIn(0, src.lastIndex)
            val right = (left + 1).coerceIn(0, src.lastIndex)
            val frac = pos - left
            val v = src[left] * (1.0 - frac) + src[right] * frac
            out[i] = v.toInt().toShort()
        }
        return out
    }

    private fun buildWsUrl(): String {
        val date = rfc1123Now()
        val signatureOrigin = "host: $WS_HOST\ndate: $date\nGET $WS_PATH HTTP/1.1"
        val signature = hmacSha256Base64(signatureOrigin, apiSecret)
        val authOrigin = "api_key=\"$apiKey\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"$signature\""
        val auth = Base64.encodeToString(authOrigin.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        val url = "$WS_URL?authorization=${URLEncoder.encode(auth, "UTF-8")}&date=${URLEncoder.encode(date, "UTF-8")}&host=${URLEncoder.encode(WS_HOST, "UTF-8")}"
        Log.i("XfyunAudioApi", "ws auth host=$WS_HOST path=$WS_PATH date=$date apiKey=${maskKey(apiKey)} authLen=${auth.length} url=${maskUrl(url)}")
        return url
    }

    private fun maskUrl(url: String): String {
        val authIdx = url.indexOf("authorization=")
        if (authIdx < 0) return url
        val end = url.indexOf('&', authIdx).let { if (it < 0) url.length else it }
        return url.substring(0, authIdx) + "authorization=***" + url.substring(end)
    }

    private fun maskKey(key: String): String = if (key.length <= 6) "***" else "${key.take(3)}***${key.takeLast(3)}"

    private fun hmacSha256Base64(content: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.encodeToString(mac.doFinal(content.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun rfc1123Now(): String {
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("GMT")
        return formatter.format(Date())
    }

    private fun <T> envelopeError(code: String, message: String): ApiEnvelope<T> = ApiEnvelope(
        requestId = "xfyun-error",
        serverTime = (System.currentTimeMillis() / 1000L).toString(),
        data = null,
        error = ApiError(code = code, message = message, retryable = true)
    )
}
