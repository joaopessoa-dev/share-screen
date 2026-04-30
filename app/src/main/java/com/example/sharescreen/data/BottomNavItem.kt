package com.example.sharescreen.data

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.sharescreen.navigation.Screen

data class BottomNavItem(
    val screen : Screen,
    val label : String,
    val icon : ImageVector
)

