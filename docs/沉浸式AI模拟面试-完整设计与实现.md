# 沉浸式 AI 模拟面试 — 完整设计与实现

## 一、设计思路

### 1.1 核心需求

打造一个**像打电话一样**的 AI 模拟面试体验：

- AI 自动朗读问题（TTS）
- 用户语音回答，实时显示识别字幕
- 回答结束自动检测：静音 8 秒 **或** 周期性语义模型判断 **或** 用户主动点击"回答完毕"
- 每轮回答后 AI 自动评分，显示评分结果和参考答案
- 左侧面板始终显示历史对话记录（按轮次分组）
- 自动进入下一题，循环 N 轮
- 全程无需手动操作（除了开始和结束）

### 1.2 与文字面试的对比

| 维度 | 文字面试（InterviewRoom.vue） | 沉浸式面试（ImmersiveInterview.vue） |
|------|------|------|
| 交互方式 | 手动输入/录音/发送 | 全自动流水线 |
| 问题展示 | 文字显示在聊天区 | AI 语音朗读 + 文字显示 |
| 回答方式 | 手动点击录音按钮 | TTS 播放结束后自动开始录音 |
| 结束检测 | 手动点击发送 | 静音 8s / 语义模型 / 手动按钮 |
| 语音识别 | 录音后一次性上传 STT | Web Speech API 实时字幕 + MiMo ASR 最终识别 |
| 评分展示 | 聊天消息气泡 | 独立评分卡片 + 左侧历史面板 |
| UI 风格 | 聊天界面（浅色主题） | 电话式全屏界面（深色主题） |
| 后端编排 | 前端发送 answer，后端评分 | ASR WebSocket → 编排服务自动评分 |

### 1.3 关键设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 新建组件 vs 改造 InterviewRoom | **新建 ImmersiveInterview.vue** | UI 完全不同，状态机逻辑复杂 |
| 双 WebSocket 架构 | **Interview WS + ASR WS** | 面试控制和语音识别分离，各自独立生命周期 |
| 实时字幕方案 | **Web Speech API（浏览器内置）** | 零延迟、无需后端资源，用于打字机效果 |
| 最终识别方案 | **MiMo ASR（后端）** | 准确率高，用于实际评分 |
| 语义判断方案 | **周期性 HTTP 调用 DeepSeek** | 每 3s 检查一次，严格模式只有结束语才触发 |
| 静音检测 | **前端 Web Audio API AnalyserNode** | 后端无法获取音频流，前端检测最直接 |
| 编排位置 | **后端 ImmersiveInterviewOrchestrator** | ASR 完成后后端自动评分，前端只负责 UI |
| 麦克风管理 | **整个面试期间保持打开** | 避免每轮重复请求权限 |
| 回答写入时机 | **pendingAnswer 延迟写入** | 后端确认 saveQA() 后才写入本地，防止刷新导致问答错位 |
| 历史恢复 | **sessionStorage + 服务端双兜底** | 优先本地缓存，失败从数据库加载，保证跨会话一致性 |

## 二、架构总览

### 2.1 系统架构图

```
┌───────────────────────────────────────────────────────────────────────────────────────┐
│                              浏览器（Vue 3 SPA）                                      │
│                                                                                       │
│  ┌────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    ImmersiveInterview.vue                                      │   │
│  │                                                                                │   │
│  │  ┌──────────────┐  ┌───────────────┐  ┌────────────────────────┐              │   │
│  │  │ Web Speech   │  │ MediaRecorder │  │  AnalyserNode          │              │   │
│  │  │ API 实时字幕  │  │ 录音+分片      │  │  波形+静音检测          │              │   │
│  │  └──────┬───────┘  └──────┬────────┘  └──────────┬─────────────┘              │   │
│  │         │                 │                       │                            │   │
│  │         │  currentAnswer  │  audio chunks         │ silence                    │   │
│  │         ▼                 ▼                       ▼                            │   │
│  │  ┌──────────────────────────────────────────────────────────────────────┐      │   │
│  │  │                    状态机（Phase）驱动                                │      │   │
│  │  │  idle → connecting → tts_playing → recording →                     │      │   │
│  │  │  processing → scoring → showing_score → (下一题/完成)               │      │   │
│  │  └──────────────────────────────────────────────────────────────────────┘      │   │
│  └───────────┬──────────────────┬───────────────────────┬─────────────────────────┘   │
│              │                  │                       │                             │
│     WS /ws/interview    WS /ws/asr              HTTP POST /check-complete            │
│     (面试控制)           (语音识别)                (语义判断, 每3s)                     │
└──────────────┼──────────────────┼───────────────────────┼─────────────────────────────┘
               │                  │                       │
┌──────────────┼──────────────────┼───────────────────────┼─────────────────────────────┐
│              ▼                  ▼                       ▼        Spring Boot 后端      │
│  ┌──────────────────┐  ┌───────────────────────┐  ┌──────────────────────────────┐   │
│  │InterviewWebSocket│  │     AsrWebSocket      │  │    InterviewController       │   │
│  │                  │  │                       │  │                              │   │
│  │ immersive_start  │  │ asr_init → 存储 ctx   │  │ /api/interview/check-complete│   │
│  │ → next_question  │  │ audio_final → MiMo ASR│  │   → DeepSeek 语义判断        │   │
│  │                  │  │   → orchestrator()    │  │                              │   │
│  └────────┬─────────┘  └───────────┬───────────┘  └──────────────┬───────────────┘   │
│           │                        │                              │                   │
│           │            ┌───────────▼──────────────────┐          │                   │
│           │            │ ImmersiveInterviewOrchestrator│          │                   │
│           │            │                               │          │                   │
│           │            │ 1. 接收 ASR 最终文本           │          │                   │
│           │            │ 2. 调用 DeepSeek 评分          │          │                   │
│           │            │ 3. 保存 QA 记录               │          │                   │
│           │            │ 4. 推送 asr_evaluation 给前端  │          │                   │
│           │            │ 5. 最后一轮生成报告            │          │                   │
│           │            └───────────┬──────────────────┘          │                   │
│           │                        │                              │                   │
│  ┌────────▼─────────┐  ┌──────────▼────────────────┐  ┌─────────▼─────────────────┐  │
│  │ InterviewService │  │ DeepSeekService           │  │ DeepSeekService           │  │
│  │ (问题/评分/报告)  │  │ (AI 评分 + 语义判断)      │  │ (AI 评分 + 语义判断)      │  │
│  └──────────────────┘  └───────────────────────────┘  └───────────────────────────┘  │
│                                                                                      │
│  ┌──────────────────┐  ┌───────────────────────────────────────────────────────────┐  │
│  │ MiMoTtsService   │  │ InterviewController                                       │  │
│  │ (TTS 语音合成)    │  │ /api/interview/tts                                        │  │
│  │                  │  │ /api/interview/check-complete                              │  │
│  └──────────────────┘  └───────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

**数据一致性流程：**

```
前端 stopAndProcessRecording
  → pendingAnswer = text（暂存，不写入 messages）
  → audio_final → ASR WS → Orchestrator.evaluateAndSave()
    → saveQA()（写入数据库）→ asr_evaluation（推送前端）
  → 前端收到 asr_evaluation
    → pendingAnswer → messages（写入本地）→ saveMessages()
  → 本地缓存与数据库始终一致，刷新不会错位
```

**三种连接方式：**

| 连接 | 协议 | 生命周期 | 职责 |
|------|------|---------|------|
| Interview WebSocket | WS `/ws/interview` | 整个面试期间 | 面试控制：启动、下一题、评分结果、完成通知 |
| ASR WebSocket | WS `/ws/asr` | 每轮建立一次 | 语音识别：发送音频、接收识别文本、触发后端评分 |
| 语义判断 | HTTP `POST /check-complete` | 每 3s 轮询 | 判断回答是否结束，严格模式只识别结束语 |


### 2.2 双 WebSocket 架构

系统使用两个独立的 WebSocket 连接：

| 连接 | 端点 | 生命周期 | 职责 |
|------|------|---------|------|
| Interview WebSocket | `/ws/interview?id={interviewId}` | 整个面试期间 | 面试控制：启动、下一题、评分结果、完成通知 |
| ASR WebSocket | `/ws/asr` | 每轮建立一次 | 语音识别：发送音频、接收识别文本、触发后端评分 |

**为什么需要两个 WebSocket？**

1. **关注点分离**：面试控制（哪些题、评分结果）和语音识别（音频处理）是不同的关注点
2. **独立生命周期**：ASR WebSocket 每轮重建，避免长时间连接积累状态
3. **编排解耦**：ASR WebSocket 收到最终文本后，由 Orchestrator 自动触发评分，无需前端中转

## 三、时序图

### 3.1 完整面试流程（单轮）

```
前端                        Interview WS        ASR WS           Orchestrator      DeepSeek/MiMo
  │                             │                  │                  │                 │
  │═════════════════════════════│══════════════════│══════════════════│═════════════════│
  │  第 N 轮开始                 │                  │                  │                 │
  │═════════════════════════════│══════════════════│══════════════════│═════════════════│
  │                             │                  │                  │                 │
  │  1. 收到 next_question       │                  │                  │                 │
  │◄────────────────────────────│                  │                  │                 │
  │                             │                  │                  │                 │
  │  2. POST /tts 播放问题语音   │──────────────────────────────────────────────────────>│
  │◄══════════════════════════════════════════════════════════════════════════ MP3 ════>│
  │                             │                  │                  │                 │
  │  3. TTS 播放完成             │                  │                  │                 │
  │     → 自动开始录音           │                  │                  │                 │
  │                             │                  │                  │                 │
  │  4. 连接 ASR WebSocket      │                  │                  │                 │
  │     发送 asr_init           │─────────────────>│                  │                 │
  │                             │                  │ 存储 context     │                 │
  │                             │                  │                  │                 │
  │  5. Web Speech API 实时字幕  │                  │                  │                 │
  │     每 3s POST /check-complete ─────────────────────────────────────────────────────>│
  │     ◄──────────────────────────────────────────────── complete: false ──────────────│
  │                             │                  │                  │                 │
  │  6. 用户说话...              │                  │                  │                 │
  │     MediaRecorder 录音       │                  │                  │                 │
  │     AnalyserNode 波形可视化   │                  │                  │                 │
  │                             │                  │                  │                 │
  │  7. 静音 8s / 语义判断YES    │                  │                  │                 │
  │     / 用户点击"回答完毕"     │                  │                  │                 │
  │     → stopAndProcessRecording│                  │                  │                 │
  │     → pendingAnswer = text   │                  │                  │                 │
  │                             │                  │                  │                 │
  │  8. 合并音频为 WAV base64    │                  │                  │                 │
  │     发送 audio_final         │─────────────────>│                  │                 │
  │                             │                  │  调用 MiMo ASR   │                 │
  │                             │                  │─────────────────────────────────────>│
  │                             │                  │  流式 SSE 返回    │                 │
  │                             │                  │<─────────────────────────────────────│
  │  9. asr_result (实时字幕)    │                  │                  │                 │
  │◄────────────────────────────│                  │                  │                 │
  │                             │                  │  asr_final       │                 │
  │  10. asr_final              │                  │                  │                 │
  │◄────────────────────────────│                  │                  │                 │
  │                             │                  │  onAsrFinal()    │                 │
  │                             │                  │─────────────────>│                 │
  │                             │                  │                  │  evaluateAnswer │
  │                             │                  │                  │────────────────>│
  │                             │                  │                  │  ScoreResult    │
  │                             │                  │                  │<────────────────│
  │                             │                  │                  │  saveQA()       │
  │                             │                  │                  │                 │
  │  11. asr_evaluation         │                  │                  │                 │
  │◄────────────────────────────│                  │                  │                 │
  │                             │                  │                  │                 │
  │  12. 写入 pendingAnswer → messages           │                  │
  │     显示评分卡片            │                  │                  │                 │
  │     更新左侧面板             │                  │                  │                 │
  │     延迟 2s → handleNextQuestion             │                  │
  │                         (去重+TTS+录音)       │                  │                 │
  │                             │                  │                  │                 │
  │═════════════════════════════│══════════════════│══════════════════│═════════════════│
  │  循环到第 N+1 轮             │                  │                  │                 │
  │═════════════════════════════│══════════════════│══════════════════│═════════════════│
```

### 3.2 面试启动流程

```
前端                         Interview WS          后端
  │                              │                   │
  │  点击"开始面试"               │                   │
  │  → requestMicPermission()    │                   │
  │  → connectWebSocket()        │                   │
  │                              │                   │
  │  WS 连接成功                  │                   │
  │  发送 immersive_start        │                   │
  │─────────────────────────────>│                   │
  │                              │  查询面试状态       │
  │                              │──────────────────>│
  │                              │  获取 QA 历史      │
  │                              │  找到未回答的轮次   │
  │                              │                   │
  │  接收 next_question           │                   │
  │◄─────────────────────────────│                   │
  │                              │                   │
  │  → playTTSAuto(question)     │                   │
  │  → POST /api/interview/tts   │                   │
  │──────────────────────────────────────────────────>│
  │  ← MP3 音频流                 │                   │
  │◄══════════════════════════════════════════════════│
  │                              │                   │
  │  → Audio.play()              │                   │
  │  → onended → startAutoRecording│                  │
  │                              │                   │
```

### 3.3 语义判断流程

```
前端                         DeepSeek API
  │                              │
  │  每 3s 触发一次               │
  │  setInterval(async () => {   │
  │    if (phase !== RECORDING)  │
  │      return                  │
  │    if (semanticChecking)     │
  │      return  // 防并发       │
  │    if (text.length < 5)      │
  │      return  // 太短不判断   │
  │                              │
  │    semanticChecking = true   │
  │    POST /check-complete      │
  │    { text: accumulatedText } │
  │─────────────────────────────>│
  │                              │  DeepSeek 判断：
  │                              │  - 有结束语 → YES
  │                              │  - 无结束语 → NO
  │    { complete: true/false }  │
  │<─────────────────────────────│
  │                              │
  │    if (complete)             │
  │      stopAndProcessRecording │
  │    semanticChecking = false  │
  │  })                          │
```

**语义判断的严格模式 prompt：**

```
判断以下面试回答是否已经结束。只回答YES或NO。

【严格规则】只有包含明确结束语才算结束，其他情况一律回答NO。

结束语包括：
- 回答完毕
- 我说完了
- 我的回答完毕
- 以上就是我的回答
- 就这些
- 没有了
- 总的来说...
- 综上所述...
- 以上是...

注意：
- 语句完整不算结束，必须有结束语
- 话没说完不算结束
- 即使内容很完整，没有结束语也回答NO
```

## 四、后端实现

### 4.1 WebSocket 配置

```java
// WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 面试 WebSocket — 控制面试流程
        registry.addHandler(interviewWebSocket, "/ws/interview")
                .setAllowedOrigins("*");

        // ASR WebSocket — 实时语音识别 + 后端编排
        registry.addHandler(asrWebSocket, "/ws/asr")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 5MB — 完整录音 base64 可达数 MB
        container.setMaxTextMessageBufferSize(5 * 1024 * 1024);
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024);
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return container;
    }
}
```

### 4.2 AsrWebSocket — 实时语音识别

**核心职责：**
1. 接收前端音频分片（base64 WAV）
2. 调用 MiMo ASR 流式接口
3. 将识别结果实时转发给前端
4. 最终结果通知 Orchestrator 进行评分

```java
// AsrWebSocket.java（关键片段）
@Slf4j
@Component
public class AsrWebSocket extends TextWebSocketHandler {

    private final ImmersiveInterviewOrchestrator orchestrator;
    private final ConcurrentHashMap<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    record SessionContext(Long interviewId, int round) {}

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) payload.get("type");

        // 1. 初始化：记录面试 ID 和轮次
        if ("asr_init".equals(type)) {
            Long interviewId = Long.valueOf(payload.get("interviewId").toString());
            int round = Integer.parseInt(payload.get("round").toString());
            sessionContexts.put(session.getId(), new SessionContext(interviewId, round));
            return;
        }

        // 2. 接收音频 → 异步调用 MiMo ASR
        boolean isFinal = "audio_final".equals(type);
        SessionContext ctx = sessionContexts.get(session.getId());

        CompletableFuture.runAsync(() -> {
            byte[] wavBytes = Base64.getDecoder().decode(audioBase64);
            callMimoAsrStreaming(wavBytes, session, isFinal, ctx, segmentIndex);
        }, aiExecutor);
    }

    private void callMimoAsrStreaming(byte[] wavBytes, WebSocketSession session,
                                       boolean isFinal, SessionContext ctx, int segmentIndex) {
        // 构建 MiMo ASR 请求
        Map<String, Object> requestBody = Map.of(
            "model", "mimo-v2.5-asr",
            "messages", List.of(
                Map.of("role", "user", "content", List.of(
                    Map.of("type", "input_audio",
                        "input_audio", Map.of("data", "data:audio/wav;base64," + base64Audio))
                ))
            ),
            "stream", true
        );

        // 流式读取 SSE
        StringBuilder fullText = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            JsonNode json = objectMapper.readTree(data);
            String content = json.at("/choices/0/delta/content").asText(null);
            if (content != null) {
                fullText.append(content);
                // 实时转发给前端
                sendResult(session, fullText.toString(), false, segmentIndex);
            }
        }

        // 发送最终结果
        sendResult(session, finalText, true, segmentIndex);

        // 最终结果 → 通知编排服务评分
        if (isFinal && ctx != null) {
            orchestrator.onAsrFinal(session, ctx.interviewId(), ctx.round(), finalText);
        }
    }
}
```

### 4.3 ImmersiveInterviewOrchestrator — 后端编排

**核心职责：** 接收 ASR 最终文本 → 自动评分 → 保存 → 推送结果

```java
// ImmersiveInterviewOrchestrator.java（关键片段）
@Slf4j
@Service
public class ImmersiveInterviewOrchestrator {

    /** interviewId -> (round -> 锁) 防止同一轮次并发处理 */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, ReentrantLock>> locks
        = new ConcurrentHashMap<>();

    public void onAsrFinal(WebSocketSession session, Long interviewId, int round, String asrText) {
        // 加锁防止并发
        ReentrantLock lock = locks.computeIfAbsent(interviewId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(round, k -> new ReentrantLock());
        lock.lock();
        try {
            // 异步执行评分流程
            CompletableFuture.runAsync(() -> {
                evaluateAndSave(session, interviewId, round, asrText);
            }, aiExecutor);
        } finally {
            lock.unlock();
        }
    }

    private void evaluateAndSave(WebSocketSession session, Long interviewId,
                                  int round, String answer) throws Exception {
        // 1. 查询面试和问题
        Interview interview = interviewService.getInterviewById(interviewId);
        InterviewQA qa = interviewService.getQAByRound(interviewId, round);
        String question = qa.getQuestion();

        // 2. AI 评分
        ScoreResult scoreResult = deepSeekService.evaluateAnswer(
            question, answer, interview.getId() + "_" + interview.getUserId());

        // 3. 保存问答记录
        interviewService.saveQA(interviewId, round, question, answer, scoreResult);

        // 4. 推送评分结果给前端
        boolean isLastRound = round >= interview.getTotalRounds();
        sendMessage(session, Map.of(
            "type", "asr_evaluation",
            "round", round,
            "score", scoreResult,
            "feedback", scoreResult.getFeedback(),
            "referenceAnswer", scoreResult.getReferenceAnswer(),
            "isLastRound", isLastRound
        ));

        // 5. 最后一轮 → 生成报告
        if (isLastRound) {
            interviewService.completeInterview(interviewId);
            String report = interviewService.generateReportForInterview(...);
            interviewService.saveReport(interviewId, report);
            sendMessage(session, Map.of("type", "completed", "report", report));
        }

        // 6. 清理锁
        cleanup(interviewId, round);
    }
}
```

### 4.4 DeepSeekService — AI 评分 + 语义判断

```java
// DeepSeekServiceImpl.java（关键片段）

/**
 * 多维度评分（5 个维度，0-10 分）
 * accuracy(30%) + clarity(20%) + logic(25%) + depth(15%) + practice(10%)
 */
public ScoreResult evaluateAnswer(String question, String answer, String conversationId) {
    String systemPrompt = "你是一位技术面试官。请严格评估面试回答。";
    String userPrompt = String.format(
        "请严格评估以下面试回答。\n\n" +
        "问题：%s\n回答：%s\n\n" +
        "评分标准（0-10分）：\n" +
        "- 0分：回答为不知道/忘了/不会...\n" +
        "- 1-3分：回答严重不足\n" +
        "- 4-6分：基本回答了问题\n" +
        "- 7-9分：回答准确、有深度\n" +
        "- 10分：回答完美\n\n" +
        "【关键规则】如果回答中出现'不知道'等词语，所有维度都必须给0分。\n\n" +
        "只返回纯JSON：{\"accuracy\":数字,\"clarity\":数字,\"logic\":数字," +
        "\"depth\":数字,\"practice\":数字,\"feedback\":\"...\",\"referenceAnswer\":\"...\"}",
        question, answer
    );

    String response = aiClient.call(systemPrompt, userPrompt, conversationId);
    return parseScoreResult(response);
}

/**
 * 语义判断回答是否结束（严格模式）
 * 只有包含明确结束语才返回 YES
 */
public boolean isAnswerComplete(String answer) {
    String prompt = String.format(
        "判断以下面试回答是否已经结束。只回答YES或NO。\n\n" +
        "【严格规则】只有包含明确结束语才算结束，其他情况一律回答NO。\n\n" +
        "结束语包括：回答完毕、我说完了、以上就是我的回答...\n\n" +
        "回答内容：%s", answer
    );
    String result = aiClient.call("你是语义分析助手。只回答YES或NO。", prompt, null);
    return result.trim().toUpperCase().contains("YES");
}
```

### 4.5 TTS 服务

```java
// MiMoTtsService.java（关键片段）
// MiMo TTS 走 /v1/chat/completions 端点（不是 /v1/audio/speech）
public byte[] synthesize(String text) throws Exception {
    Map<String, Object> requestBody = Map.of(
        "model", "mimo-v2.5-tts-voicedesign",
        "messages", List.of(
            Map.of("role", "user", "content",
                "Give me a calm, professional, and steady male interviewer tone"),
            Map.of("role", "assistant", "content", text)
        ),
        "audio", Map.of("format", "mp3"),
        "stream", true
    );

    // 流式读取 SSE，收集所有 base64 音频 chunk
    List<byte[]> audioChunks = new ArrayList<>();
    while ((line = reader.readLine()) != null) {
        JsonNode json = objectMapper.readTree(data);
        String audioData = json.at("/choices/0/delta/audio/data").asText(null);
        if (audioData != null) {
            audioChunks.add(Base64.getDecoder().decode(audioData));
        }
    }

    // 合并所有 chunk
    return mergeChunks(audioChunks);
}
```

## 五、前端实现

### 5.1 状态机设计

```javascript
const PHASE = {
  IDLE: 'idle',           // 初始状态
  CONNECTING: 'connecting', // WebSocket 连接中
  TTS_PLAYING: 'tts_playing', // AI 正在朗读问题
  WAITING_ANSWER: 'waiting_answer', // 等待用户回答
  RECORDING: 'recording',   // 录音中
  PROCESSING: 'processing', // 处理中（停止录音、发送音频）
  SCORING: 'scoring',       // 等待 AI 评分
  SHOWING_SCORE: 'showing_score', // 显示评分
  COMPLETED: 'completed',   // 面试完成
  ERROR: 'error'            // 出错
}
```

**状态流转：**

```
IDLE → CONNECTING → TTS_PLAYING → WAITING_ANSWER → RECORDING
                                                      ↓
                                              PROCESSING → SHOWING_SCORE
                                                      ↑         ↓
                                                      └─── (下一题)
                                                            ↓
                                                     COMPLETED
```

### 5.2 双 WebSocket 管理

```javascript
// Interview WebSocket — 整个面试期间保持
const connectWebSocket = () => {
  phase.value = PHASE.CONNECTING
  ws = new WebSocket(`ws://${wsHost}/ws/interview?id=${interviewId}`)

  ws.onopen = () => {
    ws.send(JSON.stringify({ type: 'immersive_start', interviewId: Number(interviewId) }))
  }

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    handleWebSocketMessage(data) // next_question / evaluation / completed
  }
}

// ASR WebSocket — 每轮建立一次
const connectAsrWebSocket = () => {
  asrWs = new WebSocket(`ws://${wsHost}/ws/asr`)

  asrWs.onopen = () => {
    asrWs.send(JSON.stringify({
      type: 'asr_init',
      interviewId: Number(interviewId),
      round: currentRound.value
    }))
  }

  asrWs.onmessage = (event) => {
    const data = JSON.parse(event.data)
    if (data.type === 'asr_result') {
      // 实时字幕更新
      currentAnswerText.value = data.text
    } else if (data.type === 'asr_evaluation') {
      // 后端自动评分结果
      handleAsrEvaluation(data)
    }
  }
}
```

### 5.3 Web Speech API 实时字幕

```javascript
const startSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  speechRecognition = new SpeechRecognition()
  speechRecognition.lang = 'zh-CN'
  speechRecognition.continuous = true
  speechRecognition.interimResults = true

  speechConfirmedText = ''

  speechRecognition.onresult = (event) => {
    let interim = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0].transcript
      if (event.results[i].isFinal) {
        speechConfirmedText += transcript  // 已确认文本累积
      } else {
        interim += transcript              // 中间文本
      }
    }
    // 实时更新字幕（已确认 + 中间文本）
    currentAnswerText.value = speechConfirmedText + interim
  }

  // 自动重启（浏览器可能自动停）
  speechRecognition.onend = () => {
    if (phase.value === PHASE.RECORDING) {
      speechRecognition.start()
    }
  }

  speechRecognition.start()
}
```

### 5.4 录音 + 静音检测 + 波形可视化

```javascript
const startAutoRecording = async () => {
  phase.value = PHASE.RECORDING

  // 1. MediaRecorder 分片录制（每 1.5s 一个 chunk）
  mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' })
  mediaRecorder.ondataavailable = (e) => audioChunks.push(e.data)
  mediaRecorder.start(1500)

  // 2. AnalyserNode — 波形可视化 + 静音检测
  audioContext = new AudioContext()
  const source = audioContext.createMediaStreamSource(stream)
  analyser = audioContext.createAnalyser()
  source.connect(analyser)

  drawWaveform()          // Canvas 波形动画
  startSilenceDetection() // 静音检测（8s 触发）

  // 3. Web Speech API 实时字幕
  startSpeechRecognition()

  // 4. 周期性语义判断（每 3s）
  startSemanticCheck()
}

// 静音检测：校准环境噪音 → 检测音量 → 8s 静音触发
const startSilenceDetection = () => {
  const checkVolume = () => {
    analyser.getByteFrequencyData(dataArray)
    const avg = dataArray.reduce((a, b) => a + b, 0) / dataArray.length

    if (!noiseCalibrated) {
      // 前 500ms 校准环境噪音
      calibrateSamples.push(avg)
      if (Date.now() >= calibrateEndTime) {
        ambientNoiseLevel = avg / calibrateSamples.length
        noiseCalibrated = true
      }
    } else {
      const threshold = Math.max(ambientNoiseLevel * 2.5, 15)
      if (avg > threshold) {
        lastSpeechTime = Date.now()  // 有声音
      } else if (Date.now() - lastSpeechTime > 8000) {
        stopAndProcessRecording()    // 静音 8s → 停止
        return
      }
    }
    silenceCheckId = requestAnimationFrame(checkVolume)
  }
  silenceCheckId = requestAnimationFrame(checkVolume)
}
```

### 5.5 停止录音并处理（pendingAnswer 机制）

录音停止后，**不立即**将回答写入 `messages`，而是暂存到 `pendingAnswer`，等后端 `asr_evaluation` 确认保存后才写入。这保证了页面刷新后本地缓存与数据库的一致性。

```javascript
// 暂存待确认的回答文本
let pendingAnswer = ''

const stopAndProcessRecording = async () => {
  phase.value = PHASE.PROCESSING

  // 停止所有子系统
  stopSpeechRecognition()
  stopSemanticCheck()
  cancelAnimationFrame(silenceCheckId)
  audioContext.close()

  // 停止 MediaRecorder
  await new Promise(resolve => {
    mediaRecorder.onstop = resolve
    mediaRecorder.stop()
  })

  // 合并 webm 分片 → 转为 WAV base64
  const completeWavBase64 = await mergeChunksToWavBase64(audioChunks)

  // 发送最终音频到 ASR WebSocket
  // 后端收到 audio_final → MiMo ASR → orchestrator.onAsrFinal()
  //   → 评分 → saveQA() → 发送 asr_evaluation
  sendAudioChunk(completeWavBase64, true)

  // 【关键】暂存回答文本，等后端 asr_evaluation 确认后再写入 messages
  // 避免：用户刷新页面 → 回答在本地存在但数据库未保存 → 重新加载后问答错位
  pendingAnswer = asrAccumulatedText || currentAnswerText.value || ''

  // 不再主动 push 到 messages，由 handleAsrEvaluation 统一处理
}
```

**为什么不能立即写入 messages？**

```
时序风险（旧方案）：
  前端 push 回答 → 发送 audio_final → [用户刷新页面] → 数据库无记录
  → loadHistoryFromServer 加载空数据 → 问答错位

时序安全（新方案）：
  前端暂存 pendingAnswer → 发送 audio_final → 后端 saveQA() → asr_evaluation
  → 前端收到确认 → 才 push 回答到 messages → saveMessages()
  → 此时数据库已有记录，刷新后可从服务器恢复
```

### 5.6 音频编码：WebM → WAV

```javascript
// audioChunkEncoder.js
// MediaRecorder 输出 webm/opus，MiMo ASR 需要 WAV/PCM

export async function mergeChunksToWavBase64(chunks) {
  // 1. 合并所有 webm 分片为一个 Blob
  const mergedBlob = new Blob(chunks, { type: 'audio/webm' })

  // 2. AudioContext 解码 webm → PCM AudioBuffer
  const arrayBuffer = await mergedBlob.arrayBuffer()
  const audioCtx = new AudioContext()
  const audioBuffer = await audioCtx.decodeAudioData(arrayBuffer)

  // 3. 手动编码 WAV（16-bit PCM）
  const wavBlob = encodeWav(audioBuffer)

  // 4. 转为 base64
  return await blobToBase64(wavBlob)
}

function encodeWav(audioBuffer) {
  const numChannels = audioBuffer.numberOfChannels
  const sampleRate = audioBuffer.sampleRate
  const samples = audioBuffer.getChannelData(0)
  const dataLength = samples.length * 2  // 16-bit = 2 bytes

  // WAV 头（44 字节）+ PCM 数据
  const buffer = new ArrayBuffer(44 + dataLength)
  const view = new DataView(buffer)

  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + dataLength, true)
  writeString(view, 8, 'WAVE')
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true)           // fmt chunk size
  view.setUint16(20, 1, true)            // PCM format
  view.setUint16(22, numChannels, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * numChannels * 2, true)
  view.setUint16(32, numChannels * 2, true)
  view.setUint16(34, 16, true)           // bit depth
  writeString(view, 36, 'data')
  view.setUint32(40, dataLength, true)

  // 写入 PCM 样本
  let offset = 44
  for (let i = 0; i < samples.length; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]))
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true)
    offset += 2
  }

  return new Blob([buffer], { type: 'audio/wav' })
}
```

### 5.7 历史面板（按轮次分组）

```javascript
// 按轮次分组的计算属性
const groupedMessages = computed(() => {
  const rounds = {}
  for (const msg of messages.value) {
    const r = msg.round
    if (!rounds[r]) rounds[r] = { round: r, question: '', answer: '', evaluation: null }
    if (msg.type === 'question') rounds[r].question = msg.content
    else if (msg.type === 'user') rounds[r].answer = msg.content
    else if (msg.type === 'evaluation') rounds[r].evaluation = msg
  }
  return Object.values(rounds).sort((a, b) => a.round - b.round)
})
```

**模板结构：**

```html
<aside class="history-panel">
  <div class="history-panel__title">历史对话</div>
  <div class="history-panel__list">
    <div class="history-round" v-for="roundData in groupedMessages">
      <div class="history-round__header">第 {{ roundData.round }} 轮</div>

      <!-- 问题 -->
      <div class="history-round__question">
        <div class="history-round__label">问题</div>
        <div class="history-round__text">{{ roundData.question }}</div>
      </div>

      <!-- 回答 -->
      <div class="history-round__answer" v-if="roundData.answer">
        <div class="history-round__label">回答</div>
        <div class="history-round__text">{{ roundData.answer }}</div>
      </div>

      <!-- 评分 -->
      <div class="history-round__score" v-if="roundData.evaluation">
        <span class="history-round__score-value">
          {{ roundData.evaluation.score?.totalScore?.toFixed(1) }} 分
        </span>
        <div class="history-round__feedback">{{ roundData.evaluation.feedback }}</div>

        <!-- 参考答案 -->
        <div class="history-round__ref" v-if="roundData.evaluation.referenceAnswer">
          <div class="history-round__ref-label">参考答案</div>
          <div class="history-round__text">{{ roundData.evaluation.referenceAnswer }}</div>
        </div>
      </div>
    </div>
  </div>
</aside>
```

### 5.8 评分处理与自动推进（handleAsrEvaluation）

收到后端 `asr_evaluation` 后，统一处理：写入回答 → 保存评分 → 自动进入下一题。

```javascript
const handleAsrEvaluation = (data) => {
  // 1. 后端已确认 saveQA()，此时才将 pendingAnswer 写入 messages
  if (pendingAnswer) {
    messages.value.push({ type: 'user', content: pendingAnswer, round: data.round })
    pendingAnswer = ''
  }

  // 2. 保存评分到 messages 和 latestScore
  latestScore.value = {
    score: data.score,
    feedback: data.feedback,
    referenceAnswer: data.referenceAnswer
  }
  messages.value.push({
    type: 'evaluation',
    round: data.round,
    score: data.score,
    feedback: data.feedback,
    referenceAnswer: data.referenceAnswer
  })
  saveMessages()

  phase.value = PHASE.SHOWING_SCORE

  // 3. 关闭本轮 ASR WebSocket
  if (asrWs) { asrWs.close(); asrWs = null }

  // 4. 非最后一轮 → 延迟 2s 后自动进入下一题
  if (!data.isLastRound) {
    setTimeout(() => {
      const nextRound = data.round + 1
      const questions = JSON.parse(
        sessionStorage.getItem(`interview_questions_${interviewId}`) || '[]'
      )
      const nextQuestion = questions[nextRound - 1]
      if (nextQuestion) {
        // 走 handleNextQuestion 统一入口（自动去重 + 清理状态）
        handleNextQuestion({
          round: nextRound,
          question: nextQuestion,
          totalRounds: totalRounds.value
        })
      }
    }, 2000)
  }
}
```

**为什么自动推进也走 `handleNextQuestion`？** 所有"添加问题到 messages"的操作统一走一个入口，确保去重逻辑和状态清理一致，避免遗漏。

### 5.9 问题去重（handleNextQuestion）

`handleNextQuestion` 是所有问题添加的统一入口，内置去重检查：

```javascript
const handleNextQuestion = (data) => {
  // 重置状态
  currentRound.value = data.round
  currentQuestion.value = data.question
  latestScore.value = null
  currentAnswerText.value = ''
  asrAccumulatedText = ''
  speechConfirmedText = ''
  pendingAnswer = ''        // 清除上一轮未确认的回答

  // 关闭上一轮的 ASR WebSocket
  if (asrWs) { asrWs.close(); asrWs = null }

  // 去重：如果该轮问题已存在（从历史加载），不重复添加
  const alreadyHasQuestion = messages.value.some(
    m => m.type === 'question' && m.round === data.round
  )
  if (!alreadyHasQuestion) {
    messages.value.push({ type: 'question', content: data.question, round: data.round })
  }
  saveMessages()

  // 自动播放 TTS
  phase.value = PHASE.TTS_PLAYING
  playTTSAuto(data.question)
}
```

**触发场景：**
1. **Interview WS `next_question`** — 首次启动或服务端推送
2. **`handleAsrEvaluation` 自动推进** — 评分完成后 2s 自动进入下一题
3. 两种场景共用同一入口，去重逻辑防止重复添加

### 5.10 历史记录加载策略

页面加载时按优先级恢复历史：

```javascript
onMounted(async () => {
  // 优先级 1: 从 sessionStorage 恢复（最快，无网络请求）
  if (!loadCachedMessages()) {
    // 优先级 2: 从服务器加载（sessionStorage 失败时的兜底）
    await loadHistoryFromServer()
  }
})
```

**loadHistoryFromServer 实现：**

```javascript
const loadHistoryFromServer = async () => {
  const res = await request.get(`/api/interview/${interviewId}/history`)
  if (res.data.success && res.data.data.length > 0) {
    const historyMessages = []
    // 只加载已回答的问题（answer 非空）
    res.data.data.filter(qa => qa.answer).forEach(qa => {
      historyMessages.push({ type: 'question', content: qa.question, round: qa.round })
      historyMessages.push({ type: 'user', content: qa.answer, round: qa.round })
      if (qa.feedback) {
        let scores = {}
        try { scores = JSON.parse(qa.scores) } catch {}
        historyMessages.push({
          type: 'evaluation',
          score: scores,
          feedback: qa.feedback,
          referenceAnswer: qa.referenceAnswer || scores.referenceAnswer || '',
          round: qa.round
        })
      }
      currentRound.value = qa.round
      currentQuestion.value = qa.question
    })
    if (historyMessages.length > 0) {
      messages.value = historyMessages
      saveMessages()  // 回写 sessionStorage，下次加载更快
      return true
    }
  }
  return false
}
```

**为什么需要两种加载方式？**

| 场景 | sessionStorage | 服务端 |
|------|---------------|--------|
| 正常刷新 | 有缓存，立即恢复 | 不需要 |
| 关闭再打开 | 已清空 | 从数据库加载 |
| 换浏览器/设备 | 不共享 | 从数据库加载 |
| pendingAnswer 期间刷新 | 只有已确认的回答 | 从数据库加载（一致） |

### 5.11 麦克风泄漏防护

```javascript
// 路由离开时强制释放麦克风
onBeforeRouteLeave(() => {
  cleanup()
  if (ws) { ws.close(); ws = null }
})

// 关闭/刷新页面时释放
window.addEventListener('beforeunload', () => cleanup())

// cleanup 函数 — 逐项清理，try-catch 保护
const cleanup = () => {
  try { stopTTS() } catch {}
  try { stopSpeechRecognition() } catch {}
  try { stopSemanticCheck() } catch {}
  try { asrWs?.close() } catch {}
  try { mediaRecorder?.stop() } catch {}
  // 关键：释放麦克风
  try {
    micStream.value?.getTracks().forEach(t => t.stop())
    micStream.value = null
  } catch {}
  try { audioContext?.close() } catch {}
}
```

## 六、消息协议汇总

### 6.1 Interview WebSocket

| type | 方向 | 字段 | 说明 |
|------|------|------|------|
| `immersive_start` | 前端→后端 | interviewId | 启动沉浸式面试 |
| `next_question` | 后端→前端 | round, question, totalRounds | 下一个问题 |
| `evaluation` | 后端→前端 | round, score, feedback, referenceAnswer, command | 评分结果 |
| `completed` | 后端→前端 | report | 面试完成 |
| `immersive_exit` | 前端→后端 | interviewId | 提前退出 |

### 6.2 ASR WebSocket

| type | 方向 | 字段 | 说明 |
|------|------|------|------|
| `asr_init` | 前端→后端 | interviewId, round | 初始化会话 |
| `audio_chunk` | 前端→后端 | audio(base64) | 音频分片 |
| `audio_final` | 前端→后端 | audio(base64) | 最终完整音频 |
| `asr_result` | 后端→前端 | text, isFinal:false | 实时识别结果 |
| `asr_final` | 后端→前端 | text, isFinal:true | 最终识别结果 |
| `asr_evaluation` | 后端→前端 | round, score, feedback, referenceAnswer, isLastRound | 自动评分结果 |
| `asr_error` | 后端→前端 | message | 错误信息 |

### 6.3 HTTP 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/interview/tts` | TTS 语音合成（返回 MP3） |
| POST | `/api/interview/check-complete` | 语义判断回答是否结束 |

## 七、文件清单

### 7.1 后端新增/修改

| 文件 | 类型 | 说明 |
|------|------|------|
| `websocket/AsrWebSocket.java` | 新建 | ASR WebSocket 处理器 |
| `service/ImmersiveInterviewOrchestrator.java` | 新建 | 后端编排服务 |
| `service/DeepSeekService.java` | 修改 | 新增 `isAnswerComplete()` |
| `service/impl/DeepSeekServiceImpl.java` | 修改 | 实现语义判断 |
| `controller/InterviewController.java` | 修改 | 新增 `/check-complete` |
| `filter/JwtFilter.java` | 修改 | 白名单 `/check-complete` |
| `config/WebSocketConfig.java` | 修改 | 注册 ASR 端点 |

### 7.2 前端新增/修改

| 文件 | 类型 | 说明 |
|------|------|------|
| `views/ImmersiveInterview.vue` | 新建 | 沉浸式面试主组件（1700+ 行） |
| `utils/audioChunkEncoder.js` | 新建 | WebM → WAV 编码工具 |
| `api/stt.js` | 修改 | STT API 封装 |

## 八、关键技术点

### 8.1 双重识别架构

系统同时使用两种语音识别：

| 维度 | Web Speech API | MiMo ASR |
|------|---------------|----------|
| 位置 | 浏览器端 | 后端 |
| 延迟 | 极低（实时） | 中等（流式 SSE） |
| 准确率 | 中等 | 高 |
| 用途 | 打字机效果实时字幕 | 最终评分用文本 |
| 触发 | 持续运行 | 录音结束时调用 |

**为什么需要双重识别？**
- Web Speech API 提供零延迟的打字机效果，用户体验好
- MiMo ASR 准确率高，确保评分基于正确的文本
- 两者互为备份：Web Speech API 失败时 MiMo ASR 兜底

### 8.2 三重结束检测

| 机制 | 触发条件 | 可靠性 | 延迟 |
|------|---------|--------|------|
| 静音检测 | 8 秒无声音 | 高 | 8s |
| 语义模型 | 包含结束语 | 中（严格模式） | 3s 周期 |
| 手动按钮 | 用户点击"回答完毕" | 最高 | 即时 |

三者任一触发即停止录音，确保灵活性和可靠性。

### 8.3 线程安全

```java
// ReentrantLock 保证同一轮次不并发处理
ConcurrentHashMap<Long, ConcurrentHashMap<Integer, ReentrantLock>> locks

// 使用方式：
ReentrantLock lock = locks
    .computeIfAbsent(interviewId, k -> new ConcurrentHashMap<>())
    .computeIfAbsent(round, k -> new ReentrantLock());
lock.lock();
try {
    // 评分流程
} finally {
    lock.unlock();
}
```

### 8.4 降级策略

| 场景 | 降级方案 |
|------|---------|
| AI 评分失败 | 返回默认评分（各项 5 分） |
| ASR 识别失败 | 使用 Web Speech API 文本 |
| 语义判断异常 | 默认回答未结束（继续录音） |
| TTS 失败 | 跳过播放，直接进入录音 |
| WebSocket 断连 | 自动重连（最多 3 次） |

### 8.5 数据一致性保证

**问题：** 用户在 AI 评分期间（2-5 秒）刷新页面，回答已发送但数据库未保存，导致重新加载后问答错位。

**解决方案：pendingAnswer 延迟写入 + 服务端历史兜底**

```
时间线：
  T1: 用户停止录音 → pendingAnswer = "回答文本"（不写入 messages）
  T2: 发送 audio_final → 后端 MiMo ASR → Orchestrator.evaluateAndSave()
  T3: 后端 saveQA() 写入数据库 → 发送 asr_evaluation
  T4: 前端收到 asr_evaluation → 此时才将 pendingAnswer 写入 messages → saveMessages()

  若用户在 T1~T3 之间刷新：
    - messages 中没有该回答（因为还没写入）
    - 数据库中也没有（因为 saveQA 还没执行）
    - 一致性：两边都没有，不会错位

  若用户在 T3 之后刷新：
    - messages 中有该回答（已写入）
    - 数据库中也有（已保存）
    - 一致性：两边都有，不会错位

  若用户在 T4 之后刷新：
    - messages 中有该回答（saveMessages 已执行）
    - 数据库中也有
    - 一致性：两边都有
```

**历史恢复兜底：** 即使 `sessionStorage` 被清空（关闭浏览器/换设备），`loadHistoryFromServer()` 从数据库加载，数据同样一致——因为只有 `saveQA()` 成功后才会收到 `asr_evaluation`。
