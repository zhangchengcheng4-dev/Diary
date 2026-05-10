package com.vibecoding.diary

object DiaryAiPromptFactory {
    private const val CATEGORY_LIST = "reading, food, mood, work, sports, entertainment"

    fun systemPrompt(): String {
        return """
            You are a diary writing assistant. Convert a speech transcript into a diary JSON object.
            Keep the writing natural and close to the speaker's tone. Do not make it sound like an AI summary.
            Do not invent facts, people, places, actions, feelings, or details that are not supported by the transcript.
            Return only one valid JSON object. Do not include markdown, code fences, explanations, or extra text.
        """.trimIndent()
    }

    fun userPrompt(transcript: String): String {
        return """
            Transcript:
            $transcript

            Generate this exact JSON shape:
            {
              "title": "short diary title",
              "polishedArticle": "natural polished diary article",
              "primaryCategoryId": "one category id",
              "dynamicTags": ["tag1", "tag2"]
            }

            Rules:
            - primaryCategoryId must be exactly one of: $CATEGORY_LIST
            - dynamicTags can contain at most 5 items
            - dynamicTags must not contain duplicates
            - dynamicTags must not contain the selected primaryCategoryId or category words
            - dynamicTags should be short display tags, preferably Chinese if the transcript is Chinese
            - title must be concise and based only on the transcript
            - polishedArticle must preserve the original meaning and oral feeling
            - polishedArticle must not add details that are not in the transcript
            - Return JSON only
        """.trimIndent()
    }
}
