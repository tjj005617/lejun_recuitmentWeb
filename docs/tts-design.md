# 面试问题 TTS 语音朗读功能设计

## 一、需求概述

在 AI 模拟面试页面中，每轮问题下方显示一个小喇叭图标。用户点击后，后端将问题文本转为语音（MP3），前端播放。

## 二、整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue)                                │
│                                                                 │
│  InterviewRoom.vue                                              │
│    │                                                            │
│    ├─► 用户点击朗读按钮                                          │
│    │                                                            │
│    ├─► playTTS(text)                                            │
│    │     ├─ 同步创建 MediaSource + Audio                         │
│    │     ├─ 同步 play()（占住用户手势）                           │
│    │     └─ 异步 fetch POST /api/interview/tts                   │
│    │                                                            │
│    └─► 收到 MP3 → appendBuffer 喂入 MediaSource → 浏览器播放     │
└─────────────────────────────────────────────────────────────────┘
                              │ POST { text: "问题内容" }
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        后端 (Spring Boot)                        │
│                                                                 │
│  InterviewController                                            │
│    │                                                            │
│    └─► MiMoTtsService.synthesize(text)                          │
│          │                                                      │
│          ├─ 构建请求体（voicedesign 模型 + 音色描述 + 文本）      │
│          │                                                      │
│          └─ HttpClient POST → MiMo TTS API                      │
│               │                                                 │
│               └─ SSE 流式响应 → 收集 base64 音频 chunk           │
│                  → 合并 → 返回完整 MP3 byte[]                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MiMo TTS API                                 │
│  POST {apiUrl}/v1/chat/completions                              │
│  model: mimo-v2.5-tts-voicedesign                               │
│  messages[0] (user): 音色描述指令（英文）                         │
│  messages[1] (assistant): 要朗读的文本                           │
│  输出: SSE 流，delta.audio.data 逐 chunk 返回 base64 MP3         │
└─────────────────────────────────────────────────────────────────┘
```

## 三、技术选型

| 组件 | 选择 | 说明 |
|------|------|------|
| TTS 服务 | MiMo-V2.5-TTS-VoiceDesign | 小米语音合成，支持文本描述自定义音色 |
| 模型 | `mimo-v2.5-tts-voicedesign` | 通过自然语言描述音色，无需预设音色列表 |
| API 端点 | `/v1/chat/completions` | MiMo TTS 不走 `/v1/audio/speech` |
| 传输方式 | 后端一次性返回完整 MP3 | 后端收集 SSE 所有 chunk → 合并 → 返回 `ResponseEntity<byte[]>` |
| 前端播放 | `MediaSource` API | 解决浏览器自动播放策略限制 |

### 3.1 为什么不用 Spring AI

Spring AI 的 `OpenAiAudioSpeechModel` 固定调用 `/v1/audio/speech` 端点，而 MiMo TTS 走的是 `/v1/chat/completions`。实测确认调用返回 404，因此改用手动 HttpClient 调用。

### 3.2 为什么不用预设音色模型

最初使用 `mimo-v2.5-tts`（预设音色列表：冰糖/茉莉/苏打等），但效果不理想。切换到 `mimo-v2.5-tts-voicedesign` 后，通过英文自然语言描述音色，可以精确控制语调、语速、风格，效果更好。

## 四、完整流程（详细）

### 4.1 端到端时序图

```
 用户              InterviewRoom          InterviewController       MiMoTtsService           MiMo TTS
  │                    │                        │                        │                     │
  │  点击朗读按钮      │                        │                        │                     │
  │───────────────────►│                        │                        │                     │
  │                    │                        │                        │                     │
  │                    │  playTTS(text)         │                        │                     │
  │                    │───►│                   │                        │                     │
  │                    │    │                   │                        │                     │
  │                    │  同步创建 MediaSource  │                        │                     │
  │                    │  + Audio(url)          │                        │                     │
  │                    │    │                   │                        │                     │
  │                    │  audio.play()          │                        │                     │
  │                    │  （占住用户手势）       │                        │                     │
  │                    │    │                   │                        │                     │
  │                    │  sourceopen 事件触发   │                        │                     │
  │                    │    │                   │                        │                     │
  │                    │  fetch POST /tts       │                        │                     │
  │                    │───────────────────────►│                        │                     │
  │                    │                        │ ttsService.synthesize  │                     │
  │                    │                        │───────────────────────►│                     │
  │                    │                        │                        │                     │
  │                    │                        │                        │ 构建请求体          │
  │                    │                        │                        │ voicedesign 模型    │
  │                    │                        │                        │                     │
  │                    │                        │                        │ POST /v1/chat/      │
  │                    │                        │                        │ completions         │
  │                    │                        │                        │────────────────────►│
  │                    │                        │                        │                     │
  │                    │                        │                        │ SSE: delta.audio    │
  │                    │                        │                        │ .data (base64)      │
  │                    │                        │                        │ chunk1, chunk2...   │
  │                    │                        │                        │◄────────────────────│
  │                    │                        │                        │                     │
  │                    │                        │                        │ Base64 解码         │
  │                    │                        │                        │ 合并所有 chunk      │
  │                    │                        │                        │ → byte[]            │
  │                    │                        │  byte[] (完整 MP3)     │                     │
  │                    │                        │◄───────────────────────│                     │
  │                    │                        │                        │                     │
  │                    │  ResponseEntity        │                        │                     │
  │                    │  Content-Type:         │                        │                     │
  │                    │  audio/mpeg            │                        │                     │
  │                    │◄───────────────────────│                        │                     │
  │                    │                        │                        │                     │
  │                    │  appendBuffer(data)    │                        │                     │
  │                    │  喂入 MediaSource      │                        │                     │
  │                    │    │                   │                        │                     │
  │                    │  浏览器解码播放        │                        │                     │
  │                    │    │                   │                        │                     │
  │  听到语音朗读      │◄──────────────────────│                        │                     │
```

### 4.2 MiMo TTS API 详解

#### 请求格式

```
POST {ai.api-url}/v1/chat/completions
Authorization: Bearer {ai.api-key}
Content-Type: application/json
```

#### 请求体

```json
{
    "model": "mimo-v2.5-tts-voicedesign",
    "messages": [
        {
            "role": "user",
            "content": "Give me a calm, professional, and steady male interviewer tone, moderate speed, clear pronunciation"
        },
        {
            "role": "assistant",
            "content": "请介绍一下你最熟悉的项目，你在其中扮演了什么角色？"
        }
    ],
    "audio": {
        "format": "mp3"
    },
    "stream": true
}
```

| 字段 | 说明 |
|------|------|
| `model` | `mimo-v2.5-tts-voicedesign`（文本描述音色） |
| `messages[0]` (user) | 音色描述指令，用英文效果更好 |
| `messages[1]` (assistant) | 要朗读的文本 |
| `audio.format` | `mp3` |
| `stream` | `true`（流式返回，逐 chunk 传输 base64 音频） |

#### SSE 流式响应

每个 chunk 的 `data` 行：

```json
{"choices":[{"delta":{"audio":{"data":"base64编码的mp3音频数据"}}}]}
{"choices":[{"delta":{"audio":{"data":"另一段base64音频"}}}]}
data: [DONE]
```

后端逐行读取，提取 `choices[0].delta.audio.data`，Base64 解码后收集到 `List<byte[]>`，最后合并为完整 MP3。

### 4.3 后端处理流程（MiMoTtsService）

```java
@Override
public byte[] synthesize(String text) throws Exception {
    // 1. 构建请求体
    Map<String, Object> requestBody = Map.of(
        "model", "mimo-v2.5-tts-voicedesign",
        "messages", List.of(
            // user 消息：音色描述指令（英文效果更好）
            Map.of("role", "user", "content",
                "Give me a calm, professional, and steady male interviewer tone, moderate speed, clear pronunciation"),
            // assistant 消息：要朗读的文本
            Map.of("role", "assistant", "content", text)
        ),
        "audio", Map.of("format", "mp3"),
        "stream", true
    );

    // 2. HttpClient POST 到 MiMo API
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

    // 3. 流式读取 SSE，收集 base64 音频 chunk
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
                // 提取 base64 音频数据
                String audioData = json.at("/choices/0/delta/audio/data").asText(null);
                if (audioData != null) {
                    audioChunks.add(Base64.getDecoder().decode(audioData));
                }
            } catch (Exception e) {
                // 忽略非音频的 delta
            }
        }
    }

    // 4. 合并所有 chunk 为完整 byte[]
    int totalLength = audioChunks.stream().mapToInt(c -> c.length).sum();
    byte[] result = new byte[totalLength];
    int offset = 0;
    for (byte[] chunk : audioChunks) {
        System.arraycopy(chunk, 0, result, offset, chunk.length);
        offset += chunk.length;
    }

    return result;  // 完整 MP3 字节数组
}
```

#### 后端处理流程图

```
synthesize(text) 被调用
    │
    ▼
构建请求体
    ├─ model: "mimo-v2.5-tts-voicedesign"
    ├─ messages[0].role: "user"
    ├─ messages[0].content: "Give me a calm, professional..."（音色描述）
    ├─ messages[1].role: "assistant"
    ├─ messages[1].content: text（要朗读的文本）
    ├─ audio.format: "mp3"
    └─ stream: true
    │
    ▼
HttpClient POST → MiMo API /v1/chat/completions
    │
    ▼
读取 SSE 响应流
    │
    ├─ "data: {"choices":[{"delta":{"audio":{"data":"base64chunk1"}}}]}"
    │   → Base64 解码 → byte[] → 加入 audioChunks
    │
    ├─ "data: {"choices":[{"delta":{"audio":{"data":"base64chunk2"}}}]}"
    │   → Base64 解码 → byte[] → 加入 audioChunks
    │
    └─ "data: [DONE]"
        → 结束循环
    │
    ▼
合并所有 chunk
    byte[] result = new byte[totalLength]
    System.arraycopy 每个 chunk
    │
    ▼
返回完整 MP3 byte[]
```

### 4.4 Controller 接口

```java
// InterviewController.java
@PostMapping(value = "/tts", produces = "audio/mpeg")
public ResponseEntity<byte[]> tts(@RequestBody TtsRequest request) {
    try {
        byte[] audio = ttsService.synthesize(request.getText());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tts.mp3\"")
                .body(audio);
    } catch (Exception e) {
        log.error("TTS 合成失败", e);
        return ResponseEntity.internalServerError().build();
    }
}
```

返回 `ResponseEntity<byte[]>`，`Content-Type: audio/mpeg`，前端可以直接用 `MediaSource` 播放。

### 4.5 TtsRequest DTO

```java
@Data
public class TtsRequest {
    private String text;
}
```

### 4.6 配置

复用 `ai.api-key` 和 `ai.api-url`，无需额外配置。`MiMoTtsService` 通过 `@Value` 注入。

### 4.7 JWT 白名单

`JwtFilter.WHITE_LIST` 中添加 `/api/interview/tts`。

## 五、前端播放实现（详细）

### 5.1 核心问题：浏览器自动播放策略

浏览器要求 `audio.play()` 必须在同步的用户手势事件中调用。如果先 `await fetch()` 再 `play()`，会抛出异常：

```
DOMException: play() failed because the user didn't interact with the document first
```

**解决方案：** 使用 `MediaSource` API。先同步创建 `MediaSource` 并调用 `play()`，再异步获取音频数据通过 `appendBuffer` 喂入。

### 5.2 播放流程时序

```
用户点击朗读按钮（同步用户手势上下文）
    │
    ▼
创建 MediaSource + Audio(url)
    │
    ▼
audio.play()  ← 同步调用，占住用户手势
    │
    ▼
sourceopen 事件触发（异步）
    │
    ▼
fetch POST /api/interview/tts（异步）
    │
    ▼
收到 MP3 ArrayBuffer
    │
    ▼
sourceBuffer.appendBuffer(data)
    │
    ▼
浏览器自动解码播放
    │
    ▼
播放结束 → onended 事件 → 清理资源
```

### 5.3 完整播放代码

```javascript
let ttsAudio = null
const ttsPlaying = ref(false)
const ttsLoading = ref(false)

const playTTS = (text) => {
  if (ttsPlaying.value) {
    stopTTS()
    return
  }

  // 1. 同步创建 MediaSource + Audio
  const mediaSource = new MediaSource()
  const url = URL.createObjectURL(mediaSource)
  ttsAudio = new Audio(url)

  // 播放结束回调
  ttsAudio.onended = () => {
    ttsPlaying.value = false
    cleanup()
  }

  let sourceBuffer = null

  const cleanup = () => {
    if (ttsAudio) { ttsAudio = null }
    URL.revokeObjectURL(url)
  }

  // 2. MediaSource 就绪后，异步获取音频
  mediaSource.addEventListener('sourceopen', () => {
    sourceBuffer = mediaSource.addSourceBuffer('audio/mpeg')
    sourceBuffer.mode = 'sequence'

    ttsLoading.value = true

    fetch('/api/interview/tts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text })
    })
      .then(res => res.arrayBuffer())
      .then(data => {
        // 安全写入：等上一次 appendBuffer 完成
        const appendData = () => {
          if (sourceBuffer.updating) {
            sourceBuffer.addEventListener('updateend', appendData, { once: true })
            return
          }
          sourceBuffer.appendBuffer(data)
        }
        appendData()
      })
      .catch(err => {
        ElMessage.error('语音合成失败: ' + err.message)
        stopTTS()
      })
      .finally(() => { ttsLoading.value = false })
  })

  // 3. 同步 play()，占住用户手势（关键！）
  ttsAudio.play()
  ttsPlaying.value = true
}

const stopTTS = () => {
  if (ttsAudio) {
    ttsAudio.pause()
    ttsAudio = null
  }
  ttsPlaying.value = false
}
```

### 5.4 三态按钮

```html
<div v-if="isCurrentQuestion(msg)" class="msg__tts">
  <button class="tts-btn"
          :class="{ playing: ttsPlaying, loading: ttsLoading }"
          @click="playTTS(msg.content)"
          :disabled="ttsLoading">
    <span class="tts-btn__text">
      {{ ttsLoading ? '生成中...' : ttsPlaying ? '停止' : '朗读' }}
    </span>
  </button>
</div>
```

| 状态 | 外观 | 行为 |
|------|------|------|
| 空闲 | 喇叭图标 + "朗读" | 点击触发 TTS |
| 加载中 | 转圈 + "生成中..." | 等待后端返回音频 |
| 播放中 | 高亮 + "停止" | 点击停止播放 |

### 5.5 判断当前问题

只在当前轮次的 AI 问题上显示朗读按钮：

```javascript
const isCurrentQuestion = (msg) => {
  return msg.type === 'ai' && msg.content === currentQuestion.value
}
```

## 六、踩坑记录

### 6.1 Spring AI 不兼容 MiMo TTS

- **现象：** `OpenAiAudioSpeechModel` 调用返回 404
- **原因：** Spring AI 固定请求 `/v1/audio/speech`，MiMo TTS 使用 `/v1/chat/completions`
- **解决：** 用 `java.net.http.HttpClient` 直接调用

### 6.2 API 认证方式

- **现象：** 401 Unauthorized
- **原因：** 早期使用了错误的 API 地址（`api.xiaomimimo.com`）和错误的认证头（`api-key`）
- **解决：** 使用 `Authorization: Bearer {token}` 方式

### 6.3 浏览器自动播放策略

- **现象：** `DOMException: play() failed because the user didn't interact with the document first`
- **原因：** 先 `await fetch()` 再 `play()`，`play()` 不在同步用户手势上下文中
- **解决：** 使用 `MediaSource`，同步 `play()` 后异步喂数据

### 6.4 appendBuffer 并发写入

- **现象：** `Failed to execute 'appendBuffer' on 'SourceBuffer': This SourceBuffer has been removed`
- **原因：** 上一次 `appendBuffer` 还未完成就调用了下一次
- **解决：** 写入前检查 `sourceBuffer.updating`，如果正在更新则监听 `updateend` 事件

### 6.5 先写测试再写代码

接入 MiMo TTS API 时，应先写独立测试类验证 API 能跑通，再写正式代码。避免了 Spring AI 方案不可用后的反复返工。

## 七、文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `MiMoTtsService.java` | 新建 | MiMo TTS 实现，HttpClient 调用 `/v1/chat/completions` |
| `TtsService.java` | 新建 | TTS 服务接口 |
| `TtsRequest.java` | 新建 | TTS 请求 DTO |
| `InterviewController.java` | 修改 | 新增 `/tts` 接口，返回 `ResponseEntity<byte[]>` |
| `JwtFilter.java` | 修改 | 白名单添加 `/api/interview/tts` |
| `InterviewRoom.vue` | 修改 | 添加朗读按钮、MediaSource 播放逻辑 |
