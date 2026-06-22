/**
 * 音频分片编码工具
 * 将浏览器录制的 webm 音频分片转为 base64 WAV 格式，供 MiMo ASR 识别
 */

/**
 * 将多个 webm 分片合并后转为 WAV base64
 * MediaRecorder timeslice 产生的分片不是独立文件，必须合并后再解码
 * @param {Blob[]} chunks - webm 分片数组
 * @returns {Promise<string>} WAV 格式的 base64 字符串
 */
export async function mergeChunksToWavBase64(chunks) {
  const mergedBlob = new Blob(chunks, { type: 'audio/webm' })
  return await webmBlobToWavBase64(mergedBlob)
}

/**
 * 将 Blob 转为 base64 字符串（去掉 Data URI 前缀）
 * @param {Blob} blob
 * @returns {Promise<string>} 纯 base64 字符串
 */
export function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => {
      const base64 = reader.result.split(',')[1]
      resolve(base64)
    }
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

/**
 * 将 webm Blob 解码并编码为 WAV base64
 * 使用 AudioContext.decodeAudioData 解码 webm，再手动编码为 WAV
 * @param {Blob} webmBlob - webm 格式的音频 Blob
 * @returns {Promise<string>} WAV 格式的 base64 字符串
 */
export async function webmBlobToWavBase64(webmBlob) {
  const arrayBuffer = await webmBlob.arrayBuffer()
  const audioCtx = new (window.AudioContext || window.webkitAudioContext)()
  try {
    const audioBuffer = await audioCtx.decodeAudioData(arrayBuffer)
    const wavBlob = encodeWav(audioBuffer)
    return await blobToBase64(wavBlob)
  } finally {
    audioCtx.close()
  }
}

/**
 * 将 AudioBuffer 编码为 WAV Blob（16-bit PCM）
 * @param {AudioBuffer} audioBuffer
 * @returns {Blob} WAV 格式的 Blob
 */
function encodeWav(audioBuffer) {
  const numChannels = audioBuffer.numberOfChannels
  const sampleRate = audioBuffer.sampleRate
  const bitDepth = 16
  const samples = numChannels === 1
    ? audioBuffer.getChannelData(0)
    : interleave(audioBuffer)
  const dataLength = samples.length * (bitDepth / 8)
  const buffer = new ArrayBuffer(44 + dataLength)
  const view = new DataView(buffer)

  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + dataLength, true)
  writeString(view, 8, 'WAVE')
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, numChannels, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * numChannels * (bitDepth / 8), true)
  view.setUint16(32, numChannels * (bitDepth / 8), true)
  view.setUint16(34, bitDepth, true)
  writeString(view, 36, 'data')
  view.setUint32(40, dataLength, true)

  let offset = 44
  for (let i = 0; i < samples.length; i++, offset += 2) {
    const s = Math.max(-1, Math.min(1, samples[i]))
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true)
  }

  return new Blob([buffer], { type: 'audio/wav' })
}

function interleave(audioBuffer) {
  const left = audioBuffer.getChannelData(0)
  const right = audioBuffer.getChannelData(1)
  const interleaved = new Float32Array(left.length + right.length)
  for (let i = 0, j = 0; i < left.length; i++) {
    interleaved[j++] = left[i]
    interleaved[j++] = right[i]
  }
  return interleaved
}

function writeString(view, offset, str) {
  for (let i = 0; i < str.length; i++) {
    view.setUint8(offset + i, str.charCodeAt(i))
  }
}
