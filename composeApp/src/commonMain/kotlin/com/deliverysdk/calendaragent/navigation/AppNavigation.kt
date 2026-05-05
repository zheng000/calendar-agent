package com.deliverysdk.calendaragent.navigation

sealed class Screen(val route: String) {
    data object Input : Screen("input")
    data object Preview : Screen("preview")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
