package com.deliverysdk.calendaragent.features.event_input

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deliverysdk.calendaragent.model.ParseResult
import com.deliverysdk.calendaragent.model.ParsedEvent
import com.deliverysdk.calendaragent.model.ParsingContext
import com.deliverysdk.calendaragent.parser.EventParser
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInputScreen(
    eventParser: EventParser = koinInject(),
    onNavigateToPreview: (ParsedEvent) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日历 Agent") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "历史记录")
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 标题区
            Text(
                text = "用自然语言创建日历事件",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp),
            )

            Text(
                text = "例如：明天下午3点和张三开会",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            // 输入区
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("描述你的事件") },
                placeholder = { Text("明天下午3点 开会") },
                textStyle = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 错误提示
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 提交按钮
            Button(
                onClick = {
                    if (inputText.isBlank()) {
                        errorMessage = "请输入事件描述"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        val context = ParsingContext(
                            now = kotlinx.datetime.Clock.System.now()
                        )
                        val result = eventParser.parse(inputText, context)
                        isLoading = false

                        when (result) {
                            is ParseResult.Success -> {
                                onNavigateToPreview(result.event)
                            }
                            is ParseResult.Error -> {
                                errorMessage = result.message
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isLoading && inputText.isNotBlank(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("解析中...")
                } else {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("创建事件")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部提示
            Text(
                text = "💡 支持格式：\n" +
                        "• 明天下午3点开会\n" +
                        "• 今天10点看医生\n" +
                        "• 下周一上午9点周会\n" +
                        "• 5月20号 全天出差",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
