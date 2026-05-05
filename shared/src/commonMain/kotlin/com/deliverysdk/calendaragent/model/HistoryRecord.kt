package com.deliverysdk.calendaragent.model

import kotlinx.serialization.Serializable

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
