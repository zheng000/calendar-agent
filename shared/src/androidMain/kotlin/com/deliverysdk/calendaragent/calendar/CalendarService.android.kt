package com.deliverysdk.calendaragent.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract
import com.deliverysdk.calendaragent.model.CalendarEvent
import com.deliverysdk.calendaragent.model.CalendarPermissionResult
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toJavaLocalDate
import java.util.TimeZone as JavaTimeZone

actual class CalendarService {
    private var contentResolver: ContentResolver? = null

    /**
     * 设置 ContentResolver（从 Android 端注入）
     */
    fun setContentResolver(resolver: ContentResolver) {
        this.contentResolver = resolver
    }

    actual suspend fun createEvent(event: CalendarEvent): Result<String> = runCatching {
        val resolver = contentResolver
            ?: error("ContentResolver not set. Call setContentResolver() first.")

        val calendarId = getPrimaryCalendarId(resolver)
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)

            if (event.isAllDay) {
                put(CalendarContract.Events.ALL_DAY, 1)
                put(
                    CalendarContract.Events.DTSTART,
                    event.startTime.toJavaInstant().toEpochMilli()
                )
                put(
                    CalendarContract.Events.DTEND,
                    event.endTime.toJavaInstant().toEpochMilli()
                )
            } else {
                put(CalendarContract.Events.ALL_DAY, 0)
                put(
                    CalendarContract.Events.DTSTART,
                    event.startTime.toJavaInstant().toEpochMilli()
                )
                put(
                    CalendarContract.Events.DTEND,
                    event.endTime.toJavaInstant().toEpochMilli()
                )
                put(CalendarContract.Events.EVENT_TIMEZONE, JavaTimeZone.getDefault().id)
            }
        }

        val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: error("Failed to insert calendar event")

        val eventId = ContentUris.parseId(uri).toString()
        eventId
    }

    actual suspend fun requestPermission(): CalendarPermissionResult {
        // 权限由 Activity 通过 ActivityResultContracts 处理
        // 此处仅返回默认结果，实际权限状态通过回调传递
        return CalendarPermissionResult(granted = true)
    }

    actual suspend fun isPermissionGranted(): Boolean {
        return true // 简化版，实际应检查权限
    }

    private fun getPrimaryCalendarId(resolver: ContentResolver): Long {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
        )
        val cursor = resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null, null,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }

        // 回退：查找任意可用日历
        val fallbackCursor = resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            null, null, null
        )
        fallbackCursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }

        error("No calendar available on device")
    }
}
