package com.vibecoding.ui.placeholder.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecoding.ui.placeholder.model.DiaryUiModel
import com.vibecoding.ui.placeholder.state.DateField
import com.vibecoding.ui.placeholder.state.DateRangeUi
import com.vibecoding.ui.placeholder.theme.PlaceholderColors

@Composable
fun SearchScreen(
    diaries: List<DiaryUiModel>,
    tagOptions: List<String>,
    query: String = "",
    selectedTag: String = "All",
    dateRange: DateRangeUi = DateRangeUi(),
    onQueryChange: (String) -> Unit = {},
    onTagSelect: (String) -> Unit = {},
    onDateRangeClick: (DateField) -> Unit = {},
    onDiaryClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SearchBarField(query = query, onQueryChange = onQueryChange)
        SearchFilterSection(
            tags = tagOptions,
            selectedTag = selectedTag,
            dateRange = dateRange,
            onTagSelect = onTagSelect,
            onDateRangeClick = onDateRangeClick
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(diaries) { diary ->
                CompactResultCard(
                    diary = diary,
                    onClick = { onDiaryClick(diary.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SearchBarField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text("搜索日记标题或内容", color = PlaceholderColors.SecondaryText)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PlaceholderColors.Surface,
            unfocusedContainerColor = PlaceholderColors.Surface,
            focusedBorderColor = PlaceholderColors.Accent,
            unfocusedBorderColor = PlaceholderColors.TagBackground,
            focusedTextColor = PlaceholderColors.PrimaryText,
            unfocusedTextColor = PlaceholderColors.PrimaryText,
            cursorColor = PlaceholderColors.Accent
        )
    )
}

@Composable
private fun SearchFilterSection(
    tags: List<String>,
    selectedTag: String,
    dateRange: DateRangeUi,
    onTagSelect: (String) -> Unit,
    onDateRangeClick: (DateField) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags) { tag ->
                SelectableFilterTag(
                    text = tag,
                    selected = selectedTag == tag,
                    onClick = { onTagSelect(tag) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateChip(
                text = dateRange.start ?: "开始日期",
                onClick = { onDateRangeClick(DateField.Start) }
            )
            DateChip(
                text = dateRange.end ?: "结束日期",
                onClick = { onDateRangeClick(DateField.End) }
            )
        }
    }
}

@Composable
private fun DateChip(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = PlaceholderColors.SecondaryText,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PlaceholderColors.TagBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun SelectableFilterTag(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) PlaceholderColors.Accent else PlaceholderColors.TagBackground
    val textColor = if (selected) PlaceholderColors.Surface else PlaceholderColors.SecondaryText

    Text(
        text = text,
        color = textColor,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun CompactResultCard(
    diary: DiaryUiModel,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PlaceholderColors.Surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = diary.title,
                color = PlaceholderColors.PrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = diary.preview,
                color = PlaceholderColors.SecondaryText,
                fontSize = 13.sp,
                maxLines = 2
            )
        }
    }
}
