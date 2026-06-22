package com.interview.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.service.SttService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * MiMo ASR 语音识别实现
 * 调用 MiMo 的 /v1/chat/completions 端点，使用 mimo-v2.5-asr 模型
 */
@Slf4j
@Service
public class MiMoSttService implements SttService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String recognize(byte[] audioData) throws Exception {
        // 1. 将 WAV 音频转为 Base64 Data URI
        String base64Audio = Base64.getEncoder().encodeToString(audioData);
        String dataUri = "data:audio/wav;base64," + base64Audio;

        // 2. 构建请求体
        Map<String, Object> requestBody = Map.of(
                "model", "mimo-v2.5-asr",
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "input_audio",
                                        "input_audio", Map.of("data", dataUri))
                        ))
                ),
                "asr_options", Map.of("language", "auto"),
                "stream", true
        );

        // 3. 发送 HTTP 请求
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<InputStream> response = client.send(httpRequest,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("ASR API 返回错误: " + response.statusCode() + " - " + errorBody);
        }

        // 4. 流式读取 SSE，收集识别文字
        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) break;
                try {
                    JsonNode json = objectMapper.readTree(data);
                    String content = json.at("/choices/0/delta/content").asText(null);
                    if (content != null) {
                        result.append(content);
                    }
                } catch (Exception e) {
                    // 忽略非文本的 delta
                }
            }
        }

        String text = result.toString().trim();
        if (text.isEmpty()) {
            throw new RuntimeException("ASR 未返回识别结果");
        }

        log.info("ASR 识别完成，文本长度: {}", text.length());
        return text;
    }
}
