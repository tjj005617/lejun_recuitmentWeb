package com.interview.controller;

import com.interview.common.Result;
import com.interview.domain.po.Interview;
import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Resume;
import com.interview.dto.TtsRequest;
import com.interview.service.DeepSeekService;
import com.interview.service.InterviewService;
import com.interview.service.ResumeService;
import com.interview.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final ResumeService resumeService;
    private final TtsService ttsService;
    private final DeepSeekService deepSeekService;

    /**
     * 创建面试
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
}
