package com.interview.service;

import com.interview.domain.dto.ScoreResult;
import com.interview.domain.po.Interview;
import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Resume;

import java.util.List;
import java.util.Map;

/**
 * 面试业务服务接口
 * 定义面试的完整生命周期管理：创建、查询、评分保存、完成、报告生成
 * 协调 AIService（问题/报告生成）和数据库操作
 */
public interface InterviewService {
    Interview createInterview(Long userId, Long resumeId, String jobType);
    /** 创建面试（支持三种模式） */
    Interview createInterview(Long userId, Long resumeId, String jobType, String interviewMode, String categoryIds);
    Interview getInterviewById(Long id);
    List<Interview> getInterviewsByUserId(Long userId);
    List<InterviewQA> getQAHistory(Long interviewId);
    InterviewQA getQAByRound(Long interviewId, int round);
    void saveQA(Long interviewId, int round, String question, String answer, ScoreResult scoreResult);
    void updateCurrentRound(Long interviewId, int round);
    void completeInterview(Long interviewId);
    void saveReport(Long interviewId, String report);
    List<String> generateAllQuestions(Interview interview, Resume resume);
    List<String> generateAllQuestions(Interview interview, Resume resume, String conversationId);
    /** 批量生成问题（支持分类上下文） */
    List<String> generateAllQuestions(Interview interview, Resume resume, String conversationId, String interviewMode, List<String> categoryNames);
    /** 生成选择题（20道四选一） */
    List<String> generateChoiceQuestions(Interview interview, Resume resume, String conversationId, String interviewMode, List<String> categoryNames);
    /** 提交选择题答案并判分 */
    Map<String, Object> submitChoiceAnswers(Long interviewId, Map<String, String> answers);

    // 报告生成
    String generateReportForInterview(Resume resume, List<InterviewQA> qaList, String jobType);
    String generateReportForInterview(Resume resume, List<InterviewQA> qaList, String jobType, String conversationId);
}
