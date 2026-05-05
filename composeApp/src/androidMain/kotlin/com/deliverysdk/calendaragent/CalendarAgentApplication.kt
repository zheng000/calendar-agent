package com.deliverysdk.calendaragent

import android.app.Application
import com.deliverysdk.calendaragent.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CalendarAgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@CalendarAgentApplication)
            modules(appModule(this@CalendarAgentApplication))
        }
    }
}
