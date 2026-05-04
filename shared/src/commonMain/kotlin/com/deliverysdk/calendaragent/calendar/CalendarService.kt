package com.deliverysdk.calendaragent.calendar

import com.deliverysdk.calendaragent.model.CalendarEvent
import com.deliverysdk.calendaragent.model.CalendarPermissionResult

/**
 * 日历服务接口（平台实现）
 *
 * 通过 expect/actual 机制在各平台调用原生日历 API：
 * - Android: Calendar Provider (CalendarContract)
 * - iOS: EventKit (EKEventStore)
 */
expect class CalendarService() {
    /**
     * 创建日历事件
     * @return 成功返回事件 ID，失败返回错误信息
     */
    suspend fun createEvent(event: CalendarEvent): Result<String>

    /**
     * 请求日历权限
     */
    suspend fun requestPermission(): CalendarPermissionResult

    /**
     * 检查日历权限是否已授予
     */
    suspend fun isPermissionGranted(): Boolean
}
