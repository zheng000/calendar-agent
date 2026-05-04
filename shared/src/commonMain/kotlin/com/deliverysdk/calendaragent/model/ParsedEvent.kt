package com.deliverysdk.calendaragent.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 结构化日历事件 —— 从自然语言解析后得到的结果
 */
@Serializable
data class ParsedEvent(
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val location: String? = null,
    val description: String? = null,
    val isAllDay: Boolean = false,
) {
    companion object {
        /**
         * 创建明天 10:00-11:00 的默认事件（占位用）
         */
        fun defaultForTomorrow(now: Instant): ParsedEvent {
            val start = now.plus(86_400L, kotlinx.datetime.DateTimeUnit.SECOND)
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                .let { local ->
                    kotlinx.datetime.LocalDateTime(
                        local.date,
                        kotlinx.datetime.LocalTime(10, 0)
                    ).toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
                }
            return ParsedEvent(
                title = "新事件",
                startTime = start,
                endTime = start.plus(3600L, kotlinx.datetime.DateTimeUnit.SECOND),
            )
        }
    }
}

/**
 * 解析上下文 —— 提供给解析器当前时间、时区等信息
 */
data class ParsingContext(
    val now: Instant,
    val timeZone: kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.currentSystemDefault(),
)

/**
 * 解析结果
 */
sealed interface ParseResult {
    data class Success(val event: ParsedEvent) : ParseResult
    data class Error(val message: String) : ParseResult
}
