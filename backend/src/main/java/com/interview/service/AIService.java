package com.interview.service;

import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Resume;

import java.util.List;

/**
 * AI 面试服务接口
 * 定义面试过程中的 AI 能力：开场白生成、问题生成、批量问题生成、报告生成
 * 底层调用 MiMo 模型（OpenAI 兼容格式）
 */
public interface AIService {
    String generateOpening(Resume resume, String jobType);
    String generateQuestion(Resume resume, String previousAnswer, int round, List<InterviewQA> history);
    List<String> generateAllQuestions(Resume resume, String jobType);
    List<String> generateAllQuestions(Resume resume, String jobType, String conversationId);
    String generateReport(Resume resume, List<InterviewQA> qaList, String jobType);
    String generateReport(Resume resume, List<InterviewQA> qaList, String jobType, String conversationId);
}
