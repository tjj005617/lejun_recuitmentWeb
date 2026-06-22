package com.interview.config;

import com.interview.config.memory.RedisChatMemoryRepository;
import com.interview.service.ai.DeepSeekAiClient;
import com.interview.service.ai.MiMoAiClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/**
 * AI 服务配置类
 * 负责创建和装配两个 AI 模型的完整调用链：
 *
 * <pre>
 *   RedisChatMemoryRepository (Redis 持久化存储)
 *     → MessageWindowChatMemory (消息窗口管理，保留最近 N 条)
 *       → MessageChatMemoryAdvisor (Spring AI 顾问，自动加载/保存历史)
 *         → ChatClient (对外暴露的 AI 调用入口)
 * </pre>
 *
 * <p>两个模型各自独立：
 * <ul>
 *   <li>MiMo（OpenAI 兼容）：用于问题生成、报告生成</li>
 *   <li>DeepSeek（OpenAI 兼容）：用于答案评分</li>
 * </ul>
 *
 * <p>会话记忆 key 格式：ai:interview:{interviewId}_{userId}</p>
 */
@Configuration
@EnableConfigurationProperties({AiProperties.class, DeepSeekProperties.class})
public class AiConfig {

    // ==================== 会话记忆存储层 ====================

    /**
     * Redis 会话记忆存储仓库
     * 两个模型共享同一个 Repository 实例，底层都存储在同一个 Redis 中
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository(StringRedisTemplate redisTemplate) {
        return new RedisChatMemoryRepository(redisTemplate);
    }

    /**
     * MiMo 模型的会话记忆管理器
     * 使用消息窗口策略，保留最近 20 条消息，防止上下文过长
     */
    @Bean("mimoChatMemory")
    public ChatMemory mimoChatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    /**
     * DeepSeek 模型的会话记忆管理器
     * 与 MiMo 独立，各自维护各自的对话历史
     */
    @Bean("deepseekChatMemory")
    public ChatMemory deepseekChatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    // ==================== 会话记忆 Advisor 层 ====================

    /**
     * MiMo 会话记忆顾问
     * 作为 ChatClient 的拦截器，在每次调用前自动加载历史消息，调用后自动保存新消息
     */
    @Bean("mimoMemoryAdvisor")
    public MessageChatMemoryAdvisor mimoMemoryAdvisor(
            @Qualifier("mimoChatMemory") ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /**
     * DeepSeek 会话记忆顾问
     */
    @Bean("deepseekMemoryAdvisor")
    public MessageChatMemoryAdvisor deepseekMemoryAdvisor(
            @Qualifier("deepseekChatMemory") ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    // ==================== ChatClient 调用层 ====================

    /**
     * MiMo 模型 ChatClient（OpenAI 兼容格式）
     * 注册了 mimoMemoryAdvisor 作为默认顾问，所有通过此 client 的调用都自动具备记忆能力
     * 调用方通过 .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id)) 指定会话 ID
     */
    @Bean("mimoChatClient")
    public ChatClient mimoChatClient(AiProperties aiProperties,
                                     @Qualifier("mimoMemoryAdvisor") MessageChatMemoryAdvisor advisor) {
        // 配置 RestClient 超时时间
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(aiProperties.getTimeout()));
        org.springframework.web.client.RestClient.Builder restClientBuilder =
            org.springframework.web.client.RestClient.builder().requestFactory(requestFactory);

        OpenAiApi openAiApi = OpenAiApi.builder()
            .baseUrl(aiProperties.getApiUrl())
            .apiKey(aiProperties.getApiKey())
            .restClientBuilder(restClientBuilder)
            .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(aiProperties.getModel())
                .build())
            .build();

        return ChatClient.builder(chatModel)
            .defaultAdvisors(advisor)
            .build();
    }

    /**
     * DeepSeek 模型 ChatClient（OpenAI 兼容格式）
     * 注册了 deepseekMemoryAdvisor 作为默认顾问
     */
    @Bean("deepseekChatClient")
    public ChatClient deepseekChatClient(DeepSeekProperties deepSeekProperties,
                                         @Qualifier("deepseekMemoryAdvisor") MessageChatMemoryAdvisor advisor) {
        // 配置 RestClient 超时时间
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(deepSeekProperties.getTimeout()));
        org.springframework.web.client.RestClient.Builder restClientBuilder =
            org.springframework.web.client.RestClient.builder().requestFactory(requestFactory);

        OpenAiApi openAiApi = OpenAiApi.builder()
            .baseUrl(deepSeekProperties.getApiUrl())
            .apiKey(deepSeekProperties.getApiKey())
            .restClientBuilder(restClientBuilder)
            .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(deepSeekProperties.getModel())
                .build())
            .build();

        return ChatClient.builder(chatModel)
            .defaultAdvisors(advisor)
            .build();
    }

    // ==================== AiClient 实例 ====================

    /**
     * MiMo AiClient 实例
     * 封装 mimoChatClient，提供统一的 AI 调用接口
     */
    @Bean("mimoAiClient")
    public MiMoAiClient mimoAiClient(@Qualifier("mimoChatClient") ChatClient chatClient) {
        return new MiMoAiClient(chatClient);
    }

    /**
     * DeepSeek AiClient 实例
     * 封装 deepseekChatClient，提供统一的 AI 调用接口
     */
    @Bean("deepseekAiClient")
    public DeepSeekAiClient deepseekAiClient(@Qualifier("deepseekChatClient") ChatClient chatClient) {
        return new DeepSeekAiClient(chatClient);
    }
}
