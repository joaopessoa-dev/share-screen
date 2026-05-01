package com.example.sharescreen.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val screen : Screen,
    val label : String,
    val icon : ImageVector
)