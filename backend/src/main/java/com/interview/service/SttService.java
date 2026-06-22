package com.interview.service;

/**
 * 语音识别服务接口
 */
public interface SttService {

    /**
     * 语音识别，返回识别出的文字
     * @param audioData WAV 音频文件字节数组
     * @return 识别出的文字
     * @throws Exception 识别失败时抛出异常
     */
    String recognize(byte[] audioData) throws Exception;
}
