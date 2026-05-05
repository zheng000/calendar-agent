package com.deliverysdk.calendaragent.storage

import android.content.Context
import com.deliverysdk.calendaragent.model.HistoryRecord
import com.tencent.mmkv.MMKV
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 事件历史记录存储
 *
 * 使用 MMKV + Kotlinx Serialization 持久化。
 */
class EventHistoryStorage(
    context: Context,
) {
    private val mmkv = MMKV.mmkvWithID("event_history", MMKV.SINGLE_PROCESS_MODE)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_RECORDS = "records"

        fun init(context: Context) {
            MMKV.initialize(context)
        }
    }

    init {
        init(context)
    }

    /**
     * 保存事件到历史记录
     */
    fun saveEvent(record: HistoryRecord) {
        val existing = loadAll()
        val updated = listOf(record) + existing
        mmkv.encode(KEY_RECORDS, json.encodeToString(updated.take(100)))
    }

    /**
     * 加载所有历史记录
     */
    fun loadAll(): List<HistoryRecord> {
        val raw = mmkv.decodeString(KEY_RECORDS, "")
        return if (raw.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<HistoryRecord>>(raw)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * 清空历史记录
     */
    fun clear() {
        mmkv.removeValueForKey(KEY_RECORDS)
    }
}
