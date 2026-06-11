package com.example.sharescreen.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Room : Screen("room")
    data object CreateOrJoin : Screen("createOrJoin")
    data object HostRoom : Screen("host_room/{roomCode}/{userName}") {
        fun createRoute(roomCode: String, userName: String): String =
            "host_room/$roomCode/${URLEncoder.encode(userName, "UTF-8")}"
    }
    data object ViewerRoom : Screen("viewer_room/{roomCode}/{userName}") {
        fun createRoute(roomCode: String, userName: String): String =
            "viewer_room/$roomCode/${URLEncoder.encode(userName, "UTF-8")}"
    }
}
