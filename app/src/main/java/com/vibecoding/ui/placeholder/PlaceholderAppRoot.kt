package com.vibecoding.ui.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vibecoding.ui.placeholder.model.MockData
import com.vibecoding.ui.placeholder.screens.CalendarScreen
import com.vibecoding.ui.placeholder.screens.DiaryDetailScreen
import com.vibecoding.ui.placeholder.screens.DiaryListScreen
import com.vibecoding.ui.placeholder.screens.ProcessingScreen
import com.vibecoding.ui.placeholder.screens.ProfileScreen
import com.vibecoding.ui.placeholder.screens.RecordScreen
import com.vibecoding.ui.placeholder.screens.SearchScreen
import com.vibecoding.ui.placeholder.theme.PlaceholderColors

@Composable
fun PlaceholderAppRoot(
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()

    Box(modifier = Modifier.background(PlaceholderColors.Background)) {
        Scaffold(
            modifier = Modifier.background(PlaceholderColors.Background),
            bottomBar = {
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination
                NavigationBar {
                    val leftItems = listOf(
                        PlaceholderRoute.Home to "日记",
                        PlaceholderRoute.Search to "搜索"
                    )
                    val rightItems = listOf(
                        PlaceholderRoute.Calendar to "日历",
                        PlaceholderRoute.Profile to "我的"
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        leftItems.forEach { (route, title) ->
                            NavigationBarItem(
                                selected = currentDestination.isInTopRoute(route.route),
                                onClick = { navController.navigate(route.route) },
                                icon = { Text("•") },
                                label = { Text(title) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Box(modifier = Modifier.weight(1f))
                        rightItems.forEach { (route, title) ->
                            NavigationBarItem(
                                selected = currentDestination.isInTopRoute(route.route),
                                onClick = { navController.navigate(route.route) },
                                icon = { Text("•") },
                                label = { Text(title) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = PlaceholderRoute.Home.route,
                modifier = Modifier.padding(padding).padding(horizontal = 12.dp)
            ) {
                composable(PlaceholderRoute.Home.route) {
                    DiaryListScreen(
                        diaries = MockData.diaries,
                        onDiaryClick = { id -> navController.navigate(PlaceholderRoute.Detail.withId(id)) }
                    )
                }
                composable(PlaceholderRoute.Record.route) {
                    RecordScreen(
                        onFinish = { navController.navigate(PlaceholderRoute.Processing.route) }
                    )
                }
                composable(PlaceholderRoute.Processing.route) {
                    ProcessingScreen(
                        onDone = { navController.navigate(PlaceholderRoute.Detail.withId(MockData.diaries.first().id)) }
                    )
                }
                composable(
                    route = PlaceholderRoute.Detail.route,
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { backStack ->
                    val id = backStack.arguments?.getString("id").orEmpty()
                    val diary = MockData.diaries.firstOrNull { it.id == id } ?: MockData.diaries.first()
                    DiaryDetailScreen(diary = diary)
                }
                composable(PlaceholderRoute.Calendar.route) { CalendarScreen() }
                composable(PlaceholderRoute.Search.route) {
                    SearchScreen(
                        diaries = MockData.diaries,
                        tagOptions = listOf("All", "Mood", "Food", "Work", "Sports")
                    )
                }
                composable(PlaceholderRoute.Profile.route) {
                    ProfileScreen(
                        stats = MockData.profileStats,
                        settings = MockData.profileSettings,
                        onLogout = onLogout
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate(PlaceholderRoute.Record.route) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .offset(y = (-24).dp)
                .size(68.dp)
                .clip(CircleShape),
            shape = CircleShape,
            containerColor = PlaceholderColors.Accent
        ) {
            Text(
                text = "麦克风",
                color = PlaceholderColors.Background
            )
        }
    }
}

private fun NavDestination?.isInTopRoute(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}
