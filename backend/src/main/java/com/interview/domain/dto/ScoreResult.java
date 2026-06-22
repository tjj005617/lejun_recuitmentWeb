package com.interview.domain.dto;

import lombok.Data;

/**
 * 面试评分结果 DTO
 * DeepSeek 模型返回的评分数据结构，包含五个维度的分数和加权总分
 * totalScore 由各维度按权重计算：accuracy*0.3 + clarity*0.2 + logic*0.25 + depth*0.15 + practice*0.1
 */
@Data
public class ScoreResult {
    private int accuracy;
    private int clarity;
    private int logic;
    private int depth;
    private int practice;
    private double totalScore;
    private String feedback;
    private String referenceAnswer;
}
