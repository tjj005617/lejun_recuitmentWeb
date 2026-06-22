package com.interview.service.ai;

/**
 * 统一 AI 客户端接口
 * 定义 AI 模型调用的统一契约，屏蔽底层 MiMo / DeepSeek 的差异
 *
 * <p>每个实现类封装一个特定 AI 提供商的 Spring AI ChatClient，
 * 通过 {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor}
 * 实现基于 Redis 的会话记忆能力。</p>
 *
 * <p>会话记忆通过 conversationId 参数控制：
 * <ul>
 *   <li>传入 conversationId：启用记忆，自动加载/保存历史消息</li>
 *   <li>传入 null：不启用记忆，单次无状态调用</li>
 * </ul>
 */
public interface AiClient {

    /**
     * 发送提示词并返回文本响应（无记忆）
     *
     * @param systemPrompt 系统级指令（设定 AI 角色）
     * @param userPrompt   用户消息内容
     * @param maxTokens    最大 token 数
     * @return 模型的文本响应
     */
    String call(String systemPrompt, String userPrompt, int maxTokens);

    /**
     * 发送提示词并返回文本响应（带会话记忆）
     *
     * @param systemPrompt   系统级指令
     * @param userPrompt     用户消息内容
     * @param maxTokens      最大 token 数
     * @param conversationId 会话标识（格式：{interviewId}_{userId}），传 null 则不启用记忆
     * @return 模型的文本响应
     */
    String call(String systemPrompt, String userPrompt, int maxTokens, String conversationId);

    /**
     * 发送提示词，使用默认 maxTokens（1024），不启用记忆
     */
    default String call(String systemPrompt, String userPrompt) {
        return call(systemPrompt, userPrompt, 1024);
    }

    /**
     * 发送提示词，使用默认 maxTokens（1024），启用会话记忆
     */
    default String call(String systemPrompt, String userPrompt, String conversationId) {
        return call(systemPrompt, userPrompt, 1024, conversationId);
    }
}
