package com.vibecoding.diary

data class DiaryAiResult(
    val title: String,
    val polishedArticle: String,
    val primaryCategoryId: String,
    val dynamicTags: List<String>
)

interface DiaryAiProcessor {
    suspend fun process(transcript: String): DiaryAiResult
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
            lower.containsAny("学习", "课程", "读书", "考试", "复习", "study", "book") -> "study"
            lower.containsAny("运动", "跑步", "睡眠", "身体", "健康", "医院", "health", "run") -> "health"
            lower.containsAny("开心", "难过", "焦虑", "生气", "情绪", "心情", "emotion") -> "emotion"
            else -> "life"
        }
    }

    private fun buildTags(transcript: String): List<String> {
        val lower = transcript.lowercase()
        val tags = mutableListOf<String>()
        if (lower.containsAny("工作", "会议", "项目", "work", "meeting")) tags += "工作"
        if (lower.containsAny("学习", "读书", "课程", "study", "book")) tags += "学习"
        if (lower.containsAny("运动", "跑步", "健身", "run")) tags += "运动"
        if (lower.containsAny("朋友", "家人", "聚餐", "family", "friend")) tags += "生活"
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
