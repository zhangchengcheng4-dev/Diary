package com.vibecoding.ui.placeholder.state

enum class DateField {
    Start,
    End
}

data class DateRangeUi(
    val start: String? = null,
    val end: String? = null
)
