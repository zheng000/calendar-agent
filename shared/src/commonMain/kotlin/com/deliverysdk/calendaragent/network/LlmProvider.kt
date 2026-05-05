package com.deliverysdk.calendaragent.network

import kotlinx.serialization.Serializable

/**
 * LLM Provider 枚举
 *
 * 新增 Provider 只需在此添加条目，所有 Provider 遵循 OpenAI 兼容协议。
 * 元数据集中定义，UI 用枚举遍历展示为选项列表。
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
    GROQ(
        displayName = "Groq（超快推理）",
        baseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "qwen-2.5-32b",
    ),
}

/**
 * LLM 配置
 *
 * 只存两个字段：选中的 Provider + 用户的 API Key。
 * Provider 元数据（baseUrl, defaultModel）在 enum 中定义，不在这里重复。
 */
@Serializable
data class LlmConfig(
    val provider: LlmProvider = LlmProvider.RULE_BASED,
    val apiKey: String = "",
) {
    /** 是否已配置 LLM（非本地规则 + 有 Key） */
    val isConfigured: Boolean
        get() = provider != LlmProvider.RULE_BASED && apiKey.isNotBlank()

    /** 有效模型名（当前 provider 的默认模型） */
    val effectiveModel: String
        get() = provider.defaultModel

    /** 有效 baseUrl（当前 provider 的 baseUrl） */
    val effectiveBaseUrl: String
        get() = provider.baseUrl
}
