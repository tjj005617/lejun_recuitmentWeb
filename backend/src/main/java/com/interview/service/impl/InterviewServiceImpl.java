package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.domain.dto.ScoreResult;
import com.interview.domain.po.Interview;
import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Report;
import com.interview.domain.po.Resume;
import com.interview.mapper.InterviewMapper;
import com.interview.mapper.InterviewQAMapper;
import com.interview.mapper.ReportMapper;
import com.interview.service.AIService;
import com.interview.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 面试业务服务实现类
 * 管理面试的完整生命周期：
 * <ul>
 *   <li>创建面试：初始化面试记录 + AI 批量生成 10 道问题</li>
 *   <li>评分保存：将 DeepSeek 评分结果持久化到 interview_qa 表</li>
 *   <li>完成面试：计算总分 + 生成综合评估报告</li>
 * </ul>
 *
 * <p>协调 AIService（MiMo 模型生成问题/报告）和 InterviewMapper/InterviewQAMapper（数据库操作）</p>
 */
public class InterviewServiceImpl implements InterviewService {

    private final InterviewMapper interviewMapper;
    private final InterviewQAMapper interviewQAMapper;
    private final ReportMapper reportMapper;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    /**
     * 创建面试（兼容旧接口）
     */
    @Override
    public Interview createInterview(Long userId, Long resumeId, String jobType) {
        return createInterview(userId, resumeId, jobType, "resume", null);
    }

    /**
     * 创建面试（支持三种模式：resume/topic/hybrid）
     */
    @Override
    public Interview createInterview(Long userId, Long resumeId, String jobType, String interviewMode, String categoryIds) {
        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setResumeId(resumeId);
        interview.setJobType(jobType);
        interview.setInterviewMode(interviewMode);
        interview.setCategoryIds(categoryIds);
        interview.setTotalRounds(10);
        interview.setCurrentRound(1);
        interview.setStatus("IN_PROGRESS");
        interviewMapper.insert(interview);
        log.info("面试创建成功: id={}, mode={}", interview.getId(), interviewMode);
        return interview;
    }

    @Override
    public Interview getInterviewById(Long id) {
        return interviewMapper.selectById(id);
    }

    @Override
    public List<Interview> getInterviewsByUserId(Long userId) {
        return interviewMapper.selectList(
            new LambdaQueryWrapper<Interview>()
                .eq(Interview::getUserId, userId)
                .orderByDesc(Interview::getCreatedAt)
        );
    }

    @Override
    public List<InterviewQA> getQAHistory(Long interviewId) {
        return interviewQAMapper.selectList(
            new LambdaQueryWrapper<InterviewQA>()
                .eq(InterviewQA::getInterviewId, interviewId)
                .orderByAsc(InterviewQA::getRound)
        );
    }

    @Override
    public InterviewQA getQAByRound(Long interviewId, int round) {
        return interviewQAMapper.selectOne(
            new LambdaQueryWrapper<InterviewQA>()
                .eq(InterviewQA::getInterviewId, interviewId)
                .eq(InterviewQA::getRound, round)
        );
    }

    @Override
    public void saveQA(Long interviewId, int round, String question, String answer, ScoreResult scoreResult) {
        try {
            InterviewQA qa = getQAByRound(interviewId, round);
            if (qa == null) {
                qa = new InterviewQA();
                qa.setInterviewId(interviewId);
                qa.setRound(round);
                qa.setQuestion(question);
                qa.setAnswer(answer);
                qa.setScores(objectMapper.writeValueAsString(scoreResult));
                qa.setFeedback(scoreResult.getFeedback());
                qa.setReferenceAnswer(scoreResult.getReferenceAnswer());
                interviewQAMapper.insert(qa);
            } else {
                qa.setAnswer(answer);
                qa.setScores(objectMapper.writeValueAsString(scoreResult));
                qa.setFeedback(scoreResult.getFeedback());
                qa.setReferenceAnswer(scoreResult.getReferenceAnswer());
                interviewQAMapper.updateById(qa);
            }
        } catch (Exception e) {
            throw new RuntimeException("保存问答记录失败", e);
        }
    }

    @Override
    public void updateCurrentRound(Long interviewId, int round) {
        Interview interview = interviewMapper.selectById(interviewId);
        interview.setCurrentRound(round);
        interview.setStatus("IN_PROGRESS");
        interviewMapper.updateById(interview);
    }

    @Override
    public void completeInterview(Long interviewId) {
        Interview interview = interviewMapper.selectById(interviewId);
        interview.setStatus("COMPLETED");
        interview.setCompletedAt(LocalDateTime.now());

        List<InterviewQA> qaList = getQAHistory(interviewId);
        double totalScore = calculateTotalScore(qaList);
        interview.setTotalScore(totalScore);

        interviewMapper.updateById(interview);
        log.info("面试完成: id={}, 总分={}", interviewId, totalScore);
    }

    @Override
    public void saveReport(Long interviewId, String reportContent) {
        try {
            Report report = new Report();
            report.setInterviewId(interviewId);
            report.setSummary(reportContent);
            reportMapper.insert(report);
        } catch (Exception e) {
            throw new RuntimeException("保存报告失败", e);
        }
    }

    /**
     * 同步批量生成所有面试问题（兼容旧接口）
     */
    @Override
    public List<String> generateAllQuestions(Interview interview, Resume resume) {
        return generateAllQuestions(interview, resume, null, null, null);
    }

    @Override
    public List<String> generateAllQuestions(Interview interview, Resume resume, String conversationId) {
        return generateAllQuestions(interview, resume, conversationId, null, null);
    }

    /**
     * 批量生成问题（支持分类上下文）
     */
    @Override
    public List<String> generateAllQuestions(Interview interview, Resume resume, String conversationId, String interviewMode, List<String> categoryNames) {
        Long interviewId = interview.getId();
        log.info("开始批量生成面试问题: interviewId={}, mode={}", interviewId, interviewMode);

        // 单次AI调用生成全部问题
        List<String> questions = aiService.generateAllQuestions(resume, interview.getJobType(), conversationId, interviewMode, categoryNames);
        log.info("问题生成完成，共{}道: interviewId={}", questions.size(), interviewId);

        // 存入数据库
        for (int i = 0; i < questions.size(); i++) {
            InterviewQA qa = new InterviewQA();
            qa.setInterviewId(interviewId);
            qa.setRound(i + 1);
            qa.setQuestion(questions.get(i));
            interviewQAMapper.insert(qa);
        }

        log.info("问题已入库: interviewId={}", interviewId);
        return questions;
    }

    /**
     * 生成选择题（20道四选一）
     */
    @Override
    public List<String> generateChoiceQuestions(Interview interview, Resume resume,
            String conversationId, String interviewMode, List<String> categoryNames) {
        Long interviewId = interview.getId();
        log.info("开始生成选择题: interviewId={}, mode={}, resume={}, categoryNames={}", interviewId, interviewMode, resume != null ? "有" : "无", categoryNames);

        try {
            // AI 生成选择题（返回 List<Map>）
            List<Map<String, Object>> choiceQuestions = aiService.generateChoiceQuestions(
                    resume, interview.getJobType(), conversationId, interviewMode, categoryNames);
            log.info("选择题生成完成，共{}道: interviewId={}", choiceQuestions.size(), interviewId);

            // 将每道题序列化为 JSON 字符串存入 question 字段
            List<String> questionStrs = new ArrayList<>();
            for (int i = 0; i < choiceQuestions.size(); i++) {
                Map<String, Object> q = choiceQuestions.get(i);
                String questionJson = objectMapper.writeValueAsString(q);

                InterviewQA qa = new InterviewQA();
                qa.setInterviewId(interviewId);
                qa.setRound(i + 1);
                qa.setQuestion(questionJson);
                interviewQAMapper.insert(qa);

                questionStrs.add(questionJson);
            }

            // 设置总轮数
            interview.setTotalRounds(choiceQuestions.size());
            interviewMapper.updateById(interview);

            log.info("选择题已入库: interviewId={}, 共{}道", interviewId, questionStrs.size());
            return questionStrs;
        } catch (Exception e) {
            log.error("选择题生成异常: interviewId={}", interviewId, e);
            throw new RuntimeException("选择题生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提交选择题答案并判分
     * 正确得5分，错误得0分，满分100
     */
    @Override
    public Map<String, Object> submitChoiceAnswers(Long interviewId, Map<String, String> answers) {
        List<InterviewQA> qaList = getQAHistory(interviewId);
        int correctCount = 0;
        int total = qaList.size();
        List<Map<String, Object>> details = new ArrayList<>();

        for (InterviewQA qa : qaList) {
            int round = qa.getRound();
            String userAnswer = answers.get(String.valueOf(round));
            if (userAnswer == null) userAnswer = "";

            // 从 question JSON 中提取正确答案和解析
            String correctAnswer = "";
            String explanation = "";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> qData = objectMapper.readValue(qa.getQuestion(), Map.class);
                correctAnswer = (String) qData.get("correctAnswer");
                explanation = (String) qData.getOrDefault("explanation", "");
            } catch (Exception e) {
                log.error("解析选择题JSON失败: round={}", round, e);
            }

            boolean isCorrect = userAnswer.equalsIgnoreCase(correctAnswer);
            if (isCorrect) correctCount++;

            // 更新 QA 记录
            qa.setAnswer(userAnswer);
            try {
                Map<String, Object> scoreMap = new HashMap<>();
                scoreMap.put("correct", isCorrect);
                scoreMap.put("correctAnswer", correctAnswer);
                scoreMap.put("explanation", explanation);
                scoreMap.put("totalScore", isCorrect ? 5.0 : 0.0);
                qa.setScores(objectMapper.writeValueAsString(scoreMap));
            } catch (Exception e) {
                log.error("序列化评分失败", e);
            }
            qa.setFeedback(isCorrect ? "回答正确" : "回答错误，正确答案是 " + correctAnswer);
            qa.setReferenceAnswer(explanation);
            interviewQAMapper.updateById(qa);

            // 收集详情
            Map<String, Object> detail = new HashMap<>();
            detail.put("round", round);
            detail.put("userAnswer", userAnswer);
            detail.put("correctAnswer", correctAnswer);
            detail.put("correct", isCorrect);
            detail.put("explanation", explanation);
            details.add(detail);
        }

        // 计算总分（满分100）
        double totalScore = total > 0 ? (correctCount * 100.0 / total) : 0;

        // 更新面试记录
        Interview interview = interviewMapper.selectById(interviewId);
        interview.setTotalScore(totalScore);
        interview.setStatus("COMPLETED");
        interview.setCompletedAt(LocalDateTime.now());
        interviewMapper.updateById(interview);

        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", totalScore);
        result.put("correctCount", correctCount);
        result.put("total", total);
        result.put("details", details);

        log.info("选择题判分完成: interviewId={}, 正确{}/{}", interviewId, correctCount, total);
        return result;
    }

    // ==================== 报告生成 ====================

    /**
     * 生成面试报告
     */
    @Override
    public String generateReportForInterview(Resume resume, List<InterviewQA> qaList, String jobType) {
        return generateReportForInterview(resume, qaList, jobType, null);
    }

    @Override
    public String generateReportForInterview(Resume resume, List<InterviewQA> qaList, String jobType, String conversationId) {
        try {
            return aiService.generateReport(resume, qaList, jobType, conversationId);
        } catch (Exception e) {
            log.error("生成报告失败", e);
            return "报告生成失败，请稍后查看。";
        }
    }

    // ==================== 私有方法 ====================

    private double calculateTotalScore(List<InterviewQA> qaList) {
        if (qaList.isEmpty()) return 0;

        double total = 0;
        int count = 0;
        for (InterviewQA qa : qaList) {
            try {
                if (qa.getScores() != null && !qa.getScores().isEmpty()) {
                    ScoreResult score = objectMapper.readValue(qa.getScores(), ScoreResult.class);
                    total += score.getTotalScore();
                    count++;
                }
            } catch (Exception e) {
                log.error("解析评分失败", e);
            }
        }
        return count > 0 ? total / count : 0;
    }
}
