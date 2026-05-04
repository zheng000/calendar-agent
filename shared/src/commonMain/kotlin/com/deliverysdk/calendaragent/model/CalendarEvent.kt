package com.deliverysdk.calendaragent.model

import kotlinx.datetime.Instant

/**
 * 完整的日历事件（包含系统 ID，用于更新/删除）
 */
data class CalendarEvent(
    val id: String?,              // 平台生成的事件 ID
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val location: String?,
    val description: String?,
    val isAllDay: Boolean,
)

/**
 * 权限请求结果
 */
data class CalendarPermissionResult(
    val granted: Boolean,
    val permanentlyDenied: Boolean = false,
)
