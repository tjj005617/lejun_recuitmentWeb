# 语音输入（ASR）功能设计

## 一、需求概述

在聊天消息中心和 AI 模拟面试两个场景中，支持语音输入：
- 点一下开始录音，再点一下结束
- 语音转文字后，文字显示在输入框供用户确认/修改
- 用户确认后发送

## 二、技术选型

| 组件 | 选择 | 说明 |
|------|------|------|
| ASR 服务 | MiMo-V2.5-ASR | 复用现有 MiMo API，无需引入新服务 |
| 模型 | `mimo-v2.5-asr` | 支持自动语言检测 |
| API 端点 | `/v1/chat/completions` | 与 TTS 相同端点格式 |
| 前端录音 | `MediaRecorder` API | 浏览器原生录音，输出 webm/opus |
| 格式转换 | `AudioContext` + WAV 编码 | webm → PCM → WAV（MiMo ASR 仅支持 wav/mp3） |
| 传输方式 | HTTP POST（multipart） | 前端上传 wav 文件，后端返回识别文字 |

## 三、MiMo ASR API

### 3.1 请求格式

```
POST {ai.api-url}/v1/chat/completions
Authorization: Bearer {ai.api-key}
Content-Type: application/json
```

### 3.2 请求体

```json
{
    "model": "mimo-v2.5-asr",
    "messages": [
        {
            "role": "user",
            "content": [
                {
                    "type": "input_audio",
                    "input_audio": {
                        "data": "data:audio/wav;base64,{BASE64_AUDIO}"
                    }
                }
            ]
        }
    ],
    "asr_options": {
        "language": "auto"
    },
    "stream": true
}
```

| 字段 | 说明 |
|------|------|
| `model` | `mimo-v2.5-asr` |
| `messages[0].content` | 数组格式，包含 `input_audio` 类型 |
| `input_audio.data` | Data URI 格式：`data:{MIME};base64,{数据}` |
| `asr_options.language` | `auto` 自动检测语言 |
| `stream` | `true`（流式返回识别结果） |

### 3.3 响应格式

SSE 流，与 TTS 格式一致：

```json
{"choices":[{"delta":{"content":"识别出的文字"}}]}
```

结束标记：`data: [DONE]`

## 四、交互流程

```
用户点击麦克风按钮
  → 浏览器弹出权限请求（首次）
  → MediaRecorder 开始录音
  → 按钮变为"录音中"状态（红色脉冲动效）
  → 用户再次点击 → 停止录音
  → 前端将音频 Blob 转为 FormData
  → POST /api/stt 上传音频
  → 后端转 base64 → 调用 MiMo ASR → 返回识别文字
  → 前端将文字填入输入框
  → 用户确认/修改后手动发送
```

## 五、后端实现

### 5.1 STT 服务接口

**文件：** `com.interview.service.SttService`

```java
public interface SttService {
    /**
     * 语音识别，返回识别出的文字
     * @param audioData WAV 音频文件字节数组
     * @return 识别出的文字
     */
    String recognize(byte[] audioData) throws Exception;
}
```

### 5.2 MiMo ASR 实现

**文件：** `com.interview.service.impl.MiMoSttService.java`

核心逻辑：
1. 将 WAV 音频字节数组转为 Base64，构建 Data URI
2. 构建请求体（`mimo-v2.5-asr` 模型 + input_audio 格式）
3. HttpClient POST 到 `/v1/chat/completions`
4. 流式读取 SSE，收集 `delta.content` 文本
5. 拼接返回完整识别文字

```java
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
        // 1. 构建 Data URI（前端已转换为 WAV 格式）
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

        // 3. 发送请求
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
                    // 忽略解析错误
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
```

### 5.3 Controller 接口

**文件：** `com.interview.controller.SttController`

```java
@Slf4j
@RestController
@RequestMapping("/api/stt")
@RequiredArgsConstructor
public class SttController {

    private final SttService sttService;

    /**
     * 语音识别
     * POST /api/stt
     * Content-Type: multipart/form-data
     * Body: file (音频文件)
     */
    @PostMapping
    public Result<String> recognize(@RequestParam("file") MultipartFile file) {
        try {
            String text = sttService.recognize(file.getBytes());
            return Result.ok(text);
        } catch (Exception e) {
            log.error("语音识别失败", e);
            return Result.fail("语音识别失败: " + e.getMessage());
        }
    }
}
```

### 5.4 JWT 白名单

`JwtFilter.WHITE_LIST` 中添加 `/api/stt`。

## 六、前端实现

### 6.1 录音工具函数

**文件：** `frontend/src/utils/audioRecorder.js`（新建）

封装 `MediaRecorder` + WAV 转换，提供简洁的录音接口：

```javascript
/**
 * 录音工具
 * MediaRecorder 录 webm → AudioContext 解码 → 编码为 WAV
 * 使用方法：
 *   const recorder = await startRecording()
 *   // ... 用户说话 ...
 *   const wavBlob = await recorder.stop()  // 返回 WAV Blob
 */
export function startRecording() {
  return new Promise(async (resolve, reject) => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const mediaRecorder = new MediaRecorder(stream, {
        mimeType: 'audio/webm;codecs=opus'
      })
      const chunks = []

      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunks.push(e.data)
      }

      mediaRecorder.start()

      resolve({
        stop: () => {
          return new Promise((res) => {
            mediaRecorder.onstop = async () => {
              stream.getTracks().forEach(t => t.stop())  // 关闭麦克风
              // webm → PCM → WAV
              const webmBlob = new Blob(chunks, { type: 'audio/webm' })
              const arrayBuffer = await webmBlob.arrayBuffer()
              const audioCtx = new (window.AudioContext || window.webkitAudioContext)()
              const audioBuffer = await audioCtx.decodeAudioData(arrayBuffer)
              const wavBlob = encodeWav(audioBuffer)
              audioCtx.close()
              res(wavBlob)
            }
            mediaRecorder.stop()
          })
        }
      })
    } catch (err) {
      reject(err)
    }
  })
}

/**
 * 将 AudioBuffer 编码为 WAV 文件（16-bit PCM）
 */
function encodeWav(audioBuffer) {
  const numChannels = audioBuffer.numberOfChannels
  const sampleRate = audioBuffer.sampleRate
  const format = 1  // PCM
  const bitDepth = 16

  // 合并多声道为交错采样
  const samples = interleave(audioBuffer)
  const dataLength = samples.length * (bitDepth / 8)
  const buffer = new ArrayBuffer(44 + dataLength)
  const view = new DataView(buffer)

  // WAV 文件头（44 字节）
  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + dataLength, true)
  writeString(view, 8, 'WAVE')
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true)          // fmt chunk size
  view.setUint16(20, format, true)      // audio format (PCM)
  view.setUint16(22, numChannels, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * numChannels * (bitDepth / 8), true)  // byte rate
  view.setUint16(32, numChannels * (bitDepth / 8), true)              // block align
  view.setUint16(34, bitDepth, true)
  writeString(view, 36, 'data')
  view.setUint32(40, dataLength, true)

  // 写入 PCM 采样数据
  floatTo16BitPCM(view, 44, samples)

  return new Blob([buffer], { type: 'audio/wav' })
}

/** 交错合并多声道 */
function interleave(audioBuffer) {
  if (audioBuffer.numberOfChannels === 1) return audioBuffer.getChannelData(0)
  const left = audioBuffer.getChannelData(0)
  const right = audioBuffer.getChannelData(1)
  const interleaved = new Float32Array(left.length + right.length)
  for (let i = 0, j = 0; i < left.length; i++) {
    interleaved[j++] = left[i]
    interleaved[j++] = right[i]
  }
  return interleaved
}

function floatTo16BitPCM(view, offset, samples) {
  for (let i = 0; i < samples.length; i++, offset += 2) {
    const s = Math.max(-1, Math.min(1, samples[i]))
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true)
  }
}

function writeString(view, offset, str) {
  for (let i = 0; i < str.length; i++) {
    view.setUint8(offset + i, str.charCodeAt(i))
  }
}
```

### 6.2 STT API 封装

**文件：** `frontend/src/api/stt.js`（新建）

```javascript
import request from '@/utils/request'

/**
 * 语音识别
 * @param {Blob} audioBlob 录音音频 Blob
 * @returns {Promise<string>} 识别出的文字
 */
export function recognizeSpeech(audioBlob) {
  const formData = new FormData()
  formData.append('file', audioBlob, 'recording.wav')  // 已转换为 WAV 格式
  return request.post('/stt', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
```

### 6.3 面试场景（InterviewRoom.vue）

在输入区域的发送按钮旁添加麦克风按钮：

```html
<div class="chat-input__wrap">
  <el-input v-model="answer" type="textarea" ... />
  <!-- 麦克风按钮 -->
  <button class="chat-input__voice" :class="{ recording: isRecording }"
          @click="toggleVoiceInput" :disabled="isCompleted || isAiTyping || sttLoading">
    <!-- 录音中 / 识别中 / 空闲 三种状态 -->
  </button>
  <button class="chat-input__send" @click="submitAnswer" ...>
    <!-- 发送按钮 -->
  </button>
</div>
```

**逻辑：**

```javascript
const isRecording = ref(false)
const sttLoading = ref(false)
let recorderInstance = null

const toggleVoiceInput = async () => {
  if (isRecording.value) {
    // 停止录音 → 识别 → 填入输入框
    isRecording.value = false
    sttLoading.value = true
    try {
      const blob = await recorderInstance.stop()
      const res = await recognizeSpeech(blob)
      answer.value = res.data  // 识别文字填入输入框，用户确认后手动发送
    } catch (err) {
      ElMessage.error('语音识别失败: ' + err.message)
    } finally {
      sttLoading.value = false
    }
  } else {
    // 开始录音
    recorderInstance = await startRecording()
    isRecording.value = true
  }
}
```

### 6.4 聊天场景（MessageCenter.vue）

在输入行的表情按钮旁添加麦克风按钮：

```html
<div class="input-row">
  <el-input v-model="newMessage" placeholder="输入消息..." @keyup.enter="sendTextMessage" />
  <!-- 麦克风按钮 -->
  <button class="voice-btn" :class="{ recording: isRecording }"
          @click="toggleVoiceInput" :disabled="sttLoading">
  </button>
  <div class="emoji-picker-wrapper">...</div>
  <el-button @click="sendTextMessage" :disabled="!newMessage.trim()">发送</el-button>
</div>
```

**逻辑同面试场景**，识别结果填入 `newMessage`，用户确认后发送。

### 6.5 按钮状态

| 状态 | 图标 | 文字/样式 | 说明 |
|------|------|-----------|------|
| 空闲 | 麦克风 | 默认色 | 点击开始录音 |
| 录音中 | 停止方块 | 红色脉冲 | 再次点击停止录音 |
| 识别中 | 加载旋转 | 黄色 | 等待 ASR 返回 |

## 七、踩坑记录

### 7.1 MiMo ASR 仅支持 wav/mp3

MediaRecorder 只能录制 webm/opus 格式，但 MiMo ASR 只接受 `audio/wav` 和 `audio/mpeg`(mp3)。必须在前端做格式转换：

```
MediaRecorder 录 webm → AudioContext.decodeAudioData() 解码为 PCM
  → 手动编码 WAV 文件头（44字节 RIFF/WAVE/fmt/data） → 上传 wav
```

### 7.2 WAV 编码注意事项

- WAV 文件头固定 44 字节（RIFF + fmt chunk + data chunk）
- PCM 格式：16-bit 有符号整数，小端序
- 多声道需交错合并（interleave）
- 采样率保持 AudioContext 原始值（通常 44100 或 48000）

### 7.3 麦克风权限

首次使用会弹出浏览器权限请求。如果用户拒绝，需要友好提示并引导到设置页面。

### 7.4 录音时长

建议前端加一个最大录音时长限制（如 60 秒），超时自动停止并识别。

## 八、文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `SttService.java` | 新建 | STT 服务接口 |
| `MiMoSttService.java` | 新建 | MiMo ASR 实现 |
| `SttController.java` | 新建 | 语音识别 REST 接口 |
| `JwtFilter.java` | 修改 | 白名单添加 `/api/stt` |
| `audioRecorder.js` | 新建 | 前端录音工具函数 |
| `api/stt.js` | 新建 | STT API 封装 |
| `InterviewRoom.vue` | 修改 | 输入区添加麦克风按钮 |
| `MessageCenter.vue` | 修改 | 输入行添加麦克风按钮 |

## 九、测试要点

1. 先写独立测试类验证 MiMo ASR API 能跑通（用一段 base64 wav 音频测试，确认返回文字）
2. 前端录音后 WAV 转换是否正确（文件大小、播放验证）
3. 测试识别准确率和响应速度
4. 测试 10MB 大小限制（base64 后），长录音需提示截断
