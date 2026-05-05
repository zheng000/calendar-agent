package com.deliverysdk.calendaragent.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deliverysdk.calendaragent.network.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    configStorage: LlmConfigStorage = koinInject(),
) {
    var config by remember { mutableStateOf(configStorage.loadConfig()) }
    var apiKeyInput by remember { mutableStateOf(config.apiKey) }
    var selectedProvider by remember { mutableStateOf(config.provider) }
    var testResult by remember { mutableStateOf<LlmTestResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // LLM 配置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "LLM 配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    // Provider 选择
                    Text(
                        text = "解析引擎",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // 引擎选项
                    val llmProviders = listOf(
                        LlmProvider.RULE_BASED,
                        LlmProvider.SILICON_CLOUD,
                    )
                    llmProviders.forEach { provider ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedProvider == provider,
                                onClick = {
                                    selectedProvider = provider
                                    configStorage.updateProvider(provider)
                                    config = configStorage.loadConfig()
                                },
                            )
                            Column {
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (provider == LlmProvider.RULE_BASED) {
                                    Text(
                                        text = "本地正则规则，无需网络，支持常见中文时间表达",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else if (provider == LlmProvider.SILICON_CLOUD) {
                                    Text(
                                        text = "硅基流动 API，模型: ${provider.defaultModel}（免费额度）",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // API Key 输入（仅 LLM Provider 显示）
                    if (selectedProvider != LlmProvider.RULE_BASED) {
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                testResult = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("API Key") },
                            placeholder = { Text("输入你的 SiliconCloud API Key") },
                            visualTransformation = if (apiKeyInput.length > 8) {
                                androidx.compose.ui.text.input.PasswordVisualTransformation()
                            } else {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            singleLine = true,
                        )

                        // 获取 Key 的链接提示
                        Text(
                            text = "💡 前往 cloud.siliconflow.cn 注册获取免费 API Key",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 保存按钮
                        Button(
                            onClick = {
                                configStorage.updateApiKey(apiKeyInput)
                                config = configStorage.loadConfig()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = apiKeyInput.isNotBlank(),
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("保存 API Key")
                        }

                        // 测试连接按钮
                        OutlinedButton(
                            onClick = {
                                isTesting = true
                                testResult = null
                                kotlinx.coroutines.CoroutineScope(
                                    kotlinx.coroutines.Dispatchers.Default
                                ).launch {
                                    val result = testLlmConnection(
                                        provider = selectedProvider,
                                        apiKey = apiKeyInput,
                                        model = selectedProvider.defaultModel,
                                    )
                                    testResult = result
                                    isTesting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = apiKeyInput.isNotBlank() && !isTesting,
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("测试连接中...")
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("测试连接")
                            }
                        }

                        // 测试结果
                        testResult?.let { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (result) {
                                        is LlmTestResult.Success ->
                                            MaterialTheme.colorScheme.primaryContainer
                                        is LlmTestResult.Error ->
                                            MaterialTheme.colorScheme.errorContainer
                                    },
                                ),
                            ) {
                                Text(
                                    text = when (result) {
                                        is LlmTestResult.Success -> "✅ ${result.message}"
                                        is LlmTestResult.Error -> "❌ ${result.message}"
                                    },
                                    modifier = Modifier.padding(12.dp),
                                    color = when (result) {
                                        is LlmTestResult.Success ->
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        is LlmTestResult.Error ->
                                            MaterialTheme.colorScheme.onErrorContainer
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            // 当前状态信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "当前状态",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    val currentConfig = configStorage.loadConfig()
                    Text(
                        text = "解析引擎: ${currentConfig.provider.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (currentConfig.isConfigured) {
                        val keyDisplay = "sk-***${currentConfig.apiKey.takeLast(4)}"
                        Text(
                            text = "API Key: $keyDisplay",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            text = "使用本地规则解析（离线可用）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
