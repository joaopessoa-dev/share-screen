package com.example.sharescreen.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdUnits
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sharescreen.data.BottomNavItem
import com.example.sharescreen.ui.HomeScreen
import com.example.sharescreen.ui.player.CreateOrJoinRoom
import com.example.sharescreen.ui.room.RoomScreen


val BottomNavList = listOf(
    BottomNavItem(Screen.Home,"home", Icons.Filled.Home),
    BottomNavItem(Screen.Room, "room", Icons.Filled.Airplay),
    BottomNavItem(Screen.CreateOrJoin, "createOrJoin", Icons.Filled.AdUnits)
)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route


    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Room.route,
        Screen.CreateOrJoin.route,
    )

    Scaffold (
        bottomBar = {
            NavigationBar {
                val current = navBackStackEntry?.destination

                BottomNavList.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected =  current?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }

                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            Screen.Home.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController)
            }

            composable(Screen.Room.route) {
                RoomScreen(navController)
            }

            composable(Screen.CreateOrJoin.route) {
                CreateOrJoinRoom(navController)
            }
        }

    }


}