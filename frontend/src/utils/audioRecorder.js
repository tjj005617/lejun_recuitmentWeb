/**
 * 录音工具
 * MediaRecorder 录 webm → AudioContext 解码 → 编码为 WAV
 * MiMo ASR 只支持 wav/mp3，浏览器 MediaRecorder 只能录 webm，需要格式转换
 */

/**
 * 开始录音
 * @param {MediaStream} [externalStream] - 可选：外部传入的音频流（iOS 兼容，在用户手势中提前获取）
 * @returns {Promise<{stop: () => Promise<Blob>}>} 录音控制器，stop() 返回 WAV Blob
 */
export async function startRecording(externalStream) {
  const stream = externalStream || await navigator.mediaDevices.getUserMedia({ audio: true })
  const mediaRecorder = new MediaRecorder(stream, {
    mimeType: 'audio/webm;codecs=opus'
  })
  const chunks = []

  mediaRecorder.ondataavailable = (e) => {
    if (e.data.size > 0) chunks.push(e.data)
  }

  mediaRecorder.start()

  return {
    stop: () => {
      return new Promise((resolve, reject) => {
        mediaRecorder.onstop = async () => {
          stream.getTracks().forEach(t => t.stop())
          try {
            const webmBlob = new Blob(chunks, { type: 'audio/webm' })
            const arrayBuffer = await webmBlob.arrayBuffer()
            const audioCtx = new (window.AudioContext || window.webkitAudioContext)()
            const audioBuffer = await audioCtx.decodeAudioData(arrayBuffer)
            const wavBlob = encodeWav(audioBuffer)
            audioCtx.close()
            resolve(wavBlob)
          } catch (err) {
            reject(err)
          }
        }
        mediaRecorder.stop()
      })
    }
  }
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

/**
 * 从已有 MediaStream 创建录音器（沉浸式面试专用）
 * 不会在 stop() 时关闭 stream，允许下一轮复用同一麦克风流
 * @param {MediaStream} stream - 已有的音频流
 * @returns {{stop: () => Promise<Blob>}} 录音控制器，stop() 返回 WAV Blob
 */
export function createRecorderFromStream(stream) {
  const mediaRecorder = new MediaRecorder(stream, {
    mimeType: 'audio/webm;codecs=opus'
  })
  const chunks = []

  mediaRecorder.ondataavailable = (e) => {
    if (e.data.size > 0) chunks.push(e.data)
  }

  mediaRecorder.start()

  return {
    stop: () => {
      return new Promise((resolve, reject) => {
        mediaRecorder.onstop = async () => {
          try {
            const webmBlob = new Blob(chunks, { type: 'audio/webm' })
            const arrayBuffer = await webmBlob.arrayBuffer()
            const audioCtx = new (window.AudioContext || window.webkitAudioContext)()
            const audioBuffer = await audioCtx.decodeAudioData(arrayBuffer)
            const wavBlob = encodeWav(audioBuffer)
            audioCtx.close()
            resolve(wavBlob)
          } catch (err) {
            reject(err)
          }
        }
        mediaRecorder.stop()
      })
    }
  }
}
