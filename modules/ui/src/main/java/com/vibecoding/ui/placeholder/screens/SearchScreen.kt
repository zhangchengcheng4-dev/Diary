package com.vibecoding.ui.placeholder.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibecoding.ui.placeholder.model.DiaryUiModel
import com.vibecoding.ui.placeholder.theme.PlaceholderColors

@Composable
fun SearchScreen(diaries: List<DiaryUiModel>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PlaceholderColors.Surface)
        ) {
            Text("搜索框占位", modifier = Modifier.padding(14.dp), color = PlaceholderColors.SecondaryText)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(diaries) { diary ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PlaceholderColors.Surface)
                ) {
                    Text(
                        text = "${diary.title} - ${diary.preview}",
                        modifier = Modifier.padding(12.dp),
                        color = PlaceholderColors.SecondaryText
                    )
                }
            }
        }
    }
}
