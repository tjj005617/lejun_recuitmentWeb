package com.interview.controller;

import com.interview.common.Result;
import com.interview.service.SttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音识别控制器
 * 接收前端上传的音频文件，调用 MiMo ASR 识别后返回文字
 */
@Slf4j
@RestController
@RequestMapping("/api/stt")
@RequiredArgsConstructor
public class SttController {

    private final SttService sttService;

    /**
     * 语音识别
     * POST /api/stt
     * Content-Type: multipart/form-data
     * Body: file (WAV 音频文件)
     */
    @PostMapping
    public Result<String> recognize(@RequestParam("file") MultipartFile file) {
        try {
            String text = sttService.recognize(file.getBytes());
            return Result.ok(text);
        } catch (Exception e) {
            log.error("语音识别失败", e);
            return Result.fail("语音识别失败: " + e.getMessage());
        }
    }
}
