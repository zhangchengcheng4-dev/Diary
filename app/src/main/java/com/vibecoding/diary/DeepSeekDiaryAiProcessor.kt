package com.vibecoding.diary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DeepSeekDiaryAiProcessor(
    private val apiKey: String,
    private val baseUrl: String,
    private val model: String,
    private val client: OkHttpClient = defaultClient()
) : DiaryAiProcessor {
    override suspend fun process(transcript: String): DiaryAiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) error("DeepSeek API key is missing")
        val endpoint = baseUrl.trimEnd('/') + "/chat/completions"
        val payload = buildRequestPayload(transcript)
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", JSON_MEDIA_TYPE.toString())
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("DeepSeek HTTP ${response.code}")
            }
            parseResponse(bodyText)
        }
    }

    private fun buildRequestPayload(transcript: String): JSONObject {
        return JSONObject()
            .put("model", model)
            .put("stream", false)
            .put("temperature", 0.4)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("response_format", JSONObject().put("type", "json_object"))
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", DiaryAiPromptFactory.systemPrompt()))
                    .put(JSONObject().put("role", "user").put("content", DiaryAiPromptFactory.userPrompt(transcript)))
            )
    }

    private fun parseResponse(bodyText: String): DiaryAiResult {
        val root = JSONObject(bodyText)
        val choices = root.optJSONArray("choices") ?: error("DeepSeek response missing choices")
        val first = choices.optJSONObject(0) ?: error("DeepSeek response missing first choice")
        val content = first
            .optJSONObject("message")
            ?.optString("content")
            ?.takeIf { it.isNotBlank() }
            ?: error("DeepSeek response missing message content")
        val result = JSONObject(content)
        return DiaryAiResult(
            title = result.optString("title").trim(),
            polishedArticle = result.optString("polishedArticle").trim(),
            primaryCategoryId = result.optString("primaryCategoryId").trim(),
            dynamicTags = result.optJSONArray("dynamicTags").toStringList()
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length())
            .mapNotNull { index -> optString(index).trim().takeIf { it.isNotBlank() } }
    }

    private companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }
}
