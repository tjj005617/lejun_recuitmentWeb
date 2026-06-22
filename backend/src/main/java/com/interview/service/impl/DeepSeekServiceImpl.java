package com.interview.service.impl;

import com.interview.domain.dto.ScoreResult;
import com.interview.service.DeepSeekService;
import com.interview.service.ai.AiClient;
import com.interview.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * DeepSeek 评分服务实现类
 * 调用 DeepSeek 模型（通过 AiClient）对面试回答进行多维度评分
 *
 * <p>评分维度（0-10分）：</p>
 * <ul>
 *   <li>accuracy（准确度）- 权重 30%</li>
 *   <li>clarity（清晰度）- 权重 20%</li>
 *   <li>logic（逻辑性）- 权重 25%</li>
 *   <li>depth（深度）- 权重 15%</li>
 *   <li>practice（实践性）- 权重 10%</li>
 * </ul>
 *
 * <p>支持会话记忆：通过 conversationId 参数，DeepSeek 能够参考前面轮次的评分，
 * 保持整体评分尺度的一致性。</p>
 */
@Slf4j
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    private final AiClient aiClient;

    /**
     * 手动构造函数，确保 @Qualifier 生效
     */
    public DeepSeekServiceImpl(@Qualifier("deepseekAiClient") AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public ScoreResult evaluateAnswer(String question, String answer) {
        return evaluateAnswer(question, answer, null);
    }

    @Override
    public ScoreResult evaluateAnswer(String question, String answer, String conversationId) {
        String systemPrompt = "你是一位技术面试官。请严格评估面试回答。";
        String userPrompt = String.format(
            "请严格评估以下面试回答。\n\n" +
            "问题：%s\n回答：%s\n\n" +
            "评分标准（0-10分）：\n" +
            "- 0分：回答为不知道/忘了/不会/不清楚/没印象，或回答为空白、只有1-2个无意义的字\n" +
            "- 1-3分：回答严重不足，有明显错误或答非所问\n" +
            "- 4-6分：基本回答了问题，但不够深入\n" +
            "- 7-9分：回答准确、有深度、有实际经验\n" +
            "- 10分：回答完美，超出预期\n\n" +
            "【关键规则】如果回答中出现了'不知道'、'忘了'、'忘记了'、'不记得'、'不会'、'不清楚'、'没印象'等表示无法回答的词语，所有维度都必须给0分，不要给任何同情分。\n\n" +
            "feedback要求：用中文给出反馈。如果是0分，直接说'未能回答该问题，建议加强相关知识点的学习。'\n\n" +
            "referenceAnswer要求：用中文给出该问题的参考答案，简洁但完整地回答问题，3-5句话。\n\n" +
            "【严格要求】只返回纯JSON对象，不要有任何其他文字。直接以 { 开始，以 } 结束。\n" +
            "{\"accuracy\":数字,\"clarity\":数字,\"logic\":数字,\"depth\":数字,\"practice\":数字,\"feedback\":\"反馈内容\",\"referenceAnswer\":\"参考答案\"}",
            question, answer
        );

        String response = aiClient.call(systemPrompt, userPrompt, conversationId);
        log.info("DeepSeek 原始响应: {}", response);

        ScoreResult result = parseScoreResult(response);
        log.info("评分结果: accuracy={}, clarity={}, logic={}, depth={}, practice={}",
            result.getAccuracy(), result.getClarity(), result.getLogic(), result.getDepth(), result.getPractice());
        return result;
    }

    /**
     * AI 语义分析：判断面试者的回答是否已结束
     * 用轻量级 prompt 快速判断，控制延迟
     */
    @Override
    public boolean isAnswerComplete(String answer) {
        if (answer == null || answer.isBlank()) return true;

        try {
            String prompt = String.format(
                "判断以下面试回答是否已经结束。只回答YES或NO。\n\n" +
                "【严格规则】只有包含明确结束语才算结束，其他情况一律回答NO。\n\n" +
                "结束语包括：\n" +
                "- 回答完毕\n" +
                "- 我说完了\n" +
                "- 我的回答完毕\n" +
                "- 以上就是我的回答\n" +
                "- 就这些\n" +
                "- 没有了\n" +
                "- 总的来说...\n" +
                "- 综上所述...\n" +
                "- 以上是...\n\n" +
                "注意：\n" +
                "- 语句完整不算结束，必须有结束语\n" +
                "- 话没说完不算结束\n" +
                "- 即使内容很完整，没有结束语也回答NO\n\n" +
                "回答内容：%s", answer
            );
            String result = aiClient.call("你是语义分析助手。只回答YES或NO。严格按规则判断。", prompt, null);
            String normalized = result.trim().toUpperCase();
            boolean complete = normalized.contains("YES");
            log.info("回答完成判断: text='{}', result='{}', complete={}",
                answer.length() > 50 ? answer.substring(0, 50) + "..." : answer, normalized, complete);
            return complete;
        } catch (Exception e) {
            log.warn("AI回答完成判断失败，默认认为未结束", e);
            return false;
        }
    }

    /**
     * 解析评分 JSON
     */
    private ScoreResult parseScoreResult(String response) {
        try {
            String json = JsonUtils.extractJsonObject(response);
            log.info("解析的评分JSON: {}", json);

            ScoreResult result = JsonUtils.parseJson(json, ScoreResult.class);

            result.setTotalScore(
                result.getAccuracy() * 0.3 +
                result.getClarity() * 0.2 +
                result.getLogic() * 0.25 +
                result.getDepth() * 0.15 +
                result.getPractice() * 0.1
            );
            return result;
        } catch (Exception e) {
            log.error("评分结果解析失败，使用默认值", e);
            ScoreResult defaultResult = new ScoreResult();
            defaultResult.setAccuracy(5);
            defaultResult.setClarity(5);
            defaultResult.setLogic(5);
            defaultResult.setDepth(5);
            defaultResult.setPractice(5);
            defaultResult.setTotalScore(5.0);
            defaultResult.setFeedback("评分服务暂不可用，使用默认评分");
            defaultResult.setReferenceAnswer("参考答案暂不可用");
            return defaultResult;
        }
    }
}
