package com.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.domain.dto.ScoreResult;
import com.interview.domain.po.Interview;
import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Resume;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 沉浸式面试编排服务
 *
 * 负责后端编排 ASR→评分 的完整流程：
 * 1. 前端静音检测触发录音停止，发送 audio_final
 * 2. 后端收到 asr_final 后直接触发评分
 * 3. 评分完成 → 保存问答记录 → 推送 asr_evaluation 给前端
 *
 * 线程安全：使用 ReentrantLock 保证同一轮次不并发处理
 */
@Slf4j
@Service
public class ImmersiveInterviewOrchestrator {

    private final DeepSeekService deepSeekService;
    private final InterviewService interviewService;
    private final ResumeService resumeService;
    private final ObjectMapper objectMapper;
    private final ExecutorService aiExecutor;

    /** interviewId -> (round -> 锁) 用于防止同一轮次并发处理 */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, ReentrantLock>> locks = new ConcurrentHashMap<>();

    public ImmersiveInterviewOrchestrator(DeepSeekService deepSeekService,
                                          InterviewService interviewService,
                                          ResumeService resumeService,
                                          ObjectMapper objectMapper,
                                          @Qualifier("aiExecutor") ExecutorService aiExecutor) {
        this.deepSeekService = deepSeekService;
        this.interviewService = interviewService;
        this.resumeService = resumeService;
        this.objectMapper = objectMapper;
        this.aiExecutor = aiExecutor;
    }

    /**
     * ASR 最终结果回调 — 核心编排逻辑
     *
     * @param session     ASR WebSocket 会话（用于发送结果给前端）
     * @param interviewId 面试 ID
     * @param round       当前轮次
     * @param asrText     本次 ASR 识别的最终文本
     */
    public void onAsrFinal(WebSocketSession session, Long interviewId, int round, String asrText) {
        // 获取该轮次的锁，防止并发处理
        ReentrantLock lock = locks.computeIfAbsent(interviewId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(round, k -> new ReentrantLock());
        lock.lock();
        try {
            log.info("ASR 最终文本 [面试{}, 轮{}]: {}", interviewId, round, asrText);

            // 直接进入评分（静音检测已代表回答结束）
            CompletableFuture.runAsync(() -> {
                try {
                    evaluateAndSave(session, interviewId, round, asrText);
                } catch (Exception e) {
                    log.error("评分流程失败 [面试{}, 轮{}]", interviewId, round, e);
                    sendError(session, "评分失败: " + e.getMessage());
                }
            }, aiExecutor);

        } finally {
            lock.unlock();
        }
    }

    /**
     * 评分 + 保存 + 推送结果
     */
    private void evaluateAndSave(WebSocketSession session, Long interviewId, int round, String answer) throws Exception {
        Interview interview = interviewService.getInterviewById(interviewId);
        if (interview == null) {
            log.warn("面试 {} 不存在，跳过评分", interviewId);
            sendMessage(session, Map.of(
                    "type", "asr_evaluation",
                    "round", round,
                    "score", createFallbackScore(),
                    "feedback", "面试不存在，无法评分",
                    "referenceAnswer", "",
                    "isLastRound", true
            ));
            return;
        }
        String conversationId = interview.getId() + "_" + interview.getUserId();

        // 获取该轮次的问题
        InterviewQA qa = interviewService.getQAByRound(interviewId, round);
        if (qa == null) {
            log.error("找不到轮次 {} 的问题", round);
            sendError(session, "找不到第 " + round + " 轮的问题");
            return;
        }
        String question = qa.getQuestion();

        // 调用 AI 评分
        ScoreResult scoreResult;
        try {
            scoreResult = deepSeekService.evaluateAnswer(question, answer, conversationId);
        } catch (Exception e) {
            log.error("AI 评分失败，使用默认评分", e);
            scoreResult = createFallbackScore();
        }

        // 保存问答记录
        interviewService.saveQA(interviewId, round, question, answer, scoreResult);

        // 推送评分结果给前端
        boolean isLastRound = round >= interview.getTotalRounds();
        sendMessage(session, Map.of(
                "type", "asr_evaluation",
                "round", round,
                "score", scoreResult,
                "feedback", scoreResult.getFeedback(),
                "referenceAnswer", scoreResult.getReferenceAnswer(),
                "isLastRound", isLastRound
        ));

        // 最后一轮 → 生成报告
        if (isLastRound) {
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

        // 清理累积文本
        cleanup(interviewId, round);

        log.info("评分完成 [面试{}, 轮{}]: 总分={}", interviewId, round, scoreResult.getTotalScore());
    }

    /**
     * 清理指定面试轮次的累积文本
     */
    public void cleanup(Long interviewId, int round) {
        // 清理锁
        ConcurrentHashMap<Integer, ReentrantLock> roundLocks = locks.get(interviewId);
        if (roundLocks != null) {
            roundLocks.remove(round);
            if (roundLocks.isEmpty()) {
                locks.remove(interviewId);
            }
        }
    }

    /**
     * 创建默认评分（AI 评分失败时的降级方案）
     */
    private ScoreResult createFallbackScore() {
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

    private void sendMessage(WebSocketSession session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        sendMessage(session, Map.of(
                "type", "asr_error",
                "message", message
        ));
    }
}
