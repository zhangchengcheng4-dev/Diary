package com.vibecoding.diary

import com.vibecoding.app.BuildConfig

data class DiaryAiResult(
    val title: String,
    val polishedArticle: String,
    val primaryCategoryId: String,
    val dynamicTags: List<String>
)

interface DiaryAiProcessor {
    suspend fun process(transcript: String): DiaryAiResult
}

fun createDefaultDiaryAiProcessor(): DiaryAiProcessor {
    return if (BuildConfig.DEEPSEEK_USE_FAKE) {
        FakeDiaryAiProcessor()
    } else {
        DeepSeekDiaryAiProcessor(
            apiKey = BuildConfig.DEEPSEEK_API_KEY,
            baseUrl = BuildConfig.DEEPSEEK_BASE_URL,
            model = BuildConfig.DEEPSEEK_MODEL
        )
    }
}

class FakeDiaryAiProcessor : DiaryAiProcessor {
    override suspend fun process(transcript: String): DiaryAiResult {
        val normalized = transcript.trim()
        val category = inferCategory(normalized)
        return DiaryAiResult(
            title = buildTitle(normalized),
            polishedArticle = polish(normalized),
            primaryCategoryId = category,
            dynamicTags = buildTags(normalized)
        )
    }

    private fun buildTitle(transcript: String): String {
        return transcript
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(MAX_TITLE_LENGTH)
            ?.ifBlank { null }
            ?: "语音日记"
    }

    private fun polish(transcript: String): String {
        return transcript
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")
            .ifBlank { transcript.trim() }
    }

    private fun inferCategory(transcript: String): String {
        val lower = transcript.lowercase()
        return when {
            lower.containsAny("工作", "会议", "项目", "需求", "上线", "bug", "work", "meeting") -> "work"
            lower.containsAny("读书", "阅读", "小说", "课程", "学习", "reading", "book") -> "reading"
            lower.containsAny("吃饭", "晚饭", "午饭", "早餐", "餐厅", "咖啡", "food") -> "food"
            lower.containsAny("运动", "跑步", "健身", "篮球", "游泳", "sports", "run") -> "sports"
            lower.containsAny("电影", "音乐", "游戏", "综艺", "娱乐", "entertainment") -> "entertainment"
            lower.containsAny("开心", "难过", "焦虑", "生气", "情绪", "心情", "疲惫", "mood") -> "mood"
            else -> "mood"
        }
    }

    private fun buildTags(transcript: String): List<String> {
        val lower = transcript.lowercase()
        val tags = mutableListOf<String>()
        if (lower.containsAny("工作", "会议", "项目", "work", "meeting")) tags += "工作"
        if (lower.containsAny("学习", "读书", "阅读", "课程", "book")) tags += "阅读"
        if (lower.containsAny("吃饭", "晚饭", "午饭", "早餐", "咖啡")) tags += "美食"
        if (lower.containsAny("运动", "跑步", "健身", "run")) tags += "运动"
        if (lower.containsAny("电影", "音乐", "游戏", "综艺")) tags += "娱乐"
        if (lower.containsAny("开心", "难过", "焦虑", "心情")) tags += "情绪"
        return tags.distinct().take(MAX_TAG_COUNT)
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { contains(it) }
    }

    private companion object {
        private const val MAX_TITLE_LENGTH = 28
        private const val MAX_TAG_COUNT = 5
    }
}
