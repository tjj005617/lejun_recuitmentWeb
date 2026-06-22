package com.interview.service;

/**
 * TTS 语音合成服务接口
 */
public interface TtsService {

    /**
     * 语音合成，返回完整 MP3 音频字节数组
     * @param text 要朗读的文本
     * @return MP3 音频数据
     * @throws Exception 合成失败时抛出异常
     */
    byte[] synthesize(String text) throws Exception;
}
