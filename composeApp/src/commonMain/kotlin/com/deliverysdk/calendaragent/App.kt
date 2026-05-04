package com.deliverysdk.calendaragent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deliverysdk.calendaragent.features.event_input.EventInputScreen
import com.deliverysdk.calendaragent.features.event_preview.EventPreviewScreen
import com.deliverysdk.calendaragent.features.history.HistoryScreen
import com.deliverysdk.calendaragent.model.ParsedEvent
import com.deliverysdk.calendaragent.navigation.Screen
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun App(
    calendarService: com.deliverysdk.calendaragent.calendar.CalendarService = koinInject(),
) {
    val navController = rememberNavController()
    var parsedEvent by remember { mutableStateOf<ParsedEvent?>(null) }

    MaterialTheme(
        colorScheme = darkColorScheme(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Input.route,
            ) {
                composable(Screen.Input.route) {
                    EventInputScreen(
                        onNavigateToPreview = { event ->
                            parsedEvent = event
                            navController.navigate(Screen.Preview.route)
                        },
                        onNavigateToHistory = {
                            navController.navigate(Screen.History.route)
                        },
                    )
                }

                composable(Screen.Preview.route) {
                    val event = parsedEvent ?: run {
                        navController.navigateUp()
                        return@composable
                    }
                    EventPreviewScreen(
                        event = event,
                        calendarService = calendarService,
                        onBack = { navController.navigateUp() },
                        onSaved = {
                            navController.navigate(Screen.Input.route) {
                                popUpTo(Screen.Input.route) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        onBack = { navController.navigateUp() },
                    )
                }
            }
        }
    }
}
