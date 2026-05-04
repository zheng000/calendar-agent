package com.deliverysdk.calendaragent.features.event_preview

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deliverysdk.calendaragent.calendar.CalendarService
import com.deliverysdk.calendaragent.model.CalendarEvent
import com.deliverysdk.calendaragent.model.ParsedEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventPreviewScreen(
    event: ParsedEvent,
    calendarService: CalendarService,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    var title by remember { mutableStateOf(event.title) }
    var location by remember { mutableStateOf(event.location ?: "") }
    var description by remember { mutableStateOf(event.description ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var saveResult by remember { mutableStateOf<SaveState?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val startTimeDisplay = formatInstant(event.startTime)
    val endTimeDisplay = formatInstant(event.endTime)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("确认事件") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "请确认或编辑事件信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // 标题
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题") },
                leadingIcon = {
                    Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(20.dp))
                },
            )

            // 开始时间
            OutlinedTextField(
                value = startTimeDisplay,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("开始时间") },
                leadingIcon = {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                readOnly = true,
            )

            // 结束时间
            OutlinedTextField(
                value = endTimeDisplay,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("结束时间") },
                leadingIcon = {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                readOnly = true,
            )

            // 地点
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("地点（可选）") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                },
            )

            // 描述
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                label = { Text("备注（可选）") },
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                maxLines = 3,
            )

            // 结果提示
            saveResult?.let { state ->
                when (state) {
                    is SaveState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Text(
                                text = "✅ 事件已保存到日历",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    is SaveState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Text(
                                text = "❌ ${state.message}",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 保存到日历按钮
            Button(
                onClick = {
                    isSaving = true
                    saveResult = null

                    val calendarEvent = CalendarEvent(
                        id = null,
                        title = title,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        location = location.takeIf { it.isNotBlank() },
                        description = description.takeIf { it.isNotBlank() },
                        isAllDay = event.isAllDay,
                    )

                    coroutineScope.launch {
                        val result = calendarService.createEvent(calendarEvent)
                        isSaving = false

                        result.fold(
                            onSuccess = { eventId ->
                                saveResult = SaveState.Success(eventId)
                                delay(1500)
                                onSaved()
                            },
                            onFailure = { error ->
                                saveResult = SaveState.Error(error.message ?: "保存失败")
                            },
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isSaving && title.isNotBlank(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存中...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存到日历")
                }
            }
        }
    }
}

private fun formatInstant(instant: Instant): String {
    val localDt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val dayOfWeek = when (localDt.date.dayOfWeek.name) {
        "MONDAY" -> "周一"
        "TUESDAY" -> "周二"
        "WEDNESDAY" -> "周三"
        "THURSDAY" -> "周四"
        "FRIDAY" -> "周五"
        "SATURDAY" -> "周六"
        "SUNDAY" -> "周日"
        else -> ""
    }
    return "${localDt.date.year}-${localDt.date.monthNumber.toString().padStart(2, '0')}-${localDt.date.dayOfMonth.toString().padStart(2, '0')} " +
            "($dayOfWeek) " +
            "${localDt.time.hour.toString().padStart(2, '0')}:${localDt.time.minute.toString().padStart(2, '0')}"
}

sealed class SaveState {
    data class Success(val eventId: String) : SaveState()
    data class Error(val message: String) : SaveState()
}
