package com.vibecoding.ui.placeholder.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibecoding.ui.placeholder.theme.PlaceholderColors

@Composable
fun ProfileScreen(
    stats: List<String>,
    settings: List<String>,
    onLogout: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("我的", color = PlaceholderColors.PrimaryText)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PlaceholderColors.Surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("统计", color = PlaceholderColors.PrimaryText)
                stats.forEach { Text(it, color = PlaceholderColors.SecondaryText) }
            }
        }
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PlaceholderColors.Surface)
        ) {
            LazyColumn(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("设置", color = PlaceholderColors.PrimaryText) }
                items(settings) { item ->
                    Text(item, color = PlaceholderColors.SecondaryText)
                }
            }
        }
        Button(onClick = onLogout) {
            Text("退出登录")
        }
    }
}
