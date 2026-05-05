package com.deliverysdk.calendaragent.parser

import co.touchlab.kermit.Logger
import com.deliverysdk.calendaragent.model.ParseResult
import com.deliverysdk.calendaragent.model.ParsedEvent
import com.deliverysdk.calendaragent.model.ParsingContext
import com.deliverysdk.calendaragent.network.LlmConfig
import com.deliverysdk.calendaragent.network.LlmProvider
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * LLM 事件解析器
 *
 * 通过 OpenAI 兼容的 Chat Completions API 调用 LLM，
 * 将自然语言解析为结构化日历事件。
 *
 * 支持的 Provider：
 * - SiliconCloud (硅基流动)
 * - 任何兼容 OpenAI 协议的服务
 */
class LlmEventParser(
    private val config: LlmConfig,
) : EventParser {

    private val logger = Logger.withTag("LlmEventParser")

    private val client: HttpClient by lazy { createClient() }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 系统 Prompt —— 引导 LLM 输出标准 JSON
     */
    private val systemPrompt = """
你是一个日历事件提取助手。用户的输入是自然语言描述，请将其解析为结构化的日历事件。

请严格以 JSON 格式返回，不要包含任何其他文字、解释或 Markdown 格式。

返回的 JSON 必须符合以下 schema：
{
  "title": "事件标题（简短描述）",
  "startTime": "开始时间，ISO 8601 格式，如 2026-05-06T15:00:00+08:00",
  "endTime": "结束时间，ISO 8601 格式",
  "location": "地点（可选，没有则为 null）",
  "description": "详细描述（可选，没有则为 null）",
  "isAllDay": "是否全天事件（布尔值）"
}

规则：
1. 默认事件时长为 1 小时
2. 如果用户说"全天"、"一天"等，isAllDay 设为 true，endTime 设为次日
3. 从描述中智能提取标题，去掉时间信息
4. 如果用户输入中没有明确时间信息，返回 {"error": "无法识别时间信息，请提供具体时间"}
5. startTime 和 endTime 必须使用用户当前时区的完整 ISO 8601 格式
""".trimIndent()

    /**
     * 用户 Prompt 模板
     */
    private fun userPrompt(text: String, timeZoneId: String) = """
当前时间时区：$timeZoneId

请将以下描述解析为日历事件：

$text
""".trimIndent()

    override suspend fun parse(text: String, context: ParsingContext): ParseResult {
        if (text.isBlank()) {
            return ParseResult.Error("请输入事件描述")
        }

        if (!config.isConfigured) {
            return ParseResult.Error(
                "请先配置 LLM API Key。\n前往 设置 > LLM 配置 输入你的 API Key。"
            )
        }

        return try {
            val response = callLlm(text, context.timeZone.id)
            parseLlmResponse(response)
        } catch (e: Exception) {
            logger.e(e) { "LLM request failed: ${e.message}" }
            ParseResult.Error("LLM 调用失败：${e.message}")
        }
    }

    /**
     * 调用 LLM API
     */
    private suspend fun callLlm(text: String, timeZoneId: String): String {
        val provider = config.provider
        val url = "${provider.baseUrl}/chat/completions"

        logger.i { "Calling LLM: ${provider.displayName}, model=${config.effectiveModel}" }

        val requestBody = ChatCompletionsRequest(
            model = config.effectiveModel,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt(text, timeZoneId)),
            ),
            responseFormat = ResponseFormat(type = "json_object"),
            temperature = 0.1,
        )

        val response = client.post(url) {
            header("Authorization", "Bearer ${config.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw RuntimeException(
                "API 请求失败 (HTTP ${response.status.value}): ${errorBody.take(200)}"
            )
        }

        val body = response.bodyAsText()
        logger.d { "LLM raw response: ${body.take(500)}" }

        val chatResponse = json.decodeFromString<ChatCompletionsResponse>(body)
        val content = chatResponse.choices.firstOrNull()?.message?.content
            ?: throw RuntimeException("LLM 返回空响应")

        return content
    }

    /**
     * 解析 LLM 返回的 JSON 为 ParsedEvent
     */
    private fun parseLlmResponse(content: String): ParseResult {
        return try {
            // LLM 可能返回 Markdown 代码块包裹的 JSON，清理之
            val cleanedJson = content
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val eventJson = json.decodeFromString<ParsedEventJson>(cleanedJson)

            // 检查 LLM 是否返回了错误标记
            if (eventJson.error != null) {
                return ParseResult.Error(eventJson.error)
            }

            if (eventJson.title == null || eventJson.startTime == null || eventJson.endTime == null) {
                return ParseResult.Error("LLM 返回的数据不完整，缺少必要字段")
            }

            val startTime = Instant.parse(eventJson.startTime)
            val endTime = Instant.parse(eventJson.endTime)

            val event = ParsedEvent(
                title = eventJson.title,
                startTime = startTime,
                endTime = endTime,
                location = eventJson.location?.takeIf { it.isNotBlank() },
                description = eventJson.description?.takeIf { it.isNotBlank() },
                isAllDay = eventJson.isAllDay ?: false,
            )

            logger.i { "Parsed event via LLM: title=${event.title}, start=${event.startTime}" }
            ParseResult.Success(event)
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse LLM response: ${e.message}" }
            ParseResult.Error("解析 LLM 返回结果失败：${e.message}。原始内容：${content.take(100)}")
        }
    }

    /**
     * 创建 HttpClient（平台相关引擎）
     */
    private fun createClient(): HttpClient {
        val engine = createEngine()
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
            install(HttpRequestRetry) {
                maxRetries = 1
                delayMillis = 1_000
                retryIf { _, response -> response.status.value >= 500 }
            }
        }
    }

    private fun createEngine(): HttpClientEngine {
        return when (config.provider) {
            // LLM providers use network; engine selected by platform
            LlmProvider.SILICON_CLOUD -> createPlatformEngine()
            else -> createPlatformEngine()
        }
    }

    /**
     * 平台相关 HTTP 引擎选择
     */
    private fun createPlatformEngine(): HttpClientEngine {
        // Use Ktor's default engine selection via reflection
        // For multiplatform, we check the runtime platform
        val platform = getPlatform()
        return when (platform) {
            "android" -> OkHttp.create()
            "ios" -> Darwin.create()
            else -> OkHttp.create() // fallback
        }
    }

    private fun getPlatform(): String {
        return getPlatformName()
    }
}

// --- Platform detection (expect/actual) ---

expect fun getPlatformName(): String

// --- LLM API 数据结构 ---

@Serializable
private data class ChatCompletionsRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val responseFormat: ResponseFormat? = null,
    val temperature: Double = 0.1,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ResponseFormat(
    val type: String = "json_object",
)

@Serializable
private data class ChatCompletionsResponse(
    val choices: List<Choice>,
)

@Serializable
private data class Choice(
    val message: ChatMessage,
)

/**
 * LLM 返回的事件 JSON 结构
 */
@Serializable
private data class ParsedEventJson(
    val title: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val location: String? = null,
    val description: String? = null,
    val isAllDay: Boolean? = null,
    val error: String? = null,
)
