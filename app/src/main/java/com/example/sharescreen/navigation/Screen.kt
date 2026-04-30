package com.example.sharescreen.navigation

sealed class Screen(val route: String) {

    data object Home : Screen("home")
    data object Room : Screen("room")
    data object CreateOrJoin : Screen("createOrJoin")
}