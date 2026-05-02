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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecoding.ui.placeholder.model.DiaryUiModel
import com.vibecoding.ui.placeholder.theme.PlaceholderColors

@Composable
fun DiaryListScreen(
    diaries: List<DiaryUiModel>,
    onDiaryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("今天 · 5月2日", color = PlaceholderColors.SecondaryText)
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PlaceholderColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("搜索占位", modifier = Modifier.padding(14.dp), color = PlaceholderColors.SecondaryText)
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(diaries) { diary ->
                DiaryCard(diary = diary, onClick = { onDiaryClick(diary.id) })
            }
            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun DiaryCard(diary: DiaryUiModel, onClick: () -> Unit) {
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
            Text(diary.title, color = PlaceholderColors.PrimaryText)
            Spacer(modifier = Modifier.height(6.dp))
            Text(diary.preview, color = PlaceholderColors.SecondaryText, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag(text = diary.category)
                diary.tags.forEach { Tag(text = it) }
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
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
