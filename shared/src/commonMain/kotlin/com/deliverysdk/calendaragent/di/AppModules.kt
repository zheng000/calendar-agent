package com.deliverysdk.calendaragent.di

import co.touchlab.kermit.Logger
import com.deliverysdk.calendaragent.calendar.CalendarService
import com.deliverysdk.calendaragent.parser.EventParser
import com.deliverysdk.calendaragent.parser.RuleBasedEventParser
import com.deliverysdk.calendaragent.storage.EventHistoryStorage
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin 依赖注入模块定义
 *
 * 当前使用 [RuleBasedEventParser] 作为占位实现，
 * Phase 2 替换为 LLM 实现时只需修改这一处。
 */
val appModule: Module = module {
    // 解析器 —— Phase 1: 本地规则占位
    single<EventParser> { RuleBasedEventParser() }

    // Phase 2: 替换为 LLM 实现
    // single<EventParser> { GeminiEventParser(get(), get()) }

    // 日历服务（平台相关，通过 expect/actual 提供）
    single<CalendarService> { CalendarService() }

    // 历史记录存储
    single<Settings> { Settings() }
    single { EventHistoryStorage(get()) }

    // 日志
    single { Logger.withTag("CalendarAgent") }
}
