package com.interview.service;

import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Job;
import com.interview.domain.po.Resume;

import java.util.List;
import java.util.Map;

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
    /** 批量生成问题（支持分类上下文） */
    List<String> generateAllQuestions(Resume resume, String jobType, String conversationId, String interviewMode, List<String> categoryNames);
    /** 生成20道选择题（含选项、正确答案、解析） */
    List<Map<String, Object>> generateChoiceQuestions(Resume resume, String jobType, String conversationId, String interviewMode, List<String> categoryNames);
    String generateReport(Resume resume, List<InterviewQA> qaList, String jobType);
    String generateReport(Resume resume, List<InterviewQA> qaList, String jobType, String conversationId);

    /**
     * 生成简历摘要（用于 embedding 向量化）
     * 提取核心技能、工作经验、技术栈、求职意向，输出 200-300 字结构化摘要
     */
    String generateResumeSummary(Resume resume);

    /**
     * 生成岗位摘要（用于 embedding 向量化）
     * 提取核心技术要求、经验要求、岗位职责要点，输出 200-300 字结构化摘要
     */
    String generateJobSummary(Job job);
}
