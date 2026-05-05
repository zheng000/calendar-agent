package com.deliverysdk.calendaragent.di

import android.content.Context
import co.touchlab.kermit.Logger
import com.deliverysdk.calendaragent.calendar.CalendarService
import com.deliverysdk.calendaragent.network.LlmConfig
import com.deliverysdk.calendaragent.network.LlmConfigStorage
import com.deliverysdk.calendaragent.network.LlmProvider
import com.deliverysdk.calendaragent.parser.EventParser
import com.deliverysdk.calendaragent.parser.LlmEventParser
import com.deliverysdk.calendaragent.parser.RuleBasedEventParser
import com.deliverysdk.calendaragent.storage.EventHistoryStorage
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin 依赖注入模块定义
 *
 * 动态选择 Parser：
 * - 有 API Key + 选择 LLM → LlmEventParser
 * - 无 API Key 或选择本地规则 → RuleBasedEventParser（离线兜底）
 */
fun appModule(context: Context): Module = module {
    // Context
    single<Context> { context }

    // LLM 配置存储 (MMKV)
    single { LlmConfigStorage() }

    // 解析器 —— 根据配置动态选择
    single<EventParser> {
        val config = get<LlmConfigStorage>().loadConfig()
        when {
            config.isConfigured -> {
                Logger.withTag("AppModules").i { "Using LLM parser: ${config.provider.displayName}" }
                LlmEventParser(config)
            }
            else -> {
                Logger.withTag("AppModules").i { "Using rule-based parser (no LLM configured)" }
                RuleBasedEventParser()
            }
        }
    }

    // 日历服务（平台相关，通过 expect/actual 提供）
    single<CalendarService> { CalendarService() }

    // 历史记录存储 (MMKV)
    single { EventHistoryStorage(context) }

    // 日志
    single { Logger.withTag("CalendarAgent") }
}

/**
 * 动态重建 EventParser（配置变更后调用）
 */
fun reloadParser() {
    try {
        val config = org.koin.core.context.GlobalContext.get()
            .getOrNull<LlmConfigStorage>()?.loadConfig() ?: LlmConfig()
        val parser: EventParser = if (config.isConfigured) {
            LlmEventParser(config)
        } else {
            RuleBasedEventParser()
        }
        org.koin.core.context.GlobalContext.get()
            .getKoin().factory<EventParser> { _, _ -> parser }
    } catch (e: Exception) {
        Logger.withTag("AppModules").e(e) { "Failed to reload parser" }
    }
}
