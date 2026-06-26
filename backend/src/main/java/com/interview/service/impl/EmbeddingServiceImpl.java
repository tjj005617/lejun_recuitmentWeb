package com.interview.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.config.DashScopeProperties;
import com.interview.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding 服务实现
 * 调用阿里云 DashScope OpenAI 兼容接口进行文本向量化
 * API: POST https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    /** DashScope OpenAI 兼容接口地址 */
    private static final String EMBEDDING_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

    /** 每次批量最多处理的文本数 */
    private static final int BATCH_SIZE = 6;

    private final DashScopeProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EmbeddingServiceImpl(DashScopeProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Float> embed(String text) {
        List<List<Float>> results = callEmbeddingApiBatch(List.of(text));
        if (results.isEmpty()) {
            throw new RuntimeException("Embedding 返回结果为空");
        }
        return results.get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<List<Float>> allResults = new ArrayList<>();

        // 分批处理，每批最多 BATCH_SIZE 条
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);
            List<List<Float>> batchResults = callEmbeddingApiBatch(batch);
            allResults.addAll(batchResults);
        }

        return allResults;
    }

    /**
     * 调用 DashScope Embedding API（批量）
     * 使用 OpenAI 兼容格式：POST /compatible-mode/v1/embeddings
     * 响应中 data 数组包含每个输入对应的 embedding
     */
    private List<List<Float>> callEmbeddingApiBatch(List<String> texts) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", properties.getModel());
            requestBody.put("input", texts);
            requestBody.put("dimensions", properties.getDimension());

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("调用 Embedding API: model={}, count={}", properties.getModel(), texts.size());

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                    EMBEDDING_URL, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Embedding API 调用失败: " + response.getStatusCode());
            }

            // 解析响应
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            if (data == null || data.isEmpty()) {
                throw new RuntimeException("Embedding API 返回数据为空");
            }

            // 提取每个输入对应的 embedding
            List<List<Float>> results = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embeddingNode = item.get("embedding");
                List<Float> embedding = new ArrayList<>();
                for (JsonNode val : embeddingNode) {
                    embedding.add(val.floatValue());
                }
                results.add(embedding);
            }

            log.debug("Embedding 完成: count={}, 维度={}", results.size(),
                    results.isEmpty() ? 0 : results.get(0).size());
            return results;

        } catch (Exception e) {
            log.error("Embedding API 调用异常: {}", e.getMessage(), e);
            throw new RuntimeException("Embedding 调用失败: " + e.getMessage(), e);
        }
    }
}
