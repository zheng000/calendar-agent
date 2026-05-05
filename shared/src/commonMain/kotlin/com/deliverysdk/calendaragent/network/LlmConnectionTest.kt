package com.deliverysdk.calendaragent.network

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 测试 LLM 连接
 *
 * 发送一个简单的测试请求到 API，验证 Key 和连接是否正常。
 */
suspend fun testLlmConnection(
    provider: LlmProvider,
    apiKey: String,
    model: String,
): LlmTestResult {
    val logger = Logger.withTag("LlmConnectionTest")
    val baseUrl = provider.baseUrl

    if (provider == LlmProvider.RULE_BASED) {
        return LlmTestResult.Success("本地规则解析器无需配置")
    }

    if (apiKey.isBlank()) {
        return LlmTestResult.Error("API Key 不能为空")
    }

    val url = "$baseUrl/chat/completions"
    logger.i { "Testing connection to: $baseUrl, model: $model" }

    val client = createTestClient()
    return try {
        val requestBody = TestChatRequest(
            model = model,
            messages = listOf(
                TestMessage(role = "user", content = "回复 OK"),
            ),
            maxTokens = 10,
        )

        val response = client.post(url) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            val parsed = Json { ignoreUnknownKeys = true }
                .decodeFromString<TestChatResponse>(body)
            val reply = parsed.choices.firstOrNull()?.message?.content ?: "OK"
            LlmTestResult.Success("连接成功！模型回复：${reply.take(50)}")
        } else {
            val errorBody = response.bodyAsText()
            val errorMsg = parseApiError(errorBody)
            LlmTestResult.Error("连接失败 (HTTP ${response.status.value}): $errorMsg")
        }
    } catch (e: Exception) {
        logger.e(e) { "Connection test failed: ${e.message}" }
        LlmTestResult.Error("连接失败：${e.message}")
    } finally {
        client.close()
    }
}

private fun createTestClient(): HttpClient {
    return HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 5_000
        }
    }
}

private fun parseApiError(raw: String): String {
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val error = json.decodeFromString<ApiErrorResponse>(raw)
        error.error?.message ?: raw.take(100)
    } catch (e: Exception) {
        raw.take(150)
    }
}

// --- API 请求/响应 ---

@Serializable
private data class TestChatRequest(
    val model: String,
    val messages: List<TestMessage>,
    val maxTokens: Int = 10,
)

@Serializable
private data class TestMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class TestChatResponse(
    val choices: List<TestChoice>,
)

@Serializable
private data class TestChoice(
    val message: TestMessage,
)

@Serializable
private data class ApiErrorResponse(
    val error: ApiError? = null,
)

@Serializable
private data class ApiError(
    val message: String? = null,
    val type: String? = null,
)

/**
 * 连接测试结果
 */
sealed interface LlmTestResult {
    data class Success(val message: String) : LlmTestResult
    data class Error(val message: String) : LlmTestResult
}
