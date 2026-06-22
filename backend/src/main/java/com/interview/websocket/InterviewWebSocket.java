package com.interview.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.domain.dto.ScoreResult;
import com.interview.domain.po.Interview;
import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Resume;
import com.interview.service.DeepSeekService;
import com.interview.service.InterviewService;
import com.interview.service.ResumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 面试 WebSocket 处理器
 *
 * 流程：
 * 1. 客户端发送 answer → 异步评分，前端自主取下一题
 * 2. 最后一轮评分完成 → 生成报告
 */
@Slf4j
@Component
public class InterviewWebSocket extends TextWebSocketHandler {

    private final InterviewService interviewService;
    private final ResumeService resumeService;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;
    private final ExecutorService aiExecutor;

    public InterviewWebSocket(InterviewService interviewService, ResumeService resumeService,
                              DeepSeekService deepSeekService,
                              ObjectMapper objectMapper,
                              @Qualifier("aiExecutor") ExecutorService aiExecutor) {
        this.interviewService = interviewService;
        this.resumeService = resumeService;
        this.deepSeekService = deepSeekService;
        this.objectMapper = objectMapper;
        this.aiExecutor = aiExecutor;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 连接: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            switch (type) {
                case "answer" -> handleAnswer(session, payload);
                case "exit" -> handleExit(session, payload);
                // 沉浸式面试
                case "immersive_start" -> handleImmersiveStart(session, payload);
                case "immersive_answer" -> handleImmersiveAnswer(session, payload);
                case "immersive_exit" -> handleImmersiveExit(session, payload);
            }
        } catch (Exception e) {
            log.error("处理消息失败", e);
            sendMessage(session, Map.of("type", "error", "message", e.getMessage()));
        }
    }

    /**
     * 处理回答 - 只做评分，不发送下一题（前端自主从数组取题）
     */
    private void handleAnswer(WebSocketSession session, Map<String, Object> payload) throws Exception {
        Long interviewId = Long.valueOf(payload.get("interviewId").toString());
        int round = Integer.parseInt(payload.get("round").toString());
        String answer = (String) payload.get("content");
        String currentQuestion = (String) payload.get("question");

        Interview interview = interviewService.getInterviewById(interviewId);
        String conversationId = interview.getId() + "_" + interview.getUserId();

        // 异步评分
        CompletableFuture<ScoreResult> evaluationFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return deepSeekService.evaluateAnswer(currentQuestion, answer, conversationId);
            } catch (Exception e) {
                log.error("评分失败", e);
                ScoreResult fallback = new ScoreResult();
                fallback.setAccuracy(5);
                fallback.setClarity(5);
                fallback.setLogic(5);
                fallback.setDepth(5);
                fallback.setPractice(5);
                fallback.setTotalScore(5.0);
                fallback.setFeedback("评分服务暂不可用，使用默认评分");
                fallback.setReferenceAnswer("参考答案暂不可用");
                return fallback;
            }
        }, aiExecutor);

        // 评分完成 → 保存 + 发送
        evaluationFuture.thenAccept(scoreResult -> {
            try {
                interviewService.saveQA(interviewId, round, currentQuestion, answer, scoreResult);

                Map<String, Object> evalResponse = new HashMap<>();
                evalResponse.put("type", "evaluation");
                evalResponse.put("round", round);
                evalResponse.put("score", scoreResult);
                evalResponse.put("feedback", scoreResult.getFeedback());
                evalResponse.put("referenceAnswer", scoreResult.getReferenceAnswer());

                sendMessage(session, evalResponse);

                // 最后一轮评分完成 → 面试结束
                if (round >= interview.getTotalRounds()) {
                    interviewService.completeInterview(interviewId);
                    Resume resume = resumeService.getResumeById(interview.getResumeId());
                    List<InterviewQA> history = interviewService.getQAHistory(interviewId);

                    String report = interviewService.generateReportForInterview(resume, history, interview.getJobType(), conversationId);
                    interviewService.saveReport(interviewId, report);

                    sendMessage(session, Map.of(
                        "type", "completed",
                        "report", report
                    ));
                }
            } catch (Exception e) {
                log.error("发送评分失败", e);
            }
        });
    }

    /**
     * 提前结束面试
     */
    private void handleExit(WebSocketSession session, Map<String, Object> payload) throws Exception {
        Long interviewId = Long.valueOf(payload.get("interviewId").toString());
        Interview interview = interviewService.getInterviewById(interviewId);
        String conversationId = interview.getId() + "_" + interview.getUserId();
        Resume resume = resumeService.getResumeById(interview.getResumeId());

        // 评分最后一轮未评分的回答
        List<InterviewQA> history = interviewService.getQAHistory(interviewId);
        if (!history.isEmpty()) {
            InterviewQA lastQA = history.get(history.size() - 1);
            if (lastQA.getScores() == null || lastQA.getScores().isEmpty()) {
                ScoreResult scoreResult = deepSeekService.evaluateAnswer(lastQA.getQuestion(), lastQA.getAnswer(), conversationId);
                interviewService.saveQA(interviewId, lastQA.getRound(), lastQA.getQuestion(), lastQA.getAnswer(), scoreResult);
            }
        }

        interviewService.completeInterview(interviewId);
        history = interviewService.getQAHistory(interviewId);
        String report = interviewService.generateReportForInterview(resume, history, interview.getJobType(), conversationId);
        interviewService.saveReport(interviewId, report);

        sendMessage(session, Map.of(
            "type", "completed",
            "feedback", "面试已提前结束，正在生成评估报告...",
            "report", report
        ));
    }

    /**
     * 沉浸式面试启动 — 查询面试状态，发送第一题或已完成状态
     */
    private void handleImmersiveStart(WebSocketSession session, Map<String, Object> payload) throws Exception {
        Long interviewId = Long.valueOf(payload.get("interviewId").toString());
        Interview interview = interviewService.getInterviewById(interviewId);

        // 面试已完成 → 直接发送报告
        if ("COMPLETED".equals(interview.getStatus())) {
            String conversationId = interview.getId() + "_" + interview.getUserId();
            Resume resume = resumeService.getResumeById(interview.getResumeId());
            List<InterviewQA> history = interviewService.getQAHistory(interviewId);
            String report = interviewService.generateReportForInterview(resume, history, interview.getJobType(), conversationId);
            sendMessage(session, Map.of("type", "completed", "report", report));
            return;
        }

        // 获取已有 QA 历史，找到第一个未回答的问题
        List<InterviewQA> history = interviewService.getQAHistory(interviewId);
        int currentRound = 1;
        for (InterviewQA qa : history) {
            if (qa.getAnswer() == null || qa.getAnswer().isEmpty()) {
                currentRound = qa.getRound();
                break;
            }
            currentRound = qa.getRound() + 1;
        }

        // 取当前轮次的问题
        String question = null;
        for (InterviewQA qa : history) {
            if (qa.getRound() == currentRound) {
                question = qa.getQuestion();
                break;
            }
        }

        if (question == null) {
            sendMessage(session, Map.of("type", "error", "message", "无法获取问题"));
            return;
        }

        sendMessage(session, Map.of(
            "type", "next_question",
            "round", currentRound,
            "question", question,
            "totalRounds", interview.getTotalRounds()
        ));
    }

    /**
     * 沉浸式面试回答 — 评分，完成后发送结果
     * 与 handleAnswer 的区别：前端控制 TTS 播放下一题的时机
     */
    private void handleImmersiveAnswer(WebSocketSession session, Map<String, Object> payload) throws Exception {
        Long interviewId = Long.valueOf(payload.get("interviewId").toString());
        int round = Integer.parseInt(payload.get("round").toString());
        String answer = (String) payload.get("content");
        String currentQuestion = (String) payload.get("question");

        final String finalAnswer = (answer == null || answer.isBlank())
            ? "（面试者表示回答完毕，但未给出实质性回答）"
            : answer;

        Interview interview = interviewService.getInterviewById(interviewId);
        String conversationId = interview.getId() + "_" + interview.getUserId();

        // AI 评分
        CompletableFuture<ScoreResult> evaluationFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return deepSeekService.evaluateAnswer(currentQuestion, finalAnswer, conversationId);
            } catch (Exception e) {
                log.error("沉浸式面试评分失败", e);
                ScoreResult fallback = new ScoreResult();
                fallback.setAccuracy(5);
                fallback.setClarity(5);
                fallback.setLogic(5);
                fallback.setDepth(5);
                fallback.setPractice(5);
                fallback.setTotalScore(5.0);
                fallback.setFeedback("评分服务暂不可用，使用默认评分");
                fallback.setReferenceAnswer("参考答案暂不可用");
                return fallback;
            }
        }, aiExecutor);

        evaluationFuture.thenAccept(scoreResult -> {
            try {
                interviewService.saveQA(interviewId, round, currentQuestion, finalAnswer, scoreResult);

                Map<String, Object> evalResponse = new HashMap<>();
                evalResponse.put("type", "evaluation");
                evalResponse.put("round", round);
                evalResponse.put("score", scoreResult);
                evalResponse.put("feedback", scoreResult.getFeedback());
                evalResponse.put("referenceAnswer", scoreResult.getReferenceAnswer());
                evalResponse.put("command", true);

                sendMessage(session, evalResponse);

                // 最后一轮评分完成 → 面试结束
                if (round >= interview.getTotalRounds()) {
                    interviewService.completeInterview(interviewId);
                    Resume resume = resumeService.getResumeById(interview.getResumeId());
                    List<InterviewQA> history = interviewService.getQAHistory(interviewId);
                    String report = interviewService.generateReportForInterview(resume, history, interview.getJobType(), conversationId);
                    interviewService.saveReport(interviewId, report);
                    sendMessage(session, Map.of("type", "completed", "report", report));
                }
            } catch (Exception e) {
                log.error("沉浸式面试评分发送失败", e);
            }
        });
    }

    /**
     * 沉浸式面试提前退出 — 复用 handleExit 逻辑
     */
    private void handleImmersiveExit(WebSocketSession session, Map<String, Object> payload) throws Exception {
        handleExit(session, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 断开: {}", session.getId());
    }

    private void sendMessage(WebSocketSession session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }
}
