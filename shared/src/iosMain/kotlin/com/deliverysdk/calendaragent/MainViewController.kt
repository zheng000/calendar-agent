package com.deliverysdk.calendaragent

import com.deliverysdk.calendaragent.di.appModule
import org.koin.core.context.startKoin

/**
 * iOS 端 Koin 初始化入口
 * 在 iOS App 启动时调用
 */
fun initKoin() {
    try {
        startKoin {
            modules(appModule)
        }
    } catch (e: org.koin.core.error.KoinApplicationAlreadyStartedException) {
        // Already started, ignore
    }
}
