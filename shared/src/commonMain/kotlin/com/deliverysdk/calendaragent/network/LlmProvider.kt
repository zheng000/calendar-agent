package com.deliverysdk.calendaragent.network

/**
 * LLM Provider 枚举
 *
 * 新增 Provider 只需在此添加条目，LlmEventParser 自动使用 baseUrl + model。
 * 所有 Provider 遵循 OpenAI 兼容的 Chat Completions API 协议。
 */
enum class LlmProvider(
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
) {
    RULE_BASED(
        displayName = "本地规则（免费离线）",
        baseUrl = "",
        defaultModel = "",
    ),
    SILICON_CLOUD(
        displayName = "硅基流动 SiliconCloud",
        baseUrl = "https://api.siliconflow.cn/v1",
        defaultModel = "Qwen/Qwen2.5-7B-Instruct",
    ),
}

/**
 * LLM 配置
 */
@Serializable
data class LlmConfig(
    val provider: LlmProvider = LlmProvider.RULE_BASED,
    val apiKey: String = "",
    val customModel: String? = null,
) {
    val effectiveModel: String
        get() = customModel?.takeIf { it.isNotBlank() } ?: provider.defaultModel

    val isConfigured: Boolean
        get() = provider != LlmProvider.RULE_BASED && apiKey.isNotBlank()
}
