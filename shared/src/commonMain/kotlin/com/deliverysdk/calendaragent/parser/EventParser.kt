package com.deliverysdk.calendaragent.parser

import com.deliverysdk.calendaragent.model.ParseResult
import com.deliverysdk.calendaragent.model.ParsingContext

/**
 * 事件解析接口
 *
 * Phase 1: [RuleBasedEventParser] — 本地规则占位（正则）
 * Phase 2: 替换为 LLM 实现（如 GeminiEventParser、OpenAiEventParser）
 */
interface EventParser {
    /**
     * 解析自然语言文本为结构化日历事件
     *
     * @param text 用户输入的自然语言
     * @param context 解析上下文（当前时间、时区等）
     * @return [ParseResult.Success] 解析成功返回 [com.deliverysdk.calendaragent.model.ParsedEvent]
     *         [ParseResult.Error] 解析失败返回错误信息
     */
    suspend fun parse(text: String, context: ParsingContext): ParseResult
}
