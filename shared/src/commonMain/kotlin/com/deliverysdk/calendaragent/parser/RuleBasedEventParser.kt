package com.deliverysdk.calendaragent.parser

import co.touchlab.kermit.Logger
import com.deliverysdk.calendaragent.model.ParseResult
import com.deliverysdk.calendaragent.model.ParsedEvent
import com.deliverysdk.calendaragent.model.ParsingContext
import kotlinx.datetime.*

/**
 * 基于规则的本地事件解析器（占位实现）
 *
 * 使用正则匹配中文时间表达，提取事件标题。
 * 后续 Phase 2 替换为 LLM API 调用。
 *
 * 支持的格式示例：
 * - "明天下午3点开会"        → 明天 15:00-16:00
 * - "今天10点看医生"         → 今天 10:00-11:00
 * - "下周一上午9点周会"      → 下周一 09:00-10:00
 * - "5月20号全天出差"        → 5月20日 全天
 */
class RuleBasedEventParser : EventParser {

    private val logger = Logger.withTag("RuleBasedParser")

    override suspend fun parse(text: String, context: ParsingContext): ParseResult {
        logger.i { "Parsing: $text" }

        if (text.isBlank()) {
            return ParseResult.Error("请输入事件描述")
        }

        val timeMatch = parseTime(text, context)
        if (timeMatch == null) {
            logger.w { "Failed to parse time, returning default" }
            return ParseResult.Error(
                "无法解析时间，请使用格式：「时间+事件」\n例如：明天下午3点开会、今天10点看医生"
            )
        }

        val title = extractTitle(text, timeMatch.matchedText)
        val location = extractLocation(text)

        val event = ParsedEvent(
            title = title,
            startTime = timeMatch.start,
            endTime = timeMatch.end,
            location = location,
            description = text,
            isAllDay = timeMatch.isAllDay,
        )

        logger.i { "Parsed event: title=${event.title}, start=${event.startTime}, end=${event.endTime}" }
        return ParseResult.Success(event)
    }

    private fun parseTime(text: String, context: ParsingContext): TimeMatch? {
        val now = context.now
        val tz = context.timeZone
        val localNow = now.toLocalDateTime(tz)
        val today = localNow.date

        // --- 1. "明天" + 时间 ---
        val tomorrowRegex = Regex(
            """明天.{0,2}(\d{1,2})[点时](\d{0,2})"""
        )
        tomorrowRegex.find(text)?.let { match ->
            val hour = match.groupValues[1].toInt().coerceIn(0, 23)
            val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val startDt = LocalDateTime(
                date = today.plus(1, DateTimeUnit.DAY),
                time = LocalTime(hour, minute)
            )
            val start = startDt.toInstant(tz)
            return TimeMatch(
                start = start,
                end = start.plus(1, DateTimeUnit.HOUR),
                isAllDay = false,
                matchedText = match.value,
            )
        }

        // "明天"（无具体时间）
        if (text.contains("明天")) {
            val startDt = LocalDateTime(
                date = today.plus(1, DateTimeUnit.DAY),
                time = LocalTime(10, 0)
            )
            val start = startDt.toInstant(tz)
            return TimeMatch(
                start = start,
                end = start.plus(1, DateTimeUnit.HOUR),
                isAllDay = false,
                matchedText = "明天",
            )
        }

        // --- 2. "今天" + 时间 ---
        val todayRegex = Regex(
            """今天.{0,2}(\d{1,2})[点时](\d{0,2})"""
        )
        todayRegex.find(text)?.let { match ->
            val hour = match.groupValues[1].toInt().coerceIn(0, 23)
            val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val startDt = LocalDateTime(date = today, time = LocalTime(hour, minute))
            val start = startDt.toInstant(tz)
            return TimeMatch(
                start = start,
                end = start.plus(1, DateTimeUnit.HOUR),
                isAllDay = false,
                matchedText = match.value,
            )
        }

        // --- 3. "下周一/二/..." + 时间 ---
        val dayOfWeekMap = mapOf(
            "周一" to DayOfWeek.MONDAY, "星期二" to DayOfWeek.TUESDAY,
            "周二" to DayOfWeek.TUESDAY, "星期三" to DayOfWeek.WEDNESDAY,
            "周三" to DayOfWeek.WEDNESDAY, "星期四" to DayOfWeek.THURSDAY,
            "周四" to DayOfWeek.THURSDAY, "星期五" to DayOfWeek.FRIDAY,
            "周五" to DayOfWeek.FRIDAY, "星期六" to DayOfWeek.SATURDAY,
            "周六" to DayOfWeek.SATURDAY, "星期日" to DayOfWeek.SUNDAY,
            "周日" to DayOfWeek.SUNDAY, "星期天" to DayOfWeek.SUNDAY,
        )

        for ((keyword, targetDay) in dayOfWeekMap) {
            val dowRegex = Regex("""下$keyword.{0,2}(\d{1,2})[点时](\d{0,2})""")
            dowRegex.find(text)?.let { match ->
                val hour = match.groupValues[1].toInt().coerceIn(0, 23)
                val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
                val targetDate = getNextDayOfWeek(today, targetDay)
                val startDt = LocalDateTime(date = targetDate, time = LocalTime(hour, minute))
                val start = startDt.toInstant(tz)
                return TimeMatch(
                    start = start,
                    end = start.plus(1, DateTimeUnit.HOUR),
                    isAllDay = false,
                    matchedText = match.value,
                )
            }

            // "下周一"（无具体时间）
            if (text.contains("下$keyword")) {
                val targetDate = getNextDayOfWeek(today, targetDay)
                val startDt = LocalDateTime(date = targetDate, time = LocalTime(10, 0))
                val start = startDt.toInstant(tz)
                return TimeMatch(
                    start = start,
                    end = start.plus(1, DateTimeUnit.HOUR),
                    isAllDay = false,
                    matchedText = "下$keyword",
                )
            }
        }

        // --- 4. "X月X号/日" + 时间 ---
        val dateRegex = Regex(
            """(\d{1,2})[月/](\d{1,2})[号日].{0,2}(?:上午|下午|晚上)?(\d{1,2})[点时](\d{0,2})"""
        )
        dateRegex.find(text)?.let { match ->
            val month = match.groupValues[1].toInt().coerceIn(1, 12)
            val day = match.groupValues[2].toInt().coerceIn(1, 31)
            val hour = match.groupValues[3].toInt().coerceIn(0, 23)
            val minute = match.groupValues[4].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val date = LocalDate(today.year, month, day)
            val startDt = LocalDateTime(date = date, time = LocalTime(hour, minute))
            val start = startDt.toInstant(tz)
            return TimeMatch(
                start = start,
                end = start.plus(1, DateTimeUnit.HOUR),
                isAllDay = false,
                matchedText = match.value,
            )
        }

        // --- 5. "X月X号/日"（全天）---
        val allDayRegex = Regex("""(\d{1,2})[月/](\d{1,2})[号日].*全天""")
        allDayRegex.find(text)?.let { match ->
            val month = match.groupValues[1].toInt().coerceIn(1, 12)
            val day = match.groupValues[2].toInt().coerceIn(1, 31)
            val date = LocalDate(today.year, month, day)
            val startDt = LocalDateTime(date, LocalTime(0, 0))
            val start = startDt.toInstant(tz)
            return TimeMatch(
                start = start,
                end = start.plus(1, DateTimeUnit.DAY, tz),
                isAllDay = true,
                matchedText = match.value,
            )
        }

        // --- 6. 上午/下午 + 时间 ---
        val amPmRegex = Regex(
            """(?:上午|早上)(\d{1,2})[点时](\d{0,2})"""
        )
        amPmRegex.find(text)?.let { match ->
            var hour = match.groupValues[1].toInt().coerceIn(0, 23)
            val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            if (hour == 12) hour = 0
            val startDt = LocalDateTime(date = today, time = LocalTime(hour, minute))
            val start = startDt.toInstant(tz)
            return TimeMatch(
                start = start,
                end = start.plus(1, DateTimeUnit.HOUR),
                isAllDay = false,
                matchedText = match.value,
            )
        }

        val pmRegex = Regex(
            """(?:下午|晚上)(\d{1,2})[点时](\d{0,2})"""
        )
        pmRegex.find(text)?.let { match ->
            var hour = match.groupValues[1].toInt().coerceIn(0, 23)
            val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            if (hour < 12) hour += 12
            val startDt = LocalDateTime(date = today, time = LocalTime(hour, minute))
            val start = startDt.toInstant(tz)
            return TimeMatch(
                start = start,
                end = start.plus(1, DateTimeUnit.HOUR),
                isAllDay = false,
                matchedText = match.value,
            )
        }

        // --- 兜底：纯数字时间（如 "3点" "10点"）---
        val simpleTimeRegex = Regex("""(\d{1,2})[点时](\d{0,2})""")
        simpleTimeRegex.find(text)?.let { match ->
            val hour = match.groupValues[1].toInt().coerceIn(0, 23)
            val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val startDt = LocalDateTime(date = today, time = LocalTime(hour, minute))
            val start = startDt.toInstant(tz)
            return TimeMatch(
                start = start,
                end = start.plus(1, DateTimeUnit.HOUR),
                isAllDay = false,
                matchedText = match.value,
            )
        }

        return null
    }

    private fun extractTitle(text: String, matchedTimeText: String): String {
        // 去掉时间相关文字，剩余作为标题
        val timeWordsToRemove = listOf(
            "明天", "今天", "下周一", "下周二", "下周三", "下周四", "下周五",
            "下周六", "下周日", "下星期天",
            "上午", "下午", "晚上", "早上",
            "全天", "点", "时",
            Regex("""\d{1,2}[月/]\d{1,2}[号日]"""),
        )

        var title = text
        // 移除匹配到的时间文字
        title = title.replace(matchedTimeText, "")

        // 移除常见时间词
        for (word in timeWordsToRemove) {
            title = when (word) {
                is String -> title.replace(word, "")
                is Regex -> title.replace(word, "")
                else -> title
            }
        }

        // 清理多余空格和标点
        title = title.replace(Regex("""[^\p{L}\p{N}\s]"""), "").trim()

        return title.takeIf { it.isNotEmpty() } ?: "新事件"
    }

    private fun extractLocation(text: String): String? {
        // 简单匹配 "在 XX" 格式
        val locationRegex = Regex("""在(.{2,20}?)(?:开会|见|面|聚|吃饭|吃饭|办公|工作|的|了|$)""")
        return locationRegex.find(text)?.groupValues?.get(1)?.trim()
    }

    /**
     * 获取下一个指定星期几的日期
     */
    private fun getNextDayOfWeek(from: LocalDate, targetDay: DayOfWeek): LocalDate {
        val currentDow = from.dayOfWeek
        val daysUntil = (targetDay.isoDayNumber - currentDow.isoDayNumber + 7) % 7
        val adjustedDaysUntil = if (daysUntil == 0) 7 else daysUntil
        return from.plus(adjustedDaysUntil, DateTimeUnit.DAY)
    }

    private data class TimeMatch(
        val start: Instant,
        val end: Instant,
        val isAllDay: Boolean,
        val matchedText: String,
    )
}
