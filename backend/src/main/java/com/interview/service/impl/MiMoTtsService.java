package com.interview.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.service.TtsService;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * MiMo TTS 实现
 * MiMo 的 TTS 走 /v1/chat/completions 端点（不是 /v1/audio/speech）
 * Spring AI 的 OpenAiAudioSpeechModel 固定调 /v1/audio/speech，不兼容
 */
@Slf4j
@Service
public class MiMoTtsService implements TtsService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] synthesize(String text) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", "mimo-v2.5-tts-voicedesign",
                "messages", List.of(
                        Map.of("role", "user", "content",
                                "Give me a calm, professional, and steady male interviewer tone, moderate speed, clear pronunciation"),
                        Map.of("role", "assistant", "content", text)
                ),
                "audio", Map.of(
                        "format", "mp3"
                ),
                "stream", true
        );

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
            throw new RuntimeException("TTS API 返回错误: " + response.statusCode() + " - " + errorBody);
        }

        // 流式读取 SSE，收集所有 base64 音频 chunk
        List<byte[]> audioChunks = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) break;
                try {
                    JsonNode json = objectMapper.readTree(data);
                    String audioData = json.at("/choices/0/delta/audio/data").asText(null);
                    if (audioData != null) {
                        audioChunks.add(Base64.getDecoder().decode(audioData));
                    }
                } catch (Exception e) {
                    // 忽略非音频的 delta
                }
            }
        }

        if (audioChunks.isEmpty()) {
            throw new RuntimeException("TTS 未返回音频数据");
        }

        int totalLength = audioChunks.stream().mapToInt(c -> c.length).sum();
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] chunk : audioChunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }

        log.info("TTS 合成完成，音频大小: {} bytes", result.length);
        return result;
    }
}
