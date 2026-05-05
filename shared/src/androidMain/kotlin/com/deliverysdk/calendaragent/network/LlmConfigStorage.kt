package com.deliverysdk.calendaragent.network

import com.tencent.mmkv.MMKV
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * LLM 配置存储
 *
 * 使用 MMKV + Kotlinx Serialization 持久化。
 * MMKV 初始化由 EventHistoryStorage.init() 统一处理。
 */
class LlmConfigStorage {
    private val mmkv = MMKV.mmkvWithID("llm_config", MMKV.SINGLE_PROCESS_MODE)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_CONFIG = "config"
    }

    /**
     * 加载当前配置
     */
    fun loadConfig(): LlmConfig {
        val raw = mmkv.decodeString(KEY_CONFIG, "")
        return if (raw.isNullOrEmpty()) {
            LlmConfig()
        } else {
            try {
                json.decodeFromString<LlmConfig>(raw)
            } catch (e: Exception) {
                LlmConfig()
            }
        }
    }

    /**
     * 保存配置
     */
    fun saveConfig(config: LlmConfig) {
        mmkv.encode(KEY_CONFIG, json.encodeToString(config))
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
        mmkv.removeValueForKey(KEY_CONFIG)
    }
}
