package com.interview.service;

import com.interview.domain.dto.ScoreResult;

/**
 * DeepSeek 评分服务接口
 * 定义面试回答的 AI 评分能力
 * 底层调用 DeepSeek 模型（OpenAI 兼容格式），对回答进行多维度评分
 */
public interface DeepSeekService {
    ScoreResult evaluateAnswer(String question, String answer);
    ScoreResult evaluateAnswer(String question, String answer, String conversationId);

    /**
     * AI 语义分析：判断面试者的回答是否已结束
     *
     * @param answer 面试者的回答文本
     * @return true 表示回答已结束
     */
    boolean isAnswerComplete(String answer);
}
