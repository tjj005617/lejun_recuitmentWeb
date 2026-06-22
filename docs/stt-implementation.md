# 语音输入（STT）实现记录

## 一、需求背景

AI 模拟面试和聊天消息中心都需要语音输入能力。用户点一下开始录音，再点一下结束，语音转文字后填入输入框供确认/修改。

## 二、整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue)                                │
│                                                                 │
│  InterviewRoom.vue / MessageCenter.vue                          │
│    │                                                            │
│    ├─► toggleVoiceInput()                                       │
│    │     ├─ 开始录音 → audioRecorder.startRecording()            │
│    │     └─ 停止录音 → audioRecorder.stop() → WAV Blob          │
│    │                                                            │
│    └─► recognizeSpeech(blob) → POST /api/stt                    │
└─────────────────────────────────────────────────────────────────┘
                              │ multipart/form-data (WAV)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        后端 (Spring Boot)                        │
│                                                                 │
│  SttController                                                  │
│    │                                                            │
│    └─► MiMoSttService.recognize(byte[])                         │
│          │                                                      │
│          ├─ Base64 编码 → Data URI                               │
│          │                                                      │
│          └─ HttpClient POST → MiMo ASR API                      │
│               │                                                 │
│               └─ SSE 流式响应 → 收集 delta.content → 返回文字    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MiMo ASR API                                 │
│  POST {apiUrl}/v1/chat/completions                              │
│  model: mimo-v2.5-asr                                           │
│  输入: audio/wav (base64 Data URI)                               │
│  输出: SSE 流，delta.content 逐字返回                            │
└─────────────────────────────────────────────────────────────────┘
```

## 三、ASR 模型选择

| 方案 | 优点 | 缺点 |
|------|------|------|
| 浏览器原生 `SpeechRecognition` | 免费、无需后端 | 中文识别质量一般，需要持续网络，依赖浏览器实现 |
| Whisper（OpenAI） | 识别质量高 | 需要部署独立服务，资源占用大 |
| MiMo-V2.5-ASR | 复用现有 MiMo API，接入成本低 | 仅支持 wav/mp3 格式 |

最终选择 MiMo ASR——项目已经接了 MiMo 的 TTS 和对话模型，ASR 只是多一个请求格式，不用引入新服务。

## 四、完整流程（详细）

### 4.1 端到端时序图

```
 用户              InterviewRoom          audioRecorder.js        SttController        MiMoSttService          MiMo ASR
  │                    │                        │                      │                     │                     │
  │  点击麦克风        │                        │                      │                     │                     │
  │───────────────────►│                        │                      │                     │                     │
  │                    │  startRecording()      │                      │                     │                     │
  │                    │───────────────────────►│                      │                     │                     │
  │                    │                        │ getUserMedia(audio)  │                     │                     │
  │                    │                        │───►│                 │                     │                     │
  │                    │                        │    │                 │                     │                     │
  │                    │                        │  MediaRecorder       │                     │                     │
  │                    │                        │  开始录制 webm/opus  │                     │                     │
  │                    │                        │◄───│                 │                     │                     │
  │                    │  recorderInstance      │                      │                     │                     │
  │                    │◄───────────────────────│                      │                     │                     │
  │                    │                        │                      │                     │                     │
  │  (录音中...)       │                        │                      │                     │                     │
  │                    │                        │                      │                     │                     │
  │  再次点击麦克风    │                        │                      │                     │                     │
  │───────────────────►│                        │                      │                     │                     │
  │                    │  recorder.stop()       │                      │                     │                     │
  │                    │───────────────────────►│                      │                     │                     │
  │                    │                        │ MediaRecorder.stop() │                     │                     │
  │                    │                        │───►│                 │                     │                     │
  │                    │                        │    │                 │                     │                     │
  │                    │                        │  webm Blob           │                     │                     │
  │                    │                        │  decodeAudioData()   │                     │                     │
  │                    │                        │  → PCM Float32       │                     │                     │
  │                    │                        │  encodeWav()         │                     │                     │
  │                    │                        │  → WAV Blob          │                     │                     │
  │                    │  WAV Blob              │                      │                     │                     │
  │                    │◄───────────────────────│                      │                     │                     │
  │                    │                        │                      │                     │                     │
  │                    │  POST /api/stt         │                      │                     │                     │
  │                    │  multipart/form-data   │                      │                     │                     │
  │                    │─────────────────────────────────────────────►│                     │                     │
  │                    │                        │                      │ file.getBytes()     │                     │
  │                    │                        │                      │────────────────────►│                     │
  │                    │                        │                      │                     │ Base64编码          │
  │                    │                        │                      │                     │ → Data URI          │
  │                    │                        │                      │                     │                     │
  │                    │                        │                      │                     │ POST /v1/chat/      │
  │                    │                        │                      │                     │ completions         │
  │                    │                        │                      │                     │────────────────────►│
  │                    │                        │                      │                     │                     │
  │                    │                        │                      │                     │ SSE: delta.content  │
  │                    │                        │                      │                     │ "你""好""，""我"   │
  │                    │                        │                      │                     │◄────────────────────│
  │                    │                        │                      │                     │                     │
  │                    │                        │                      │  "你好，我是..."    │                     │
  │                    │                        │                      │◄────────────────────│                     │
  │                    │                        │                      │                     │                     │
  │                    │  { data: "你好，我是..." }                    │                     │                     │
  │                    │◄─────────────────────────────────────────────│                     │                     │
  │                    │                        │                      │                     │                     │
  │  输入框显示文字    │                        │                      │                     │                     │
  │  用户确认后发送    │                        │                      │                     │                     │
  │◄───────────────────│                        │                      │                     │                     │
```

### 4.2 前端录音流程（audioRecorder.js）

```javascript
// 1. 开始录音
export function startRecording() {
  return new Promise(async (resolve, reject) => {
    // 请求麦克风权限
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })

    // 创建 MediaRecorder（只能录 webm/opus）
    const mediaRecorder = new MediaRecorder(stream, {
      mimeType: 'audio/webm;codecs=opus'
    })
    const chunks = []

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunks.push(e.data)
    }

    mediaRecorder.start()  // 开始采集音频数据

    // 返回控制器：调用 stop() 停止录音并获取 WAV Blob
    resolve({
      stop: () => {
        return new Promise((res) => {
          mediaRecorder.onstop = async () => {
            // 释放麦克风
            stream.getTracks().forEach(t => t.stop())

            // webm → PCM → WAV 转换
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
  })
}
```

### 4.3 格式转换流程（核心难点）

浏览器 `MediaRecorder` 只能录 `webm/opus`，但 MiMo ASR 只支持 `wav/mp3`。必须在前端做格式转换：

```
MediaRecorder 录制
    │
    ▼
webm/opus Blob
    │
    ▼
AudioContext.decodeAudioData()
    │  浏览器原生解码 API，支持 webm/opus
    │  输出：PCM AudioBuffer（Float32 采样，-1.0 ~ 1.0）
    │
    ▼
encodeWav(audioBuffer)
    │
    ├─ 1. interleave()：多声道交错合并
    │     左右声道 [L0,L1,L2...] + [R0,R1,R2...]
    │     → [L0,R0,L1,R1,L2,R2...]
    │
    ├─ 2. 写 44 字节 WAV 文件头
    │     RIFF + WAVE + fmt + data 四个 chunk
    │
    └─ 3. floatTo16BitPCM()：Float32 → Int16
          正数 × 0x7FFF (32767)
          负数 × 0x8000 (32768)
    │
    ▼
WAV Blob → 上传后端
```

#### WAV 文件结构（44 字节头）

```
偏移  大小  内容
0     4    "RIFF"（文件标识）
4     4    文件大小 - 8
8     4    "WAVE"（格式标识）
12    4    "fmt "（格式块标识）
16    4    16（格式块大小）
20    2    1（PCM 格式）
22    2    声道数
24    4    采样率
28    4    字节率（采样率 × 声道 × 位深/8）
32    2    块对齐（声道 × 位深/8）
34    2    位深（16）
36    4    "data"（数据块标识）
40    4    数据大小
44    ...  PCM 采样数据
```

#### WAV 编码关键代码

```javascript
function encodeWav(audioBuffer) {
  const numChannels = audioBuffer.numberOfChannels
  const sampleRate = audioBuffer.sampleRate
  const bitDepth = 16

  const samples = interleave(audioBuffer)  // 多声道交错合并
  const dataLength = samples.length * (bitDepth / 8)
  const buffer = new ArrayBuffer(44 + dataLength)
  const view = new DataView(buffer)

  // 文件头
  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + dataLength, true)
  writeString(view, 8, 'WAVE')
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)           // PCM
  view.setUint16(22, numChannels, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * numChannels * 2, true)
  view.setUint16(32, numChannels * 2, true)
  view.setUint16(34, bitDepth, true)
  writeString(view, 36, 'data')
  view.setUint32(40, dataLength, true)

  // PCM 数据：Float32 → Int16
  floatTo16BitPCM(view, 44, samples)
  return new Blob([buffer], { type: 'audio/wav' })
}
```

#### Float32 转 Int16

```javascript
function floatTo16BitPCM(view, offset, samples) {
  for (let i = 0; i < samples.length; i++, offset += 2) {
    const s = Math.max(-1, Math.min(1, samples[i]))
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true)
  }
}
```

注意正数用 `0x7FFF`（32767），负数用 `0x8000`（32768），这是 16-bit 有符号整数的不对称范围。

### 4.4 后端处理流程（MiMoSttService）

```java
@Override
public String recognize(byte[] audioData) throws Exception {
    // 1. WAV 字节 → Base64 → Data URI
    String base64Audio = Base64.getEncoder().encodeToString(audioData);
    String dataUri = "data:audio/wav;base64," + base64Audio;

    // 2. 构建 MiMo ASR 请求体
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

    // 3. HttpClient POST 到 MiMo API
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

    // 4. 流式读取 SSE 响应，收集 delta.content
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
                    result.append(content);  // 逐字拼接
                }
            } catch (Exception e) {
                // 忽略非文本的 delta
            }
        }
    }

    return result.toString().trim();  // 返回完整识别文字
}
```

#### MiMo ASR 请求格式

```json
{
    "model": "mimo-v2.5-asr",
    "messages": [{
        "role": "user",
        "content": [{
            "type": "input_audio",
            "input_audio": {
                "data": "data:audio/wav;base64,UklGR..."
            }
        }]
    }],
    "asr_options": { "language": "auto" },
    "stream": true
}
```

#### SSE 流式响应

```json
{"choices":[{"delta":{"content":"你"}}]}
{"choices":[{"delta":{"content":"好"}}]}
{"choices":[{"delta":{"content":"，"}}]}
{"choices":[{"delta":{"content":"我"}}]}
data: [DONE]
```

### 4.5 Controller 接口

```java
// SttController.java
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
     * Body: file (WAV 音频文件)
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

### 4.6 前端 API 封装

```javascript
// api/stt.js
import request from './request'

export function recognizeSpeech(audioBlob) {
  const formData = new FormData()
  formData.append('file', audioBlob, 'recording.wav')
  return request.post('/stt', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
```

## 五、前端交互设计

### 5.1 三态按钮

麦克风按钮有三种状态：

| 状态 | 外观 | 行为 |
|------|------|------|
| 空闲 | 麦克风图标 | 点击开始录音 |
| 录音中 | 红色方块 + 脉冲动效 | 点击停止录音 |
| 识别中 | 旋转加载图标 | 等待后端返回 |

### 5.2 使用流程

```javascript
const toggleVoiceInput = async () => {
  if (isRecording.value) {
    // 停止录音 → 格式转换 → 上传识别 → 填入输入框
    isRecording.value = false
    sttLoading.value = true

    const blob = await recorderInstance.stop()  // webm → WAV
    const res = await recognizeSpeech(blob)      // POST /api/stt
    answer.value = res.data                      // 填入输入框

    sttLoading.value = false
  } else {
    // 开始录音
    recorderInstance = await startRecording()
    isRecording.value = true
  }
}
```

关键设计：**识别结果不自动发送，而是填入输入框让用户确认**。避免语音识别错误导致发出不想要的消息。

### 5.3 使用场景

| 场景 | 页面 | 输入框 |
|------|------|--------|
| AI 面试回答 | InterviewRoom.vue | 面试回答输入框 |
| 聊天消息 | MessageCenter.vue | 聊天消息输入框 |

## 六、测试验证

按照项目规范，先写独立测试类验证 API 能跑通：

```java
@SpringBootTest
public class AsrTest {
    @Test
    void testAsr() throws Exception {
        byte[] audio = Files.readAllBytes(Path.of("tts-test-output.mp3"));
        String base64 = Base64.getEncoder().encodeToString(audio);
        // 调用 MiMo ASR
        // 输出识别结果
    }
}
```

测试结果：识别出 "Yeah."，耗时 1081ms，API 通路正常。

## 七、踩坑总结

| 问题 | 原因 | 解决 |
|------|------|------|
| ASR 返回 400 | 上传了 webm 格式 | 前端先转 WAV |
| WAV 文件播放有杂音 | Float32→Int16 时正负范围不对称 | 正数 `0x7FFF`，负数 `0x8000` |
| 多声道 WAV 播放异常 | 左右声道数据未交错合并 | 加 interleave 处理 |
| API import 路径错误 | `@/utils/request` 解析失败 | 改为相对路径 `./request` |

## 八、文件清单

| 文件 | 说明 |
|------|------|
| `MiMoSttService.java` | ASR 核心实现，HttpClient 调用 MiMo API |
| `SttService.java` | 服务接口 |
| `SttController.java` | REST 接口，接收 multipart 音频 |
| `JwtFilter.java` | 白名单添加 `/api/stt` |
| `audioRecorder.js` | 录音 + WAV 转换工具 |
| `api/stt.js` | 前端 API 封装 |
| `InterviewRoom.vue` | 面试页面添加语音输入 |
| `MessageCenter.vue` | 聊天页面添加语音输入 |
| `AsrTest.java` | 独立测试类，验证 API |
