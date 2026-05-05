package com.deliverysdk.calendaragent.network

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue

/**
 * LLM 配置持久化（跨平台共享）
 *
 * 使用 Multiplatform Settings 存储 API Key 和 Provider 选择。
 */
class LlmConfigStorage(
    private val settings: Settings = Settings(),
) {
    private val logger = Logger.withTag("LlmConfigStorage")

    companion object {
        private const val KEY_CONFIG = "llm_config"
        private const val KEY_API_KEY = "llm_api_key" // legacy
    }

    /**
     * 加载当前配置
     */
    fun loadConfig(): LlmConfig {
        return settings.decodeValueOrNull(
            serializer = LlmConfig.serializer(),
            key = KEY_CONFIG,
            defaultValue = null,
        ) ?: run {
            // 兼容旧存储格式（仅存了 apiKey 的情况）
            val legacyKey = settings.getStringOrNull(KEY_API_KEY) ?: ""
            if (legacyKey.isNotBlank()) {
                LlmConfig(
                    provider = LlmProvider.SILICON_CLOUD,
                    apiKey = legacyKey,
                )
            } else {
                LlmConfig()
            }
        }
    }

    /**
     * 保存配置
     */
    fun saveConfig(config: LlmConfig) {
        settings.encodeValue(
            serializer = LlmConfig.serializer(),
            key = KEY_CONFIG,
            value = config,
        )
        logger.i { "Config saved: provider=${config.provider.displayName}, apiKey=${config.apiKey.take(4)}***" }
    }

    /**
     * 仅更新 API Key
     */
    fun updateApiKey(key: String) {
        val current = loadConfig()
        saveConfig(current.copy(apiKey = key.trim()))
    }

    /**
     * 仅更新 Provider
     */
    fun updateProvider(provider: LlmProvider) {
        val current = loadConfig()
        saveConfig(current.copy(provider = provider))
    }

    /**
     * 清除配置（恢复为默认）
     */
    fun clearConfig() {
        settings.remove(KEY_CONFIG)
        settings.remove(KEY_API_KEY)
        logger.i { "Config cleared" }
    }
}
