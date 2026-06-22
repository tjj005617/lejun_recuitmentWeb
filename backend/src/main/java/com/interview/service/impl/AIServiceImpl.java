package com.interview.service.impl;

import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Resume;
import com.interview.service.AIService;
import com.interview.service.ai.AiClient;
import com.interview.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 面试服务实现类
 * 调用 MiMo 模型（通过 AiClient）完成：
 * - 开场白生成：概括候选人优势 + 介绍流程 + 首个问题
 * - 问题生成：根据简历和历史回答逐轮生成问题
 * - 批量问题生成：单次 AI 调用生成全部 10 道面试题
 * - 报告生成：综合所有轮次的问答记录生成评估报告
 *
 * <p>支持会话记忆：通过 conversationId 参数启用 Redis 持久化的对话历史，
 * 使 AI 能够参考前面轮次的内容，生成更有针对性的问题和报告。</p>
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private final AiClient aiClient;

    /**
     * 手动构造函数，确保 @Qualifier 生效
     * Lombok 的 @RequiredArgsConstructor 不会将字段上的 @Qualifier 传递到构造函数参数
     */
    public AIServiceImpl(@Qualifier("mimoAiClient") AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public String generateOpening(Resume resume, String jobType) {
        String systemPrompt = "你是一位专业的面试官。";
        String userPrompt = String.format(
            "候选人简历内容如下：\n%s\n\n" +
            "请仔细阅读简历内容，然后：\n" +
            "1. 用一句话概括候选人的核心优势\n" +
            "2. 简单介绍面试流程（共10轮，每轮会根据回答评分）\n" +
            "3. 提出第一个针对简历中具体项目经验的问题\n\n" +
            "注意：问题必须基于简历中的实际内容，不要问与简历无关的问题。",
            resume.getRawContent()
        );
        return aiClient.call(systemPrompt, userPrompt);
    }

    @Override
    public String generateQuestion(Resume resume, String previousAnswer, int round, List<InterviewQA> history) {
        StringBuilder historyContext = new StringBuilder();
        for (InterviewQA qa : history) {
            historyContext.append(String.format("第%d轮：\n问题：%s\n回答：%s\n\n", qa.getRound(), qa.getQuestion(), qa.getAnswer()));
        }

        String systemPrompt = "你是一位专业的技术面试官。";
        String userPrompt = String.format(
            "候选人简历内容如下：\n%s\n\n" +
            "已完成的面试轮次：\n%s\n" +
            "上一轮回答：%s\n\n" +
            "当前是第%d轮/共10轮面试。\n\n" +
            "请根据候选人的简历和之前的回答，生成下一个面试问题。\n\n" +
            "要求：\n" +
            "1. 问题必须基于简历中的具体项目或技术栈\n" +
            "2. 逐步深入，从项目细节到原理\n" +
            "3. 结合实际场景提问\n" +
            "4. 不要重复已经问过的问题\n\n" +
            "请直接返回问题，不要添加其他内容。",
            resume.getRawContent(), historyContext, previousAnswer, round
        );
        return aiClient.call(systemPrompt, userPrompt);
    }

    @Override
    public List<String> generateAllQuestions(Resume resume, String jobType) {
        return generateAllQuestions(resume, jobType, null);
    }

    @Override
    public List<String> generateAllQuestions(Resume resume, String jobType, String conversationId) {
        String systemPrompt = "你是一位专业的技术面试官。请严格返回合法的JSON数组，不要包含任何额外文本。";
        String userPrompt = String.format(
            "候选人简历内容如下：\n%s\n\n" +
            "请根据候选人背景生成10道面试题，要求：\n" +
            "1. 第1题为自我介绍\n" +
            "2. 覆盖项目经验、技术深度、基础知识、编码能力、系统设计\n" +
            "3. 难度从易到难递进\n" +
            "4. 结合候选人简历中的具体项目和技术栈\n" +
            "5. 不要重复考察相同知识点\n\n" +
            "返回纯JSON数组，格式：\n" +
            "[{\"index\":0,\"question\":\"题目内容\",\"category\":\"分类\"}]\n" +
            "分类包括：self-intro, project, technical, coding, system-design\n\n" +
            "重要约束：\n" +
            "- question字段中的双引号必须用\\\"转义\n" +
            "- question字段中不要使用方括号[]或花括号{}\n" +
            "- 如果需要列举内容，用顿号、逗号或换行分隔，不要用JSON数组格式\n" +
            "- 直接返回JSON数组，不要添加markdown代码块或其他内容",
            resume.getRawContent()
        );

        String response = aiClient.call(systemPrompt, userPrompt, 2048, conversationId);
        return parseQuestionArray(response);
    }

    @Override
    public String generateReport(Resume resume, List<InterviewQA> qaList, String jobType) {
        return generateReport(resume, qaList, jobType, null);
    }

    @Override
    public String generateReport(Resume resume, List<InterviewQA> qaList, String jobType, String conversationId) {
        StringBuilder qaContext = new StringBuilder();
        for (InterviewQA qa : qaList) {
            qaContext.append(String.format("第%d轮：\n问题：%s\n回答：%s\n评分：%s\n\n",
                qa.getRound(), qa.getQuestion(), qa.getAnswer(), qa.getScores()));
        }

        String systemPrompt = "你是一位专业的面试评估专家。";
        String userPrompt = String.format(
            "请根据以下面试记录生成综合评估报告。\n\n" +
            "候选人简历：%s\n" +
            "应聘岗位：%s\n" +
            "面试记录：%s\n\n" +
            "请生成包含以下内容的报告：\n" +
            "1. 综合评分\n" +
            "2. 优势分析\n" +
            "3. 不足与改进建议\n" +
            "4. 录用建议（强烈推荐/推荐/待定/不推荐）",
            resume.getRawContent(), jobType, qaContext
        );
        return aiClient.call(systemPrompt, userPrompt, conversationId);
    }

    private List<String> parseQuestionArray(String response) {
        try {
            String json = JsonUtils.extractJsonArray(response);
            List<Map<String, Object>> questionList = JsonUtils.parseJsonList(json);

            List<String> questions = new ArrayList<>();
            for (Map<String, Object> item : questionList) {
                questions.add((String) item.get("question"));
            }
            log.info("批量生成问题成功，共{}道", questions.size());
            return questions;
        } catch (Exception e) {
            log.warn("JSON标准解析失败，尝试正则提取: {}", e.getMessage());
            return parseQuestionArrayByRegex(response);
        }
    }

    /**
     * 正则兜底解析：AI 返回的 JSON 可能包含未转义的引号导致 Jackson 解析失败，
     * 此时用正则逐个提取 "question":"..." 字段
     */
    private List<String> parseQuestionArrayByRegex(String response) {
        List<String> questions = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\"question\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
        );
        java.util.regex.Matcher matcher = pattern.matcher(response);
        while (matcher.find()) {
            String question = matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
            questions.add(question);
        }
        if (questions.isEmpty()) {
            log.error("正则提取也失败，原始响应: {}", response);
            questions.add("请做一下自我介绍");
            for (int i = 1; i < 10; i++) {
                questions.add("请描述你的技术栈和项目经验（第" + (i + 1) + "题）");
            }
        } else {
            log.info("正则提取问题成功，共{}道", questions.size());
        }
        return questions;
    }
}
