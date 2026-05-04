package com.deliverysdk.calendaragent.storage

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.Serializable

/**
 * 事件历史记录存储（跨平台共享）
 */
class EventHistoryStorage(
    private val settings: Settings,
) {
    private val logger = Logger.withTag("EventHistoryStorage")

    companion object {
        private const val KEY_HISTORY = "event_history"
    }

    /**
     * 保存事件到历史记录
     */
    fun saveEvent(record: HistoryRecord) {
        val existing = loadAll()
        val updated = listOf(record) + existing
        settings.encodeValue(
            serializer = ListSerializer(HistoryRecord.serializer()),
            key = KEY_HISTORY,
            value = updated.take(100), // 最多保留 100 条
        )
        logger.i { "Saved event: ${record.title}" }
    }

    /**
     * 加载所有历史记录
     */
    fun loadAll(): List<HistoryRecord> {
        return settings.decodeValueOrNull(
            serializer = ListSerializer(HistoryRecord.serializer()),
            key = KEY_HISTORY,
        ) ?: emptyList()
    }

    /**
     * 清空历史记录
     */
    fun clear() {
        settings.remove(KEY_HISTORY)
        logger.i { "History cleared" }
    }
}

/**
 * 历史记录条目
 */
@Serializable
data class HistoryRecord(
    val id: String,
    val title: String,
    val startTimeEpochSeconds: Long,
    val endTimeEpochSeconds: Long,
    val createdAtEpochSeconds: Long,
    val createdByAgent: Boolean = true,
)
