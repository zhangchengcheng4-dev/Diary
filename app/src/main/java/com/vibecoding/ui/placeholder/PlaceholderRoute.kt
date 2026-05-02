package com.vibecoding.ui.placeholder

sealed class PlaceholderRoute(val route: String) {
    data object Home : PlaceholderRoute("home")
    data object Record : PlaceholderRoute("record")
    data object Processing : PlaceholderRoute("processing")
    data object Detail : PlaceholderRoute("detail/{id}") {
        fun withId(id: String): String = "detail/$id"
    }
    data object Calendar : PlaceholderRoute("calendar")
    data object Search : PlaceholderRoute("search")
    data object Profile : PlaceholderRoute("profile")
}
