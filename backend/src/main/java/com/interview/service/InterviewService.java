package com.interview.service;

import com.interview.domain.dto.ScoreResult;
import com.interview.domain.po.Interview;
import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Resume;

import java.util.List;

/**
 * 面试业务服务接口
 * 定义面试的完整生命周期管理：创建、查询、评分保存、完成、报告生成
 * 协调 AIService（问题/报告生成）和数据库操作
 */
public interface InterviewService {
    Interview createInterview(Long userId, Long resumeId, String jobType);
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

    // 报告生成
    String generateReportForInterview(Resume resume, List<InterviewQA> qaList, String jobType);
    String generateReportForInterview(Resume resume, List<InterviewQA> qaList, String jobType, String conversationId);
}
