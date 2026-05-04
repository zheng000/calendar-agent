# 日历 Agent — Calendar Agent App

一个基于 **Kotlin Multiplatform + Compose Multiplatform** 的跨平台日历应用，支持 Android 和 iOS。

用户通过自然语言输入（如"明天下午3点开会"），App 解析后添加到设备本地日历。

## 技术栈

| 组件 | 技术 |
|------|------|
| 跨平台框架 | Kotlin Multiplatform (KMP) |
| UI 框架 | Compose Multiplatform（一套代码，双平台） |
| AI 解析 | Phase 1: 本地规则（正则占位） → Phase 2: LLM API |
| 状态管理 | MVI + Kotlin Flow |
| 依赖注入 | Koin |
| 日历写入 | Android: Calendar Provider / iOS: EventKit |
| 本地存储 | Multiplatform Settings |

## 快速开始

### 前置准备

1. 安装 JDK 17+（推荐 JDK 21）
2. 安装 Android Studio Ladybug (2024.2+) 或 IntelliJ IDEA
3. 生成 Gradle Wrapper（首次需要）：

```bash
# 如果你本地安装了 Gradle，在项目根目录执行：
gradle wrapper

# 或者直接用 Android Studio 打开项目，IDE 会自动生成
```

### Android

```bash
# 1. 用 Android Studio (Ladybug 2024.2+) 打开项目根目录
# 2. 同步 Gradle
# 3. 运行 composeApp 配置
./gradlew :composeApp:installDebug
```

### iOS

```bash
# 1. 用 Xcode (15+) 打开 iosApp/iosApp.xcodeproj
# 2. 先构建 ComposeApp framework（Xcode 会自动调用 Gradle）
# 3. 运行 iOS 模拟器或真机
```

## 项目结构

```
calendar-agent/
├── composeApp/                    # 共享 UI + Android/iOS 入口
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/            # 共享 Compose UI
│       │   ├── App.kt             # 根组件 + 导航
│       │   └── features/
│       │       ├── event_input/   # 输入页面
│       │       ├── event_preview/ # 预览/编辑页面
│       │       └── history/       # 历史记录页面
│       ├── androidMain/           # Android 入口
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/.../        # MainActivity, Application
│       └── iosMain/               # iOS 入口
│
├── shared/                        # 共享业务逻辑
│   └── src/
│       ├── commonMain/            # 100% 共享代码
│       │   ├── model/             # 数据模型
│       │   ├── parser/            # 事件解析（接口 + 规则实现）
│       │   ├── calendar/          # 日历服务（expect class）
│       │   ├── storage/           # 历史记录存储
│       │   └── di/                # Koin 模块
│       ├── androidMain/           # Android 实现
│       │   └── calendar/          # Calendar Provider 封装
│       └── iosMain/               # iOS 实现
│           └── calendar/          # EventKit 封装
│
└── iosApp/                        # iOS Xcode 项目
    └── iosApp/
        ├── Info.plist
        └── ContentView.swift      # 桥接到 Compose
```

## 核心流程

```
用户输入自然语言
    ↓
RuleBasedEventParser（本地规则占位）
    ↓
解析为 ParsedEvent
    ↓
EventPreviewScreen（预览/编辑）
    ↓
CalendarService.createEvent()
    ↓
写入设备日历（Android: Calendar Provider / iOS: EventKit）
```

## Phase 2: 接入 LLM

当前使用 `RuleBasedEventParser` 作为占位实现。接入真实 LLM 只需：

1. 在 `shared/src/commonMain/parser/` 创建新的 `EventParser` 实现：

```kotlin
class GeminiEventParser(
    private val apiKey: String,
    private val httpClient: HttpClient,
) : EventParser {
    override suspend fun parse(text: String, context: ParsingContext): ParseResult {
        // 调用 Gemini API
    }
}
```

2. 在 `di/AppModules.kt` 中替换绑定：

```kotlin
// 从：
single<EventParser> { RuleBasedEventParser() }

// 改为：
single<EventParser> { GeminiEventParser(get(), get()) }
```

对 UI 层零影响。

## 支持的时间格式（Phase 1 占位）

| 输入 | 解析结果 |
|------|----------|
| 明天下午3点开会 | 明天 15:00-16:00 |
| 今天10点看医生 | 今天 10:00-11:00 |
| 下周一上午9点周会 | 下周一 09:00-10:00 |
| 5月20号全天出差 | 5月20日 全天 |
| 上午9点晨会 | 今天 09:00-10:00 |
| 下午2点面试 | 今天 14:00-15:00 |
