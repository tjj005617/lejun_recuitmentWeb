package com.interview.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * MiMo 模型客户端（OpenAI 兼容格式）
 * 用于问题生成、开场白生成、面试报告生成等需要创造力的场景
 *
 * <p>通过 {@link ChatMemory} 实现会话记忆：
 * 当传入 conversationId 时，Advisor 会自动从 Redis 加载历史消息，
 * 并在调用完成后将新的 user/assistant 消息保存回 Redis。</p>
 *
 * <p>会话记忆使 MiMo 能够：
 * <ul>
 *   <li>生成问题时参考前面轮次的问答内容，避免重复提问</li>
 *   <li>生成报告时综合所有轮次的表现进行评估</li>
 * </ul>
 */
@Slf4j
public class MiMoAiClient extends AbstractAiClient {

    private final ChatClient chatClient;

    public MiMoAiClient(@Qualifier("mimoChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    protected String doCall(String systemPrompt, String userPrompt, int maxTokens) {
        return doCall(systemPrompt, userPrompt, maxTokens, null);
    }

    /**
     * 调用 MiMo 模型，支持会话记忆
     * 通过 Advisor 链自动管理消息的历史加载和持久化
     *
     * @param systemPrompt   系统指令（如"你是一位专业的技术面试官"）
     * @param userPrompt     用户消息（包含简历、问题、回答等上下文）
     * @param maxTokens      最大 token 数
     * @param conversationId 会话标识（{interviewId}_{userId}），null 则不启用记忆
     * @return MiMo 模型的文本响应
     */
    @Override
    protected String doCall(String systemPrompt, String userPrompt, int maxTokens, String conversationId) {
        log.debug("MiMo 调用: maxTokens={}, conversationId={}", maxTokens, conversationId);
        var spec = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt);
        // 非空时启用会话记忆，Advisor 会自动加载/保存历史消息
        if (conversationId != null) {
            spec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
        }
        String result = spec.call().content();
        log.debug("MiMo 响应: {}", result != null ? result.substring(0, Math.min(100, result.length())) : "null");
        return result;
    }
}
