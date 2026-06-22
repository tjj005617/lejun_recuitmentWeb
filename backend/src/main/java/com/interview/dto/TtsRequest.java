package com.interview.dto;

import lombok.Data;

/**
 * TTS 语音合成请求
 */
@Data
public class TtsRequest {
    /** 要朗读的文本 */
    private String text;
}
