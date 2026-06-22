package com.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileInputStream;
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
 * 独立测试：验证 MiMo ASR API（语音识别）
 * 用 TTS 生成的 mp3 作为输入，验证 ASR 能正确识别
 */
public class AsrTest {

    public static void main(String[] args) throws Exception {
        String apiUrl = "https://token-plan-cn.xiaomimimo.com";
        String apiKey = "tp-cjn6l45egnx146gm77kvdgm3gyh7h6l1kthigy53oht284io";

        // 用 TTS 生成的 mp3 作为输入
        String audioPath = "E:/java/ai-interview/backend/tts-test-output.mp3";

        System.out.println("=== MiMo ASR 测试 ===");
        System.out.println("API: " + apiUrl + "/v1/chat/completions");
        System.out.println("音频文件: " + audioPath);

        // 读取音频文件并转 base64
        byte[] audioBytes;
        try (FileInputStream fis = new FileInputStream(audioPath)) {
            audioBytes = fis.readAllBytes();
        }
        System.out.println("音频大小: " + audioBytes.length + " bytes");

        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
        String dataUri = "data:audio/mpeg;base64," + base64Audio;
        System.out.println("Base64 长度: " + base64Audio.length());

        // 构建请求体
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

        System.out.println("正在调用 ASR...");
        long start = System.currentTimeMillis();

        HttpResponse<InputStream> response = client.send(httpRequest,
                HttpResponse.BodyHandlers.ofInputStream());

        System.out.println("HTTP 状态码: " + response.statusCode());

        if (response.statusCode() != 200) {
            String error = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("错误响应: " + error);
            return;
        }

        // 流式读取 SSE，收集识别文字
        StringBuilder result = new StringBuilder();
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
                    String content = json.at("/choices/0/delta/content").asText(null);
                    if (content != null) {
                        result.append(content);
                        System.out.print(content);  // 实时打印识别文字
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println();
        System.out.println("---");
        System.out.println("识别结果: " + result.toString().trim());
        System.out.println("耗时: " + elapsed + "ms");
        System.out.println("=== 测试完成 ===");
    }
}
