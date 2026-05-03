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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecoding.ui.placeholder.model.DiaryUiModel
import com.vibecoding.ui.placeholder.theme.PlaceholderColors

@Composable
fun DiaryListScreen(
    diaries: List<DiaryUiModel>,
    selectedTag: String = "All",
    filterTags: List<String> = listOf("All", "Mood", "Food", "Work", "Sports"),
    onTagSelect: (String) -> Unit = {},
    onDiaryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "今天 · 5月2日",
            color = PlaceholderColors.SecondaryText,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        TagFilterRow(
            tags = filterTags,
            selectedTag = selectedTag,
            onTagSelect = onTagSelect
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(diaries) { diary ->
                CozyDiaryCard(diary = diary, onClick = { onDiaryClick(diary.id) })
            }
            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<String>,
    selectedTag: String,
    onTagSelect: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tags) { tag ->
            SelectableTag(
                text = tag,
                selected = selectedTag == tag,
                onClick = { onTagSelect(tag) }
            )
        }
    }
}

@Composable
private fun CozyDiaryCard(diary: DiaryUiModel, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PlaceholderColors.Surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(diary.dateText, color = PlaceholderColors.SecondaryText, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = diary.title,
                color = PlaceholderColors.PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(diary.preview, color = PlaceholderColors.SecondaryText, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SoftTag(text = diary.category)
                diary.tags.take(2).forEach { SoftTag(text = it) }
            }
        }
    }
}

@Composable
private fun SelectableTag(
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
private fun SoftTag(text: String) {
    Text(
        text = text,
        color = PlaceholderColors.SecondaryText,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PlaceholderColors.TagBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
