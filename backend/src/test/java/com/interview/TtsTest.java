package com.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileOutputStream;
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
 * 独立测试：验证 MiMo TTS API（/v1/chat/completions 格式）
 */
public class TtsTest {

    public static void main(String[] args) throws Exception {
        String apiUrl = "https://token-plan-cn.xiaomimimo.com";
        String apiKey = "tp-cjn6l45egnx146gm77kvdgm3gyh7h6l1kthigy53oht284io";
        String text = "请介绍一下你最熟悉的项目，你在其中扮演了什么角色？";

        System.out.println("=== MiMo TTS 测试（chat/completions 格式）===");
        System.out.println("API: " + apiUrl + "/v1/chat/completions");

        // 构建请求体
        Map<String, Object> requestBody = Map.of(
                "model", "mimo-v2.5-tts",
                "messages", List.of(
                        Map.of("role", "user", "content",
                                "用专业、沉稳的面试官语气朗读以下问题，语速适中，吐字清晰"),
                        Map.of("role", "assistant", "content", text)
                ),
                "audio", Map.of(
                        "format", "mp3",
                        "voice", "苏打"
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
                        new ObjectMapper().writeValueAsString(requestBody)))
                .build();

        System.out.println("正在调用...");
        HttpResponse<InputStream> response = client.send(httpRequest,
                HttpResponse.BodyHandlers.ofInputStream());

        System.out.println("HTTP 状态码: " + response.statusCode());

        if (response.statusCode() != 200) {
            String error = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("错误响应: " + error);
            return;
        }

        // 流式读取 SSE
        List<byte[]> audioChunks = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    System.out.println("收到 [DONE]");
                    break;
                }
                try {
                    JsonNode json = new ObjectMapper().readTree(data);
                    String audioData = json.at("/choices/0/delta/audio/data").asText(null);
                    if (audioData != null) {
                        audioChunks.add(Base64.getDecoder().decode(audioData));
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }
        }

        // 合并并保存
        if (audioChunks.isEmpty()) {
            System.out.println("未收到音频数据！");
            return;
        }

        int totalLen = audioChunks.stream().mapToInt(c -> c.length).sum();
        byte[] result = new byte[totalLen];
        int offset = 0;
        for (byte[] chunk : audioChunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }

        String outputPath = "E:/java/ai-interview/backend/tts-test-output.mp3";
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(result);
        }

        System.out.println("音频大小: " + result.length + " bytes");
        System.out.println("已保存到: " + outputPath);
        System.out.println("=== 测试完成 ===");
    }
}
