package com.rmconnect.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rmconnect.app.UsbSerialManager
import com.rmconnect.app.ui.screens.CalibrationScreen
import com.rmconnect.app.ui.screens.ElrsScreen
import com.rmconnect.app.ui.screens.MainScreen
import com.rmconnect.app.ui.screens.SettingsScreen
import com.rmconnect.app.ProtocolParser

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Calibration : Screen("calibration")
    object Settings : Screen("settings")
    object Elrs : Screen("elrs")
}

@Composable
fun RMNavHost(
    usbManager: UsbSerialManager,
    navController: NavHostController = rememberNavController()
) {
    // Shared State for ELRS (Hoisted to maintain state across nav)
    // Actually, we can reuse the ViewModels later. For now, pass state.
    
    // We need to move the state hoisting from MainActivity here or keep it in MainActivity and pass down?
    // Let's pass usbManager and handled data inside screens or hoist buffers here?
    // The previous MainActivity had `elrsParams` and `elrsBuffer` logic.
    // We should probably move that logic into a ViewModel or keep in MainActivity and pass to MainScreen/ElrsScreen.
    // For this refactor step, I will assume MainActivity sets up the host and data is accessible.
    
    // However, MainScreen needs to navigate.
    
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            MainScreen(
                usbManager = usbManager,
                onNavigateToCalibration = { navController.navigate(Screen.Calibration.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToElrs = { navController.navigate(Screen.Elrs.route) }
            )
        }
        
        composable(Screen.Calibration.route) {
            CalibrationScreen(
                usbManager = usbManager,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
             // We need current volume. 
             // Ideally we ask UsbManager for cached info. 
             // Doing a dirty read: SettingsScreen asks for it or uses default.
             SettingsScreen(
                usbManager = usbManager,
                initialVolume = 0, // Todo: Get real one
                onBack = { navController.popBackStack() }
             )
        }
        
        composable(Screen.Elrs.route) {
            // State for ELRS params needs to be persisted or re-fetched.
            // Let's let ElrsScreen handle its own data fetching for now to maintain self-containment.
            ElrsScreen(
                usbManager = usbManager,
                parameters = emptyList(), // Todo: Refactor this to be self-fetching or ViewModel based
                onBack = { navController.popBackStack() }
            )
        }
    }
}
