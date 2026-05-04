package com.deliverysdk.calendaragent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.deliverysdk.calendaragent.calendar.CalendarService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 注入 ContentResolver 到 CalendarService
        val calendarService = CalendarService()
        calendarService.setContentResolver(contentResolver)

        setContent {
            App()
        }
    }
}
