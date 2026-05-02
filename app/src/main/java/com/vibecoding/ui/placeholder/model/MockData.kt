package com.vibecoding.ui.placeholder.model

data class DiaryUiModel(
    val id: String,
    val title: String,
    val preview: String,
    val dateText: String,
    val category: String,
    val tags: List<String>
)

object MockData {
    val diaries = listOf(
        DiaryUiModel(
            id = "1",
            title = "晚饭后的散步",
            preview = "今天吃完饭后在小区散步，风很舒服，心情慢慢平静下来。",
            dateText = "5月2日 周六",
            category = "mood",
            tags = listOf("#散步", "#放松")
        ),
        DiaryUiModel(
            id = "2",
            title = "读书一小时",
            preview = "晚上读了一个小时技术书，整理了几条关键笔记。",
            dateText = "5月1日 周五",
            category = "reading",
            tags = listOf("#读书", "#笔记")
        )
    )

    val profileStats = listOf(
        "本月日记: 12",
        "连续记录: 5天",
        "常用分类: 心情"
    )

    val profileSettings = listOf(
        "账号与安全",
        "通知设置",
        "隐私设置",
        "关于应用"
    )
}
