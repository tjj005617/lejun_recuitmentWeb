package com.interview.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * DeepSeek 模型客户端（OpenAI 兼容格式）
 * 用于答案评分等需要判断力的场景
 *
 * <p>通过 {@link ChatMemory} 实现会话记忆：
 * 当传入 conversationId 时，Advisor 会自动从 Redis 加载历史评分记录，
 * 使 DeepSeek 能够参考前面轮次的评分结果，保持评分标准的一致性。</p>
 *
 * <p>会话记忆使 DeepSeek 能够：
 * <ul>
 *   <li>参考前面轮次的评分，保持整体评分尺度一致</li>
 *   <li>在 feedback 中引用之前的评估，给出更连贯的反馈</li>
 * </ul>
 */
@Slf4j
public class DeepSeekAiClient extends AbstractAiClient {

    private final ChatClient chatClient;

    public DeepSeekAiClient(@Qualifier("deepseekChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    protected String doCall(String systemPrompt, String userPrompt, int maxTokens) {
        return doCall(systemPrompt, userPrompt, maxTokens, null);
    }

    /**
     * 调用 DeepSeek 模型，支持会话记忆
     *
     * @param systemPrompt   系统指令（如"你是一位技术面试官"）
     * @param userPrompt     评分请求（包含问题和回答）
     * @param maxTokens      最大 token 数
     * @param conversationId 会话标识（{interviewId}_{userId}），null 则不启用记忆
     * @return DeepSeek 模型的 JSON 评分响应
     */
    @Override
    protected String doCall(String systemPrompt, String userPrompt, int maxTokens, String conversationId) {
        log.debug("DeepSeek 调用: maxTokens={}, conversationId={}", maxTokens, conversationId);
        var spec = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt);
        if (conversationId != null) {
            spec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
        }
        String result = spec.call().content();
        log.debug("DeepSeek 响应: {}", result != null ? result.substring(0, Math.min(100, result.length())) : "null");
        return result;
    }

    /**
     * DeepSeek 评分失败时返回空字符串，由调用方使用默认评分
     */
    @Override
    protected String getDefaultFallback() {
        return "";
    }
}
