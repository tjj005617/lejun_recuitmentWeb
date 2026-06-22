package com.interview.service.ai;

import com.interview.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * AI 客户端抽象基类
 * 采用模板方法模式，提供公共功能：异常兜底、JSON 清洗、消息序列化
 *
 * <p>子类只需实现 {@link #doCall} 方法调用各自的 Spring AI ChatClient，
 * 异常处理和默认兜底由基类统一完成。</p>
 *
 * <p>继承关系：</p>
 * <pre>
 *   AiClient (接口)
 *     └── AbstractAiClient (抽象类：模板方法 + JSON 工具 + 异常兜底)
 *           ├── MiMoAiClient (OpenAI 兼容 - 问题/报告生成)
 *           └── DeepSeekAiClient (OpenAI 兼容 - 答案评分)
 * </pre>
 */
@Slf4j
public abstract class AbstractAiClient implements AiClient {

    /**
     * 模板方法 - 子类实现具体的 AI 调用逻辑（无记忆版本）
     *
     * @param systemPrompt 系统级指令
     * @param userPrompt   用户消息内容
     * @param maxTokens    最大 token 数
     * @return 模型的文本响应
     */
    protected abstract String doCall(String systemPrompt, String userPrompt, int maxTokens);

    /**
     * 带会话记忆的模板方法 - 子类可覆写以支持 advisor 传入 conversationId
     * 默认实现调用无记忆版本，保证向下兼容
     *
     * @param systemPrompt   系统级指令
     * @param userPrompt     用户消息内容
     * @param maxTokens      最大 token 数
     * @param conversationId 会话标识，传 null 则不启用记忆
     * @return 模型的文本响应
     */
    protected String doCall(String systemPrompt, String userPrompt, int maxTokens, String conversationId) {
        return doCall(systemPrompt, userPrompt, maxTokens);
    }

    /**
     * 无记忆调用 - 包装 doCall 并捕获异常，失败时返回兜底内容
     */
    @Override
    public String call(String systemPrompt, String userPrompt, int maxTokens) {
        try {
            return doCall(systemPrompt, userPrompt, maxTokens);
        } catch (Exception e) {
            logException(e);
            return getDefaultFallback();
        }
    }

    /**
     * 带记忆调用 - 包装 doCall 并捕获异常，失败时返回兜底内容
     */
    @Override
    public String call(String systemPrompt, String userPrompt, int maxTokens, String conversationId) {
        try {
            return doCall(systemPrompt, userPrompt, maxTokens, conversationId);
        } catch (Exception e) {
            logException(e);
            return getDefaultFallback();
        }
    }

    /**
     * 详细记录 AI 调用异常，包含响应状态码和响应体（如有）
     */
    private void logException(Exception e) {
        log.error("AI 调用失败 [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        // 如果是 HTTP 响应异常，记录状态码和响应体，便于排查接口兼容性问题
        if (e instanceof org.springframework.web.client.HttpClientErrorException httpEx) {
            log.error("HTTP 错误: status={}, body={}", httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
        } else if (e instanceof org.springframework.web.client.HttpServerErrorException httpEx) {
            log.error("HTTP 服务端错误: status={}, body={}", httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
        }
        log.debug("完整异常堆栈:", e);
    }

    /** 清洗 AI 响应中的 markdown 代码块标记 */
    protected String cleanJsonResponse(String raw) {
        return JsonUtils.cleanJsonResponse(raw);
    }

    /** 从 AI 响应中提取 JSON 对象（{...}） */
    protected String extractJsonObject(String raw) {
        return JsonUtils.extractJsonObject(raw);
    }

    /** 从 AI 响应中提取 JSON 数组（[...]） */
    protected String extractJsonArray(String raw) {
        return JsonUtils.extractJsonArray(raw);
    }

    /** 解析 JSON 字符串为指定类型的对象 */
    protected <T> T parseJson(String json, Class<T> type) throws Exception {
        return JsonUtils.parseJson(json, type);
    }

    /** 解析 JSON 数组为 List&lt;Map&gt; 结构 */
    protected List<Map<String, Object>> parseJsonList(String json) throws Exception {
        return JsonUtils.parseJsonList(json);
    }

    /**
     * AI 调用失败时的默认兜底响应
     * 子类可覆写（如 DeepSeekAiClient 返回空字符串）
     */
    protected String getDefaultFallback() {
        return "AI服务暂时不可用，请稍后重试。";
    }
}
