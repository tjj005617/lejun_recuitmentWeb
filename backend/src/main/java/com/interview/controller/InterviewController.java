package com.interview.controller;

import com.interview.common.Result;
import com.interview.domain.po.Interview;
import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.KgCategory;
import com.interview.domain.po.Resume;
import com.interview.dto.TtsRequest;
import com.interview.mapper.InterviewMapper;
import com.interview.service.DeepSeekService;
import com.interview.service.InterviewService;
import com.interview.service.KgCategoryService;
import com.interview.service.ResumeService;
import com.interview.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试控制器
 * 提供面试创建、查询、历史记录等 REST 接口
 * 面试的实时问答和评分通过 WebSocket 处理
 */
@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewMapper interviewMapper;
    private final ResumeService resumeService;
    private final TtsService ttsService;
    private final DeepSeekService deepSeekService;
    private final KgCategoryService kgCategoryService;

    /**
     * 创建面试
     * 支持三种模式：resume(简历分析)、topic(八股栏目)、hybrid(混合)
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/create")
    public Result<?> createInterview(@RequestBody Map<String, Object> request) {
        log.info("创建面试请求: {}", request);
        Long userId = Long.valueOf(request.get("userId").toString());
        String jobType = (String) request.get("jobType");
        String interviewMode = request.get("interviewMode") != null ? (String) request.get("interviewMode") : "resume";
        String questionType = request.get("questionType") != null ? (String) request.get("questionType") : "essay";

        // 简历ID（topic模式可为null）
        Long resumeId = null;
        if (request.get("resumeId") != null) {
            resumeId = Long.valueOf(request.get("resumeId").toString());
        }

        // 分类ID列表（topic/hybrid模式必填）
        String categoryIds = null;
        List<String> categoryNames = null;
        if (request.get("categoryIds") != null) {
            List<?> rawIds = (List<?>) request.get("categoryIds");
            categoryIds = rawIds.stream().map(Object::toString).reduce((a, b) -> a + "," + b).orElse(null);
            // 解析分类名称用于 prompt
            categoryNames = new ArrayList<>();
            for (Object idObj : rawIds) {
                KgCategory cat = kgCategoryService.getById(Long.valueOf(idObj.toString()));
                if (cat != null) {
                    categoryNames.add(cat.getName());
                }
            }
        }

        log.info("创建面试参数: userId={}, jobType={}, mode={}, questionType={}, resumeId={}, categoryIds={}, categoryNames={}",
                userId, jobType, interviewMode, questionType, resumeId, categoryIds, categoryNames);

        // 创建面试记录
        Interview interview = interviewService.createInterview(userId, resumeId, jobType, interviewMode, categoryIds);
        interview.setQuestionType(questionType);
        interviewMapper.updateById(interview);

        // 加载简历（可选）
        Resume resume = resumeId != null ? resumeService.getResumeById(resumeId) : null;

        // 根据题型生成问题
        String conversationId = interview.getId() + "_" + userId;
        List<String> questions;
        try {
            if ("choice".equals(questionType)) {
                log.info("开始生成选择题: interviewId={}", interview.getId());
                questions = interviewService.generateChoiceQuestions(interview, resume, conversationId, interviewMode, categoryNames);
            } else {
                questions = interviewService.generateAllQuestions(interview, resume, conversationId, interviewMode, categoryNames);
            }
        } catch (Exception e) {
            log.error("问题生成失败: interviewId={}", interview.getId(), e);
            throw e;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("interview", interview);
        data.put("questions", questions);
        return Result.ok(data);
    }

    @GetMapping("/{id}")
    public Result<?> getInterview(@PathVariable Long id) {
        Interview interview = interviewService.getInterviewById(id);
        if (interview == null) {
            return Result.fail("面试不存在");
        }
        return Result.ok(interview);
    }

    @GetMapping("/{id}/history")
    public Result<?> getInterviewHistory(@PathVariable Long id) {
        List<InterviewQA> history = interviewService.getQAHistory(id);
        return Result.ok(history);
    }

    /**
     * 获取用户的面试记录列表
     */
    @GetMapping("/user/{userId}")
    public Result<?> getInterviewsByUserId(@PathVariable Long userId) {
        List<Interview> interviews = interviewService.getInterviewsByUserId(userId);
        return Result.ok(interviews);
    }

    /**
     * 问题语音朗读（返回 MP3 音频字节流）
     * POST /api/interview/tts
     */
    @PostMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<byte[]> tts(@RequestBody TtsRequest request) {
        try {
            byte[] audio = ttsService.synthesize(request.getText());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tts.mp3\"")
                    .body(audio);
        } catch (Exception e) {
            log.error("TTS 合成失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 语义判断回答是否结束
     * 前端录音过程中每隔几秒调用，决定是否停止录音
     */
    @PostMapping("/check-complete")
    public Result<?> checkComplete(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        boolean complete = deepSeekService.isAnswerComplete(text);
        return Result.ok(Map.of("complete", complete));
    }

    /**
     * 提交选择题答案并判分
     * POST /api/interview/{id}/submit-choice
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/{id}/submit-choice")
    public Result<?> submitChoiceAnswers(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, String> answers = (Map<String, String>) request.get("answers");
        if (answers == null || answers.isEmpty()) {
            return Result.fail("答案不能为空");
        }
        Map<String, Object> result = interviewService.submitChoiceAnswers(id, answers);
        return Result.ok(result);
    }
}
