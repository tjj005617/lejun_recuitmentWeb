# AI 文字模拟面试 — 完整设计与实现

## 一、设计思路

### 1.1 核心需求

打造一个**聊天式**的 AI 模拟面试体验：

- AI 面试官根据候选人简历自动生成 10 道面试题
- 候选人通过文字输入或语音输入回答
- 每轮回答后 AI 自动评分，显示评分卡片和参考答案
- 支持 TTS 朗读问题（可选）
- 面试结束后自动生成综合评估报告
- 支持中途退出，基于已有对话生成报告
- 刷新页面可恢复面试进度（sessionStorage + 服务端历史）

### 1.2 与沉浸式面试的对比

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
| 问题生成方式 | **创建时批量生成 10 题** | 单次 AI 调用，减少延迟，前端缓存问题列表 |
| 问题推进 | **前端自主取题** | 后端只评分不推题，前端从 `questions[]` 数组取下一题 |
| 评分模型 | **DeepSeek** | 评分需要严格逻辑，DeepSeek 擅长结构化评估 |
| 问题/报告模型 | **MiMo** | 生成类任务用 MiMo，创意性更好 |
| 会话记忆 | **Redis + conversationId** | `{interviewId}_{userId}` 作为 key，AI 能参考历史轮次 |
| 语音输入 | **录音 → WAV → 后端 STT** | 浏览器 MediaRecorder 录 webm，转 WAV 后调 MiMo ASR |
| 消息持久化 | **sessionStorage + 服务端双写** | 快速恢复 + 断连兜底 |

## 二、架构总览

### 2.1 系统架构图

```
┌───────────────────────────────────────────────────────────────────────────┐
│                          浏览器（Vue 3 SPA）                              │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                   InterviewRoom.vue                                 │  │
│  │                                                                     │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐   │  │
│  │  │ 聊天消息列表 │  │ 文字输入框    │  │  语音按钮               │   │  │
│  │  │ (AI/用户/评分)│  │ Ctrl+Enter   │  │  toggleVoiceInput()    │   │  │
│  │  └─────────────┘  └──────────────┘  └──────────┬──────────────┘   │  │
│  │                                                  │                  │  │
│  │  ┌──────────────────────────────────────────────┐│                  │  │
│  │  │            状态管理（ref）                     ││                  │  │
│  │  │  messages / currentRound / questions /        ││                  │  │
│  │  │  isCompleted / isAiTyping / ttsPlaying        ││                  │  │
│  │  └──────────────────────────────────────────────┘│                  │  │
│  └────────────────────┬──────────────────────────────┘                  │
│                       │                                                  │
│          WS /ws/interview?id={id}          HTTP POST /api/interview/*   │
│          (面试控制 + 评分)                   (创建/历史/TTS/STT)          │
└───────────────────────┼──────────────────────────────────────────────────┘
                        │
┌───────────────────────┼──────────────────────────────────────────────────┐
│                       ▼              Spring Boot 后端                    │
│  ┌────────────────────────────┐  ┌──────────────────────────────────┐   │
│  │    InterviewWebSocket      │  │       InterviewController        │   │
│  │                            │  │                                  │   │
│  │  answer → 异步评分          │  │ POST /create    → 创建+生成问题  │   │
│  │  → 保存QA → 发送evaluation │  │ GET  /{id}      → 查询面试       │   │
│  │  → 最后一轮生成报告         │  │ GET  /{id}/history → QA历史      │   │
│  │  exit → 补评分+生成报告     │  │ POST /tts       → TTS语音合成    │   │
│  └─────────────┬──────────────┘  └──────────────┬───────────────────┘   │
│                │                                │                       │
│  ┌─────────────▼──────────────┐  ┌──────────────▼───────────────────┐   │
│  │    InterviewService        │  │     SttController                │   │
│  │  (面试生命周期管理)         │  │ POST /api/stt → MiMo ASR         │   │
│  └─────────────┬──────────────┘  └──────────────┬───────────────────┘   │
│                │                                │                       │
│  ┌─────────────▼────────────────────────────────▼───────────────────┐   │
│  │                       AI 服务层                                   │   │
│  │                                                                   │   │
│  │  ┌─────────────────────┐  ┌──────────────────────────────────┐   │   │
│  │  │ AIService (MiMo)    │  │ DeepSeekService (DeepSeek)       │   │   │
│  │  │ - generateAllQuestions│  │ - evaluateAnswer()               │   │   │
│  │  │ - generateReport()   │  │ - isAnswerComplete()             │   │   │
│  │  └─────────────────────┘  └──────────────────────────────────┘   │   │
│  │                                                                   │   │
│  │  ┌─────────────────────┐  ┌──────────────────────────────────┐   │   │
│  │  │ MiMoTtsService      │  │ MiMoSttService                   │   │   │
│  │  │ (TTS 语音合成)       │  │ (STT 语音识别)                   │   │   │
│  │  └─────────────────────┘  └──────────────────────────────────┘   │   │
│  │                                                                   │   │
│  │  ┌─────────────────────────────────────────────────────────────┐  │   │
│  │  │ AiClient 抽象层（Template Method）                          │  │   │
│  │  │ AbstractAiClient → MiMoAiClient / DeepSeekAiClient          │  │   │
│  │  │ + Spring AI ChatMemory（Redis 会话记忆）                    │  │   │
│  │  └─────────────────────────────────────────────────────────────┘  │   │
│  └───────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────┘
```

### 2.2 通信方式

| 连接 | 协议 | 生命周期 | 职责 |
|------|------|---------|------|
| Interview WebSocket | WS `/ws/interview?id={interviewId}` | 整个面试期间 | 实时问答：发送回答、接收评分、完成通知 |
| REST API | HTTP `/api/interview/*` | 按需调用 | 面试创建、历史查询、TTS 合成 |
| STT API | HTTP `POST /api/stt` | 录音结束时调用 | 语音识别（MiMo ASR） |

**为什么用 WebSocket + REST 混合架构？**

1. **WebSocket 负责实时交互**：回答→评分→下一题的流程需要低延迟，WebSocket 避免了 HTTP 轮询
2. **REST 负责一次性操作**：创建面试、查询历史、TTS 合成等不需要持久连接
3. **简单可靠**：相比沉浸式的双 WebSocket 架构，文字面试只需一个 WS 连接

## 三、时序图

### 3.1 面试创建流程

```
前端                         InterviewController       AIService (MiMo)      InterviewService
  │                              │                          │                      │
  │  点击"开始面试"               │                          │                      │
  │  POST /api/interview/create  │                          │                      │
  │  {userId, resumeId, jobType} │                          │                      │
  │─────────────────────────────>│                          │                      │
  │                              │  创建 Interview 记录      │                      │
  │                              │─────────────────────────────────────────────────>│
  │                              │  获取 Resume              │                      │
  │                              │─────────────────────────────────────────────────>│
  │                              │                          │                      │
  │                              │  generateAllQuestions()   │                      │
  │                              │─────────────────────────>│                      │
  │                              │                          │  单次AI调用生成10题   │
  │                              │                          │  返回 JSON 数组       │
  │                              │<─────────────────────────│                      │
  │                              │                          │                      │
  │                              │  逐题存入 interview_qa   │                      │
  │                              │─────────────────────────────────────────────────>│
  │                              │                          │                      │
  │  返回 {interview, questions} │                          │                      │
  │<─────────────────────────────│                          │                      │
  │                              │                          │                      │
  │  sessionStorage 存储 questions│                         │                      │
  │  路由跳转 /interview/{id}    │                          │                      │
```

### 3.2 面试执行流程（单轮）

```
前端                         Interview WS        DeepSeekService       InterviewService
  │                             │                      │                      │
  │═════════════════════════════│══════════════════════│══════════════════════│
  │  第 N 轮开始                 │                      │                      │
  │═════════════════════════════│══════════════════════│══════════════════════│
  │                             │                      │                      │
  │  1. 显示问题                 │                      │                      │
  │  (从 questions[] 数组取)     │                      │                      │
  │  可选：点击"朗读"TTS         │                      │                      │
  │                             │                      │                      │
  │  2. 用户输入回答             │                      │                      │
  │  (文字输入 / 语音→STT)      │                      │                      │
  │                             │                      │                      │
  │  3. 点击发送                 │                      │                      │
  │  WS: {type:'answer',        │                      │                      │
  │    interviewId,round,       │                      │                      │
  │    content,question}        │                      │                      │
  │────────────────────────────>│                      │                      │
  │                             │                      │                      │
  │  4. 后端异步评分             │                      │                      │
  │                             │  evaluateAnswer()     │                      │
  │                             │─────────────────────>│                      │
  │                             │  ScoreResult          │                      │
  │                             │<─────────────────────│                      │
  │                             │                      │                      │
  │  5. 保存 QA 记录            │                      │                      │
  │                             │─────────────────────────────────────────────>│
  │                             │                      │                      │
  │  6. 发送评分结果            │                      │                      │
  │  WS: {type:'evaluation',   │                      │                      │
  │    round,score,feedback,   │                      │                      │
  │    referenceAnswer}        │                      │                      │
  │<────────────────────────────│                      │                      │
  │                             │                      │                      │
  │  7. 显示评分卡片            │                      │                      │
  │  自动推进到下一题           │                      │                      │
  │  (前端从 questions[] 取)    │                      │                      │
  │                             │                      │                      │
  │═════════════════════════════│══════════════════════│══════════════════════│
  │  循环到第 N+1 轮             │                      │                      │
  │═════════════════════════════│══════════════════════│══════════════════════│
```

### 3.3 面试完成流程

```
前端                         Interview WS        InterviewService      AIService (MiMo)
  │                             │                      │                      │
  │  第 10 轮评分完成            │                      │                      │
  │                             │                      │                      │
  │                             │  round >= totalRounds │                      │
  │                             │  completeInterview()  │                      │
  │                             │─────────────────────>│                      │
  │                             │  计算总分              │                      │
  │                             │  status=COMPLETED     │                      │
  │                             │                      │                      │
  │                             │  getQAHistory()       │                      │
  │                             │─────────────────────>│                      │
  │                             │                      │                      │
  │                             │  generateReport()     │                      │
  │                             │─────────────────────────────────────────────>│
  │                             │                      │  综合评估报告          │
  │                             │<─────────────────────────────────────────────│
  │                             │                      │                      │
  │                             │  saveReport()         │                      │
  │                             │─────────────────────>│                      │
  │                             │                      │                      │
  │  WS: {type:'completed',    │                      │                      │
  │    report}                 │                      │                      │
  │<────────────────────────────│                      │                      │
  │                             │                      │                      │
  │  2秒后跳转 /report/{id}    │                      │                      │
```

### 3.4 语音输入流程

```
前端                         audioRecorder.js       SttController       MiMoSttService
  │                              │                      │                     │
  │  点击麦克风按钮               │                      │                     │
  │  toggleVoiceInput()          │                      │                     │
  │─────────────────────────────>│                      │                     │
  │                              │  startRecording()    │                     │
  │                              │  getUserMedia()      │                     │
  │                              │  MediaRecorder.start │                     │
  │                              │                      │                     │
  │  [用户说话...]               │                      │                     │
  │                              │                      │                     │
  │  再次点击麦克风               │                      │                     │
  │  toggleVoiceInput()          │                      │                     │
  │─────────────────────────────>│                      │                     │
  │                              │  recorder.stop()     │                     │
  │                              │  webm → WAV 编码     │                     │
  │  返回 WAV Blob               │                      │                     │
  │<─────────────────────────────│                      │                     │
  │                              │                      │                     │
  │  POST /api/stt (FormData)   │                      │                     │
  │  file: recording.wav         │                      │                     │
  │────────────────────────────────────────────────────>│                     │
  │                              │                      │  WAV → base64       │
  │                              │                      │  MiMo ASR API       │
  │                              │                      │────────────────────>│
  │                              │                      │  识别文本 (SSE)      │
  │                              │                      │<────────────────────│
  │  返回识别文本                 │                      │                     │
  │<────────────────────────────────────────────────────│                     │
  │                              │                      │                     │
  │  填入 answer 输入框          │                      │                     │
  │  用户确认后手动发送           │                      │                     │
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
        // 面试 WebSocket — 文字面试 + 沉浸式面试共用
        registry.addHandler(interviewWebSocket, "/ws/interview")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(5 * 1024 * 1024);
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024);
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return container;
    }
}
```

### 4.2 InterviewWebSocket — 实时问答

**核心职责：**
1. 接收前端回答，触发异步 AI 评分
2. 评分完成后保存 QA 记录，推送评分结果
3. 最后一轮评分完成后生成综合报告

```java
// InterviewWebSocket.java（关键片段）
@Slf4j
@Component
public class InterviewWebSocket extends TextWebSocketHandler {

    private final InterviewService interviewService;
    private final ResumeService resumeService;
    private final DeepSeekService deepSeekService;
    private final ExecutorService aiExecutor;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) payload.get("type");

        switch (type) {
            case "answer" -> handleAnswer(session, payload);
            case "exit" -> handleExit(session, payload);
        }
    }

    /**
     * 处理回答 — 只做评分，不发送下一题（前端自主从数组取题）
     */
    private void handleAnswer(WebSocketSession session, Map<String, Object> payload) {
        Long interviewId = Long.valueOf(payload.get("interviewId").toString());
        int round = Integer.parseInt(payload.get("round").toString());
        String answer = (String) payload.get("content");
        String currentQuestion = (String) payload.get("question");

        Interview interview = interviewService.getInterviewById(interviewId);
        String conversationId = interview.getId() + "_" + interview.getUserId();

        // 异步评分 — 不阻塞 WebSocket 线程
        CompletableFuture<ScoreResult> evaluationFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return deepSeekService.evaluateAnswer(currentQuestion, answer, conversationId);
            } catch (Exception e) {
                log.error("评分失败", e);
                // 降级：返回默认评分
                ScoreResult fallback = new ScoreResult();
                fallback.setAccuracy(5); fallback.setClarity(5);
                fallback.setLogic(5); fallback.setDepth(5);
                fallback.setPractice(5); fallback.setTotalScore(5.0);
                fallback.setFeedback("评分服务暂不可用，使用默认评分");
                fallback.setReferenceAnswer("参考答案暂不可用");
                return fallback;
            }
        }, aiExecutor);

        // 评分完成 → 保存 + 发送
        evaluationFuture.thenAccept(scoreResult -> {
            interviewService.saveQA(interviewId, round, currentQuestion, answer, scoreResult);

            sendMessage(session, Map.of(
                "type", "evaluation",
                "round", round,
                "score", scoreResult,
                "feedback", scoreResult.getFeedback(),
                "referenceAnswer", scoreResult.getReferenceAnswer()
            ));

            // 最后一轮评分完成 → 面试结束
            if (round >= interview.getTotalRounds()) {
                completeInterview(session, interview, conversationId);
            }
        });
    }

    /**
     * 提前结束面试
     */
    private void handleExit(WebSocketSession session, Map<String, Object> payload) {
        Long interviewId = Long.valueOf(payload.get("interviewId").toString());
        Interview interview = interviewService.getInterviewById(interviewId);
        String conversationId = interview.getId() + "_" + interview.getUserId();
        Resume resume = resumeService.getResumeById(interview.getResumeId());

        // 补评最后一轮未评分的回答
        List<InterviewQA> history = interviewService.getQAHistory(interviewId);
        if (!history.isEmpty()) {
            InterviewQA lastQA = history.get(history.size() - 1);
            if (lastQA.getScores() == null || lastQA.getScores().isEmpty()) {
                ScoreResult scoreResult = deepSeekService.evaluateAnswer(
                    lastQA.getQuestion(), lastQA.getAnswer(), conversationId);
                interviewService.saveQA(interviewId, lastQA.getRound(),
                    lastQA.getQuestion(), lastQA.getAnswer(), scoreResult);
            }
        }

        interviewService.completeInterview(interviewId);
        history = interviewService.getQAHistory(interviewId);
        String report = interviewService.generateReportForInterview(
            resume, history, interview.getJobType(), conversationId);
        interviewService.saveReport(interviewId, report);

        sendMessage(session, Map.of(
            "type", "completed",
            "feedback", "面试已提前结束，正在生成评估报告...",
            "report", report
        ));
    }
}
```

**关键设计：前端自主取题**

后端收到 `answer` 后只评分、不推题。前端持有完整的 `questions[]` 数组，收到 `evaluation` 后自行从数组取下一题。这样做的好处：

1. **减少一轮 WS 往返**：后端不需要先评分再推题，前端收到评分即可立即显示下一题
2. **简化后端状态**：后端不需要维护"当前轮次"状态
3. **支持断连恢复**：前端从 sessionStorage 恢复后，直接从数组取题，无需后端推送

### 4.3 InterviewService — 面试生命周期

```java
// InterviewServiceImpl.java（关键片段）
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    /**
     * 创建面试
     */
    public Interview createInterview(Long userId, Long resumeId, String jobType) {
        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setResumeId(resumeId);
        interview.setJobType(jobType);
        interview.setTotalRounds(10);
        interview.setCurrentRound(1);
        interview.setStatus("IN_PROGRESS");
        interviewMapper.insert(interview);
        return interview;
    }

    /**
     * 同步批量生成所有面试问题
     * 单次 AI 调用生成全部 10 道题，存入数据库
     */
    public List<String> generateAllQuestions(Interview interview, Resume resume, String conversationId) {
        // 单次 AI 调用生成全部问题
        List<String> questions = aiService.generateAllQuestions(
            resume, interview.getJobType(), conversationId);

        // 逐题存入 interview_qa 表（answer 为 null）
        for (int i = 0; i < questions.size(); i++) {
            InterviewQA qa = new InterviewQA();
            qa.setInterviewId(interview.getId());
            qa.setRound(i + 1);
            qa.setQuestion(questions.get(i));
            interviewQAMapper.insert(qa);
        }
        return questions;
    }

    /**
     * 保存问答记录（upsert：有则更新，无则插入）
     */
    public void saveQA(Long interviewId, int round, String question,
                       String answer, ScoreResult scoreResult) {
        InterviewQA qa = getQAByRound(interviewId, round);
        if (qa == null) {
            qa = new InterviewQA();
            qa.setInterviewId(interviewId);
            qa.setRound(round);
            qa.setQuestion(question);
            qa.setAnswer(answer);
            qa.setScores(objectMapper.writeValueAsString(scoreResult));
            qa.setFeedback(scoreResult.getFeedback());
            qa.setReferenceAnswer(scoreResult.getReferenceAnswer());
            interviewQAMapper.insert(qa);
        } else {
            qa.setAnswer(answer);
            qa.setScores(objectMapper.writeValueAsString(scoreResult));
            qa.setFeedback(scoreResult.getFeedback());
            qa.setReferenceAnswer(scoreResult.getReferenceAnswer());
            interviewQAMapper.updateById(qa);
        }
    }

    /**
     * 完成面试：设置状态 + 计算总分
     */
    public void completeInterview(Long interviewId) {
        Interview interview = interviewMapper.selectById(interviewId);
        interview.setStatus("COMPLETED");
        interview.setCompletedAt(LocalDateTime.now());

        // 遍历所有 QA 的 scores JSON，计算平均分
        List<InterviewQA> qaList = getQAHistory(interviewId);
        double totalScore = calculateTotalScore(qaList);
        interview.setTotalScore(totalScore);

        interviewMapper.updateById(interview);
    }

    private double calculateTotalScore(List<InterviewQA> qaList) {
        double total = 0;
        int count = 0;
        for (InterviewQA qa : qaList) {
            if (qa.getScores() != null && !qa.getScores().isEmpty()) {
                ScoreResult score = objectMapper.readValue(qa.getScores(), ScoreResult.class);
                total += score.getTotalScore();
                count++;
            }
        }
        return count > 0 ? total / count : 0;
    }
}
```

### 4.4 InterviewController — REST 接口

```java
// InterviewController.java
@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    /**
     * 创建面试 — 创建记录 + AI 批量生成问题
     */
    @PostMapping("/create")
    public Result<?> createInterview(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Long resumeId = Long.valueOf(request.get("resumeId").toString());
        String jobType = (String) request.get("jobType");

        Interview interview = interviewService.createInterview(userId, resumeId, jobType);
        Resume resume = resumeService.getResumeById(resumeId);
        String conversationId = interview.getId() + "_" + userId;
        List<String> questions = interviewService.generateAllQuestions(interview, resume, conversationId);

        Map<String, Object> data = new HashMap<>();
        data.put("interview", interview);
        data.put("questions", questions);
        return Result.ok(data);
    }

    @GetMapping("/{id}/history")
    public Result<?> getInterviewHistory(@PathVariable Long id) {
        List<InterviewQA> history = interviewService.getQAHistory(id);
        return Result.ok(history);
    }

    /**
     * TTS 语音合成 — 返回 MP3 音频字节流
     */
    @PostMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<byte[]> tts(@RequestBody TtsRequest request) {
        byte[] audio = ttsService.synthesize(request.getText());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }
}
```

### 4.5 DeepSeekService — AI 评分

```java
// DeepSeekServiceImpl.java（关键片段）
@Slf4j
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    private final AiClient aiClient;  // @Qualifier("deepseekAiClient")

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

    private ScoreResult parseScoreResult(String response) {
        String json = JsonUtils.extractJsonObject(response);
        ScoreResult result = JsonUtils.parseJson(json, ScoreResult.class);

        // 计算加权总分
        result.setTotalScore(
            result.getAccuracy() * 0.3 +
            result.getClarity() * 0.2 +
            result.getLogic() * 0.25 +
            result.getDepth() * 0.15 +
            result.getPractice() * 0.1
        );
        return result;
    }
}
```

### 4.6 AIService — 问题生成 + 报告生成

```java
// AIServiceImpl.java（关键片段）
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private final AiClient aiClient;  // @Qualifier("mimoAiClient")

    /**
     * 批量生成 10 道面试题
     * 单次 AI 调用，返回 JSON 数组
     */
    public List<String> generateAllQuestions(Resume resume, String jobType, String conversationId) {
        String systemPrompt = "你是一位专业的技术面试官。请严格返回合法的JSON数组。";
        String userPrompt = String.format(
            "候选人简历内容如下：\n%s\n\n" +
            "请根据候选人背景生成10道面试题，要求：\n" +
            "1. 第1题为自我介绍\n" +
            "2. 覆盖项目经验、技术深度、基础知识、编码能力、系统设计\n" +
            "3. 难度从易到难递进\n" +
            "4. 结合候选人简历中的具体项目和技术栈\n\n" +
            "返回纯JSON数组，格式：\n" +
            "[{\"index\":0,\"question\":\"题目内容\",\"category\":\"分类\"}]\n" +
            "分类：self-intro, project, technical, coding, system-design",
            resume.getRawContent()
        );

        String response = aiClient.call(systemPrompt, userPrompt, 2048, conversationId);
        return parseQuestionArray(response);
    }

    /**
     * 生成综合评估报告
     */
    public String generateReport(Resume resume, List<InterviewQA> qaList,
                                 String jobType, String conversationId) {
        StringBuilder qaContext = new StringBuilder();
        for (InterviewQA qa : qaList) {
            qaContext.append(String.format("第%d轮：\n问题：%s\n回答：%s\n评分：%s\n\n",
                qa.getRound(), qa.getQuestion(), qa.getAnswer(), qa.getScores()));
        }

        String userPrompt = String.format(
            "请根据以下面试记录生成综合评估报告。\n\n" +
            "候选人简历：%s\n应聘岗位：%s\n面试记录：%s\n\n" +
            "请生成包含以下内容的报告：\n" +
            "1. 综合评分\n2. 优势分析\n3. 不足与改进建议\n" +
            "4. 录用建议（强烈推荐/推荐/待定/不推荐）",
            resume.getRawContent(), jobType, qaContext
        );
        return aiClient.call(systemPrompt, userPrompt, conversationId);
    }
}
```

### 4.7 AI Client 架构

```
AiClient (接口)
  └── AbstractAiClient (抽象基类 — Template Method)
        ├── MiMoAiClient (@Qualifier("mimoAiClient"))
        │     └── 用于：问题生成、报告生成
        └── DeepSeekAiClient (@Qualifier("deepseekAiClient"))
              └── 用于：回答评分
```

**会话记忆机制：**

- 两个 Client 都通过 Spring AI 的 `MessageChatMemoryAdvisor` 接入 Redis
- `conversationId` 格式：`{interviewId}_{userId}`
- 评分时 DeepSeek 能参考前几轮的评分历史，保持尺度一致
- 生成问题时 MiMo 能参考已问的问题，避免重复

### 4.8 TTS 服务

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
    return mergeChunks(audioChunks);
}
```

### 4.9 STT 服务

```java
// MiMoSttService.java（关键片段）
// MiMo ASR 走 /v1/chat/completions 端点
public String recognize(byte[] wavBytes) throws Exception {
    String base64Audio = Base64.getEncoder().encodeToString(wavBytes);

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

    // 流式读取 SSE，提取识别文本
    StringBuilder fullText = new StringBuilder();
    while ((line = reader.readLine()) != null) {
        JsonNode json = objectMapper.readTree(data);
        String content = json.at("/choices/0/delta/content").asText(null);
        if (content != null) {
            fullText.append(content);
        }
    }
    return fullText.toString();
}
```

## 五、前端实现

### 5.1 状态管理

```javascript
// InterviewRoom.vue — 所有状态用 ref()，无 Vuex/Pinia
const messages = ref([])         // 聊天消息列表
const answer = ref('')           // 输入框文本
const currentRound = ref(0)      // 当前轮次 (1-10)
const currentQuestion = ref('')  // 当前问题
const questions = ref([])        // 全部 10 道问题（从 sessionStorage 加载）
const isCompleted = ref(false)   // 面试是否结束
const isAiTyping = ref(false)    // 打字动画状态

// TTS 状态
const ttsLoading = ref(false)
const ttsPlaying = ref(false)

// STT 状态
const isRecording = ref(false)
const sttLoading = ref(false)
```

### 5.2 初始化流程

```javascript
onMounted(async () => {
  // 1. 从 sessionStorage 加载问题列表
  if (!loadQuestions()) {
    ElMessage.error('问题加载失败，请重新开始面试')
    return
  }

  // 2. 先尝试从缓存恢复聊天记录，没有则从服务器加载
  if (!loadCachedMessages()) {
    await loadHistoryFromServer()
  }

  // 3. 建立 WebSocket 连接
  connectWebSocket()
})

/** 从 sessionStorage 加载问题列表 */
const loadQuestions = () => {
  const cached = sessionStorage.getItem(`interview_questions_${interviewId}`)
  if (cached) {
    questions.value = JSON.parse(cached)
    return true
  }
  return false
}

/** 从 sessionStorage 恢复聊天记录 */
const loadCachedMessages = () => {
  const cached = sessionStorage.getItem(storageKey)
  if (cached) {
    const parsed = JSON.parse(cached)
    if (parsed.length > 0) {
      messages.value = parsed
      // 恢复 currentRound 和 currentQuestion
      for (let i = parsed.length - 1; i >= 0; i--) {
        if (parsed[i].type === 'question') {
          currentQuestion.value = parsed[i].content
        }
        if (parsed[i].round) currentRound.value = parsed[i].round
      }
      return true
    }
  }
  return false
}

/** 从服务器加载历史记录（sessionStorage 失败时的兜底） */
const loadHistoryFromServer = async () => {
  const res = await axios.get(`/api/interview/${interviewId}/history`)
  if (res.data.success && res.data.data.length > 0) {
    const historyMessages = []
    res.data.data.filter(qa => qa.answer).forEach(qa => {
      historyMessages.push({ type: 'ai', content: qa.question, round: qa.round })
      historyMessages.push({ type: 'user', content: qa.answer })
      if (qa.feedback) {
        let scores = {}
        try { scores = JSON.parse(qa.scores) } catch {}
        historyMessages.push({
          type: 'evaluation', score: scores, feedback: qa.feedback,
          referenceAnswer: qa.referenceAnswer || scores.referenceAnswer || '',
          showRef: false, round: qa.round
        })
      }
      currentRound.value = qa.round
      currentQuestion.value = qa.question
    })
    messages.value = historyMessages
    return true
  }
  return false
}
```

### 5.3 WebSocket 管理

```javascript
const connectWebSocket = () => {
  const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsHost = import.meta.env.VITE_WS_HOST || location.host
  ws = new WebSocket(`${wsProtocol}//${wsHost}/ws/interview?id=${interviewId}`)

  ws.onopen = () => {
    // 没有历史记录时，显示第 1 题
    if (questions.value.length > 0 && messages.value.length === 0) {
      startTypingAnimation('面试官正在准备面试...')
      setTimeout(() => {
        stopTypingAnimation()
        const firstQuestion = questions.value[0]
        messages.value.push({ type: 'ai', content: firstQuestion })
        currentRound.value = 1
        currentQuestion.value = firstQuestion
        saveMessages()
      }, 1000)
    }
  }

  ws.onmessage = (event) => {
    handleWebSocketMessage(JSON.parse(event.data))
  }
}

const handleWebSocketMessage = (data) => {
  stopTypingAnimation()

  if (data.type === 'evaluation') {
    // 显示评分卡片
    messages.value.push({
      type: 'evaluation',
      score: data.score,
      feedback: data.feedback,
      referenceAnswer: data.referenceAnswer,
      showRef: false
    })
    saveMessages()

    // 自动推进到下一题（前端自主取题）
    const nextRound = currentRound.value + 1
    if (nextRound <= questions.value.length) {
      startTypingAnimation('正在准备下一题...')
      setTimeout(() => {
        stopTypingAnimation()
        const nextQuestion = questions.value[nextRound - 1]
        messages.value.push({ type: 'ai', content: nextQuestion })
        currentRound.value = nextRound
        currentQuestion.value = nextQuestion
        saveMessages()
      }, 1000)
    }
  } else if (data.type === 'completed') {
    messages.value.push({ type: 'ai', content: '面试结束，正在生成评估报告...' })
    isCompleted.value = true
    sessionStorage.removeItem(storageKey)
    setTimeout(() => router.push(`/report/${interviewId}`), 2000)
  }
}
```

### 5.4 提交回答

```javascript
const submitAnswer = () => {
  if (!answer.value.trim() || isAiTyping.value) return

  // 显示用户消息
  messages.value.push({ type: 'user', content: answer.value })
  saveMessages()

  // 发送到后端
  ws.send(JSON.stringify({
    type: 'answer',
    interviewId,
    round: currentRound.value,
    content: answer.value,
    question: currentQuestion.value
  }))

  answer.value = ''
  scrollToBottom()
  startTypingAnimation('面试官正在分析...')
}
```

### 5.5 语音输入（STT）

```javascript
const toggleVoiceInput = async () => {
  if (isRecording.value) {
    // 停止录音 → 识别 → 填入输入框
    isRecording.value = false
    sttLoading.value = true
    try {
      const wavBlob = await recorderInstance.stop()
      const res = await recognizeSpeech(wavBlob)
      answer.value = res.data  // 识别文字填入输入框，用户确认后手动发送
    } catch (err) {
      ElMessage.error('语音识别失败: ' + (err.response?.data?.message || err.message))
    } finally {
      sttLoading.value = false
    }
  } else {
    // 开始录音
    try {
      recorderInstance = await startRecording()
      isRecording.value = true
    } catch (err) {
      ElMessage.error('无法访问麦克风: ' + err.message)
    }
  }
}
```

### 5.6 TTS 语音朗读

```javascript
/** 播放/停止 TTS — 使用 MediaSource 实现流式播放 */
const playTTS = (text) => {
  if (ttsPlaying.value) {
    stopTTS()
    return
  }

  const mediaSource = new MediaSource()
  const url = URL.createObjectURL(mediaSource)
  ttsAudio = new Audio(url)
  ttsAudio.onended = () => { ttsPlaying.value = false; cleanup() }

  let sourceBuffer = null

  mediaSource.addEventListener('sourceopen', () => {
    sourceBuffer = mediaSource.addSourceBuffer('audio/mpeg')
    sourceBuffer.mode = 'sequence'

    ttsLoading.value = true
    fetch('/api/interview/tts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text })
    })
      .then(res => res.arrayBuffer())
      .then(data => {
        const appendData = () => {
          if (sourceBuffer.updating) {
            sourceBuffer.addEventListener('updateend', appendData, { once: true })
            return
          }
          sourceBuffer.appendBuffer(data)
        }
        appendData()
      })
      .catch(err => {
        ElMessage.error('语音合成失败: ' + err.message)
        stopTTS()
      })
      .finally(() => { ttsLoading.value = false })
  })

  ttsAudio.play()  // 占住用户手势上下文
  ttsPlaying.value = true
}
```

### 5.7 音频编码：WebM → WAV

```javascript
// audioRecorder.js
// MediaRecorder 输出 webm/opus，MiMo ASR 需要 WAV/PCM

export async function startRecording(externalStream) {
  const stream = externalStream || await navigator.mediaDevices.getUserMedia({ audio: true })
  const mediaRecorder = new MediaRecorder(stream, {
    mimeType: 'audio/webm;codecs=opus'
  })
  const chunks = []

  mediaRecorder.ondataavailable = (e) => {
    if (e.data.size > 0) chunks.push(e.data)
  }
  mediaRecorder.start()

  return {
    stop: () => {
      return new Promise((resolve, reject) => {
        mediaRecorder.onstop = async () => {
          stream.getTracks().forEach(t => t.stop())  // 释放麦克风
          try {
            const webmBlob = new Blob(chunks, { type: 'audio/webm' })
            const arrayBuffer = await webmBlob.arrayBuffer()
            const audioCtx = new AudioContext()
            const audioBuffer = await audioCtx.decodeAudioData(arrayBuffer)
            const wavBlob = encodeWav(audioBuffer)
            audioCtx.close()
            resolve(wavBlob)
          } catch (err) {
            reject(err)
          }
        }
        mediaRecorder.stop()
      })
    }
  }
}

function encodeWav(audioBuffer) {
  const numChannels = audioBuffer.numberOfChannels
  const sampleRate = audioBuffer.sampleRate
  const samples = interleave(audioBuffer)
  const dataLength = samples.length * 2  // 16-bit = 2 bytes

  // WAV 头（44 字节）+ PCM 数据
  const buffer = new ArrayBuffer(44 + dataLength)
  const view = new DataView(buffer)

  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + dataLength, true)
  writeString(view, 8, 'WAVE')
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)            // PCM format
  view.setUint16(22, numChannels, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * numChannels * 2, true)
  view.setUint16(32, numChannels * 2, true)
  view.setUint16(34, 16, true)
  writeString(view, 36, 'data')
  view.setUint32(40, dataLength, true)

  floatTo16BitPCM(view, 44, samples)
  return new Blob([buffer], { type: 'audio/wav' })
}
```

### 5.8 评分卡片展示

```javascript
// 评分维度配置
const scoreItems = (score) => [
  { key: 'accuracy', label: '准确性', value: score.accuracy || 0 },
  { key: 'clarity', label: '清晰度', value: score.clarity || 0 },
  { key: 'logic', label: '逻辑性', value: score.logic || 0 },
  { key: 'depth', label: '深度', value: score.depth || 0 },
  { key: 'practice', label: '实践', value: score.practice || 0 }
]

// 颜色编码：>=8 绿色, >=6 青色, >=4 琥珀色, <4 红色
const getScoreColor = (val) => {
  if (val >= 8) return '#10b981'
  if (val >= 6) return '#06b6d4'
  if (val >= 4) return '#f59e0b'
  return '#ef4444'
}
```

**模板结构：**

```html
<!-- 评分反馈消息 -->
<div v-if="msg.type === 'evaluation'" class="msg msg--ai">
  <div class="msg__body">
    <div class="msg__name">评分反馈</div>
    <div class="score-card">
      <!-- 五维评分条 -->
      <div class="score-card__row" v-for="item in scoreItems(msg.score)" :key="item.key">
        <span class="score-card__label">{{ item.label }}</span>
        <div class="score-card__bar-wrap">
          <div class="score-card__bar"
               :style="{ width: item.value * 10 + '%', background: getScoreColor(item.value) }" />
        </div>
        <span class="score-card__value" :style="{ color: getScoreColor(item.value) }">
          {{ item.value }}
        </span>
      </div>

      <!-- 反馈 -->
      <div v-if="msg.feedback" class="score-card__feedback">
        {{ msg.feedback }}
      </div>

      <!-- 参考答案（可折叠） -->
      <div v-if="msg.referenceAnswer" class="score-card__ref">
        <button class="score-card__ref-toggle" @click="msg.showRef = !msg.showRef">
          {{ msg.showRef ? '收起参考答案' : '查看参考答案' }}
        </button>
        <div v-if="msg.showRef" class="score-card__ref-text">
          {{ msg.referenceAnswer }}
        </div>
      </div>
    </div>
  </div>
</div>
```

## 六、数据模型

### 6.1 Interview 实体

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| userId | Long | 候选人 ID |
| resumeId | Long | 简历 ID |
| jobType | String | 应聘岗位 |
| totalRounds | Integer | 总轮数（固定 10） |
| currentRound | Integer | 当前轮次 |
| status | String | `IN_PROGRESS` / `COMPLETED` |
| totalScore | Double | 平均总分 |
| completedAt | LocalDateTime | 完成时间 |

### 6.2 InterviewQA 实体

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| interviewId | Long | 面试 ID |
| round | Integer | 轮次 (1-10) |
| question | String | AI 生成的问题 |
| answer | String | 候选人的回答 |
| scores | String | JSON 序列化的 ScoreResult |
| feedback | String | AI 反馈文本 |
| referenceAnswer | String | AI 参考答案 |

### 6.3 ScoreResult DTO

| 字段 | 类型 | 权重 | 说明 |
|------|------|------|------|
| accuracy | int | 30% | 准确度 |
| clarity | int | 20% | 清晰度 |
| logic | int | 25% | 逻辑性 |
| depth | int | 15% | 深度 |
| practice | int | 10% | 实践性 |
| totalScore | double | — | 加权总分 |
| feedback | String | — | 中文反馈 |
| referenceAnswer | String | — | 参考答案 |

### 6.4 Report 实体

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| interviewId | Long | 面试 ID |
| summary | String | 完整报告文本（AI 生成） |

## 七、消息协议

### 7.1 Interview WebSocket

| type | 方向 | 字段 | 说明 |
|------|------|------|------|
| `answer` | 前端→后端 | interviewId, round, content, question | 提交回答 |
| `exit` | 前端→后端 | interviewId | 提前结束面试 |
| `evaluation` | 后端→前端 | round, score, feedback, referenceAnswer | 评分结果 |
| `completed` | 后端→前端 | report, feedback(可选) | 面试完成 |
| `error` | 后端→前端 | message | 错误信息 |

### 7.2 REST 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/interview/create` | 创建面试 + AI 批量生成问题 |
| GET | `/api/interview/{id}` | 查询面试信息 |
| GET | `/api/interview/{id}/history` | 查询 QA 历史 |
| GET | `/api/interview/user/{userId}` | 查询用户面试记录 |
| POST | `/api/interview/tts` | TTS 语音合成（返回 MP3） |
| POST | `/api/stt` | STT 语音识别（返回文本） |

## 八、文件清单

### 8.1 后端

| 文件 | 类型 | 说明 |
|------|------|------|
| `websocket/InterviewWebSocket.java` | 核心 | WebSocket 处理器：answer/exit 消息 |
| `controller/InterviewController.java` | 核心 | REST 接口：创建/查询/TTS |
| `controller/SttController.java` | 核心 | STT 接口：语音识别 |
| `service/InterviewService.java` | 核心 | 面试服务接口 |
| `service/impl/InterviewServiceImpl.java` | 核心 | 面试生命周期管理 |
| `service/AIService.java` | 核心 | AI 服务接口 |
| `service/impl/AIServiceImpl.java` | 核心 | 问题生成 + 报告生成（MiMo） |
| `service/DeepSeekService.java` | 核心 | 评分服务接口 |
| `service/impl/DeepSeekServiceImpl.java` | 核心 | 回答评分（DeepSeek） |
| `service/TtsService.java` | 基础 | TTS 服务接口 |
| `service/impl/MiMoTtsService.java` | 基础 | TTS 实现（MiMo） |
| `service/SttService.java` | 基础 | STT 服务接口 |
| `service/impl/MiMoSttService.java` | 基础 | STT 实现（MiMo） |
| `service/ai/AiClient.java` | 基础 | AI 客户端接口 |
| `service/ai/AbstractAiClient.java` | 基础 | 抽象基类（Template Method） |
| `service/ai/MiMoAiClient.java` | 基础 | MiMo 客户端 |
| `service/ai/DeepSeekAiClient.java` | 基础 | DeepSeek 客户端 |
| `domain/po/Interview.java` | 数据 | 面试实体 |
| `domain/po/InterviewQA.java` | 数据 | 问答记录实体 |
| `domain/po/Report.java` | 数据 | 报告实体 |
| `domain/dto/ScoreResult.java` | 数据 | 评分 DTO |
| `config/WebSocketConfig.java` | 配置 | WebSocket 注册 |

### 8.2 前端

| 文件 | 类型 | 说明 |
|------|------|------|
| `views/InterviewRoom.vue` | 核心 | 面试房间主组件 |
| `utils/audioRecorder.js` | 工具 | 录音 + WebM→WAV 编码 |
| `api/stt.js` | API | STT 接口封装 |
| `router/index.js` | 路由 | `/interview/:id` 路由 |

## 九、关键技术点

### 9.1 前端自主取题 vs 后端推题

| 方式 | 优点 | 缺点 |
|------|------|------|
| **前端自主取题（当前方案）** | 减少 WS 往返、支持断连恢复、后端无状态 | 前端持有全部问题（安全性略低） |
| 后端推题 | 问题不暴露给前端 | 多一轮 WS 往返、后端需维护状态 |

当前选择前端自主取题，因为：
- 面试题是 AI 生成的，不是敏感数据
- 断连恢复体验更好
- 后端实现更简单

### 9.2 双模型分工

| 模型 | 职责 | 原因 |
|------|------|------|
| **MiMo** | 问题生成、报告生成 | 生成类任务需要创意性 |
| **DeepSeek** | 回答评分 | 评分需要严格逻辑和一致性 |

通过 `@Qualifier` 注入不同的 `AiClient` 实现，Spring AI 的 `ChatClient` 统一抽象。

### 9.3 会话记忆（Redis）

```java
// conversationId 格式：{interviewId}_{userId}
// 通过 Spring AI 的 MessageChatMemoryAdvisor 自动管理
// Redis 中存储每轮的 user/assistant 消息
// DeepSeek 评分时能参考前几轮的评分历史
// MiMo 生成问题时能参考已问的问题
```

### 9.4 消息持久化双写

```
用户发送回答
  ├── sessionStorage.setItem(storageKey, messages)  // 快速恢复
  └── WebSocket → 后端 saveQA() → 数据库            // 持久化

页面刷新
  ├── 优先从 sessionStorage 恢复（毫秒级）
  └── 失败则从 GET /history 恢复（服务端兜底）
```

### 9.5 降级策略

| 场景 | 降级方案 |
|------|---------|
| AI 评分失败 | 返回默认评分（各项 5 分） |
| 问题生成失败 | 返回兜底问题（"请做一下自我介绍" + 9 道通用题） |
| TTS 合成失败 | 返回 500，前端不显示朗读按钮 |
| STT 识别失败 | 提示错误，用户可改用文字输入 |
| WebSocket 断连 | 页面刷新后从 sessionStorage / 服务端恢复 |

### 9.6 录音格式转换

```
浏览器 MediaRecorder → webm/opus（浏览器原生格式）
    ↓ AudioContext.decodeAudioData()
PCM AudioBuffer（解码后的原始音频）
    ↓ 手动编码 WAV 头 + PCM 数据
WAV Blob（16-bit PCM，MiMo ASR 兼容）
    ↓ POST /api/stt (FormData)
MiMo ASR API → 识别文本
```

## 十、与沉浸式面试的共享组件

两个面试模式共享以下后端服务：

| 共享组件 | 文字面试用法 | 沉浸式面试用法 |
|---------|-------------|---------------|
| InterviewService | 创建/保存/完成面试 | 相同 |
| DeepSeekService | 评分 | 评分 + 语义判断 |
| AIService | 问题生成 + 报告生成 | 相同 |
| MiMoTtsService | 手动点击朗读 | 自动朗读问题 |
| InterviewWebSocket | answer/exit 消息 | immersive_start/answer/exit 消息 |
| InterviewController | create/history/tts | 相同 + check-complete |

独立组件：

| 独立组件 | 文字面试 | 沉浸式面试 |
|---------|---------|-----------|
| 前端组件 | InterviewRoom.vue | ImmersiveInterview.vue |
| ASR WebSocket | 无 | AsrWebSocket.java |
| 编排服务 | 无 | ImmersiveInterviewOrchestrator.java |
| STT 方式 | 录音后一次性上传 | 实时流式识别（Web Speech API + MiMo ASR） |
| 结束检测 | 手动发送 | 静音 8s / 语义模型 / 手动按钮 |
