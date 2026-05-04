package com.deliverysdk.calendaragent.calendar

import com.deliverysdk.calendaragent.model.CalendarEvent
import com.deliverysdk.calendaragent.model.CalendarPermissionResult
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.EventKit.*
import platform.Foundation.*

actual class CalendarService {
    private val eventStore = EKEventStore()

    actual suspend fun createEvent(event: CalendarEvent): Result<String> = runCatching {
        val ekEvent = EKEvent.eventWithEventStore(eventStore) as EKEvent
        ekEvent.title = event.title
        ekEvent.notes = event.description
        ekEvent.location = event.location
        ekEvent.startDate = event.startTime.toNSDate()
        ekEvent.endDate = event.endTime.toNSDate()

        val defaultCalendar = eventStore.defaultCalendarForNewEvents
        if (defaultCalendar != null) {
            ekEvent.calendar = defaultCalendar
        }

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val saved = eventStore.saveEvent(
                ekEvent,
                span = EKSpanThisEvent,
                commit = true,
                error = error.ptr
            )
            if (!saved) {
                val err = error.pointed.value
                error("Failed to save event: ${err?.localizedDescription ?: "Unknown error"}")
            }
        }

        ekEvent.eventIdentifier
            ?: error("Event created but no ID returned")
    }

    actual suspend fun requestPermission(): CalendarPermissionResult {
        val granted = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            eventStore.requestAccessToEntityType(EKEntityTypeEvent) { granted, _ ->
                cont.resume(CalendarPermissionResult(granted = granted))
            }
        }
        return granted
    }

    actual suspend fun isPermissionGranted(): Boolean {
        val status = EKEventStore.authorizationStatusForEntityType(EKEntityTypeEvent)
        return status == EKAuthorizationStatusAuthorized ||
               status == EKAuthorizationStatusFullAccess
    }

    private fun Instant.toNSDate(): NSDate {
        val localDt = this.toLocalDateTime(TimeZone.currentSystemDefault())
        val components = NSDateComponents().apply {
            year = localDt.date.year.toLong()
            month = localDt.date.monthNumber.toLong()
            day = localDt.date.dayOfMonth.toLong()
            hour = localDt.time.hour.toLong()
            minute = localDt.time.minute.toLong()
            second = localDt.time.second.toLong()
        }
        return NSCalendar.currentCalendar.dateFromComponents(components)!!
    }
}
